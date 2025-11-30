package net.newlydev.fileshare_android;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Central place to publish and observe the lifecycle of {@link MainService} without relying on
 * deprecated process-wide service queries.
 */
public final class ServiceStatusTracker {

    public enum Status {
        STOPPED,
        STARTING,
        RUNNING
    }

    private static final MutableLiveData<Status> statusLiveData = new MutableLiveData<>(Status.STOPPED);
    private static final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private ServiceStatusTracker() {
    }

    public static LiveData<Status> getStatus() {
        return statusLiveData;
    }

    public static LiveData<String> getErrors() {
        return errorLiveData;
    }

    public static Status getCurrentStatus() {
        Status current = statusLiveData.getValue();
        return current == null ? Status.STOPPED : current;
    }

    public static boolean isRunning() {
        return getCurrentStatus() == Status.RUNNING;
    }

    public static void updateStatus(@NonNull Status status) {
        statusLiveData.postValue(status);
        if (status == Status.RUNNING) {
            // Clear error state once service reaches a healthy state again.
            errorLiveData.postValue(null);
        }
    }

    public static void reportError(@Nullable String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        errorLiveData.postValue(message);
    }

    @MainThread
    public static void clearError() {
        errorLiveData.setValue(null);
    }
}
