package com.neith.subjectdemo.fn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neith.subjectdemo.R;
import com.neith.subjectdemo.fn.model.TransportExpense;
import java.util.List;

public class TransportExpenseAdapter extends RecyclerView.Adapter<TransportExpenseAdapter.ViewHolder> {

    private List<TransportExpense> expenseList;

    public TransportExpenseAdapter(List<TransportExpense> expenseList) {
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transport_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransportExpense item = expenseList.get(position);
        holder.tvDate.setText(item.getDate());
        holder.tvCode.setText(item.getCode());
        holder.tvMaterial.setText(item.getMaterial());
        holder.tvProject.setText(item.getProject());
        holder.tvAmount.setText(item.getAmount());
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvCode, tvMaterial, tvProject, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvMaterial = itemView.findViewById(R.id.tvMaterial);
            tvProject = itemView.findViewById(R.id.tvProject);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
