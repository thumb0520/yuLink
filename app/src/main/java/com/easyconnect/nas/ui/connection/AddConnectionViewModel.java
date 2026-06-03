package com.easyconnect.nas.ui.connection;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.easyconnect.nas.data.db.entity.ConnectionEntity;
import com.easyconnect.nas.data.model.ConnectionInfo;
import com.easyconnect.nas.data.model.ProtocolType;
import com.easyconnect.nas.data.repository.ConnectionRepository;
import com.easyconnect.nas.protocol.ProtocolException;
import com.easyconnect.nas.protocol.ProtocolFactory;
import com.easyconnect.nas.protocol.ProtocolManager;
import com.easyconnect.nas.util.CryptoUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddConnectionViewModel extends AndroidViewModel {
    private final ConnectionRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> testResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AddConnectionViewModel(@NonNull Application application) {
        super(application);
        repository = new ConnectionRepository(application);
    }

    public LiveData<Boolean> getTestResult() {
        return testResult;
    }

    public LiveData<Boolean> getSaveResult() {
        return saveResult;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void testConnection(String name, ProtocolType protocol, String host, int port,
                               String username, String password, String shareName,
                               boolean passiveMode, boolean useFtps, boolean useSmbEncryption) {
        executor.execute(() -> {
            try {
                ConnectionInfo info = createConnectionInfo(protocol, host, port, username,
                        password, shareName, passiveMode, useFtps, useSmbEncryption);

                ProtocolManager manager = ProtocolFactory.create(protocol);
                manager.connect(info);
                manager.disconnect();

                testResult.postValue(true);
            } catch (ProtocolException e) {
                error.postValue(e.getMessage());
                testResult.postValue(false);
            } catch (Exception e) {
                error.postValue("连接失败: " + e.getMessage());
                testResult.postValue(false);
            }
        });
    }

    public void saveConnection(String name, ProtocolType protocol, String host, int port,
                               String username, String password, String shareName,
                               String defaultPath, boolean passiveMode, boolean useFtps,
                               boolean useSmbEncryption) {
        executor.execute(() -> {
            try {
                ConnectionEntity entity = new ConnectionEntity();
                entity.name = name;
                entity.protocol = protocol;
                entity.host = host;
                entity.port = port;
                entity.username = username;
                entity.encryptedPassword = CryptoUtils.encrypt(password);
                entity.shareName = shareName;
                entity.defaultPath = defaultPath;
                entity.passiveMode = passiveMode;
                entity.useFtps = useFtps;
                entity.useSmbEncryption = useSmbEncryption;
                entity.createdAt = System.currentTimeMillis();

                repository.insertConnection(entity, id -> saveResult.postValue(true));
            } catch (Exception e) {
                error.postValue("保存失败: " + e.getMessage());
                saveResult.postValue(false);
            }
        });
    }

    private ConnectionInfo createConnectionInfo(ProtocolType protocol, String host, int port,
                                                String username, String password, String shareName,
                                                boolean passiveMode, boolean useFtps,
                                                boolean useSmbEncryption) {
        ConnectionInfo info = new ConnectionInfo();
        info.setProtocol(protocol);
        info.setHost(host);
        info.setPort(port);
        info.setUsername(username);
        info.setPassword(password);
        info.setShareName(shareName);
        info.setPassiveMode(passiveMode);
        info.setUseFtps(useFtps);
        info.setUseSmbEncryption(useSmbEncryption);
        return info;
    }
}
