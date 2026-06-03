package com.easyconnect.nas.ui.preview;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.db.AppDatabase;
import com.easyconnect.nas.data.db.entity.ConnectionEntity;
import com.easyconnect.nas.data.model.ConnectionInfo;
import com.easyconnect.nas.protocol.ProtocolException;
import com.easyconnect.nas.protocol.ProtocolFactory;
import com.easyconnect.nas.protocol.ProtocolManager;
import com.easyconnect.nas.transfer.NasDataSource;
import com.easyconnect.nas.util.CryptoUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private ProtocolManager protocolManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        playerView = findViewById(R.id.player_view);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView tvTrackName = findViewById(R.id.tv_track_name);

        String filePath = getIntent().getStringExtra("filePath");
        String fileName = getIntent().getStringExtra("fileName");
        long connectionId = getIntent().getLongExtra("connectionId", -1);

        toolbar.setTitle("音乐播放");
        toolbar.setNavigationOnClickListener(v -> finish());
        tvTrackName.setText(fileName);

        initializePlayer(connectionId, filePath);
    }

    private void initializePlayer(long connectionId, String filePath) {
        executor.execute(() -> {
            try {
                ConnectionEntity entity = AppDatabase.getInstance(this)
                        .connectionDao().getConnectionById(connectionId);

                if (entity == null) {
                    runOnUiThread(this::finish);
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

                runOnUiThread(() -> {
                    player = new ExoPlayer.Builder(this).build();
                    playerView.setPlayer(player);

                    NasDataSource.Factory dataSourceFactory = new NasDataSource.Factory(protocolManager, filePath);
                    MediaItem mediaItem = MediaItem.fromUri(filePath);

                    player.setMediaItem(mediaItem);
                    player.prepare();
                    player.play();
                });

            } catch (ProtocolException e) {
                runOnUiThread(this::finish);
            } catch (Exception e) {
                runOnUiThread(this::finish);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        if (protocolManager != null) {
            protocolManager.disconnect();
        }
    }
}
