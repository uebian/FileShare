package net.newlydev.fileshare_android.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import net.newlydev.fileshare_android.MainService;
import net.newlydev.fileshare_android.R;
import net.newlydev.fileshare_android.ServiceStatusTracker;
import net.newlydev.fileshare_android.Utils;

import java.util.Hashtable;

public class MainFragment extends Fragment {

    private SwitchCompat switchToggle;
    private TextView ipTextView;
    private ImageView qrCodeView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ipTextView = rootView.findViewById(R.id.fragment_main_ip_textview);
        qrCodeView = rootView.findViewById(R.id.fragment_main_qrcode_imageview);
        switchToggle = rootView.findViewById(R.id.fragment_status_switch);

        ipTextView.setOnClickListener(v -> refreshQrForStatus(ServiceStatusTracker.getCurrentStatus()));
        switchToggle.setOnCheckedChangeListener(this::onSwitchToggled);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ServiceStatusTracker.getStatus().observe(getViewLifecycleOwner(), status -> {
            renderSwitchForStatus(status);
            refreshQrForStatus(status);
        });
        ServiceStatusTracker.getErrors().observe(getViewLifecycleOwner(), error -> {
            if (!TextUtils.isEmpty(error) && isAdded()) {
                new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.server_start_failed)
                    .setMessage(error)
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false)
                    .show();
                ServiceStatusTracker.clearError();
            }
        });
    }

    private void onSwitchToggled(CompoundButton button, boolean shouldEnable) {
        if (!button.isPressed()) {
            return;
        }
        Context context = requireContext();
        Intent intent = new Intent(context, MainService.class);
        if (shouldEnable) {
            ContextCompat.startForegroundService(context, intent);
        } else {
            context.stopService(intent);
        }
    }

    private void renderSwitchForStatus(ServiceStatusTracker.Status status) {
        boolean serviceActive = status != ServiceStatusTracker.Status.STOPPED;
        if (switchToggle.isChecked() != serviceActive) {
            switchToggle.setChecked(serviceActive);
        }
        switchToggle.setEnabled(status != ServiceStatusTracker.Status.STARTING);
    }

    private void refreshQrForStatus(ServiceStatusTracker.Status status) {
        if (status != ServiceStatusTracker.Status.RUNNING) {
            int message = status == ServiceStatusTracker.Status.STARTING ? R.string.server_starting : R.string.server_stopped;
            ipTextView.setText(message);
            qrCodeView.setVisibility(View.GONE);
            return;
        }
        mainHandler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            String ip = Utils.getLocalIpAddress();
            if ("0".equals(ip)) {
                ipTextView.setText(getString(R.string.no_internet));
                qrCodeView.setVisibility(View.GONE);
                return;
            }
            String port = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext()).getString("serverPort", "-1");
            String url = "http://" + ip + ":" + port;
            ipTextView.setText(getString(R.string.server_started, url));
            showQrOnceLaidOut(url);
        }, 100);
    }

    private void showQrOnceLaidOut(String url) {
        qrCodeView.setVisibility(View.VISIBLE);
        if (qrCodeView.getWidth() > 0 && qrCodeView.getHeight() > 0) {
            renderQr(url);
            return;
        }
        qrCodeView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (qrCodeView.getWidth() > 0 && qrCodeView.getHeight() > 0) {
                    qrCodeView.removeOnLayoutChangeListener(this);
                    renderQr(url);
                }
            }
        });
    }

    private void renderQr(String url) {
        try {
            qrCodeView.setImageBitmap(createQrBitmap(url));
        } catch (Exception e) {
            ipTextView.setText(getString(R.string.genqr_fail, url));
            qrCodeView.setVisibility(View.GONE);
        }
    }

    private Bitmap createQrBitmap(String payload) throws Exception {
        Hashtable<EncodeHintType, String> hints = new Hashtable<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        int width = qrCodeView.getWidth();
        int height = qrCodeView.getHeight();
        BitMatrix bitMatrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, width, height, hints);
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = bitMatrix.get(x, y) ? 0xff000000 : 0xffffffff;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
