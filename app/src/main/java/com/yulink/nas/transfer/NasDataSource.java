package com.yulink.nas.transfer;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.yulink.nas.protocol.ProtocolManager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NasDataSource implements DataSource {
    private final ProtocolManager protocolManager;
    private final String remotePath;
    private InputStream inputStream;
    private long bytesRemaining;
    private boolean opened;

    public NasDataSource(ProtocolManager protocolManager, String remotePath) {
        this.protocolManager = protocolManager;
        this.remotePath = remotePath;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        try {
            InputStream rawStream = protocolManager.openFileStream(remotePath);
            if (dataSpec.position > 0) {
                rawStream.skip(dataSpec.position);
            }
            inputStream = new BufferedInputStream(rawStream, 128 * 1024); // 128KB buffer
            bytesRemaining = dataSpec.length == C.LENGTH_UNSET ? C.LENGTH_UNSET : dataSpec.length;
            opened = true;
            return bytesRemaining;
        } catch (Exception e) {
            throw new IOException("Failed to open NAS data source", e);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        if (bytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }

        int bytesToRead = bytesRemaining == C.LENGTH_UNSET ?
                length : (int) Math.min(length, bytesRemaining);

        int bytesRead = inputStream.read(buffer, offset, bytesToRead);
        if (bytesRead == C.RESULT_END_OF_INPUT) {
            if (bytesRemaining != C.LENGTH_UNSET) {
                throw new IOException("Unexpected end of stream");
            }
            return C.RESULT_END_OF_INPUT;
        }

        if (bytesRemaining != C.LENGTH_UNSET) {
            bytesRemaining -= bytesRead;
        }
        return bytesRead;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        // No-op: transfer listening not supported
    }

    @Nullable
    @Override
    public Uri getUri() {
        return Uri.parse("nas://" + remotePath);
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                throw new IOException("Failed to close NAS data source", e);
            }
        }
        opened = false;
    }

    public static class Factory implements DataSource.Factory {
        private final ProtocolManager protocolManager;
        private final String remotePath;

        public Factory(ProtocolManager protocolManager, String remotePath) {
            this.protocolManager = protocolManager;
            this.remotePath = remotePath;
        }

        @Override
        public DataSource createDataSource() {
            return new NasDataSource(protocolManager, remotePath);
        }
    }
}
