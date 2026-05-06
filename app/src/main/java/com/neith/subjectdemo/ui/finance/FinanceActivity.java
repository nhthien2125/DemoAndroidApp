package com.neith.subjectdemo.ui.finance;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.data.FinanceDbHelper;
import com.neith.subjectdemo.model.Transaction;

import java.util.List;

public class FinanceActivity extends AppCompatActivity {
    private EditText edtHanMuc, edtSoTien, edtMoTa;
    private Button btnLuuHanMuc, btnThemGiaoDich;
    private RadioGroup rgLoaiGiaoDich;
    private TextView tvThongKe;
    private RecyclerView rvGiaoDich;
    private TransactionAdapter adapter;
    private SharedPreferences sharedPrefs;
    private FinanceDbHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_finance);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.finance), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // mapping
        edtHanMuc = findViewById(R.id.edtHanMuc);
        edtSoTien = findViewById(R.id.edtSoTien);
        edtMoTa = findViewById(R.id.edtMoTa);
        btnLuuHanMuc = findViewById(R.id.btnLuuHanMuc);
        btnThemGiaoDich = findViewById(R.id.btnThemGiaoDich);
        rgLoaiGiaoDich = findViewById(R.id.rgLoaiGiaoDich);
        tvThongKe = findViewById(R.id.tvThongKe);
        rvGiaoDich = findViewById(R.id.rvGiaoDich);

        // db init
        dbHelper = new FinanceDbHelper(this);
        sharedPrefs = getSharedPreferences("TaiChinhPrefs", Context.MODE_PRIVATE);

        rvGiaoDich.setLayoutManager(new LinearLayoutManager(this));
        List<Transaction> listGD = dbHelper.getAllTransaction();
        adapter = new TransactionAdapter(listGD);
        rvGiaoDich.setAdapter(adapter);
        capNhatThongKe();

        btnLuuHanMuc.setOnClickListener(v -> {
            String hanMucStr = edtHanMuc.getText().toString();
            if (!hanMucStr.isEmpty()) {
                // Lưu vào SharedPreferences
                sharedPrefs.edit().putFloat("HAN_MUC", Float.parseFloat(hanMucStr)).apply();
                Toast.makeText(this, "Đã lưu hạn mức!", Toast.LENGTH_SHORT).show();
                capNhatThongKe();
            }
        });

        btnThemGiaoDich.setOnClickListener(v -> {
            String soTienStr = edtSoTien.getText().toString();
            String description = edtMoTa.getText().toString();

            if (soTienStr.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(soTienStr);
            int type = (rgLoaiGiaoDich.getCheckedRadioButtonId() == R.id.rbThu) ? 1 : 0;

            // Gọi hàm từ FinanceDbHelper để Insert
            long id = dbHelper.addTransaction(type, amount, description);

            if (id != -1) {
                Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                edtSoTien.setText("");
                edtMoTa.setText("");
                capNhatThongKe();
                refreshDanhSach();
            } else {
                Toast.makeText(this, "Lỗi khi lưu vào DB", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void capNhatThongKe() {
        float hanMuc = sharedPrefs.getFloat("HAN_MUC", 0);

        double tongChi = dbHelper.getTotalExpense();

        String thongKe = String.format("Đã chi: %,.1f / Hạn mức: %,.1f (Triệu VNĐ)", tongChi, hanMuc);
        tvThongKe.setText(thongKe);

        if (hanMuc > 0 && tongChi > hanMuc) {
            tvThongKe.setTextColor(android.graphics.Color.RED);
        } else {
            tvThongKe.setTextColor(android.graphics.Color.DKGRAY);
        }
    }
    private void refreshDanhSach() {
        List<Transaction> listMoi = dbHelper.getAllTransaction();
        adapter.updateData(listMoi);
    }
}