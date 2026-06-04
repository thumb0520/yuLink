package com.yulink.nas.ui.preview;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
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
    private MaterialToolbar toolbar;
    private ImageButton fullscreenButton;
    private ProtocolManager protocolManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.player_view);
        toolbar = findViewById(R.id.toolbar);
        fullscreenButton = findViewById(R.id.fullscreen_button);

        String filePath = getIntent().getStringExtra("filePath");
        String fileName = getIntent().getStringExtra("fileName");
        long connectionId = getIntent().getLongExtra("connectionId", -1);

        toolbar.setTitle(fileName);
        toolbar.setNavigationOnClickListener(v -> {
            if (isFullscreen) {
                toggleFullscreen();
            } else {
                finish();
            }
        });

        fullscreenButton.setOnClickListener(v -> toggleFullscreen());

        initializePlayer(connectionId, filePath);
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;

        if (isFullscreen) {
            // Enter fullscreen: hide toolbar, expand player to full screen
            toolbar.setVisibility(View.GONE);
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit);

            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) playerView.getLayoutParams();
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            playerView.setLayoutParams(params);

            hideSystemBars();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            // Exit fullscreen: show toolbar, restore player below toolbar
            toolbar.setVisibility(View.VISIBLE);
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen);

            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) playerView.getLayoutParams();
            params.topToTop = ConstraintLayout.LayoutParams.UNSET;
            params.topToBottom = R.id.toolbar;
            playerView.setLayoutParams(params);

            showSystemBars();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle orientation changes from system (e.g. auto-rotate)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && !isFullscreen) {
            toggleFullscreen();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && isFullscreen) {
            toggleFullscreen();
        }
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen();
        } else {
            super.onBackPressed();
        }
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
                    DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                            .setBufferDurationsMs(
                                    20000,   // minBufferMs: 20s
                                    60000,   // maxBufferMs: 60s
                                    2500,    // bufferForPlaybackMs: 2.5s
                                    5000     // bufferForPlaybackAfterRebufferMs: 5s
                            )
                            .build();
                    player = new ExoPlayer.Builder(this)
                            .setLoadControl(loadControl)
                            .build();
                    playerView.setPlayer(player);

                    NasDataSource.Factory nasDataSourceFactory = new NasDataSource.Factory(protocolManager, filePath);
                    ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(nasDataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(filePath));

                    player.setMediaSource(mediaSource);
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
        executor.shutdownNow();
        if (protocolManager != null) {
            protocolManager.disconnect();
        }
    }
}
