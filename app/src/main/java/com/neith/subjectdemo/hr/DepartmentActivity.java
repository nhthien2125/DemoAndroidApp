package com.neith.subjectdemo.hr;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.ActivityLogger;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class DepartmentActivity extends AppCompatActivity {

    SQLiteDatabase db;

    LinearLayout layoutDepartmentList;
    LinearLayout layoutDepartmentFilterToggle, layoutDepartmentFilterContent;
    TextView txtDepartmentFilterArrow;

    EditText edtSearchDepartment;
    Spinner spnSearchMode, spnFilterArea, spnHeadStatus, spnDepartmentSort;
    Button btnAddDepartment, btnSearchDepartment, btnResetDepartment;

    ArrayList<AreaItem> areas = new ArrayList<>();
    ArrayList<FilterItem> searchModes = new ArrayList<>();
    ArrayList<FilterItem> headStatuses = new ArrayList<>();
    ArrayList<FilterItem> sortModes = new ArrayList<>();

    String selectedSearchMode = "ALL";
    String selectedArea = "";
    String selectedHeadStatus = "ALL";
    String selectedSort = "CODE_ASC";

    boolean isFilterExpanded = false;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int BLUE = Color.parseColor("#3B82F6");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");

    LinearLayout currentDialogRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department);

        db = DB.openDatabase(this);

        layoutDepartmentList = findViewById(R.id.layoutDepartmentList);
        layoutDepartmentFilterToggle = findViewById(R.id.layoutDepartmentFilterToggle);
        layoutDepartmentFilterContent = findViewById(R.id.layoutDepartmentFilterContent);
        txtDepartmentFilterArrow = findViewById(R.id.txtDepartmentFilterArrow);

        edtSearchDepartment = findViewById(R.id.edtSearchDepartment);
        spnSearchMode = findViewById(R.id.spnSearchMode);
        spnFilterArea = findViewById(R.id.spnFilterArea);
        spnHeadStatus = findViewById(R.id.spnHeadStatus);
        spnDepartmentSort = findViewById(R.id.spnDepartmentSort);

        btnAddDepartment = findViewById(R.id.btnAddDepartment);
        btnSearchDepartment = findViewById(R.id.btnSearchDepartment);
        btnResetDepartment = findViewById(R.id.btnResetDepartment);

        btnAddDepartment.setBackgroundTintList(null);
        btnSearchDepartment.setBackgroundTintList(null);
        btnResetDepartment.setBackgroundTintList(null);

        layoutDepartmentFilterContent.setVisibility(View.GONE);
        txtDepartmentFilterArrow.setText("▼");

        layoutDepartmentFilterToggle.setOnClickListener(v -> toggleFilter());

        btnAddDepartment.setOnClickListener(v -> showCreateDepartmentDialog());
        btnSearchDepartment.setOnClickListener(v -> loadDepartments());

        btnResetDepartment.setOnClickListener(v -> {
            edtSearchDepartment.setText("");
            spnSearchMode.setSelection(0);
            spnFilterArea.setSelection(0);
            spnHeadStatus.setSelection(0);
            spnDepartmentSort.setSelection(0);

            selectedSearchMode = "ALL";
            selectedArea = "";
            selectedHeadStatus = "ALL";
            selectedSort = "CODE_ASC";

            loadDepartments();
        });

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.DEPARTMENT);

        loadAreas();
        setupFilterData();
        setupFilterSpinners();
        loadDepartments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAreas();
        refreshAreaSpinnerKeepSelection();
        loadDepartments();
    }

    private void toggleFilter() {
        if (isFilterExpanded) {
            isFilterExpanded = false;
            txtDepartmentFilterArrow.setText("▼");

            layoutDepartmentFilterContent.animate()
                    .alpha(0f)
                    .translationY(-dp(8))
                    .setDuration(160)
                    .withEndAction(() -> {
                        layoutDepartmentFilterContent.setVisibility(View.GONE);
                        layoutDepartmentFilterContent.setAlpha(1f);
                        layoutDepartmentFilterContent.setTranslationY(0);
                    })
                    .start();

        } else {
            isFilterExpanded = true;
            txtDepartmentFilterArrow.setText("▲");

            layoutDepartmentFilterContent.setVisibility(View.VISIBLE);
            layoutDepartmentFilterContent.setAlpha(0f);
            layoutDepartmentFilterContent.setTranslationY(-dp(8));

            layoutDepartmentFilterContent.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setInterpolator(new OvershootInterpolator())
                    .setDuration(220)
                    .start();
        }

        layoutDepartmentFilterToggle.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(80)
                .withEndAction(() -> layoutDepartmentFilterToggle.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(new OvershootInterpolator())
                        .setDuration(180)
                        .start())
                .start();
    }

    private void loadAreas() {
        areas.clear();
        areas.add(new AreaItem("", "Tất cả khu vực"));

        Cursor c = db.rawQuery(
                "SELECT MAKV, TENTINH FROM DTTHEOKV ORDER BY TENTINH",
                null
        );

        while (c.moveToNext()) {
            areas.add(new AreaItem(c.getString(0), c.getString(1)));
        }

        c.close();
    }

    private void setupFilterData() {
        searchModes.clear();
        headStatuses.clear();
        sortModes.clear();

        searchModes.add(new FilterItem("ALL", "Tìm tất cả"));
        searchModes.add(new FilterItem("CODE", "Theo mã phòng ban"));
        searchModes.add(new FilterItem("NAME", "Theo tên phòng ban"));
        searchModes.add(new FilterItem("AREA", "Theo tên khu vực"));
        searchModes.add(new FilterItem("HEAD", "Theo tên/mã trưởng phòng"));

        headStatuses.add(new FilterItem("ALL", "Tất cả trạng thái"));
        headStatuses.add(new FilterItem("HAS_HEAD", "Đã có trưởng phòng"));
        headStatuses.add(new FilterItem("NO_HEAD", "Chưa có trưởng phòng"));
        headStatuses.add(new FilterItem("HAS_EMPLOYEE", "Có nhân viên"));
        headStatuses.add(new FilterItem("NO_EMPLOYEE", "Chưa có nhân viên"));

        sortModes.add(new FilterItem("CODE_ASC", "Mã phòng ban A → Z"));
        sortModes.add(new FilterItem("CODE_DESC", "Mã phòng ban Z → A"));
        sortModes.add(new FilterItem("NAME_ASC", "Tên phòng ban A → Z"));
        sortModes.add(new FilterItem("NAME_DESC", "Tên phòng ban Z → A"));
        sortModes.add(new FilterItem("AREA_ASC", "Khu vực A → Z"));
        sortModes.add(new FilterItem("EMP_DESC", "Nhân viên nhiều → ít"));
        sortModes.add(new FilterItem("EMP_ASC", "Nhân viên ít → nhiều"));
        sortModes.add(new FilterItem("NO_HEAD_FIRST", "Chưa có trưởng phòng lên đầu"));
    }

    private void setupFilterSpinners() {
        spnSearchMode.setAdapter(new WhiteSpinnerAdapter<>(searchModes));
        spnFilterArea.setAdapter(new WhiteSpinnerAdapter<>(areas));
        spnHeadStatus.setAdapter(new WhiteSpinnerAdapter<>(headStatuses));
        spnDepartmentSort.setAdapter(new WhiteSpinnerAdapter<>(sortModes));

        spnSearchMode.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnFilterArea.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnHeadStatus.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnDepartmentSort.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);

        spnSearchMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSearchMode = searchModes.get(position).id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spnFilterArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedArea = areas.get(position).id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spnHeadStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedHeadStatus = headStatuses.get(position).id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spnDepartmentSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSort = sortModes.get(position).id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void refreshAreaSpinnerKeepSelection() {
        String oldArea = selectedArea;

        spnFilterArea.setAdapter(new WhiteSpinnerAdapter<>(areas));
        spnFilterArea.setSelection(findAreaIndex(oldArea));
        selectedArea = oldArea == null ? "" : oldArea;
    }

    private void loadDepartments() {
        layoutDepartmentList.removeAllViews();

        buildSummaryCard();

        String keyword = edtSearchDepartment == null ? "" : edtSearchDepartment.getText().toString().trim();

        StringBuilder sql = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        sql.append("SELECT PB.MAPB, PB.TENPB, IFNULL(PB.DIADIEM, ''), IFNULL(PB.MATRG_PHG, ''), ");
        sql.append("IFNULL(KV.TENTINH, ''), ");
        sql.append("IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HEAD_NAME, ");
        sql.append("(SELECT COUNT(*) FROM NHANVIEN N WHERE N.MAPB = PB.MAPB) AS EMP_COUNT, ");
        sql.append("IFNULL(KV.HESOKV, 0) AS HESOKV ");
        sql.append("FROM PHONGBAN PB ");
        sql.append("LEFT JOIN DTTHEOKV KV ON CAST(PB.DIADIEM AS TEXT) = CAST(KV.MAKV AS TEXT) ");
        sql.append("LEFT JOIN NHANVIEN NV ON PB.MATRG_PHG = NV.MANV ");
        sql.append("WHERE 1 = 1 ");

        if (!keyword.isEmpty()) {
            if ("CODE".equals(selectedSearchMode)) {
                sql.append("AND PB.MAPB LIKE ? ");
                args.add("%" + keyword + "%");

            } else if ("NAME".equals(selectedSearchMode)) {
                sql.append("AND PB.TENPB LIKE ? ");
                args.add("%" + keyword + "%");

            } else if ("AREA".equals(selectedSearchMode)) {
                sql.append("AND IFNULL(KV.TENTINH, '') LIKE ? ");
                args.add("%" + keyword + "%");

            } else if ("HEAD".equals(selectedSearchMode)) {
                sql.append("AND (IFNULL(PB.MATRG_PHG, '') LIKE ? OR IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') LIKE ?) ");
                args.add("%" + keyword + "%");
                args.add("%" + keyword + "%");

            } else {
                sql.append("AND (PB.MAPB LIKE ? OR PB.TENPB LIKE ? OR IFNULL(KV.TENTINH, '') LIKE ? ");
                sql.append("OR IFNULL(PB.MATRG_PHG, '') LIKE ? OR IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') LIKE ?) ");
                args.add("%" + keyword + "%");
                args.add("%" + keyword + "%");
                args.add("%" + keyword + "%");
                args.add("%" + keyword + "%");
                args.add("%" + keyword + "%");
            }
        }

        if (selectedArea != null && !selectedArea.trim().isEmpty()) {
            sql.append("AND CAST(PB.DIADIEM AS TEXT) = CAST(? AS TEXT) ");
            args.add(selectedArea);
        }

        if ("HAS_HEAD".equals(selectedHeadStatus)) {
            sql.append("AND PB.MATRG_PHG IS NOT NULL AND PB.MATRG_PHG <> '' ");
        } else if ("NO_HEAD".equals(selectedHeadStatus)) {
            sql.append("AND (PB.MATRG_PHG IS NULL OR PB.MATRG_PHG = '') ");
        } else if ("HAS_EMPLOYEE".equals(selectedHeadStatus)) {
            sql.append("AND (SELECT COUNT(*) FROM NHANVIEN N WHERE N.MAPB = PB.MAPB) > 0 ");
        } else if ("NO_EMPLOYEE".equals(selectedHeadStatus)) {
            sql.append("AND (SELECT COUNT(*) FROM NHANVIEN N WHERE N.MAPB = PB.MAPB) = 0 ");
        }

        sql.append(getSortSql());

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));

        if (c.getCount() == 0) {
            TextView empty = makeText("Không có phòng ban phù hợp bộ lọc.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            layoutDepartmentList.addView(empty);
        } else {
            while (c.moveToNext()) {
                DepartmentItem item = new DepartmentItem();

                item.maPB = c.getString(0);
                item.tenPB = c.getString(1);
                item.diaDiem = c.getString(2);
                item.maTruongPhong = c.getString(3);
                item.tenKhuVuc = c.getString(4);
                item.tenTruongPhong = c.getString(5).trim();
                item.soNhanVien = c.getInt(6);
                item.heSoKhuVuc = c.getDouble(7);

                layoutDepartmentList.addView(makeDepartmentCard(item));
            }
        }

        c.close();
    }

    private String getSortSql() {
        if ("CODE_DESC".equals(selectedSort)) {
            return "ORDER BY PB.MAPB DESC";
        }

        if ("NAME_ASC".equals(selectedSort)) {
            return "ORDER BY PB.TENPB COLLATE NOCASE ASC";
        }

        if ("NAME_DESC".equals(selectedSort)) {
            return "ORDER BY PB.TENPB COLLATE NOCASE DESC";
        }

        if ("AREA_ASC".equals(selectedSort)) {
            return "ORDER BY IFNULL(KV.TENTINH, '') COLLATE NOCASE ASC, PB.TENPB COLLATE NOCASE ASC";
        }

        if ("EMP_DESC".equals(selectedSort)) {
            return "ORDER BY EMP_COUNT DESC, PB.TENPB COLLATE NOCASE ASC";
        }

        if ("EMP_ASC".equals(selectedSort)) {
            return "ORDER BY EMP_COUNT ASC, PB.TENPB COLLATE NOCASE ASC";
        }

        if ("NO_HEAD_FIRST".equals(selectedSort)) {
            return "ORDER BY CASE WHEN PB.MATRG_PHG IS NULL OR PB.MATRG_PHG = '' THEN 0 ELSE 1 END, PB.TENPB COLLATE NOCASE ASC";
        }

        return "ORDER BY PB.MAPB ASC";
    }

    private void buildSummaryCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("TỔNG QUAN PHÒNG BAN"));

        int totalDept = getInt("SELECT COUNT(*) FROM PHONGBAN", null);
        int totalEmployee = getInt("SELECT COUNT(*) FROM NHANVIEN", null);
        int employeeAssigned = getInt("SELECT COUNT(*) FROM NHANVIEN WHERE MAPB IS NOT NULL AND MAPB <> ''", null);
        int noHead = getInt("SELECT COUNT(*) FROM PHONGBAN WHERE MATRG_PHG IS NULL OR MATRG_PHG = ''", null);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        row1.addView(makeStatBox("Phòng ban", String.valueOf(totalDept), "Tổng số", PRIMARY), makeHalfLp(true));
        row1.addView(makeStatBox("Nhân viên", String.valueOf(totalEmployee), "Toàn hệ thống", BLUE), makeHalfLp(false));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);

        row2.addView(makeStatBox("Đã phân phòng", String.valueOf(employeeAssigned), "Có MAPB", GREEN), makeHalfLp(true));
        row2.addView(makeStatBox("Thiếu trưởng phòng", String.valueOf(noHead), "Cần bổ nhiệm", ORANGE), makeHalfLp(false));

        card.addView(row1);
        card.addView(row2);

        layoutDepartmentList.addView(card);
    }

    private View makeDepartmentCard(DepartmentItem item) {
        LinearLayout card = makeCard();
        card.setClickable(true);
        card.setFocusable(true);

        card.setOnClickListener(v -> {
            animateClick(card);
            openDetail(item.maPB);
        });

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = makeDeptAvatar(item.tenPB);
        top.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);

        TextView name = makeText(item.tenPB, 17, TEXT, true);
        name.setSingleLine(false);
        info.addView(name);

        TextView code = makeText("Mã: " + item.maPB, 12, SUB, false);
        code.setPadding(0, dp(2), 0, 0);
        info.addView(code);

        top.addView(info, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        card.addView(top);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(0, dp(12), 0, dp(6));

        meta.addView(makeInfoRow(R.drawable.ic_people, "Nhân viên", String.valueOf(item.soNhanVien)));
        meta.addView(makeInfoRow(R.drawable.ic_manager, "Trưởng phòng", getHeadDisplay(item)));
        meta.addView(makeInfoRow(R.drawable.ic_location, "Địa điểm", getLocationDisplay(item)));
        meta.addView(makeInfoRow(R.drawable.ic_project, "Hệ số khu vực", item.heSoKhuVuc == 0 ? "—" : String.valueOf(item.heSoKhuVuc)));

        card.addView(meta);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(10), 0, 0);

        actions.addView(makeSmallButton("Chi tiết", PRIMARY, Color.BLACK, v -> openDetail(item.maPB)));
        actions.addView(makeSmallButton("Copy mã", ORANGE, Color.WHITE, v -> copyText(item.maPB)));
        actions.addView(makeSmallButton("Sửa", BLUE, Color.WHITE, v -> showEditDepartmentDialog(item)));
        actions.addView(makeSmallButton("Xóa", Color.parseColor("#991B1B"), Color.WHITE, v -> showDeleteDepartmentDialog(item)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(actions);

        card.addView(hsv);

        return card;
    }

    private String getHeadDisplay(DepartmentItem item) {
        if (item.tenTruongPhong != null && !item.tenTruongPhong.trim().isEmpty()) {
            return item.tenTruongPhong;
        }

        if (item.maTruongPhong != null && !item.maTruongPhong.trim().isEmpty()) {
            return item.maTruongPhong;
        }

        return "Chưa thiết lập";
    }

    private String getLocationDisplay(DepartmentItem item) {
        if (item.tenKhuVuc != null && !item.tenKhuVuc.trim().isEmpty()) {
            return item.tenKhuVuc;
        }

        if (item.diaDiem != null && !item.diaDiem.trim().isEmpty()) {
            return item.diaDiem;
        }

        return "N/A";
    }

    private void openDetail(String maPB) {
        Intent intent = new Intent(this, DepartmentDetailActivity.class);
        intent.putExtra("MAPB", maPB);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showCreateDepartmentDialog() {
        Dialog dialog = createStyledDialog("Thêm phòng ban mới");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtName = makeDarkInput("Tên phòng ban");
        edtName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        Spinner spnArea = makeDarkSpinner();
        spnArea.setAdapter(new WhiteSpinnerAdapter<>(areas));

        content.addView(makeDialogLabel("Tên phòng ban"));
        content.addView(edtName);

        content.addView(makeDialogLabel("Địa điểm / khu vực"));
        content.addView(spnArea);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Tạo mới", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            AreaItem area = (AreaItem) spnArea.getSelectedItem();

            if (!isValidVietnameseText(name)) {
                Toast.makeText(this, "Tên phòng ban chỉ được chứa chữ cái, số và khoảng trắng.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (area == null || area.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn khu vực.", Toast.LENGTH_SHORT).show();
                return;
            }

            createDepartment(name, area.id);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createDepartment(String tenPB, String diaDiem) {
        String maPB = generateDeptCode();

        ContentValues values = new ContentValues();
        values.put("MAPB", maPB);
        values.put("TENPB", tenPB);
        values.put("DIADIEM", diaDiem);
        values.putNull("MATRG_PHG");

        long result = db.insert("PHONGBAN", null, values);

        if (result != -1) {
            Toast.makeText(this, "Đã tạo phòng ban: " + maPB, Toast.LENGTH_SHORT).show();
            loadAreas();
            refreshAreaSpinnerKeepSelection();
            loadDepartments();
            ActivityLogger.log(db, "CREATE_DEPARTMENT", "HR", "Đã tạo phòng ban: " + maPB + " - " + tenPB);
        } else {
            Toast.makeText(this, "Không thể tạo phòng ban.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDepartmentDialog(DepartmentItem item) {
        Dialog dialog = createStyledDialog("Sửa thông tin phòng ban");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView code = makeText(item.maPB, 14, PRIMARY, true);
        code.setPadding(0, 0, 0, dp(8));

        EditText edtName = makeDarkInput("Tên phòng ban");
        edtName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtName.setText(item.tenPB);
        edtName.setSelection(edtName.getText().length());

        Spinner spnArea = makeDarkSpinner();
        spnArea.setAdapter(new WhiteSpinnerAdapter<>(areas));
        spnArea.setSelection(findAreaIndex(item.diaDiem));

        content.addView(makeDialogLabel("Mã phòng ban"));
        content.addView(code);

        content.addView(makeDialogLabel("Tên phòng ban"));
        content.addView(edtName);

        content.addView(makeDialogLabel("Địa điểm / khu vực"));
        content.addView(spnArea);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            AreaItem area = (AreaItem) spnArea.getSelectedItem();

            if (!isValidVietnameseText(name)) {
                Toast.makeText(this, "Tên phòng ban chỉ được chứa chữ cái, số và khoảng trắng.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (area == null || area.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn khu vực.", Toast.LENGTH_SHORT).show();
                return;
            }

            updateDepartment(item.maPB, name, area.id);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateDepartment(String maPB, String tenPB, String diaDiem) {
        ContentValues values = new ContentValues();
        values.put("TENPB", tenPB);
        values.put("DIADIEM", diaDiem);

        int rows = db.update(
                "PHONGBAN",
                values,
                "MAPB = ?",
                new String[]{maPB}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã cập nhật phòng ban.", Toast.LENGTH_SHORT).show();
            loadAreas();
            refreshAreaSpinnerKeepSelection();
            loadDepartments();
            ActivityLogger.log(db, "UPDATE_DEPARTMENT", "HR", "Đã cập nhật phòng ban: " + maPB + " - " + tenPB);
        } else {
            Toast.makeText(this, "Không thể cập nhật phòng ban.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteDepartmentDialog(DepartmentItem item) {
        Dialog dialog = createStyledDialog("Xóa phòng ban");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText(
                "Bạn có chắc muốn xóa phòng ban " + item.tenPB + "?\nChỉ có thể xóa nếu phòng ban không còn nhân viên.",
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
            deleteDepartment(item.maPB);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteDepartment(String maPB) {
        int count = getInt(
                "SELECT COUNT(*) FROM NHANVIEN WHERE MAPB = ?",
                new String[]{maPB}
        );

        if (count > 0) {
            Toast.makeText(this, "Không thể xóa vì phòng ban vẫn còn nhân viên.", Toast.LENGTH_LONG).show();
            return;
        }

        int rows = db.delete(
                "PHONGBAN",
                "MAPB = ?",
                new String[]{maPB}
        );

        if (rows > 0) {
            ActivityLogger.log(db, "DELETE_DEPARTMENT", "HR", "Đã xóa phòng ban: " + maPB);
            Toast.makeText(this, "Đã xóa phòng ban.", Toast.LENGTH_SHORT).show();
            loadAreas();
            refreshAreaSpinnerKeepSelection();
            loadDepartments();
        } else {
            Toast.makeText(this, "Không tìm thấy phòng ban.", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateDeptCode() {
        Random random = new Random();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int attempt = 0; attempt < 500; attempt++) {
            StringBuilder sb = new StringBuilder("PB");

            for (int i = 0; i < 4; i++) {
                sb.append(letters.charAt(random.nextInt(letters.length())));
            }

            String code = sb.toString();

            if (!departmentCodeExists(code)) {
                return code;
            }
        }

        return "PB" + System.currentTimeMillis();
    }

    private boolean departmentCodeExists(String maPB) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM PHONGBAN WHERE MAPB = ?",
                new String[]{maPB}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private int findAreaIndex(String areaId) {
        if (areaId == null) {
            areaId = "";
        }

        for (int i = 0; i < areas.size(); i++) {
            if (areaId.equals(String.valueOf(areas.get(i).id))) {
                return i;
            }
        }

        return 0;
    }

    private int getInt(String sql, String[] args) {
        Cursor c = db.rawQuery(sql, args);
        int value = 0;

        if (c.moveToFirst()) {
            value = c.getInt(0);
        }

        c.close();
        return value;
    }

    private TextView makeDeptAvatar(String name) {
        TextView avatar = makeText(getInitial(name), 18, Color.BLACK, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.employee_avatar_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatar.setLayoutParams(lp);

        return avatar;
    }

    private LinearLayout makeInfoRow(int iconRes, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(SUB);
        icon.setAlpha(0.85f);

        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        iconLp.setMargins(0, 0, dp(8), 0);
        row.addView(icon, iconLp);

        TextView l = makeText(label, 13, SUB, false);

        row.addView(l, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView v = makeText(
                value == null || value.trim().isEmpty() ? "—" : value,
                13,
                TEXT,
                true
        );
        v.setGravity(Gravity.RIGHT);

        row.addView(v, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        return row;
    }

    private LinearLayout makeStatBox(String label, String value, String note, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackgroundResource(R.drawable.employee_input_bg);

        box.addView(makeText(label, 11, SUB, false));
        box.addView(makeText(value, 20, color, true));
        box.addView(makeText(note, 10, SUB, false));

        return box;
    }

    private LinearLayout.LayoutParams makeHalfLp(boolean left) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );

        if (left) {
            lp.setMargins(0, 0, dp(6), 0);
        } else {
            lp.setMargins(dp(6), 0, 0, 0);
        }

        return lp;
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.hr_card_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        return card;
    }

    private TextView makeCardTitle(String text) {
        TextView tv = makeText(text, 13, PRIMARY, true);
        tv.setPadding(0, 0, 0, dp(10));
        return tv;
    }

    private Button makeSmallButton(String text, int bgColor, int textColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTextColor(textColor);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        btn.setOnClickListener(v -> listener.onClick(v));
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        btn.setPadding(dp(14), 0, dp(14), 0);

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

    class WhiteSpinnerAdapter<T> extends ArrayAdapter<T> {

        WhiteSpinnerAdapter(ArrayList<T> data) {
            super(DepartmentActivity.this, android.R.layout.simple_spinner_item, data);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(Color.parseColor("#111827"));
            tv.setPadding(dp(16), dp(16), dp(16), dp(16));
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        private TextView makeSpinnerText(T item) {
            TextView tv = new TextView(DepartmentActivity.this);
            tv.setText(item == null ? "" : item.toString());
            tv.setTextSize(15);
            tv.setSingleLine(false);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(null, Typeface.NORMAL);
            return tv;
        }
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

    private String getInitial(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "PB";
        }

        String[] parts = text.trim().split("\\s+");
        String last = parts[parts.length - 1];

        if (last.length() >= 1) {
            return last.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        return "PB";
    }

    private boolean isValidVietnameseText(String text) {
        if (text == null) {
            return false;
        }

        String value = text.trim();

        if (value.isEmpty()) {
            return false;
        }

        return value.matches("^[\\p{L}\\p{N}]+([\\s.'\\-/&][\\p{L}\\p{N}]+)*$");
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

    private void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Department code", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Đã copy: " + text, Toast.LENGTH_SHORT).show();
    }

    private String formatNumber(double number) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(number);
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

    static class DepartmentItem {
        String maPB;
        String tenPB;
        String diaDiem;
        String maTruongPhong;
        String tenTruongPhong;
        String tenKhuVuc;
        int soNhanVien;
        double heSoKhuVuc;
    }

    static class AreaItem {
        String id;
        String name;

        AreaItem(String id, String name) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class FilterItem {
        String id;
        String name;

        FilterItem(String id, String name) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}