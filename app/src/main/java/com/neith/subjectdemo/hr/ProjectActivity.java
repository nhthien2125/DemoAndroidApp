package com.neith.subjectdemo.hr;

import android.app.DatePickerDialog;
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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.ActivityLogger;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ProjectActivity extends AppCompatActivity {

    SQLiteDatabase db;

    LinearLayout layoutProjectList;
    LinearLayout layoutProjectFilterToggle, layoutProjectFilterContent;
    TextView txtProjectFilterArrow;

    EditText edtSearchProject;
    Spinner spnProjectSearchMode, spnProjectTypeFilter, spnProjectAreaFilter, spnProjectStatusFilter, spnProjectSort;
    Button btnAddProject, btnApplyProjectFilter, btnResetProjectFilter;

    ArrayList<OptionItem> customers = new ArrayList<>();
    ArrayList<OptionItem> areas = new ArrayList<>();
    ArrayList<OptionItem> projectTypes = new ArrayList<>();
    ArrayList<OptionItem> contractTypes = new ArrayList<>();

    ArrayList<OptionItem> searchModes = new ArrayList<>();
    ArrayList<OptionItem> filterContractTypes = new ArrayList<>();
    ArrayList<OptionItem> filterAreas = new ArrayList<>();
    ArrayList<OptionItem> statusFilters = new ArrayList<>();
    ArrayList<OptionItem> sortModes = new ArrayList<>();

    String selectedSearchMode = "ALL";
    String selectedContractType = "";
    String selectedArea = "";
    String selectedStatus = "ALL";
    String selectedSort = "NAME_ASC";

    boolean isFilterExpanded = false;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");
    final int PURPLE = Color.parseColor("#A855F7");

    LinearLayout currentDialogRoot;

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat viewDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

        db = DB.openDatabase(this);

        layoutProjectList = findViewById(R.id.layoutProjectList);
        layoutProjectFilterToggle = findViewById(R.id.layoutProjectFilterToggle);
        layoutProjectFilterContent = findViewById(R.id.layoutProjectFilterContent);
        txtProjectFilterArrow = findViewById(R.id.txtProjectFilterArrow);

        edtSearchProject = findViewById(R.id.edtSearchProject);
        spnProjectSearchMode = findViewById(R.id.spnProjectSearchMode);
        spnProjectTypeFilter = findViewById(R.id.spnProjectTypeFilter);
        spnProjectAreaFilter = findViewById(R.id.spnProjectAreaFilter);
        spnProjectStatusFilter = findViewById(R.id.spnProjectStatusFilter);
        spnProjectSort = findViewById(R.id.spnProjectSort);

        btnAddProject = findViewById(R.id.btnAddProject);
        btnApplyProjectFilter = findViewById(R.id.btnApplyProjectFilter);
        btnResetProjectFilter = findViewById(R.id.btnResetProjectFilter);

        btnAddProject.setBackgroundTintList(null);
        btnApplyProjectFilter.setBackgroundTintList(null);
        btnResetProjectFilter.setBackgroundTintList(null);

        layoutProjectFilterContent.setVisibility(View.GONE);
        txtProjectFilterArrow.setText("▼");

        layoutProjectFilterToggle.setOnClickListener(v -> toggleFilter());

        btnAddProject.setOnClickListener(v -> showCreateProjectDialog());
        btnApplyProjectFilter.setOnClickListener(v -> loadProjects());

        btnResetProjectFilter.setOnClickListener(v -> {
            edtSearchProject.setText("");
            spnProjectSearchMode.setSelection(0);
            spnProjectTypeFilter.setSelection(0);
            spnProjectAreaFilter.setSelection(0);
            spnProjectStatusFilter.setSelection(0);
            spnProjectSort.setSelection(0);

            selectedSearchMode = "ALL";
            selectedContractType = "";
            selectedArea = "";
            selectedStatus = "ALL";
            selectedSort = "NAME_ASC";

            loadProjects();
        });

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.PROJECT);

        loadOptionData();
        setupFilterData();
        setupFilterSpinners();
        loadProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOptionData();
        refreshFilterSpinnersKeepSelection();
        loadProjects();
    }

    private void toggleFilter() {
        if (isFilterExpanded) {
            isFilterExpanded = false;
            txtProjectFilterArrow.setText("▼");

            layoutProjectFilterContent.animate()
                    .alpha(0f)
                    .translationY(-dp(8))
                    .setDuration(160)
                    .withEndAction(() -> {
                        layoutProjectFilterContent.setVisibility(View.GONE);
                        layoutProjectFilterContent.setAlpha(1f);
                        layoutProjectFilterContent.setTranslationY(0);
                    })
                    .start();

        } else {
            isFilterExpanded = true;
            txtProjectFilterArrow.setText("▲");

            layoutProjectFilterContent.setVisibility(View.VISIBLE);
            layoutProjectFilterContent.setAlpha(0f);
            layoutProjectFilterContent.setTranslationY(-dp(8));

            layoutProjectFilterContent.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setInterpolator(new OvershootInterpolator())
                    .setDuration(220)
                    .start();
        }

        layoutProjectFilterToggle.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(80)
                .withEndAction(() -> layoutProjectFilterToggle.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(new OvershootInterpolator())
                        .setDuration(180)
                        .start())
                .start();
    }

    private void loadOptionData() {
        customers.clear();
        areas.clear();
        projectTypes.clear();
        contractTypes.clear();

        contractTypes.add(new OptionItem("", "-- Chọn loại --"));
        contractTypes.add(new OptionItem("1", "Loại 1"));
        contractTypes.add(new OptionItem("2", "Loại 2"));
        contractTypes.add(new OptionItem("3", "Loại 3"));
        contractTypes.add(new OptionItem("4", "Loại 4"));

        customers.add(new OptionItem("", "-- Chọn khách hàng --"));

        Cursor cCus = db.rawQuery(
                "SELECT MAKH, IFNULL(LOAIKH, MAKH) FROM KHACHHANG ORDER BY MAKH",
                null
        );

        while (cCus.moveToNext()) {
            customers.add(new OptionItem(cCus.getString(0), cCus.getString(1)));
        }

        cCus.close();

        areas.add(new OptionItem("", "-- Chọn khu vực --"));

        Cursor cArea = db.rawQuery(
                "SELECT MAKV, TENTINH FROM DTTHEOKV ORDER BY TENTINH",
                null
        );

        while (cArea.moveToNext()) {
            areas.add(new OptionItem(cArea.getString(0), cArea.getString(1)));
        }

        cArea.close();

        projectTypes.add(new OptionItem("", "-- Chọn loại hình --"));

        Cursor cType = db.rawQuery(
                "SELECT MALH, TENLH FROM DTTHEOLHCT ORDER BY TENLH",
                null
        );

        while (cType.moveToNext()) {
            projectTypes.add(new OptionItem(cType.getString(0), cType.getString(1)));
        }

        cType.close();
    }

    private void setupFilterData() {
        searchModes.clear();
        filterContractTypes.clear();
        filterAreas.clear();
        statusFilters.clear();
        sortModes.clear();

        searchModes.add(new OptionItem("ALL", "Tìm tất cả"));
        searchModes.add(new OptionItem("PROJECT_CODE", "Theo mã dự án"));
        searchModes.add(new OptionItem("CONTRACT_CODE", "Theo mã hợp đồng"));
        searchModes.add(new OptionItem("NAME", "Theo tên dự án"));
        searchModes.add(new OptionItem("CUSTOMER", "Theo khách hàng"));
        searchModes.add(new OptionItem("AREA", "Theo khu vực"));
        searchModes.add(new OptionItem("PROJECT_TYPE", "Theo loại hình công trình"));

        filterContractTypes.add(new OptionItem("", "Tất cả loại hợp đồng"));
        filterContractTypes.add(new OptionItem("1", "Loại 1"));
        filterContractTypes.add(new OptionItem("2", "Loại 2"));
        filterContractTypes.add(new OptionItem("3", "Loại 3"));
        filterContractTypes.add(new OptionItem("4", "Loại 4"));

        filterAreas.add(new OptionItem("", "Tất cả khu vực"));

        for (OptionItem area : areas) {
            if (!area.id.isEmpty()) {
                filterAreas.add(area);
            }
        }

        statusFilters.add(new OptionItem("ALL", "Tất cả trạng thái"));
        statusFilters.add(new OptionItem("upcoming", "Sắp triển khai"));
        statusFilters.add(new OptionItem("ongoing", "Đang thực hiện"));
        statusFilters.add(new OptionItem("done", "Hoàn thành"));
        statusFilters.add(new OptionItem("ontrack", "Đúng tiến độ"));

        sortModes.add(new OptionItem("NAME_ASC", "Tên A → Z"));
        sortModes.add(new OptionItem("NAME_DESC", "Tên Z → A"));
        sortModes.add(new OptionItem("START_DESC", "Ngày bắt đầu mới → cũ"));
        sortModes.add(new OptionItem("START_ASC", "Ngày bắt đầu cũ → mới"));
        sortModes.add(new OptionItem("END_ASC", "Ngày kết thúc gần nhất"));
        sortModes.add(new OptionItem("TOTAL_DESC", "Tổng nghiệm thu cao → thấp"));
        sortModes.add(new OptionItem("EMP_DESC", "Nhân sự nhiều → ít"));
    }

    private void setupFilterSpinners() {
        spnProjectSearchMode.setAdapter(new WhiteSpinnerAdapter<>(searchModes));
        spnProjectTypeFilter.setAdapter(new WhiteSpinnerAdapter<>(filterContractTypes));
        spnProjectAreaFilter.setAdapter(new WhiteSpinnerAdapter<>(filterAreas));
        spnProjectStatusFilter.setAdapter(new WhiteSpinnerAdapter<>(statusFilters));
        spnProjectSort.setAdapter(new WhiteSpinnerAdapter<>(sortModes));

        spnProjectSearchMode.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnProjectTypeFilter.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnProjectAreaFilter.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnProjectStatusFilter.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnProjectSort.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);

        spnProjectSearchMode.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSearchMode = searchModes.get(position).id;
            }
        });

        spnProjectTypeFilter.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedContractType = filterContractTypes.get(position).id;
            }
        });

        spnProjectAreaFilter.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedArea = filterAreas.get(position).id;
            }
        });

        spnProjectStatusFilter.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statusFilters.get(position).id;
            }
        });

        spnProjectSort.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSort = sortModes.get(position).id;
            }
        });
    }

    private void refreshFilterSpinnersKeepSelection() {
        String oldArea = selectedArea;

        setupFilterData();

        spnProjectAreaFilter.setAdapter(new WhiteSpinnerAdapter<>(filterAreas));
        spnProjectAreaFilter.setSelection(findOptionIndex(filterAreas, oldArea));
        selectedArea = oldArea == null ? "" : oldArea;
    }

    private void loadProjects() {
        layoutProjectList.removeAllViews();

        buildSummaryCard();

        ArrayList<ProjectItem> all = getFilteredProjects();

        if (all.isEmpty()) {
            TextView empty = makeText("Không có dự án phù hợp bộ lọc.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            layoutProjectList.addView(empty);
            return;
        }

        if (selectedContractType != null && !selectedContractType.trim().isEmpty()) {
            layoutProjectList.addView(makeSection(selectedContractType, all));
            return;
        }

        for (int i = 1; i <= 4; i++) {
            String typeCode = String.valueOf(i);
            ArrayList<ProjectItem> items = new ArrayList<>();

            for (ProjectItem item : all) {
                if (typeCode.equals(item.typeCode)) {
                    items.add(item);
                }
            }

            layoutProjectList.addView(makeSection(typeCode, items));
        }
    }

    private void buildSummaryCard() {
        LinearLayout card = makeSectionBase();

        card.addView(makeCardTitle("TỔNG QUAN DỰ ÁN"));

        int totalProject = getInt("SELECT COUNT(*) FROM DUANTHEOHOPDONG", null);
        int totalContract = getInt("SELECT COUNT(*) FROM HOPDONG", null);
        int totalEmployee = getInt("SELECT COUNT(*) FROM NVTHAMGIADA", null);

        double totalFinal = getDouble(
                "SELECT IFNULL(SUM(IFNULL(TIENNGHIEMTHU_TONG, 0)), 0) FROM DTDUAN",
                null
        );

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        row1.addView(makeStatBox("Dự án", String.valueOf(totalProject), "Tổng số", PRIMARY), halfLp(true));
        row1.addView(makeStatBox("Hợp đồng", String.valueOf(totalContract), "Đã tạo", BLUE), halfLp(false));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);

        row2.addView(makeStatBox("Lượt nhân sự", String.valueOf(totalEmployee), "Tham gia dự án", GREEN), halfLp(true));
        row2.addView(makeStatBox("Nghiệm thu", formatCompactMoney(totalFinal), "Tổng dự kiến", ORANGE), halfLp(false));

        card.addView(row1);
        card.addView(row2);

        layoutProjectList.addView(card);
    }

    private ArrayList<ProjectItem> getFilteredProjects() {
        ArrayList<ProjectItem> list = new ArrayList<>();

        String keyword = edtSearchProject == null ? "" : edtSearchProject.getText().toString().trim();

        StringBuilder sql = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        sql.append("SELECT DTHD.MADAHD, DTHD.MADA, DTHD.MAHD, ");
        sql.append("IFNULL(HD.TENHD, DTHD.MADAHD) AS TENHD, ");
        sql.append("IFNULL(HD.LOAI, '') AS LOAI, ");
        sql.append("IFNULL(HD.NGAYBD, '') AS NGAYBD, ");
        sql.append("IFNULL(DTHD.NGAYKT, '') AS NGAYKT, ");
        sql.append("IFNULL(KH.LOAIKH, '') AS KHACHHANG, ");
        sql.append("IFNULL(KV.TENTINH, '') AS KHUVUC, ");
        sql.append("IFNULL(LH.TENLH, '') AS LOAIHINH, ");
        sql.append("IFNULL(DA.TIENCOC, 0), ");
        sql.append("IFNULL(DA.TIENNGHIEMTHU_DUTINH, 0), ");
        sql.append("IFNULL(DT.HSTHAYDOI, 0), ");
        sql.append("IFNULL(DT.TIENNGHIEMTHU_TONG, 0), ");
        sql.append("(SELECT COUNT(*) FROM NVTHAMGIADA N WHERE N.MADAHD = DTHD.MADAHD) AS EMP_COUNT ");
        sql.append("FROM DUANTHEOHOPDONG DTHD ");
        sql.append("JOIN DUAN DA ON DTHD.MADA = DA.MADA ");
        sql.append("JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD ");
        sql.append("LEFT JOIN KHACHHANG KH ON DA.MAKH = KH.MAKH ");
        sql.append("LEFT JOIN DTDUAN DT ON DA.MADA = DT.MADA ");
        sql.append("LEFT JOIN DTTHEOKV KV ON DT.MAKV = KV.MAKV ");
        sql.append("LEFT JOIN DTTHEOLHCT LH ON DT.MALH = LH.MALH ");
        sql.append("WHERE 1 = 1 ");

        if (!keyword.isEmpty()) {
            if ("PROJECT_CODE".equals(selectedSearchMode)) {
                sql.append("AND DTHD.MADA LIKE ? ");
                args.add("%" + keyword + "%");

            } else if ("CONTRACT_CODE".equals(selectedSearchMode)) {
                sql.append("AND DTHD.MAHD LIKE ? ");
                args.add("%" + keyword + "%");

            } else if ("NAME".equals(selectedSearchMode)) {
                sql.append("AND IFNULL(HD.TENHD, '') LIKE ? ");
                args.add("%" + keyword + "%");

            } else if ("CUSTOMER".equals(selectedSearchMode)) {
                sql.append("AND IFNULL(KH.LOAIKH, '') LIKE ? ");
                args.add("%" + keyword + "%");

            } else if ("AREA".equals(selectedSearchMode)) {
                sql.append("AND IFNULL(KV.TENTINH, '') LIKE ? ");
                args.add("%" + keyword + "%");

            } else if ("PROJECT_TYPE".equals(selectedSearchMode)) {
                sql.append("AND IFNULL(LH.TENLH, '') LIKE ? ");
                args.add("%" + keyword + "%");

            } else {
                sql.append("AND (DTHD.MADAHD LIKE ? OR DTHD.MADA LIKE ? OR DTHD.MAHD LIKE ? ");
                sql.append("OR IFNULL(HD.TENHD, '') LIKE ? OR IFNULL(KH.LOAIKH, '') LIKE ? ");
                sql.append("OR IFNULL(KV.TENTINH, '') LIKE ? OR IFNULL(LH.TENLH, '') LIKE ?) ");

                for (int i = 0; i < 7; i++) {
                    args.add("%" + keyword + "%");
                }
            }
        }

        if (selectedContractType != null && !selectedContractType.trim().isEmpty()) {
            sql.append("AND TRIM(IFNULL(HD.LOAI, '')) = ? ");
            args.add(selectedContractType);
        }

        if (selectedArea != null && !selectedArea.trim().isEmpty()) {
            sql.append("AND CAST(DT.MAKV AS TEXT) = CAST(? AS TEXT) ");
            args.add(selectedArea);
        }

        sql.append(getSortSql());

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));

        while (c.moveToNext()) {
            ProjectItem item = new ProjectItem();

            item.projectKey = c.getString(0);
            item.projectCode = c.getString(1);
            item.contractCode = c.getString(2);
            item.projectName = c.getString(3);
            item.typeCode = c.getString(4);
            item.startDate = c.getString(5);
            item.endDate = c.getString(6);
            item.customerName = c.getString(7);
            item.areaName = c.getString(8);
            item.projectTypeName = c.getString(9);
            item.deposit = c.getDouble(10);
            item.expectedTotal = c.getDouble(11);
            item.coefficient = c.getDouble(12);
            item.finalTotal = c.getDouble(13);
            item.employeeCount = c.getInt(14);
            item.statusCode = getStatusCode(item.startDate, item.endDate);
            item.statusLabel = getStatusLabel(item.statusCode);

            if ("ALL".equals(selectedStatus) || selectedStatus.equals(item.statusCode)) {
                list.add(item);
            }
        }

        c.close();
        return list;
    }

    private String getSortSql() {
        if ("NAME_DESC".equals(selectedSort)) {
            return "ORDER BY TENHD COLLATE NOCASE DESC ";
        }

        if ("START_DESC".equals(selectedSort)) {
            return "ORDER BY IFNULL(HD.NGAYBD, '') DESC ";
        }

        if ("START_ASC".equals(selectedSort)) {
            return "ORDER BY IFNULL(HD.NGAYBD, '') ASC ";
        }

        if ("END_ASC".equals(selectedSort)) {
            return "ORDER BY IFNULL(DTHD.NGAYKT, '') ASC ";
        }

        if ("TOTAL_DESC".equals(selectedSort)) {
            return "ORDER BY IFNULL(DT.TIENNGHIEMTHU_TONG, 0) DESC ";
        }

        if ("EMP_DESC".equals(selectedSort)) {
            return "ORDER BY EMP_COUNT DESC, TENHD COLLATE NOCASE ASC ";
        }

        return "ORDER BY TENHD COLLATE NOCASE ASC ";
    }

    private View makeSection(String typeCode, ArrayList<ProjectItem> items) {
        LinearLayout section = makeSectionBase();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(10));

        TextView title = makeText(getTypeName(typeCode), 17, PRIMARY, true);
        TextView count = makeText(items.size() + " dự án", 12, SUB, false);
        count.setGravity(Gravity.RIGHT);

        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(count);

        section.addView(header);

        if (items.isEmpty()) {
            TextView empty = makeText("Chưa có dự án trong nhóm này.", 13, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(16), 0, dp(16));
            section.addView(empty);
        } else {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            for (ProjectItem item : items) {
                row.addView(makeProjectCard(item));
            }

            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            hsv.addView(row);

            section.addView(hsv);
        }

        return section;
    }

    private View makeProjectCard(ProjectItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.employee_input_bg);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(dp(286), LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, dp(10), 0);
        card.setLayoutParams(cardLp);

        card.setOnClickListener(v -> {
            animateClick(card);
            openDetail(item.projectKey);
        });

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView name = makeText(item.projectName, 15, TEXT, true);
        name.setSingleLine(false);
        titleBox.addView(name);

        TextView code = makeText(item.projectCode + " • HĐ: " + item.contractCode, 11, SUB, false);
        code.setPadding(0, dp(2), 0, 0);
        titleBox.addView(code);

        top.addView(titleBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView status = makeStatusPill(item.statusLabel, item.statusCode);
        top.addView(status);

        card.addView(top);

        card.addView(makeInfoLine("Khách hàng", emptyText(item.customerName)));
        card.addView(makeInfoLine("Khu vực", emptyText(item.areaName)));
        card.addView(makeInfoLine("Loại hình", emptyText(item.projectTypeName)));
        card.addView(makeInfoLine("Thời gian", formatDate(item.startDate) + " - " + formatDate(item.endDate)));
        card.addView(makeInfoLine("Nhân sự", item.employeeCount + " người"));
        card.addView(makeInfoLine("Cọc", formatCompactMoney(item.deposit)));
        card.addView(makeInfoLine("Nghiệm thu", formatCompactMoney(item.finalTotal)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(10), 0, 0);

        actions.addView(makeSmallButton("Chi tiết", PRIMARY, Color.BLACK, v -> openDetail(item.projectKey)));

        if ("upcoming".equals(item.statusCode)) {
            actions.addView(makeSmallButton("Xóa", Color.parseColor("#991B1B"), Color.WHITE, v -> showDeleteProjectDialog(item)));
        }

        card.addView(actions);

        return card;
    }

    private void openDetail(String projectKey) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        intent.putExtra("PROJECT_KEY", projectKey);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showCreateProjectDialog() {
        Dialog dialog = createStyledDialog("Dự án mới");

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        Spinner spnType = makeDarkSpinner();
        spnType.setAdapter(new WhiteSpinnerAdapter<>(contractTypes));

        EditText edtName = makeDarkInput("Tên dự án / hợp đồng");

        Spinner spnCustomer = makeDarkSpinner();
        spnCustomer.setAdapter(new WhiteSpinnerAdapter<>(customers));

        Spinner spnArea = makeDarkSpinner();
        spnArea.setAdapter(new WhiteSpinnerAdapter<>(areas));

        Spinner spnProjectType = makeDarkSpinner();
        spnProjectType.setAdapter(new WhiteSpinnerAdapter<>(projectTypes));

        EditText edtStart = makeDarkInput("Ngày bắt đầu yyyy-MM-dd");
        setupDateInput(edtStart);
        edtStart.setOnClickListener(v -> pickDate(edtStart));

        EditText edtEnd = makeDarkInput("Ngày kết thúc yyyy-MM-dd");
        setupDateInput(edtEnd);
        edtEnd.setOnClickListener(v -> pickDate(edtEnd));

        EditText edtDeposit = makeNumberInput("Tiền cọc");
        EditText edtExpected = makeNumberInput("Dự toán cơ sở / doanh thu dự kiến");
        EditText edtCoefficient = makeNumberInput("Hệ số điều chỉnh, trống = 1");
        EditText edtFinalTotal = makeNumberInput("Tổng nghiệm thu dự kiến tự tính");
        setupReadOnlyInput(edtFinalTotal);

        TextView formula = makeText(
                "Công thức: MAX(Dự toán cơ sở, Tiền cọc / tỷ lệ cọc loại) × hệ số loại × hệ số thời gian × hệ số khu vực × hệ số điều chỉnh.",
                12,
                PRIMARY,
                true
        );
        formula.setPadding(0, dp(6), 0, dp(8));

        Runnable updateTotal = () -> updateFinalTotalPreview(
                spnType,
                spnArea,
                edtStart,
                edtEnd,
                edtDeposit,
                edtExpected,
                edtCoefficient,
                edtFinalTotal
        );

        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateTotal.run();
            }
        };

        edtStart.addTextChangedListener(watcher);
        edtEnd.addTextChangedListener(watcher);
        edtDeposit.addTextChangedListener(watcher);
        edtExpected.addTextChangedListener(watcher);
        edtCoefficient.addTextChangedListener(watcher);

        spnType.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTotal.run();
            }
        });

        spnArea.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTotal.run();
            }
        });

        content.addView(makeDialogLabel("Loại hợp đồng / dự án"));
        content.addView(spnType);

        content.addView(makeDialogLabel("Tên dự án"));
        content.addView(edtName);

        content.addView(makeDialogLabel("Khách hàng"));
        content.addView(spnCustomer);

        content.addView(makeDialogLabel("Khu vực"));
        content.addView(spnArea);

        content.addView(makeDialogLabel("Loại hình công trình"));
        content.addView(spnProjectType);

        content.addView(makeDialogLabel("Ngày bắt đầu"));
        content.addView(edtStart);

        content.addView(makeDialogLabel("Ngày kết thúc"));
        content.addView(edtEnd);

        content.addView(makeDialogLabel("Tài chính"));
        content.addView(edtDeposit);
        content.addView(edtExpected);
        content.addView(edtCoefficient);
        content.addView(edtFinalTotal);
        content.addView(formula);

        TextView hint = makeText("Mã dự án và mã hợp đồng sẽ tự sinh sau khi lưu.", 12, SUB, false);
        hint.setPadding(0, dp(4), 0, dp(8));
        content.addView(hint);

        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(560)
        );
        scrollLp.setMargins(0, 0, 0, dp(8));

        currentDialogRoot.addView(scrollView, scrollLp);

        LinearLayout actions = makeDialogActions();
        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Tạo", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);
        currentDialogRoot.addView(actions);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            OptionItem type = (OptionItem) spnType.getSelectedItem();
            OptionItem customer = (OptionItem) spnCustomer.getSelectedItem();
            OptionItem area = (OptionItem) spnArea.getSelectedItem();
            OptionItem projectType = (OptionItem) spnProjectType.getSelectedItem();

            String name = edtName.getText().toString().trim();
            String start = edtStart.getText().toString().trim();
            String end = edtEnd.getText().toString().trim();

            if (type == null || type.id.isEmpty()) {
                toast("Vui lòng chọn loại dự án.");
                return;
            }

            if (name.isEmpty()) {
                toast("Vui lòng nhập tên dự án.");
                return;
            }

            if (customer == null || customer.id.isEmpty()) {
                toast("Vui lòng chọn khách hàng.");
                return;
            }

            if (area == null || area.id.isEmpty()) {
                toast("Vui lòng chọn khu vực.");
                return;
            }

            if (projectType == null || projectType.id.isEmpty()) {
                toast("Vui lòng chọn loại hình công trình.");
                return;
            }

            Date startDate = parseDate(start);
            Date endDate = parseDate(end);

            if (startDate == null) {
                toast("Vui lòng chọn ngày bắt đầu.");
                return;
            }

            if (endDate == null) {
                toast("Vui lòng chọn ngày kết thúc.");
                return;
            }

            if (endDate.before(startDate)) {
                toast("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu.");
                return;
            }

            double deposit = parseDoubleSafe(edtDeposit.getText().toString());
            double expected = parseDoubleSafe(edtExpected.getText().toString());
            double coefficient = parseDoubleSafe(edtCoefficient.getText().toString());

            if (deposit < 0 || expected < 0 || coefficient < 0) {
                toast("Thông tin tài chính không được âm.");
                return;
            }

            double finalTotal = calculateFinalTotal(type.id, area.id, start, end, deposit, expected, coefficient);

            String projectKey = createProject(
                    type.id,
                    name,
                    customer.id,
                    area.id,
                    projectType.id,
                    start,
                    end,
                    deposit,
                    expected,
                    coefficient <= 0 ? 1 : coefficient,
                    finalTotal
            );
            ActivityLogger.log(db, "CREATE_PROJECT", "USER", "Đã tạo dự án: " + projectKey);
            dialog.dismiss();

            if (projectKey != null) {
                openDetail(projectKey);
                ActivityLogger.log(db, "OPEN_PROJECT_DETAIL", "USER", "Mở chi tiết dự án: " + projectKey);
            }
        });
        updateTotal.run();
        dialog.show();
    }

    private void updateFinalTotalPreview(
            Spinner spnType,
            Spinner spnArea,
            EditText edtStart,
            EditText edtEnd,
            EditText edtDeposit,
            EditText edtExpected,
            EditText edtCoefficient,
            EditText edtFinalTotal
    ) {
        try {
            OptionItem type = (OptionItem) spnType.getSelectedItem();
            OptionItem area = (OptionItem) spnArea.getSelectedItem();

            if (type == null || area == null || type.id.isEmpty() || area.id.isEmpty()) {
                edtFinalTotal.setText("");
                return;
            }

            double result = calculateFinalTotal(
                    type.id,
                    area.id,
                    edtStart.getText().toString().trim(),
                    edtEnd.getText().toString().trim(),
                    parseDoubleSafe(edtDeposit.getText().toString()),
                    parseDoubleSafe(edtExpected.getText().toString()),
                    parseDoubleSafe(edtCoefficient.getText().toString())
            );

            edtFinalTotal.setText(formatCompactMoney(result));

        } catch (Exception e) {
            edtFinalTotal.setText("");
        }
    }

    private double calculateFinalTotal(
            String typeCode,
            String areaCode,
            String start,
            String end,
            double deposit,
            double expected,
            double coefficient
    ) {
        double depositRate = getDepositRate(typeCode);
        double baseFromDeposit = deposit > 0 && depositRate > 0 ? deposit / depositRate : 0;
        double base = Math.max(expected, baseFromDeposit);

        if (base <= 0) {
            base = 0;
        }

        double typeFactor = getContractTypeFactor(typeCode);
        double durationFactor = getDurationFactor(start, end);
        double areaFactor = getAreaFactor(areaCode);
        double adjustFactor = coefficient > 0 ? coefficient : 1.0;

        return base * typeFactor * durationFactor * areaFactor * adjustFactor;
    }

    private double getDepositRate(String typeCode) {
        switch ((typeCode == null ? "" : typeCode).trim()) {
            case "1":
                return 0.30;
            case "2":
                return 0.25;
            case "3":
                return 0.20;
            case "4":
                return 0.15;
            default:
                return 0.20;
        }
    }

    private double getContractTypeFactor(String typeCode) {
        switch ((typeCode == null ? "" : typeCode).trim()) {
            case "1":
                return 1.05;
            case "2":
                return 1.10;
            case "3":
                return 1.15;
            case "4":
                return 1.20;
            default:
                return 1.00;
        }
    }

    private double getDurationFactor(String start, String end) {
        Date s = parseDate(start);
        Date e = parseDate(end);

        if (s == null || e == null || e.before(s)) {
            return 1.00;
        }

        long diff = e.getTime() - s.getTime();
        long days = diff / (1000L * 60L * 60L * 24L) + 1;

        if (days <= 30) {
            return 1.00;
        }

        if (days <= 90) {
            return 1.05;
        }

        if (days <= 180) {
            return 1.10;
        }

        return 1.15;
    }

    private double getAreaFactor(String areaCode) {
        if (areaCode == null || areaCode.trim().isEmpty()) {
            return 1.0;
        }

        Cursor c = db.rawQuery(
                "SELECT IFNULL(HESOKV, 0) FROM DTTHEOKV WHERE CAST(MAKV AS TEXT) = CAST(? AS TEXT)",
                new String[]{areaCode}
        );

        double factor = 1.0;

        if (c.moveToFirst()) {
            double value = c.getDouble(0);

            if (value > 0) {
                factor = value;
            }
        }

        c.close();
        return factor;
    }

    private String createProject(
            String typeCode,
            String projectName,
            String customerId,
            String areaCode,
            String loaiHinhCode,
            String startDate,
            String endDate,
            double deposit,
            double expectedTotal,
            double coefficient,
            double finalTotal
    ) {
        String contractCode = generateContractCode();
        String projectCode = buildProjectCode(typeCode, contractCode);
        String projectKey = projectCode;

        db.beginTransaction();

        try {
            ContentValues hopDong = new ContentValues();
            hopDong.put("MAHD", contractCode);
            hopDong.put("TENHD", projectName);
            hopDong.put("LOAI", typeCode);
            putNullableString(hopDong, "NGAYBD", startDate);
            putNullableString(hopDong, "NGAYKT_DUTINH", endDate);

            if (db.insert("HOPDONG", null, hopDong) == -1) {
                toast("Thêm hợp đồng thất bại.");
                return null;
            }

            ContentValues duAn = new ContentValues();
            duAn.put("MADA", projectCode);
            duAn.put("MAKH", customerId);
            duAn.put("TIENCOC", deposit);
            duAn.put("TIENNGHIEMTHU_DUTINH", expectedTotal);

            if (db.insert("DUAN", null, duAn) == -1) {
                toast("Thêm dự án thất bại.");
                return null;
            }

            ContentValues dthd = new ContentValues();
            dthd.put("MADAHD", projectKey);
            dthd.put("MADA", projectCode);
            dthd.put("MAHD", contractCode);
            putNullableString(dthd, "NGAYKT", endDate);

            if (db.insert("DUANTHEOHOPDONG", null, dthd) == -1) {
                toast("Thêm dự án theo hợp đồng thất bại.");
                return null;
            }

            ContentValues dt = new ContentValues();
            dt.put("MADA", projectCode);
            dt.put("MAKV", areaCode);
            dt.put("MALH", loaiHinhCode);
            dt.put("HSTHAYDOI", coefficient);
            dt.put("TIENNGHIEMTHU_TONG", finalTotal);

            if (db.insert("DTDUAN", null, dt) == -1) {
                toast("Thêm đối tượng dự án thất bại.");
                return null;
            }

            db.setTransactionSuccessful();
            toast("Đã tạo dự án: " + projectKey);

        } catch (Exception e) {
            toast("Lỗi tạo dự án: " + e.getMessage());
            return null;
        } finally {
            db.endTransaction();
        }

        loadOptionData();
        refreshFilterSpinnersKeepSelection();
        loadProjects();

        return projectKey;
    }

    private void showDeleteProjectDialog(ProjectItem item) {
        if (!"upcoming".equals(item.statusCode)) {
            toast("Chỉ dự án sắp triển khai mới được xóa.");
            return;
        }

        Dialog dialog = createStyledDialog("Xóa dự án");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText(
                "Bạn có chắc muốn xóa dự án " + item.projectName + "?\nChỉ dự án sắp triển khai mới được xóa.",
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
            deleteProject(item.projectKey);
            dialog.dismiss();
        });
        ActivityLogger.log(db, "DELETE_PROJECT", "USER", "Đã xóa dự án: " + item.projectKey);
        dialog.show();
    }

    private void deleteProject(String projectKey) {
        String status = getProjectStatusByKey(projectKey);

        if (!"upcoming".equals(status)) {
            toast("Chỉ dự án sắp triển khai mới được xóa.");
            return;
        }

        Cursor c = db.rawQuery(
                "SELECT MADA, MAHD FROM DUANTHEOHOPDONG WHERE MADAHD = ?",
                new String[]{projectKey}
        );

        if (!c.moveToFirst()) {
            c.close();
            toast("Không tìm thấy dự án.");
            return;
        }

        String maDa = c.getString(0);
        String maHd = c.getString(1);
        c.close();

        db.beginTransaction();

        try {
            db.delete("NVTHAMGIADA", "MADAHD = ?", new String[]{projectKey});
            db.delete("DTDUAN", "MADA = ?", new String[]{maDa});
            db.delete("DUANTHEOHOPDONG", "MADAHD = ?", new String[]{projectKey});
            db.delete("DUAN", "MADA = ?", new String[]{maDa});

            Cursor cOther = db.rawQuery(
                    "SELECT 1 FROM DUANTHEOHOPDONG WHERE MAHD = ?",
                    new String[]{maHd}
            );

            boolean hasOther = cOther.moveToFirst();
            cOther.close();

            if (!hasOther) {
                db.delete("HOPDONG", "MAHD = ?", new String[]{maHd});
            }

            db.setTransactionSuccessful();
            toast("Đã xóa dự án.");

        } catch (Exception e) {
            toast("Không thể xóa dự án: " + e.getMessage());
        } finally {
            db.endTransaction();
        }

        loadProjects();
    }

    private String getProjectStatusByKey(String key) {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(HD.NGAYBD, ''), IFNULL(DTHD.NGAYKT, '') " +
                        "FROM DUANTHEOHOPDONG DTHD " +
                        "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                        "WHERE DTHD.MADAHD = ?",
                new String[]{key}
        );

        String status = "ontrack";

        if (c.moveToFirst()) {
            status = getStatusCode(c.getString(0), c.getString(1));
        }

        c.close();
        return status;
    }

    private TextView makeStatusPill(String text, String code) {
        int color = getStatusColor(code);

        TextView tv = makeText(text, 10, color, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(3), dp(8), dp(3));
        tv.setBackgroundResource(R.drawable.employee_tag_bg);

        return tv;
    }

    private LinearLayout makeInfoLine(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, 0);

        TextView l = makeText(label, 11, SUB, false);
        TextView v = makeText(value, 11, TEXT, true);
        v.setGravity(Gravity.RIGHT);
        v.setSingleLine(false);

        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
    }

    private LinearLayout makeSectionBase() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(14), dp(14), dp(14), dp(14));
        section.setBackgroundResource(R.drawable.hr_card_bg);

        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        sectionLp.setMargins(0, 0, 0, dp(14));
        section.setLayoutParams(sectionLp);

        return section;
    }

    private LinearLayout makeStatBox(String label, String value, String note, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackgroundResource(R.drawable.employee_input_bg);

        box.addView(makeText(label, 11, SUB, false));

        TextView valueText = makeText(value, 18, color, true);
        valueText.setSingleLine(false);
        box.addView(valueText);

        box.addView(makeText(note, 10, SUB, false));

        return box;
    }

    private LinearLayout.LayoutParams halfLp(boolean left) {
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

    private EditText makeNumberInput(String hint) {
        EditText edt = makeDarkInput(hint);
        edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
            super(ProjectActivity.this, android.R.layout.simple_spinner_item, data);
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
            TextView tv = new TextView(ProjectActivity.this);
            tv.setText(item == null ? "" : item.toString());
            tv.setTextSize(15);
            tv.setSingleLine(false);
            tv.setTextColor(Color.WHITE);
            return tv;
        }
    }

    private void pickDate(EditText target) {
        Calendar cal = Calendar.getInstance();

        Date current = parseDate(target.getText().toString().trim());

        if (current != null) {
            cal.setTime(current);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    target.setText(dbDateFormat.format(picked.getTime()));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void setupDateInput(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(true);
        editText.setInputType(InputType.TYPE_NULL);
    }

    private void setupReadOnlyInput(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(false);
        editText.setInputType(InputType.TYPE_NULL);
    }

    private String getStatusCode(String startStr, String endStr) {
        Date today = clearTime(new Date());
        Date start = parseDate(startStr);
        Date end = parseDate(endStr);

        if (start != null && today.before(start)) {
            return "upcoming";
        }

        if (end != null && today.after(end)) {
            return "done";
        }

        if (start != null && end != null && !today.before(start) && !today.after(end)) {
            return "ongoing";
        }

        return "ontrack";
    }

    private String getStatusLabel(String code) {
        switch ((code == null ? "" : code).toLowerCase()) {
            case "upcoming":
                return "Sắp triển khai";
            case "ongoing":
                return "Đang thực hiện";
            case "done":
                return "Hoàn thành";
            default:
                return "Đúng tiến độ";
        }
    }

    private int getStatusColor(String code) {
        switch ((code == null ? "" : code).toLowerCase()) {
            case "upcoming":
                return GREEN;
            case "ongoing":
                return BLUE;
            case "done":
                return PURPLE;
            default:
                return PRIMARY;
        }
    }

    private String getTypeName(String typeCode) {
        switch ((typeCode == null ? "" : typeCode).trim()) {
            case "1":
                return "Loại 1";
            case "2":
                return "Loại 2";
            case "3":
                return "Loại 3";
            case "4":
                return "Loại 4";
            default:
                return "Khác";
        }
    }

    private String buildProjectCode(String typeCode, String contractCode) {
        typeCode = typeCode == null || typeCode.trim().isEmpty() ? "0" : typeCode.trim();

        if (typeCode.length() > 1) {
            typeCode = typeCode.substring(0, 1);
        }

        return typeCode + contractCode;
    }

    private String generateContractCode() {
        String code;

        do {
            code = randomLetters(3) + randomDigits(2);
        } while (contractCodeExists(code));

        return code;
    }

    private boolean contractCodeExists(String code) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM HOPDONG WHERE MAHD = ?",
                new String[]{code}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private String randomLetters(int length) {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }

        return sb.toString();
    }

    private String randomDigits(int length) {
        String digits = "0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(digits.charAt(random.nextInt(digits.length())));
        }

        return sb.toString();
    }

    private void putNullableString(ContentValues values, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            values.putNull(key);
        } else {
            values.put(key, value.trim());
        }
    }

    private Date parseDate(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return null;
            }

            return dbDateFormat.parse(text.trim());

        } catch (Exception e) {
            return null;
        }
    }

    private Date clearTime(Date date) {
        try {
            return dbDateFormat.parse(dbDateFormat.format(date));
        } catch (Exception e) {
            return date;
        }
    }

    private String formatDate(String text) {
        try {
            Date d = parseDate(text);

            if (d == null) {
                return "?";
            }

            return viewDateFormat.format(d);

        } catch (Exception e) {
            return "?";
        }
    }

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    private LinearLayout makeDialogActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.RIGHT);
        actions.setPadding(0, dp(14), 0, 0);
        return actions;
    }

    private TextView makeDialogLabel(String text) {
        TextView tv = makeText(text, 13, SUB, true);
        tv.setPadding(0, dp(8), 0, dp(4));
        return tv;
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

    private Button makeSmallButton(String text, int bgColor, int textColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTextColor(textColor);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        btn.setOnClickListener(listener);
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

    private TextView makeCardTitle(String text) {
        TextView tv = makeText(text, 13, PRIMARY, true);
        tv.setPadding(0, 0, 0, dp(10));
        return tv;
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

    private String formatMoney(double number) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(number) + " VND";
    }

    private String formatCompactMoney(double number) {
        double abs = Math.abs(number);

        if (abs >= 1_000_000_000_000.0) {
            return trimDecimal(number / 1_000_000_000_000.0) + " nghìn tỷ";
        }

        if (abs >= 1_000_000_000.0) {
            return trimDecimal(number / 1_000_000_000.0) + " tỷ";
        }

        if (abs >= 1_000_000.0) {
            return trimDecimal(number / 1_000_000.0) + " triệu";
        }

        if (abs >= 1_000.0) {
            return trimDecimal(number / 1_000.0) + " nghìn";
        }

        return trimDecimal(number) + " VND";
    }

    private String trimDecimal(double value) {
        if (Math.abs(value - Math.round(value)) < 0.0001) {
            return String.valueOf((long) Math.round(value));
        }

        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private double parseDoubleSafe(String text) {
        try {
            text = text == null ? "" : text.trim();

            if (text.isEmpty()) {
                return 0;
            }

            return Double.parseDouble(text);

        } catch (Exception e) {
            return 0;
        }
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

    private double getDouble(String sql, String[] args) {
        Cursor c = db.rawQuery(sql, args);
        double value = 0;

        if (c.moveToFirst()) {
            value = c.getDouble(0);
        }

        c.close();
        return value;
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

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    abstract class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    static class ProjectItem {
        String projectKey;
        String projectCode;
        String contractCode;
        String projectName;
        String typeCode;
        String customerName;
        String areaName;
        String projectTypeName;
        String startDate;
        String endDate;
        int employeeCount;
        String statusCode;
        String statusLabel;
        double deposit;
        double expectedTotal;
        double coefficient;
        double finalTotal;
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
}