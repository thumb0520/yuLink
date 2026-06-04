package com.yulink.nas.ui.main;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.yulink.nas.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermission();
        setupNavigation();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

            // Handle bottom nav tab selection manually for proper back stack management
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                int currentId = navController.getCurrentDestination().getId();

                if (itemId == R.id.connectionListFragment) {
                    // "连接" tab: pop back to connection list
                    if (currentId != R.id.connectionListFragment) {
                        navController.popBackStack(R.id.connectionListFragment, false);
                    }
                    return true;
                } else if (itemId == R.id.fileBrowserFragment) {
                    // "文件" tab: do nothing (file browser is opened from connection list)
                    return true;
                } else if (itemId == R.id.transferQueueFragment) {
                    // "传输" tab: navigate to transfer queue
                    if (currentId != R.id.transferQueueFragment) {
                        navController.navigate(R.id.transferQueueFragment);
                    }
                    return true;
                }
                return false;
            });

            // Hide bottom nav on certain destinations
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.addConnectionFragment || id == R.id.settingsFragment) {
                    bottomNav.setVisibility(android.view.View.GONE);
                } else {
                    bottomNav.setVisibility(android.view.View.VISIBLE);
                    // Sync bottom nav selection with current destination
                    if (id == R.id.connectionListFragment) {
                        bottomNav.setSelectedItemId(R.id.connectionListFragment);
                    } else if (id == R.id.fileBrowserFragment) {
                        bottomNav.setSelectedItemId(R.id.fileBrowserFragment);
                    } else if (id == R.id.transferQueueFragment) {
                        bottomNav.setSelectedItemId(R.id.transferQueueFragment);
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentId = navController.getCurrentDestination().getId();
            // On file browser, pop back to connection list
            if (currentId == R.id.fileBrowserFragment) {
                navController.popBackStack(R.id.connectionListFragment, false);
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }
}
