package com.yulink.nas.ui.transfer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.yulink.nas.R;
import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.util.PathUtils;
import com.google.android.material.button.MaterialButton;

public class TransferAdapter extends ListAdapter<TransferTask, TransferAdapter.ViewHolder> {
    private final OnTransferActionListener listener;

    public TransferAdapter(OnTransferActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<TransferTask> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TransferTask>() {
                @Override
                public boolean areItemsTheSame(@NonNull TransferTask oldItem,
                                               @NonNull TransferTask newItem) {
                    return oldItem.getTaskId().equals(newItem.getTaskId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull TransferTask oldItem,
                                                  @NonNull TransferTask newItem) {
                    return oldItem.getStatus() == newItem.getStatus() &&
                            oldItem.getTransferredBytes() == newItem.getTransferredBytes();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transfer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransferTask task = getItem(position);
        holder.bind(task);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivDirection;
        private final TextView tvFileName;
        private final TextView tvStatus;
        private final ProgressBar progressBar;
        private final TextView tvProgress;
        private final TextView tvSpeed;
        private final MaterialButton btnCancel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDirection = itemView.findViewById(R.id.iv_direction);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvProgress = itemView.findViewById(R.id.tv_progress);
            tvSpeed = itemView.findViewById(R.id.tv_speed);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
        }

        void bind(TransferTask task) {
            tvFileName.setText(task.getFileName());

            // Direction icon
            ivDirection.setImageResource(task.getDirection() == TransferTask.Direction.UPLOAD ?
                    R.drawable.ic_upload : R.drawable.ic_download);

            // Status
            switch (task.getStatus()) {
                case QUEUED:
                    tvStatus.setText("等待中");
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.transfer_pending));
                    break;
                case RUNNING:
                    tvStatus.setText("传输中");
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.transfer_running));
                    break;
                case COMPLETED:
                    tvStatus.setText("完成");
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.transfer_completed));
                    break;
                case FAILED:
                    tvStatus.setText("失败");
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.transfer_failed));
                    break;
                case CANCELLED:
                    tvStatus.setText("已取消");
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.transfer_pending));
                    break;
            }

            // Progress
            int progress = task.getProgressPercent();
            progressBar.setProgress(progress);
            tvProgress.setText(PathUtils.formatSize(task.getTransferredBytes()) + " / " +
                    PathUtils.formatSize(task.getTotalBytes()));

            // Cancel button
            boolean canCancel = task.getStatus() == TransferTask.Status.RUNNING ||
                    task.getStatus() == TransferTask.Status.QUEUED;
            btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
            btnCancel.setOnClickListener(v -> listener.onCancelClick(task));
        }
    }

    public interface OnTransferActionListener {
        void onCancelClick(TransferTask task);
    }
}
