package com.yulink.nas.ui.browser;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yulink.nas.data.db.entity.ConnectionEntity;
import com.yulink.nas.data.model.ConnectionInfo;
import com.yulink.nas.data.model.NasFile;
import com.yulink.nas.data.repository.ConnectionRepository;
import com.yulink.nas.protocol.ProtocolException;
import com.yulink.nas.protocol.ProtocolFactory;
import com.yulink.nas.protocol.ProtocolManager;
import com.yulink.nas.util.CryptoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileBrowserViewModel extends AndroidViewModel {
    private final ConnectionRepository connectionRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ProtocolManager protocolManager;
    private ConnectionInfo connectionInfo;

    private final MutableLiveData<List<NasFile>> filesLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> currentPathLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<NasFile>> selectedFilesLiveData = new MutableLiveData<>();

    private List<NasFile> allFiles = new ArrayList<>();
    private List<NasFile> selectedFiles = new ArrayList<>();
    private SortMode sortMode = SortMode.NAME_ASC;
    private boolean isGridView = false;

    public enum SortMode {
        NAME_ASC, NAME_DESC, SIZE_ASC, SIZE_DESC, DATE_ASC, DATE_DESC
    }

    public FileBrowserViewModel(@NonNull Application application) {
        super(application);
        connectionRepository = new ConnectionRepository(application);
        currentPathLiveData.setValue("/");
    }

    public LiveData<List<NasFile>> getFiles() {
        return filesLiveData;
    }

    public LiveData<String> getCurrentPath() {
        return currentPathLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<List<NasFile>> getSelectedFiles() {
        return selectedFilesLiveData;
    }

    public boolean isGridView() {
        return isGridView;
    }

    public void setGridView(boolean gridView) {
        isGridView = gridView;
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
        sortAndPostFiles();
    }

    public void connectToServer(long connectionId) {
        loadingLiveData.setValue(true);
        connectionRepository.getConnectionById(connectionId, entity -> {
            if (entity == null) {
                errorLiveData.postValue("连接不存在");
                loadingLiveData.postValue(false);
                return;
            }

            try {
                connectionInfo = new ConnectionInfo();
                connectionInfo.setId(entity.id);
                connectionInfo.setName(entity.name);
                connectionInfo.setProtocol(entity.protocol);
                connectionInfo.setHost(entity.host);
                connectionInfo.setPort(entity.port);
                connectionInfo.setUsername(entity.username);
                connectionInfo.setPassword(CryptoUtils.decrypt(entity.encryptedPassword));
                connectionInfo.setShareName(entity.shareName);
                connectionInfo.setDefaultPath(entity.defaultPath);
                connectionInfo.setPassiveMode(entity.passiveMode);
                connectionInfo.setUseFtps(entity.useFtps);
                connectionInfo.setUseSmbEncryption(entity.useSmbEncryption);

                protocolManager = ProtocolFactory.create(entity.protocol);
                protocolManager.connect(connectionInfo);

                String initialPath = entity.defaultPath != null ? entity.defaultPath : "/";
                navigateTo(initialPath);
            } catch (ProtocolException e) {
                errorLiveData.postValue(e.getMessage());
                loadingLiveData.postValue(false);
            } catch (Exception e) {
                errorLiveData.postValue("连接失败: " + e.getMessage());
                loadingLiveData.postValue(false);
            }
        });
    }

    public void navigateTo(String path) {
        if (protocolManager == null) return;

        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                List<NasFile> files = protocolManager.listFiles(path);
                allFiles = files;
                currentPathLiveData.postValue(path);
                sortAndPostFiles();
            } catch (ProtocolException e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    public void navigateUp() {
        String currentPath = currentPathLiveData.getValue();
        if (currentPath == null || currentPath.equals("/")) return;

        int lastSlash = currentPath.lastIndexOf('/');
        String parentPath = lastSlash > 0 ? currentPath.substring(0, lastSlash) : "/";
        navigateTo(parentPath);
    }

    public void refresh() {
        String currentPath = currentPathLiveData.getValue();
        if (currentPath != null) {
            navigateTo(currentPath);
        }
    }

    public void toggleFileSelection(NasFile file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }
        selectedFilesLiveData.setValue(new ArrayList<>(selectedFiles));
    }

    public void clearSelection() {
        selectedFiles.clear();
        selectedFilesLiveData.setValue(new ArrayList<>());
    }

    public void selectAll() {
        selectedFiles = new ArrayList<>(allFiles);
        selectedFilesLiveData.setValue(new ArrayList<>(selectedFiles));
    }

    public void createDirectory(String name) {
        if (protocolManager == null) return;

        executor.execute(() -> {
            try {
                String currentPath = currentPathLiveData.getValue();
                String newPath = currentPath + "/" + name;
                protocolManager.createDirectory(newPath);
                refresh();
            } catch (ProtocolException e) {
                errorLiveData.postValue("创建文件夹失败: " + e.getMessage());
            }
        });
    }

    public void deleteSelected() {
        if (protocolManager == null || selectedFiles.isEmpty()) return;

        executor.execute(() -> {
            try {
                for (NasFile file : selectedFiles) {
                    if (file.isDirectory()) {
                        protocolManager.deleteDirectory(file.getFullPath());
                    } else {
                        protocolManager.deleteFile(file.getFullPath());
                    }
                }
                selectedFiles.clear();
                selectedFilesLiveData.postValue(new ArrayList<>());
                refresh();
            } catch (ProtocolException e) {
                errorLiveData.postValue("删除失败: " + e.getMessage());
            }
        });
    }

    public void renameFile(NasFile file, String newName) {
        if (protocolManager == null) return;

        executor.execute(() -> {
            try {
                String currentPath = currentPathLiveData.getValue();
                String newPath = currentPath + "/" + newName;
                protocolManager.renameFile(file.getFullPath(), newPath);
                refresh();
            } catch (ProtocolException e) {
                errorLiveData.postValue("重命名失败: " + e.getMessage());
            }
        });
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    private void sortAndPostFiles() {
        List<NasFile> sorted = new ArrayList<>(allFiles);

        // Always show directories first
        Comparator<NasFile> comparator = (a, b) -> {
            if (a.isDirectory() != b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            }
            return 0;
        };

        switch (sortMode) {
            case NAME_ASC:
                comparator = comparator.thenComparing(NasFile::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case NAME_DESC:
                comparator = comparator.thenComparing(NasFile::getName, String.CASE_INSENSITIVE_ORDER.reversed());
                break;
            case SIZE_ASC:
                comparator = comparator.thenComparingLong(NasFile::getSize);
                break;
            case SIZE_DESC:
                comparator = comparator.thenComparingLong(NasFile::getSize).reversed();
                break;
            case DATE_ASC:
                comparator = comparator.thenComparing(f -> f.getLastModified() != null ?
                        f.getLastModified() : new java.util.Date(0));
                break;
            case DATE_DESC:
                comparator = comparator.thenComparing((NasFile f) -> f.getLastModified() != null ?
                        f.getLastModified() : new java.util.Date(0)).reversed();
                break;
        }

        Collections.sort(sorted, comparator);
        filesLiveData.postValue(sorted);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (protocolManager != null) {
            protocolManager.disconnect();
        }
    }
}
