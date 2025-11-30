package net.newlydev.fileshare_android.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.newlydev.fileshare_android.PermissionRequestManager;
import net.newlydev.fileshare_android.R;

/**
 * Transparent activity that surfaces ASKME permission requests using system dialogs instead of
 * relying on overlay windows.
 */
public class PermissionPromptActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "extra_request_id";
    public static final String EXTRA_CLIENT_ADDRESS = "extra_client_address";

    private long requestId = -1L;
    private boolean decisionMade;

    public static Intent createIntent(Context context, long requestId, String clientAddress) {
        Intent intent = new Intent(context, PermissionPromptActivity.class);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        intent.putExtra(EXTRA_CLIENT_ADDRESS, clientAddress);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestId = getIntent().getLongExtra(EXTRA_REQUEST_ID, -1L);
        String clientAddress = getIntent().getStringExtra(EXTRA_CLIENT_ADDRESS);
        if (requestId == -1L || clientAddress == null || !PermissionRequestManager.isPending(requestId)) {
            finish();
            return;
        }
        showPrompt(clientAddress);
    }

    private void showPrompt(String clientAddress) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.permission_prompt_title)
            .setMessage(getString(R.string.permission_prompt_message, clientAddress))
            .setCancelable(false)
            .setPositiveButton(R.string.permission_prompt_allow, (dialog, which) -> {
                resolveAndFinish(PermissionRequestManager.Decision.granted());
            })
            .setNegativeButton(R.string.permission_prompt_deny, (dialog, which) -> {
                resolveAndFinish(PermissionRequestManager.Decision.denied(
                    getString(R.string.permission_prompt_denied_message), false));
            })
            .setNeutralButton(R.string.permission_prompt_shutdown, (dialog, which) -> {
                resolveAndFinish(PermissionRequestManager.Decision.denied(
                    getString(R.string.permission_prompt_shutdown_message), true));
            })
            .show();
    }

    private void resolveAndFinish(PermissionRequestManager.Decision decision) {
        decisionMade = true;
        PermissionRequestManager.resolve(requestId, decision);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!decisionMade && requestId != -1L && PermissionRequestManager.isPending(requestId)) {
            PermissionRequestManager.resolve(requestId, PermissionRequestManager.Decision.denied(
                getString(R.string.permission_prompt_timeout_message), false));
        }
    }
}
