package com.rapid.prominence.minty.second;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.rapid.prominence.minty.R;

import java.util.List;

public class SearchHisItemAdapter extends RecyclerView.Adapter<SearchHisItemAdapter.ViewHolder> {

    public List<SearchBean> data;
    private OnItemClickListener listener;

    public SearchHisItemAdapter(List<SearchBean> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.minty_sec_item_flowlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchBean item = data.get(position);
        holder.tvText.setText(item.getText());
        
        // 鏁翠釜Item鐐瑰嚮浜嬩欢
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
        
        // 鍒犻櫎鎸夐挳鐐瑰嚮浜嬩欢
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void removeItem(int position) {
        try{
            if (position < data.size()){
                data.remove(position);
                notifyItemRemoved(position);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;
        ImageView btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tv_text);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(SearchBean item);
        void onDeleteClick(int position);
    }
}
