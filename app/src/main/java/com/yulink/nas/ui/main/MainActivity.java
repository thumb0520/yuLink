package com.yulink.nas.ui.main;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.yulink.nas.R;
import com.yulink.nas.ui.browser.FileBrowserFragment;
import com.yulink.nas.ui.connection.ConnectionListFragment;
import com.yulink.nas.ui.transfer.TransferQueueFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements ConnectionListFragment.Callbacks {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private ConnectionListFragment connectionListFragment;
    private FileBrowserFragment fileBrowserFragment;
    private TransferQueueFragment transferQueueFragment;
    private Fragment currentTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermission();
        setupFragments();
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

    private void setupFragments() {
        connectionListFragment = new ConnectionListFragment();
        fileBrowserFragment = new FileBrowserFragment();
        transferQueueFragment = new TransferQueueFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.tab_container, connectionListFragment, "connection");
        ft.add(R.id.tab_container, fileBrowserFragment, "file_browser");
        ft.add(R.id.tab_container, transferQueueFragment, "transfer");
        ft.hide(fileBrowserFragment);
        ft.hide(transferQueueFragment);
        ft.show(connectionListFragment);
        ft.commit();

        currentTab = connectionListFragment;
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.connectionListFragment) {
                switchTab(connectionListFragment);
            } else if (itemId == R.id.fileBrowserFragment) {
                switchTab(fileBrowserFragment);
            } else if (itemId == R.id.transferQueueFragment) {
                switchTab(transferQueueFragment);
            }
            return true;
        });
    }

    private void switchTab(Fragment target) {
        if (target == currentTab) return;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.hide(currentTab);
        ft.show(target);
        ft.commit();
        currentTab = target;
    }

    // ConnectionListFragment.Callbacks — called when user selects a connection
    @Override
    public void onConnectionSelected(long connectionId) {
        Bundle args = new Bundle();
        args.putLong("connectionId", connectionId);
        // Remove old file browser and add new one in a single transaction
        getSupportFragmentManager().beginTransaction()
                .remove(fileBrowserFragment)
                .commit();
        fileBrowserFragment = new FileBrowserFragment();
        fileBrowserFragment.setArguments(args);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.tab_container, fileBrowserFragment, "file_browser");
        if (currentTab != fileBrowserFragment) {
            ft.hide(currentTab);
        }
        ft.show(fileBrowserFragment);
        ft.commit();
        currentTab = fileBrowserFragment;
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.fileBrowserFragment);
    }

    // ConnectionListFragment.Callbacks — called when user taps FAB to add connection
    @Override
    public void onAddConnectionRequested() {
        com.yulink.nas.ui.connection.AddConnectionFragment fragment =
                new com.yulink.nas.ui.connection.AddConnectionFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.tab_container, fragment, "add_connection")
                .addToBackStack("add_connection")
                .commit();
        // Hide bottom nav for overlay
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
    }

    // ConnectionListFragment.Callbacks — called when user taps edit on a connection
    @Override
    public void onEditConnectionRequested(long connectionId) {
        com.yulink.nas.ui.connection.AddConnectionFragment fragment =
                new com.yulink.nas.ui.connection.AddConnectionFragment();
        Bundle args = new Bundle();
        args.putLong("connectionId", connectionId);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.tab_container, fragment, "add_connection")
                .addToBackStack("add_connection")
                .commit();
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        // If overlay fragments are in back stack, pop them
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
            return;
        }
        // On connection tab, exit app
        if (currentTab == connectionListFragment) {
            super.onBackPressed();
            return;
        }
        // On other tabs, go back to connection tab
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.connectionListFragment);
    }
}
