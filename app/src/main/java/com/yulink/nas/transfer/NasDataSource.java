package com.yulink.nas.transfer;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.yulink.nas.protocol.ProtocolManager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class NasDataSource implements DataSource {
    private static final String TAG = "NasDataSource";
    private static final int BUFFER_SIZE = 512 * 1024; // 512KB

    private final ProtocolManager protocolManager;
    private final String remotePath;
    private InputStream rawStream;
    private BufferedInputStream inputStream;
    private long bytesRemaining;
    private boolean opened;
    private long readPosition = 0;

    public NasDataSource(ProtocolManager protocolManager, String remotePath) {
        this.protocolManager = protocolManager;
        this.remotePath = remotePath;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        try {
            if (rawStream == null) {
                Log.d(TAG, "Opening new stream for " + remotePath);
                rawStream = protocolManager.openFileStream(remotePath);
                readPosition = 0;
            }

            long targetPosition = dataSpec.position;
            if (targetPosition != readPosition) {
                if (trySeek(rawStream, targetPosition)) {
                    // True random-access seek (O(1), single SMB READ request)
                    Log.d(TAG, "Random-access seek to " + targetPosition);
                    readPosition = targetPosition;
                } else if (targetPosition > readPosition) {
                    // Forward seek: skip delta
                    long delta = targetPosition - readPosition;
                    Log.d(TAG, "Forward seek: skipping " + delta + " bytes from position " + readPosition);
                    long skipped = skipBytes(rawStream, delta);
                    readPosition += skipped;
                } else {
                    // Backward seek without random-access: reopen
                    Log.d(TAG, "Backward seek: reopening stream to position " + targetPosition);
                    try { rawStream.close(); } catch (Exception ignored) {}
                    rawStream = protocolManager.openFileStream(remotePath);
                    readPosition = 0;
                    if (targetPosition > 0) {
                        long skipped = skipBytes(rawStream, targetPosition);
                        readPosition += skipped;
                    }
                }
            }

            inputStream = new BufferedInputStream(rawStream, BUFFER_SIZE);
            bytesRemaining = dataSpec.length == C.LENGTH_UNSET ? C.LENGTH_UNSET : dataSpec.length;
            opened = true;
            Log.d(TAG, "Stream opened at position " + readPosition + ", length=" + bytesRemaining);
            return bytesRemaining;
        } catch (Exception e) {
            throw new IOException("Failed to open NAS data source", e);
        }
    }

    /**
     * Try to seek using reflection — looks for a public seek(long) method on the stream.
     * Returns true if seek was performed, false if the stream doesn't support it.
     */
    private boolean trySeek(InputStream stream, long position) {
        try {
            Method seekMethod = stream.getClass().getMethod("seek", long.class);
            seekMethod.invoke(stream, position);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private long skipBytes(InputStream stream, long bytes) throws IOException {
        byte[] buf = new byte[64 * 1024]; // 64KB
        long remaining = bytes;
        while (remaining > 0) {
            int toRead = (int) Math.min(remaining, buf.length);
            int read = stream.read(buf, 0, toRead);
            if (read <= 0) break;
            remaining -= read;
        }
        return bytes - remaining;
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
        readPosition += bytesRead;
        return bytesRead;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        // No-op
    }

    @Nullable
    @Override
    public Uri getUri() {
        return Uri.parse("nas://" + remotePath);
    }

    @Override
    public void close() throws IOException {
        // Don't close rawStream — keep SMB file handle alive for reuse.
        // Cleaned up by ProtocolManager.disconnect() in VideoPlayerActivity.onDestroy().
        inputStream = null;
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
