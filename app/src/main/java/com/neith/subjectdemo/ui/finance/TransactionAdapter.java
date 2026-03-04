package com.neith.subjectdemo.ui.finance;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neith.subjectdemo.R;
import com.neith.subjectdemo.model.Transaction;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> listTransaction;

    public TransactionAdapter(List<Transaction> list) {
        this.listTransaction = list;
    }

    // Cập nhật lại danh sách khi có dữ liệu mới
    public void updateData(List<Transaction> newList) {
        this.listTransaction = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp file giao diện item_transaction
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction gd = listTransaction.get(position);

        holder.tvMoTa.setText(gd.getDescription());
        holder.tvSoTien.setText(String.format("%,.1f Tr", gd.getAmount()));

        if (gd.getType() == 1) {
            holder.tvLoai.setText("THU");
            holder.tvLoai.setTextColor(Color.parseColor("#388E3C")); // Green
            holder.tvSoTien.setTextColor(Color.parseColor("#388E3C"));
        } else {
            holder.tvLoai.setText("CHI");
            holder.tvLoai.setTextColor(Color.parseColor("#D32F2F")); // Red
            holder.tvSoTien.setTextColor(Color.parseColor("#D32F2F"));
        }
    }

    @Override
    public int getItemCount() {
        return listTransaction.size();
    }

    // Class đại diện cho các View bên trong item_transaction
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLoai, tvMoTa, tvSoTien;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLoai = itemView.findViewById(R.id.tvLoai);
            tvMoTa = itemView.findViewById(R.id.tvMoTa);
            tvSoTien = itemView.findViewById(R.id.tvSoTien);
        }
    }
}