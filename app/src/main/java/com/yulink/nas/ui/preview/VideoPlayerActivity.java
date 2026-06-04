package com.yulink.nas.ui.preview;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.yulink.nas.R;
import com.yulink.nas.data.db.AppDatabase;
import com.yulink.nas.data.db.entity.ConnectionEntity;
import com.yulink.nas.data.model.ConnectionInfo;
import com.yulink.nas.protocol.ProtocolException;
import com.yulink.nas.protocol.ProtocolFactory;
import com.yulink.nas.protocol.ProtocolManager;
import com.yulink.nas.transfer.NasDataSource;
import com.yulink.nas.util.CryptoUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private ProtocolManager protocolManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.player_view);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        String filePath = getIntent().getStringExtra("filePath");
        String fileName = getIntent().getStringExtra("fileName");
        long connectionId = getIntent().getLongExtra("connectionId", -1);

        toolbar.setTitle(fileName);
        toolbar.setNavigationOnClickListener(v -> finish());

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

                    player.addListener(new Player.Listener() {
                        @Override
                        public void onPlayerError(PlaybackException error) {
                            // Handle error
                        }
                    });
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
