package net.newlydev.fileshare_android;

import android.content.Context;
import android.content.Intent;

import net.newlydev.fileshare_android.activities.PermissionPromptActivity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates ASKME authentication prompts by handing off incoming requests to a UI surface
 * and blocking the originating HTTP thread until the user responds or a timeout elapses.
 */
public final class PermissionRequestManager {

    private static final long DEFAULT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(1);
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    private static final ConcurrentHashMap<Long, PendingRequest> PENDING = new ConcurrentHashMap<>();
    private static volatile Context appContext;

    private PermissionRequestManager() {
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static long getDefaultTimeoutMs() {
        return DEFAULT_TIMEOUT_MS;
    }

    public static PendingRequest enqueue(String clientAddress) {
        PendingRequest request = new PendingRequest(ID_GENERATOR.incrementAndGet(), clientAddress);
        PENDING.put(request.getId(), request);
        launchPrompt(request);
        return request;
    }

    public static boolean resolve(long requestId, Decision decision) {
        PendingRequest request = PENDING.remove(requestId);
        if (request == null) {
            return false;
        }
        request.complete(decision);
        return true;
    }

    public static boolean isPending(long requestId) {
        return PENDING.containsKey(requestId);
    }

    public static void cancel(long requestId) {
        PendingRequest request = PENDING.remove(requestId);
        if (request != null) {
            request.complete(Decision.denied(null, false));
        }
    }

    private static void launchPrompt(PendingRequest request) {
        Context context = appContext;
        if (context == null) {
            return;
        }
        Intent intent = PermissionPromptActivity.createIntent(context, request.getId(), request.getClientAddress());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static final class PendingRequest {
        private final long id;
        private final String clientAddress;
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile Decision decision;

        private PendingRequest(long id, String clientAddress) {
            this.id = id;
            this.clientAddress = clientAddress;
        }

        public long getId() {
            return id;
        }

        public String getClientAddress() {
            return clientAddress;
        }

        public Decision awaitDecision(long timeoutMs) {
            try {
                if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                    return null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            return decision;
        }

        private void complete(Decision decision) {
            this.decision = decision;
            latch.countDown();
        }
    }

    public static final class Decision {
        private final boolean granted;
        private final boolean stopService;
        private final String message;

        private Decision(boolean granted, boolean stopService, String message) {
            this.granted = granted;
            this.stopService = stopService;
            this.message = message;
        }

        public static Decision granted() {
            return new Decision(true, false, null);
        }

        public static Decision denied(String message, boolean stopService) {
            return new Decision(false, stopService, message);
        }

        public boolean isGranted() {
            return granted;
        }

        public boolean shouldStopService() {
            return stopService;
        }

        public String getMessage() {
            return message;
        }
    }
}
