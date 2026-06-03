package com.easyconnect.nas.ui.browser.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.model.NasFile;
import com.easyconnect.nas.util.FileIconHelper;
import com.easyconnect.nas.util.PathUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileListAdapter extends ListAdapter<NasFile, FileListAdapter.ViewHolder> {
    private final OnFileClickListener listener;
    private final Set<String> selectedPaths = new HashSet<>();
    private boolean selectionMode = false;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public FileListAdapter(OnFileClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<NasFile> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NasFile>() {
                @Override
                public boolean areItemsTheSame(@NonNull NasFile oldItem, @NonNull NasFile newItem) {
                    return oldItem.getFullPath().equals(newItem.getFullPath());
                }

                @Override
                public boolean areContentsTheSame(@NonNull NasFile oldItem, @NonNull NasFile newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getSize() == newItem.getSize() &&
                            oldItem.isDirectory() == newItem.isDirectory();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NasFile file = getItem(position);
        holder.bind(file);
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public void setSelectedPaths(Set<String> paths) {
        this.selectedPaths.clear();
        this.selectedPaths.addAll(paths);
        notifyDataSetChanged();
    }

    public Set<String> getSelectedPaths() {
        return new HashSet<>(selectedPaths);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvName;
        private final TextView tvDate;
        private final TextView tvSize;
        private final CheckBox cbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvSize = itemView.findViewById(R.id.tv_size);
            cbSelect = itemView.findViewById(R.id.cb_select);
        }

        void bind(NasFile file) {
            tvName.setText(file.getName());

            if (file.isDirectory()) {
                tvSize.setText("");
                tvDate.setText("");
            } else {
                tvSize.setText(PathUtils.formatSize(file.getSize()));
                tvDate.setText(file.getLastModified() != null ?
                        dateFormat.format(file.getLastModified()) : "");
            }

            ivIcon.setImageResource(FileIconHelper.getFileIcon(file));

            cbSelect.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            cbSelect.setChecked(selectedPaths.contains(file.getFullPath()));
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPaths.add(file.getFullPath());
                } else {
                    selectedPaths.remove(file.getFullPath());
                }
                listener.onFileSelectionChanged(selectedPaths);
            });

            itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    cbSelect.setChecked(!cbSelect.isChecked());
                } else {
                    listener.onFileClick(file);
                }
            });

            itemView.setOnLongClickListener(v -> {
                listener.onFileLongClick(file);
                return true;
            });
        }
    }

    public interface OnFileClickListener {
        void onFileClick(NasFile file);
        void onFileLongClick(NasFile file);
        void onFileSelectionChanged(Set<String> selectedPaths);
    }
}
