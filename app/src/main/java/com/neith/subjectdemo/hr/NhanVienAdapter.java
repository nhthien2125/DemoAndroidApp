package com.neith.subjectdemo.hr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neith.subjectdemo.R;

import java.util.ArrayList;

public class NhanVienAdapter
        extends RecyclerView.Adapter<NhanVienAdapter.ViewHolder> {

    ArrayList<NhanVien> list;
    OnEditClick onEdit;
    OnDeleteClick onDelete;


    public interface OnEditClick {
        void onEdit(NhanVien nv);
    }

    public interface OnDeleteClick {
        void onDelete(NhanVien nv);
    }

    public NhanVienAdapter(ArrayList<NhanVien> list,
                           OnEditClick onEdit,
                           OnDeleteClick onDelete) {
        this.list = list;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nhanvien, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder h, int position) {

        NhanVien nv = list.get(position);

        h.txtName.setText(nv.ho + " " + nv.ten);
        h.txtInfo.setText(
                "Mã: " + nv.ma + " | Năm sinh: " + nv.namSinh);

        h.btnEdit.setOnClickListener(v -> onEdit.onEdit(nv));
        h.btnDelete.setOnClickListener(v -> onDelete.onDelete(nv));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtInfo;
        ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            txtName = v.findViewById(R.id.txtName);
            txtInfo = v.findViewById(R.id.txtInfo);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}