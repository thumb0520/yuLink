package com.easyconnect.nas.ui.preview;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.easyconnect.nas.R;
import com.easyconnect.nas.data.db.AppDatabase;
import com.easyconnect.nas.data.db.entity.ConnectionEntity;
import com.easyconnect.nas.data.model.ConnectionInfo;
import com.easyconnect.nas.data.model.ProtocolType;
import com.easyconnect.nas.protocol.ProtocolException;
import com.easyconnect.nas.protocol.ProtocolFactory;
import com.easyconnect.nas.protocol.ProtocolManager;
import com.easyconnect.nas.util.CryptoUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagePreviewActivity extends AppCompatActivity {
    private ImageView imageView;
    private ProgressBar progressBar;
    private ProtocolManager protocolManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

                InputStream stream = protocolManager.openFileStream(filePath);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Glide.with(this)
                            .load(stream)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(imageView);
                });

            } catch (ProtocolException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    finish();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (protocolManager != null) {
            protocolManager.disconnect();
        }
    }
}
