package com.neith.subjectdemo.hr;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class EmployeeActivity extends AppCompatActivity {

    SQLiteDatabase db;

    LinearLayout layoutEmployeeList;
    LinearLayout layoutFilterToggle, layoutFilterContent;
    TextView txtFilterArrow;

    EditText edtSearchEmployee;
    Spinner spnDepartment, spnPosition, spnType;
    Button btnFilter, btnReset, btnAddEmployee;

    ArrayList<OptionItem> departments = new ArrayList<>();
    ArrayList<OptionItem> positions = new ArrayList<>();
    ArrayList<String> types = new ArrayList<>();

    String selectedDepartment = "";
    String selectedPosition = "";
    String selectedType = "";

    boolean isFilterExpanded = false;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");

    LinearLayout currentDialogRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        db = DB.openDatabase(this);

        layoutEmployeeList = findViewById(R.id.layoutEmployeeList);
        layoutFilterToggle = findViewById(R.id.layoutFilterToggle);
        layoutFilterContent = findViewById(R.id.layoutFilterContent);
        txtFilterArrow = findViewById(R.id.txtFilterArrow);

        edtSearchEmployee = findViewById(R.id.edtSearchEmployee);
        spnDepartment = findViewById(R.id.spnDepartment);
        spnPosition = findViewById(R.id.spnPosition);
        spnType = findViewById(R.id.spnType);

        btnFilter = findViewById(R.id.btnFilter);
        btnReset = findViewById(R.id.btnReset);
        btnAddEmployee = findViewById(R.id.btnAddEmployee);

        btnFilter.setBackgroundTintList(null);
        btnReset.setBackgroundTintList(null);
        btnAddEmployee.setBackgroundTintList(null);

        layoutFilterContent.setVisibility(View.GONE);
        txtFilterArrow.setText("▼");

        layoutFilterToggle.setOnClickListener(v -> toggleFilter());

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.EMPLOYEE);

        loadFilterData();
        setupSpinners();

        btnFilter.setOnClickListener(v -> loadEmployees());
        btnReset.setOnClickListener(v -> resetFilters());
        btnAddEmployee.setOnClickListener(v -> showAddEmployeeDialog());

        loadEmployees();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEmployees();
    }

    private void toggleFilter() {
        if (isFilterExpanded) {
            isFilterExpanded = false;
            txtFilterArrow.setText("▼");

            layoutFilterContent.animate()
                    .alpha(0f)
                    .translationY(-dp(8))
                    .setDuration(160)
                    .withEndAction(() -> {
                        layoutFilterContent.setVisibility(View.GONE);
                        layoutFilterContent.setAlpha(1f);
                        layoutFilterContent.setTranslationY(0);
                    })
                    .start();

        } else {
            isFilterExpanded = true;
            txtFilterArrow.setText("▲");

            layoutFilterContent.setVisibility(View.VISIBLE);
            layoutFilterContent.setAlpha(0f);
            layoutFilterContent.setTranslationY(-dp(8));

            layoutFilterContent.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setInterpolator(new OvershootInterpolator())
                    .setDuration(220)
                    .start();
        }

        layoutFilterToggle.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(80)
                .withEndAction(() -> layoutFilterToggle.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(new OvershootInterpolator())
                        .setDuration(180)
                        .start())
                .start();
    }

    private void loadFilterData() {
        departments.clear();
        positions.clear();
        types.clear();

        departments.add(new OptionItem("", "Tất cả phòng ban"));

        Cursor cDept = db.rawQuery(
                "SELECT MAPB, TENPB FROM PHONGBAN ORDER BY TENPB",
                null
        );

        while (cDept.moveToNext()) {
            departments.add(new OptionItem(cDept.getString(0), cDept.getString(1)));
        }

        cDept.close();

        positions.add(new OptionItem("", "Tất cả chức vụ"));

        Cursor cPos = db.rawQuery(
                "SELECT MACV, TENCV FROM VITRICONGVIEC ORDER BY TENCV",
                null
        );

        while (cPos.moveToNext()) {
            positions.add(new OptionItem(cPos.getString(0), cPos.getString(1)));
        }

        cPos.close();

        types.add("Tất cả loại nhân viên");

        Cursor cType = db.rawQuery(
                "SELECT DISTINCT LOAINV FROM LUONG WHERE LOAINV IS NOT NULL AND LOAINV <> '' ORDER BY LOAINV",
                null
        );

        while (cType.moveToNext()) {
            types.add(cType.getString(0));
        }

        cType.close();

        if (!types.contains("FT")) {
            types.add("FT");
        }
    }

    private void setupSpinners() {
        SpinnerTextAdapter<OptionItem> deptAdapter = new SpinnerTextAdapter<>(departments);
        spnDepartment.setAdapter(deptAdapter);

        SpinnerTextAdapter<OptionItem> posAdapter = new SpinnerTextAdapter<>(positions);
        spnPosition.setAdapter(posAdapter);

        SpinnerTextAdapter<String> typeAdapter = new SpinnerTextAdapter<>(types);
        spnType.setAdapter(typeAdapter);

        spnDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDepartment = departments.get(position).id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spnPosition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = positions.get(position).id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spnType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = position == 0 ? "" : types.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    class SpinnerTextAdapter<T> extends ArrayAdapter<T> {

        SpinnerTextAdapter(ArrayList<T> data) {
            super(EmployeeActivity.this, android.R.layout.simple_spinner_item, data);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(TEXT);
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(TEXT);
            tv.setBackgroundColor(Color.parseColor("#111827"));
            tv.setPadding(dp(16), dp(16), dp(16), dp(16));
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        private TextView makeSpinnerText(T item) {
            TextView tv = new TextView(EmployeeActivity.this);
            tv.setText(item == null ? "" : item.toString());
            tv.setTextSize(15);
            tv.setSingleLine(false);
            tv.setTextColor(TEXT);
            tv.setTypeface(null, Typeface.NORMAL);
            return tv;
        }
    }

    private void resetFilters() {
        edtSearchEmployee.setText("");
        spnDepartment.setSelection(0);
        spnPosition.setSelection(0);
        spnType.setSelection(0);

        selectedDepartment = "";
        selectedPosition = "";
        selectedType = "";

        loadEmployees();
    }

    private void loadEmployees() {
        layoutEmployeeList.removeAllViews();

        String keyword = edtSearchEmployee.getText().toString().trim();

        StringBuilder sql = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        sql.append("SELECT NV.MANV, ");
        sql.append("IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, ");
        sql.append("NV.GIOITINH, NV.NAMSINH, ");
        sql.append("IFNULL(PB.TENPB, '(Chưa gán)') AS TENPB, ");
        sql.append("IFNULL(CV.TENCV, '(Chưa gán)') AS TENCV, ");
        sql.append("IFNULL(L.LOAINV, '') AS LOAINV, ");
        sql.append("IFNULL(L.LUONGCOBAN, 0) AS LUONGCOBAN, ");
        sql.append("NV.MAPB, NV.MACV ");
        sql.append("FROM NHANVIEN NV ");
        sql.append("LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB ");
        sql.append("LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV ");
        sql.append("LEFT JOIN LUONG L ON NV.MANV = L.MANV ");
        sql.append("WHERE 1 = 1 ");

        if (!keyword.isEmpty()) {
            sql.append("AND (NV.MANV LIKE ? OR IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') LIKE ?) ");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }

        if (!selectedDepartment.isEmpty()) {
            sql.append("AND NV.MAPB = ? ");
            args.add(selectedDepartment);
        }

        if (!selectedPosition.isEmpty()) {
            sql.append("AND NV.MACV = ? ");
            args.add(selectedPosition);
        }

        if (!selectedType.isEmpty()) {
            sql.append("AND L.LOAINV = ? ");
            args.add(selectedType);
        }

        sql.append("ORDER BY NV.MANV");

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));

        if (c.getCount() == 0) {
            TextView empty = makeText("Không có nhân viên nào phù hợp điều kiện lọc.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            layoutEmployeeList.addView(empty);
        } else {
            while (c.moveToNext()) {
                EmployeeItem item = new EmployeeItem();

                item.maNV = c.getString(0);
                item.hoTen = c.getString(1).trim();
                item.gioiTinh = c.getInt(2) == 1;
                item.namSinh = c.isNull(3) ? 0 : c.getInt(3);
                item.tenPB = c.getString(4);
                item.tenCV = c.getString(5);
                item.loaiNV = c.getString(6);
                item.luongCoBan = c.getDouble(7);
                item.maPB = c.getString(8);
                item.maCV = c.getString(9);

                layoutEmployeeList.addView(makeEmployeeCard(item));
            }
        }

        c.close();
    }

    private View makeEmployeeCard(EmployeeItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.hr_card_bg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardLp);

        card.setOnClickListener(v -> animateClick(card));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = makeAvatar(item);
        top.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);

        TextView name = makeText(item.hoTen, 17, TEXT, true);
        info.addView(name);

        TextView meta = makeText(
                item.maNV + " • " + (item.gioiTinh ? "Nam" : "Nữ") + " • " + item.namSinh,
                12,
                SUB,
                false
        );
        meta.setPadding(0, dp(2), 0, 0);
        info.addView(meta);

        top.addView(info, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        card.addView(top);

        LinearLayout tags = new LinearLayout(this);
        tags.setOrientation(LinearLayout.HORIZONTAL);
        tags.setPadding(0, dp(12), 0, 0);

        tags.addView(makeTag(item.tenPB));
        tags.addView(makeTag(item.tenCV));

        if (item.loaiNV != null && !item.loaiNV.isEmpty()) {
            tags.addView(makeTag(item.loaiNV));
        }

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(tags);
        card.addView(hsv);

        TextView salary = makeText(
                "Lương cơ bản: " + formatNumber(item.luongCoBan) + " VND",
                13,
                PRIMARY,
                true
        );
        salary.setPadding(0, dp(10), 0, dp(4));
        card.addView(salary);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(10), 0, 0);

        actions.addView(makeSmallButton("Hồ sơ", PRIMARY, Color.BLACK, v -> openProfile(item.maNV)));
        actions.addView(makeSmallButton("Đổi vị trí", ORANGE, Color.WHITE, v -> showChangePositionDialog(item)));
        actions.addView(makeSmallButton("Chỉnh lương", BLUE, Color.WHITE, v -> showEditSalaryDialog(item)));
        actions.addView(makeSmallButton("Thưởng", GREEN, Color.WHITE, v -> showRewardPenaltyDialog(item, true)));
        actions.addView(makeSmallButton("Phạt", RED, Color.WHITE, v -> showRewardPenaltyDialog(item, false)));
        actions.addView(makeSmallButton("Xóa", Color.parseColor("#991B1B"), Color.WHITE, v -> showDeleteDialog(item)));

        HorizontalScrollView actionScroll = new HorizontalScrollView(this);
        actionScroll.setHorizontalScrollBarEnabled(false);
        actionScroll.addView(actions);

        card.addView(actionScroll);

        return card;
    }

    private void openProfile(String maNV) {
        Intent intent = new Intent(this, EmployeeProfileActivity.class);
        intent.putExtra("MANV", maNV);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private TextView makeAvatar(EmployeeItem item) {
        TextView avatar = makeText(getInitial(item.hoTen), 18, Color.BLACK, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.employee_avatar_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatar.setLayoutParams(lp);

        return avatar;
    }

    private TextView makeTag(String text) {
        TextView tag = makeText(text, 11, SUB, false);
        tag.setPadding(dp(9), dp(4), dp(9), dp(4));
        tag.setBackgroundResource(R.drawable.employee_tag_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, dp(6), 0);
        tag.setLayoutParams(lp);

        return tag;
    }

    private Button makeSmallButton(String text, int bgColor, int textColor, View.OnClickListener listener) {
        Button btn = new Button(this);

        btn.setText(text);
        btn.setTextSize(11);
        btn.setTextColor(textColor);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        btn.setOnClickListener(listener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );
        lp.setMargins(0, 0, dp(7), 0);
        btn.setLayoutParams(lp);

        return btn;
    }

    private Dialog createStyledDialog(String title) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        currentDialogRoot = new LinearLayout(this);
        currentDialogRoot.setOrientation(LinearLayout.VERTICAL);
        currentDialogRoot.setPadding(dp(18), dp(16), dp(18), dp(16));
        currentDialogRoot.setBackgroundResource(R.drawable.employee_dialog_bg);

        TextView tvTitle = makeText(title, 20, PRIMARY, true);
        tvTitle.setPadding(0, 0, 0, dp(12));
        currentDialogRoot.addView(tvTitle);

        dialog.setContentView(currentDialogRoot);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) {
                w.setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        return dialog;
    }

    private void showChangePositionDialog(EmployeeItem item) {
        Dialog dialog = createStyledDialog("Đổi vị trí");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        Spinner deptSpinner = makeDarkSpinner();
        Spinner posSpinner = makeDarkSpinner();

        deptSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                departments
        ));

        posSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                positions
        ));

        deptSpinner.setSelection(findOptionIndex(departments, item.maPB));
        posSpinner.setSelection(findOptionIndex(positions, item.maCV));

        content.addView(makeDialogLabel("Phòng ban"));
        content.addView(deptSpinner);

        content.addView(makeDialogLabel("Chức vụ"));
        content.addView(posSpinner);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            OptionItem dept = (OptionItem) deptSpinner.getSelectedItem();
            OptionItem pos = (OptionItem) posSpinner.getSelectedItem();

            updatePosition(item.maNV, dept.id, pos.id);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updatePosition(String maNV, String maPB, String maCV) {
        ContentValues values = new ContentValues();
        values.put("MAPB", maPB);
        values.put("MACV", maCV);

        int rows = db.update(
                "NHANVIEN",
                values,
                "MANV = ?",
                new String[]{maNV}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã cập nhật vị trí.", Toast.LENGTH_SHORT).show();
            loadEmployees();
        } else {
            Toast.makeText(this, "Cập nhật vị trí thất bại.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditSalaryDialog(EmployeeItem item) {
        Dialog dialog = createStyledDialog("Chỉnh lương cơ bản");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView employee = makeText(item.hoTen + " - " + item.maNV, 14, TEXT, true);
        employee.setPadding(0, 0, 0, dp(8));
        content.addView(employee);

        EditText edtSalary = makeDarkInput("Lương cơ bản");
        edtSalary.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edtSalary.setText(String.valueOf((long) item.luongCoBan));
        edtSalary.setSelection(edtSalary.getText().length());

        content.addView(makeDialogLabel("Lương cơ bản"));
        content.addView(edtSalary);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String salaryStr = edtSalary.getText().toString().trim();

            if (salaryStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lương cơ bản.", Toast.LENGTH_SHORT).show();
                return;
            }

            double salary;

            try {
                salary = Double.parseDouble(salaryStr);
            } catch (Exception e) {
                Toast.makeText(this, "Lương cơ bản không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (salary < 0) {
                Toast.makeText(this, "Lương cơ bản không được âm.", Toast.LENGTH_SHORT).show();
                return;
            }

            updateSalary(item.maNV, salary);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateSalary(String maNV, double salary) {
        if (salaryRecordExists(maNV)) {
            ContentValues values = new ContentValues();
            values.put("LUONGCOBAN", salary);
            values.put("LOAINV", "FT");

            int rows = db.update(
                    "LUONG",
                    values,
                    "MANV = ?",
                    new String[]{maNV}
            );

            if (rows > 0) {
                Toast.makeText(this, "Đã cập nhật lương cơ bản.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cập nhật lương thất bại.", Toast.LENGTH_SHORT).show();
            }
        } else {
            ContentValues values = new ContentValues();
            values.put("MANV", maNV);
            values.put("LOAINV", "FT");
            values.put("LUONGCOBAN", salary);

            long result = db.insert("LUONG", null, values);

            if (result != -1) {
                Toast.makeText(this, "Đã thêm lương cơ bản.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Thêm lương thất bại.", Toast.LENGTH_SHORT).show();
            }
        }

        loadFilterData();
        setupSpinners();
        loadEmployees();
    }

    private boolean salaryRecordExists(String maNV) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM LUONG WHERE MANV = ?",
                new String[]{maNV}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private void showRewardPenaltyDialog(EmployeeItem item, boolean reward) {
        Dialog dialog = createStyledDialog(reward ? "Thưởng nhân viên" : "Phạt nhân viên");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView employee = makeText(item.hoTen + " - " + item.maNV, 14, TEXT, true);
        employee.setPadding(0, 0, 0, dp(8));
        content.addView(employee);

        EditText edtAmount = makeDarkInput("Số tiền");
        edtAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText edtNote = makeDarkInput("Ghi chú");
        edtNote.setMinLines(3);
        edtNote.setGravity(Gravity.TOP);

        content.addView(makeDialogLabel("Số tiền"));
        content.addView(edtAmount);

        content.addView(makeDialogLabel("Ghi chú"));
        content.addView(edtNote);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String amountStr = edtAmount.getText().toString().trim();

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền.", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;

            try {
                amount = Double.parseDouble(amountStr);
            } catch (Exception e) {
                Toast.makeText(this, "Số tiền không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount <= 0) {
                Toast.makeText(this, "Số tiền phải lớn hơn 0.", Toast.LENGTH_SHORT).show();
                return;
            }

            addRewardPenalty(item.maNV, amount, reward);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addRewardPenalty(String maNV, double amount, boolean reward) {
        ContentValues values = new ContentValues();
        values.put("MANV", maNV);
        values.put("NGAY", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        values.put("HINHTHUC", reward ? "KT" : "KL");
        values.put("SOTIEN", amount);
        values.put("TONG", amount);

        long result = db.insert("DSTHUONGPHAT", null, values);

        if (result != -1) {
            Toast.makeText(
                    this,
                    reward ? "Đã lưu thông tin thưởng." : "Đã lưu thông tin phạt.",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(this, "Lưu thông tin thưởng phạt thất bại.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteDialog(EmployeeItem item) {
        Dialog dialog = createStyledDialog("Xóa nhân viên");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText(
                "Bạn có chắc muốn xóa nhân viên " + item.hoTen + "?\nToàn bộ dữ liệu liên quan cũng sẽ bị xóa.",
                14,
                TEXT,
                false
        );
        msg.setPadding(0, 0, 0, dp(12));
        content.addView(msg);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button delete = makeDialogButton("Xóa", R.drawable.employee_button_red_bg, Color.WHITE);

        actions.addView(cancel);
        actions.addView(delete);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        delete.setOnClickListener(v -> {
            deleteEmployee(item.maNV);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteEmployee(String maNV) {
        db.beginTransaction();

        try {
            ContentValues clearHead = new ContentValues();
            clearHead.putNull("MATRG_PHG");
            db.update("PHONGBAN", clearHead, "MATRG_PHG = ?", new String[]{maNV});

            db.delete("CHAMCONG", "MANV = ?", new String[]{maNV});
            db.delete("DSTHUONGPHAT", "MANV = ?", new String[]{maNV});
            db.delete("LICHLAMVIEC", "MANV = ?", new String[]{maNV});
            db.delete("LUONG", "MANV = ?", new String[]{maNV});
            db.delete("NHANTHAN", "MANV = ?", new String[]{maNV});
            db.delete("NVTHAMGIADA", "MANV = ?", new String[]{maNV});
            db.delete("THONGTINBAOHIEM", "MANV = ?", new String[]{maNV});
            db.delete("THONGTINLIENHE", "MANV = ?", new String[]{maNV});
            db.delete("THONGTINSUCKHOE", "MANV = ?", new String[]{maNV});

            int rows = db.delete("NHANVIEN", "MANV = ?", new String[]{maNV});

            db.setTransactionSuccessful();

            if (rows > 0) {
                Toast.makeText(this, "Đã xóa nhân viên.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không tìm thấy nhân viên.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

        loadEmployees();
    }

    private void showAddEmployeeDialog() {
        Dialog dialog = createStyledDialog("Thêm nhân viên");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtHoLot = makeDarkInput("Họ lót");
        EditText edtTen = makeDarkInput("Tên");
        EditText edtNamSinh = makeDarkInput("Năm sinh");
        edtNamSinh.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText edtSalary = makeDarkInput("Lương cơ bản");
        edtSalary.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        Spinner spnGender = makeDarkSpinner();

        ArrayList<String> genders = new ArrayList<>();
        genders.add("Nam");
        genders.add("Nữ");

        spnGender.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                genders
        ));

        Spinner spnDept = makeDarkSpinner();
        spnDept.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                departments
        ));

        Spinner spnPos = makeDarkSpinner();
        spnPos.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                positions
        ));

        content.addView(makeDialogLabel("Họ lót"));
        content.addView(edtHoLot);

        content.addView(makeDialogLabel("Tên"));
        content.addView(edtTen);

        content.addView(makeDialogLabel("Giới tính"));
        content.addView(spnGender);

        content.addView(makeDialogLabel("Năm sinh"));
        content.addView(edtNamSinh);

        content.addView(makeDialogLabel("Phòng ban"));
        content.addView(spnDept);

        content.addView(makeDialogLabel("Chức vụ"));
        content.addView(spnPos);

        content.addView(makeDialogLabel("Lương cơ bản"));
        content.addView(edtSalary);

        TextView typeNote = makeText("Loại nhân viên mặc định: FT", 13, PRIMARY, true);
        typeNote.setPadding(0, dp(6), 0, dp(4));
        content.addView(typeNote);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String hoLot = edtHoLot.getText().toString().trim();
            String ten = edtTen.getText().toString().trim();
            String namSinhStr = edtNamSinh.getText().toString().trim();
            String salaryStr = edtSalary.getText().toString().trim();

            if (hoLot.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập họ lót.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ten.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (namSinhStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập năm sinh.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (salaryStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lương cơ bản.", Toast.LENGTH_SHORT).show();
                return;
            }

            int namSinh;
            double salary;

            try {
                namSinh = Integer.parseInt(namSinhStr);
            } catch (Exception e) {
                Toast.makeText(this, "Năm sinh không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                salary = Double.parseDouble(salaryStr);
            } catch (Exception e) {
                Toast.makeText(this, "Lương cơ bản không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentYear = Integer.parseInt(
                    new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date())
            );

            if (namSinh < 1950 || namSinh > currentYear) {
                Toast.makeText(this, "Năm sinh không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (salary < 0) {
                Toast.makeText(this, "Lương cơ bản không được âm.", Toast.LENGTH_SHORT).show();
                return;
            }

            OptionItem dept = (OptionItem) spnDept.getSelectedItem();
            OptionItem pos = (OptionItem) spnPos.getSelectedItem();

            if (dept.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn phòng ban.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pos.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn chức vụ.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean gender = spnGender.getSelectedItemPosition() == 0;

            addEmployee(hoLot, ten, gender, namSinh, dept.id, pos.id, salary);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addEmployee(String hoLot, String ten, boolean gender, int namSinh, String maPB, String maCV, double salary) {
        String maNV = generateEmployeeCode(ten, namSinh, gender);

        db.beginTransaction();

        try {
            ContentValues nvValues = new ContentValues();
            nvValues.put("MANV", maNV);
            nvValues.put("HOLOT", hoLot);
            nvValues.put("TENNV", ten);
            nvValues.put("GIOITINH", gender ? 1 : 0);
            nvValues.put("NAMSINH", namSinh);
            nvValues.put("MAPB", maPB);
            nvValues.put("MACV", maCV);

            long nvResult = db.insert("NHANVIEN", null, nvValues);

            if (nvResult == -1) {
                Toast.makeText(this, "Thêm nhân viên thất bại.", Toast.LENGTH_SHORT).show();
                db.endTransaction();
                return;
            }

            ContentValues luongValues = new ContentValues();
            luongValues.put("MANV", maNV);
            luongValues.put("LOAINV", "FT");
            luongValues.put("LUONGCOBAN", salary);

            long salaryResult = db.insert("LUONG", null, luongValues);

            if (salaryResult == -1) {
                Toast.makeText(this, "Thêm lương thất bại.", Toast.LENGTH_SHORT).show();
                db.endTransaction();
                return;
            }

            db.setTransactionSuccessful();

            Toast.makeText(this, "Đã thêm nhân viên: " + maNV, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi thêm nhân viên: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

        loadFilterData();
        setupSpinners();
        loadEmployees();
    }

    private EditText makeDarkInput(String hint) {
        EditText edt = new EditText(this);
        edt.setHint(hint);
        edt.setTextColor(TEXT);
        edt.setHintTextColor(Color.parseColor("#8B90A0"));
        edt.setBackgroundResource(R.drawable.employee_input_bg);
        edt.setPadding(dp(14), 0, dp(14), 0);
        edt.setSingleLine(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        lp.setMargins(0, 0, 0, dp(8));
        edt.setLayoutParams(lp);

        return edt;
    }

    private Spinner makeDarkSpinner() {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(R.drawable.employee_input_bg);
        spinner.setPadding(dp(10), 0, dp(10), 0);
        spinner.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        lp.setMargins(0, 0, 0, dp(8));
        spinner.setLayoutParams(lp);

        return spinner;
    }

    private TextView makeDialogLabel(String text) {
        TextView tv = makeText(text, 13, SUB, true);
        tv.setPadding(0, dp(8), 0, dp(4));
        return tv;
    }

    private LinearLayout makeDialogActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.RIGHT);
        actions.setPadding(0, dp(14), 0, 0);
        return actions;
    }

    private Button makeDialogButton(String text, int bgRes, int textColor) {
        Button btn = new Button(this);

        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextColor(textColor);
        btn.setTextSize(13);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(null);
        btn.setBackgroundResource(bgRes);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(96), dp(44));
        lp.setMargins(dp(8), 0, 0, 0);
        btn.setLayoutParams(lp);

        return btn;
    }

    private String generateEmployeeCode(String ten, int namSinh, boolean gender) {
        Random rnd = new Random();

        String name = removeDiacritics(ten).toUpperCase(Locale.getDefault()).replace(" ", "");

        if (name.isEmpty()) {
            name = "NV";
        }

        String namePart = name.length() >= 4 ? name.substring(0, 4) : name;

        while (namePart.length() < 4) {
            namePart += randomUppercaseLetter(rnd);
        }

        String yearPart = String.format(Locale.getDefault(), "%02d", namSinh % 100);
        String genderPart = gender ? "11" : "00";
        String randPart = "" + randomUppercaseLetter(rnd) + randomUppercaseLetter(rnd);

        String baseCode = namePart + yearPart + genderPart + randPart;
        String finalCode = baseCode;

        int counter = 0;

        while (employeeCodeExists(finalCode) && counter < 100) {
            finalCode = baseCode.substring(0, 8) + String.format(Locale.getDefault(), "%02d", counter);
            counter++;
        }

        return finalCode;
    }

    private char randomUppercaseLetter(Random rnd) {
        return (char) ('A' + rnd.nextInt(26));
    }

    private boolean employeeCodeExists(String maNV) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM NHANVIEN WHERE MANV = ?",
                new String[]{maNV}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private int findOptionIndex(ArrayList<OptionItem> list, String id) {
        if (id == null) {
            id = "";
        }

        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).id)) {
                return i;
            }
        }

        return 0;
    }

    private TextView makeText(String text, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);

        tv.setText(text == null ? "" : text);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setLineSpacing(dp(2), 1f);

        if (bold) {
            tv.setTypeface(null, Typeface.BOLD);
        }

        return tv;
    }

    private String getInitial(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "NV";
        }

        String[] parts = fullName.trim().split("\\s+");
        String last = parts[parts.length - 1];

        if (last.length() >= 1) {
            return last.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        return "NV";
    }

    private String formatNumber(double number) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(number);
    }

    private String removeDiacritics(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        return pattern.matcher(normalized).replaceAll("")
                .replace("Đ", "D")
                .replace("đ", "d");
    }

    private void animateClick(View view) {
        view.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .alpha(0.85f)
                .setDuration(90)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setInterpolator(new OvershootInterpolator())
                        .setDuration(220)
                        .start())
                .start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class OptionItem {
        String id;
        String name;

        OptionItem(String id, String name) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class EmployeeItem {
        String maNV;
        String hoTen;
        boolean gioiTinh;
        int namSinh;
        String tenPB;
        String tenCV;
        String loaiNV;
        double luongCoBan;
        String maPB;
        String maCV;
    }
}