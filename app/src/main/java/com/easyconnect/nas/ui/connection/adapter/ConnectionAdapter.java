package com.easyconnect.nas.ui.connection.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.db.entity.ConnectionEntity;
import com.easyconnect.nas.data.model.ProtocolType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConnectionAdapter extends ListAdapter<ConnectionEntity, ConnectionAdapter.ViewHolder> {
    private final OnConnectionClickListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public ConnectionAdapter(OnConnectionClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ConnectionEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ConnectionEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ConnectionEntity oldItem,
                                               @NonNull ConnectionEntity newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ConnectionEntity oldItem,
                                                  @NonNull ConnectionEntity newItem) {
                    return oldItem.name.equals(newItem.name) &&
                            oldItem.host.equals(newItem.host) &&
                            oldItem.protocol == newItem.protocol;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConnectionEntity connection = getItem(position);
        holder.bind(connection);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProtocolIcon;
        private final TextView tvConnectionName;
        private final TextView tvHost;
        private final TextView tvProtocol;
        private final TextView tvLastConnected;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProtocolIcon = itemView.findViewById(R.id.iv_protocol_icon);
            tvConnectionName = itemView.findViewById(R.id.tv_connection_name);
            tvHost = itemView.findViewById(R.id.tv_host);
            tvProtocol = itemView.findViewById(R.id.chip_protocol);
            tvLastConnected = itemView.findViewById(R.id.tv_last_connected);
        }

        void bind(ConnectionEntity connection) {
            tvConnectionName.setText(connection.name);
            tvHost.setText(connection.host + ":" + connection.port);
            tvProtocol.setText(connection.protocol.getDisplayName());

            int iconRes;
            switch (connection.protocol) {
                case SMB:
                    iconRes = R.drawable.ic_smb;
                    break;
                case FTP:
                    iconRes = R.drawable.ic_ftp;
                    break;
                case SFTP:
                    iconRes = R.drawable.ic_sftp;
                    break;
                default:
                    iconRes = R.drawable.ic_nas_server;
            }
            ivProtocolIcon.setImageResource(iconRes);

            if (connection.lastConnectedAt > 0) {
                tvLastConnected.setText(dateFormat.format(new Date(connection.lastConnectedAt)));
            } else {
                tvLastConnected.setText("从未连接");
            }

            itemView.setOnClickListener(v -> listener.onConnectionClick(connection));
            itemView.setOnLongClickListener(v -> {
                listener.onConnectionLongClick(connection);
                return true;
            });
        }
    }

    public interface OnConnectionClickListener {
        void onConnectionClick(ConnectionEntity connection);
        void onConnectionLongClick(ConnectionEntity connection);
    }
}
