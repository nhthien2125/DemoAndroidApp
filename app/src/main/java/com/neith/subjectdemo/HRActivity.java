package com.neith.subjectdemo;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipboardManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class HRActivity extends AppCompatActivity {
    private static final String DATABASE_NAME = "QLNSVATC_v2.db";
    SQLiteDatabase db;
    RecyclerView rvNhanVien;
    NhanVienAdapter adapter;
    ArrayList<NhanVien> dsNhanVien;
    Integer filterNamSinh = null;
    String filterTen = "";
    String orderBy = "MANV ASC";
    ImageButton btnBack;
    private int currentPage = 0;
    private final int PAGE_SIZE = 9;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hr);
        rvNhanVien = findViewById(R.id.rvNhanVien);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvNhanVien.setLayoutManager(layoutManager);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        dsNhanVien = new ArrayList<>();
        adapter = new NhanVienAdapter(
                dsNhanVien,
                nv -> showNhanVienDialog(nv),
                nv -> deleteNhanVien(nv)
        );

        rvNhanVien.setAdapter(adapter);
        File dbFile = getDatabasePath(DATABASE_NAME);
        db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        EditText edtSearch = findViewById(R.id.edtSearch);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
                String currentText = s.toString();
                if (currentText.length() > 0 && a == 0 && c == currentText.length()) {
                    ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("RecoveredSearch", currentText);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getApplicationContext(), "Đã lưu nội dung vừa xóa vào bộ nhớ tạm", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                String original = s.toString();
                if (original.isEmpty()) return;
                StringBuilder sb = new StringBuilder();
                boolean capitalizeNext = true;
                for (char c : original.toCharArray()) {
                    if (Character.isSpaceChar(c)) {
                        capitalizeNext = true;
                        sb.append(c);
                    } else if (capitalizeNext) {
                        sb.append(Character.toUpperCase(c));
                        capitalizeNext = false;
                    } else {
                        sb.append(Character.toLowerCase(c));
                    }
                }

                String formatted = sb.toString();
                if (!original.equals(formatted)) {
                    edtSearch.removeTextChangedListener(this);
                    s.replace(0, s.length(), formatted);
                    edtSearch.addTextChangedListener(this);
                }
            }
            @Override
            public void onTextChanged(CharSequence s, int st, int c, int a) {
                filterTen = s.toString();
                refreshData();
            }
        });

        rvNhanVien.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= PAGE_SIZE) {
                            loadNhanVien();
                        }
                    }
                }
            }
        });

        findViewById(R.id.btnAdd).setOnClickListener(v -> showNhanVienDialog(null));
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
        findViewById(R.id.btnSort).setOnClickListener(v -> showSortDialog());
        refreshData();
    }
    private void showCenterAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(50, 30, 50, 30);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(25);
        shape.setColor(Color.parseColor("#AA000000"));
        tv.setBackground(shape);
        builder.setView(tv);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

        dialog.show();
        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) dialog.dismiss();
        }, 1500);
    }

    private void refreshData() {
        currentPage = 0;
        isLastPage = false;
        dsNhanVien.clear();
        loadNhanVien();
    }

    private void loadNhanVien() {
            isLoading = true;
            String selection = null;
            ArrayList<String> args = new ArrayList<>();
            if (filterNamSinh != null) {
                selection = "NAMSINH >= ?";
                args.add(String.valueOf(filterNamSinh));
            }
            if (filterTen != null && !filterTen.isEmpty()) {
                String searchCondition = "(HOLOT || ' ' || TENNV) LIKE ?";
                selection = (selection == null) ? searchCondition : selection + " AND " + searchCondition;
                args.add("%" + filterTen + "%");
            }
            String whereClause = (selection != null) ? " WHERE " + selection : "";
            String orderClause = (orderBy != null) ? " ORDER BY " + orderBy : "";
            int offset = currentPage * PAGE_SIZE;
            String limitClause = " LIMIT " + PAGE_SIZE + " OFFSET " + offset;
            String sql = "SELECT MANV, HOLOT, TENNV, NAMSINH FROM NHANVIEN"
                    + whereClause
                    + orderClause
                    + limitClause;
            Cursor c = db.rawQuery(sql, args.isEmpty() ? null : args.toArray(new String[0]));

            Log.d("SQL_QUERY", sql);
            Log.d("SQL_ROWS", "Rows = " + c.getCount());
            int count = 0;
            if (c != null) {
                while (c.moveToNext()) {

                    String maNV = c.getString(c.getColumnIndexOrThrow("MANV"));
                    String hoLot = c.getString(c.getColumnIndexOrThrow("HOLOT"));
                    String tenNV = c.getString(c.getColumnIndexOrThrow("TENNV"));
                    int namSinh = c.getInt(c.getColumnIndexOrThrow("NAMSINH"));

                    dsNhanVien.add(new NhanVien(maNV, hoLot, tenNV, namSinh));
                    count++;
                }
                c.close();
            }

            if (count < PAGE_SIZE) {
                isLastPage = true;
            }

        adapter.notifyDataSetChanged();

        currentPage++;
        isLoading = false;
    }

    private String generateMaNV(String ten) {
        String prefix = "";
        if (ten != null && !ten.isEmpty()) {
            String cleanTen = ten.trim().toUpperCase();
            prefix = cleanTen.substring(0, Math.min(cleanTen.length(), 3));
        }
        StringBuilder randomDigits = new StringBuilder();
        java.util.Random rd = new java.util.Random();
        for (int i = 0; i < 5; i++) {
            randomDigits.append(rd.nextInt(10));
        }
        StringBuilder randomChars = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            char c = (char) (rd.nextInt(26) + 'A');
            randomChars.append(c);
        }
        return prefix + randomDigits.toString() + randomChars.toString();
    }

    private void showNhanVienDialog(NhanVien nv) {
        androidx.appcompat.view.ContextThemeWrapper cw = new androidx.appcompat.view.ContextThemeWrapper(this, R.style.CustomAlertDialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(cw);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_nhanvien, null);
        builder.setView(view);

        EditText edtMa = view.findViewById(R.id.edtMa);
        EditText edtHo = view.findViewById(R.id.edtHo);
        EditText edtTen = view.findViewById(R.id.edtTen);
        EditText edtNam = view.findViewById(R.id.edtNamSinh);

        boolean isEdit = (nv != null);
        if (isEdit) {
            edtMa.setVisibility(View.VISIBLE);
            edtMa.setText(nv.ma);
            edtMa.setEnabled(false);
            edtMa.setAlpha(0.6f);
            edtHo.setText(nv.ho);
            edtTen.setText(nv.ten);
            edtNam.setText(String.valueOf(nv.namSinh));
        } else {
            edtMa.setVisibility(View.GONE);
        }

        builder.setTitle(isEdit ? "CẬP NHẬT THÔNG TIN" : "THÊM NHÂN VIÊN MỚI");
        builder.setPositiveButton("LƯU DỮ LIỆU", (d, w) -> {
            String ho = edtHo.getText().toString().trim();
            String ten = edtTen.getText().toString().trim();
            String namStr = edtNam.getText().toString().trim();

            if (ten.isEmpty() || namStr.isEmpty()) {
                showCenterAlert("Vui lòng nhập đầy đủ thông tin!");
                return;
            }
            ContentValues cv = new ContentValues();
            cv.put("HOLOT", ho);
            cv.put("TENNV", ten);
            cv.put("NAMSINH", Integer.parseInt(namStr));

            if (isEdit) {
                db.update("NHANVIEN", cv, "MANV = ?", new String[]{nv.ma});
                showCenterAlert("Cập nhật thành công!");
            } else {
                String maTuDong = generateMaNV(ten);
                cv.put("MANV", maTuDong);
                db.insert("NHANVIEN", null, cv);
                showCenterAlert("Thêm mới thành công!\nMã: " + maTuDong);
            }
            refreshData();
        });
        builder.setNegativeButton("HỦY BỎ", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        styleDialogButtons(dialog, "#FF5F6D");
    }

    private void showFilterDialog() {
        androidx.appcompat.view.ContextThemeWrapper cw = new androidx.appcompat.view.ContextThemeWrapper(this, R.style.CustomAlertDialog);
        EditText edt = new EditText(this);
        edt.setHint("Nhập năm sinh tối thiểu...");
        edt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(60, 40, 60, 10);
        edt.setLayoutParams(lp);
        container.addView(edt);

        AlertDialog dialog = new AlertDialog.Builder(cw)
                .setTitle("LỌC DỮ LIỆU")
                .setView(container)
                .setPositiveButton("ÁP DỤNG", (d, w) -> {
                    if (!edt.getText().toString().isEmpty()) {
                        filterNamSinh = Integer.parseInt(edt.getText().toString());
                        showCenterAlert("Đã áp dụng bộ lọc");
                        refreshData();
                    }
                })
                .setNegativeButton("XÓA LỌC", (d, w) -> {
                    filterNamSinh = null;
                    showCenterAlert("Đã xóa bộ lọc");
                    refreshData();
                })
                .create();
        dialog.show();
        styleDialogButtons(dialog, "#4CAF50");
    }

    private void showSortDialog() {
        String[] options = {"Tên: A đến Z", "Tên: Z đến A", "Năm sinh: Tăng dần", "Năm sinh: Giảm dần"};
        int checkedItem = 0;
        if (orderBy != null) {
            if (orderBy.equals("TENNV DESC")) checkedItem = 1;
            else if (orderBy.equals("NAMSINH ASC")) checkedItem = 2;
            else if (orderBy.equals("NAMSINH DESC")) checkedItem = 3;
        }
        androidx.appcompat.view.ContextThemeWrapper cw = new androidx.appcompat.view.ContextThemeWrapper(this, R.style.CustomAlertDialog);
        AlertDialog dialog = new AlertDialog.Builder(cw)
                .setTitle("SẮP XẾP DANH SÁCH")
                .setSingleChoiceItems(options, checkedItem, (d, i) -> {
                    switch (i) {
                        case 0: orderBy = "TENNV ASC"; break;
                        case 1: orderBy = "TENNV DESC"; break;
                        case 2: orderBy = "NAMSINH ASC"; break;
                        case 3: orderBy = "NAMSINH DESC"; break;
                    }
                    showCenterAlert("Đã chọn: " + options[i]);
                    refreshData();
                    d.dismiss();
                })
                .setNegativeButton("HỦY BỎ", null)
                .create();

        dialog.show();
        styleDialogButtons(dialog, "#FF5F6D");
    }

    private void deleteNhanVien(NhanVien nv) {
        androidx.appcompat.view.ContextThemeWrapper cw = new androidx.appcompat.view.ContextThemeWrapper(this, R.style.CustomAlertDialog);
        AlertDialog dialog = new AlertDialog.Builder(cw)
                .setTitle("XÁC NHẬN XÓA")
                .setMessage("Bạn có chắc chắn muốn xóa nhân viên " + nv.ten.toUpperCase() + "?")
                .setPositiveButton("XÓA NGAY", (d, w) -> {
                    db.delete("NHANVIEN", "MANV = ?", new String[]{nv.ma});
                    showCenterAlert("Đã xóa nhân viên thành công");
                    refreshData();
                })
                .setNegativeButton("QUAY LẠI", null)
                .create();
        dialog.show();
        styleDialogButtons(dialog, "#FF0000");
    }

    private void styleDialogButtons(AlertDialog dialog, String colorCode) {
        Button pBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (pBtn != null) {
            pBtn.setTextColor(Color.parseColor(colorCode));
            pBtn.setTypeface(Typeface.DEFAULT_BOLD);
        }
        Button nBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (nBtn != null) {
            nBtn.setTextColor(Color.parseColor("#757575"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) db.close();
    }
}