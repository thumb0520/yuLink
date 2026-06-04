package com.yulink.nas.ui.preview;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.yulink.nas.R;
import com.yulink.nas.data.db.AppDatabase;
import com.yulink.nas.data.db.entity.ConnectionEntity;
import com.yulink.nas.data.model.ConnectionInfo;
import com.yulink.nas.protocol.ProtocolException;
import com.yulink.nas.protocol.ProtocolFactory;
import com.yulink.nas.protocol.ProtocolManager;
import com.yulink.nas.util.CryptoUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagePreviewActivity extends AppCompatActivity {
    private static final String TAG = "ImagePreviewActivity";
    private static final int BUFFER_SIZE = 64 * 1024; // 64KB

    private ImageView imageView;
    private ProgressBar progressBar;
    private ProtocolManager protocolManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        imageView = findViewById(R.id.iv_preview);
        progressBar = findViewById(R.id.progress_bar);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        String filePath = getIntent().getStringExtra("filePath");
        String fileName = getIntent().getStringExtra("fileName");
        long connectionId = getIntent().getLongExtra("connectionId", -1);

        toolbar.setTitle(fileName);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (filePath != null && connectionId > 0) {
            loadImage(connectionId, filePath);
        }
    }

    private void loadImage(long connectionId, String filePath) {
        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                ConnectionEntity entity = AppDatabase.getInstance(this)
                        .connectionDao().getConnectionById(connectionId);

                if (entity == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        finish();
                    });
                    return;
                }

                ConnectionInfo info = new ConnectionInfo();
                info.setProtocol(entity.protocol);
                info.setHost(entity.host);
                info.setPort(entity.port);
                info.setUsername(entity.username);
                info.setPassword(CryptoUtils.decrypt(entity.encryptedPassword));
                info.setShareName(entity.shareName);

                protocolManager = ProtocolFactory.create(entity.protocol);
                protocolManager.connect(info);

                // Download to temp file first — Glide needs mark/reset support
                // and efficient seeking, which SMB streams don't provide
                tempFile = new File(getCacheDir(), "preview_" + System.currentTimeMillis());
                try (InputStream in = protocolManager.openFileStream(filePath);
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    out.flush();
                }

                Log.d(TAG, "Image downloaded to temp file: " + tempFile.length() + " bytes");

                // Disconnect SMB — we no longer need it
                protocolManager.disconnect();
                protocolManager = null;

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Glide.with(ImagePreviewActivity.this)
                            .load(tempFile)
                            .into(imageView);
                });

            } catch (ProtocolException e) {
                Log.e(TAG, "Failed to load image: " + e.getMessage(), e);
                cleanup();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load image: " + e.getMessage(), e);
                cleanup();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    finish();
                });
            }
        });
    }

    private void cleanup() {
        if (protocolManager != null) {
            try { protocolManager.disconnect(); } catch (Exception ignored) {}
            protocolManager = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
        // Delete temp file
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
}
