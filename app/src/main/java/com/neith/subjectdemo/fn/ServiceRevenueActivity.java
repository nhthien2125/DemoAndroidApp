package com.neith.subjectdemo.fn;

import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.ExcelExporter;
import com.neith.subjectdemo.helper.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServiceRevenueActivity extends AppCompatActivity {

    SQLiteDatabase db;

    Button btnExportExcel, btnExportPdf, btnLogoutServiceRevenue, btnApplyFilter, btnResetFilter;
    EditText edtFrom, edtTo, edtSearchService, edtAmountMin, edtAmountMax;
    Spinner spTypeFilter, spGroupType;

    LinearLayout layoutKpiList, layoutServiceRevenueList;
    LinearLayout layoutFilterHeader, layoutFilterContent;
    LinearLayout layoutPageNumbers;

    TextView tvFilterArrow, tvPageInfo, tvChartTitle, tvChartNote;

    DonutChartView donutChart;
    GridLayout glLegend;

    String username = "";
    String auth = "";

    boolean filterExpanded = false;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int BLUE = Color.parseColor("#3B82F6");
    final int ORANGE = Color.parseColor("#F97316");
    final int RED = Color.parseColor("#EF4444");
    final int PURPLE = Color.parseColor("#A855F7");

    final String ALL = "Tất cả";

    final int PAGE_SIZE = 6;
    int currentPage = 1;
    int totalPages = 1;

    String currentType = ALL;
    String currentGroup = "Theo tháng";

    String dateColumn = "";

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    List<ServiceRevenueItem> allServices = new ArrayList<>();
    List<ServiceRevenueItem> filteredServices = new ArrayList<>();

    Map<String, Double> currentChartData = new LinkedHashMap<>();
    Map<String, Integer> currentChartCount = new LinkedHashMap<>();

    final String[] colorPalette = {
            "#BD5E00",
            "#3B82F6",
            "#EC4899",
            "#EAB308",
            "#06B6D4",
            "#10B981",
            "#A855F7",
            "#F97316"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_revenue);

        db = DB.openDatabase(this);

        username = getIntent().getStringExtra("USERNAME");
        auth = getIntent().getStringExtra("AUTH");

        if (username == null || username.trim().isEmpty()) {
            username = getSharedPreferences("LOGIN_CACHE", MODE_PRIVATE).getString("USERNAME", "");
        }

        if (auth == null || auth.trim().isEmpty()) {
            auth = getSharedPreferences("LOGIN_CACHE", MODE_PRIVATE).getString("AUTH", "");
        }

        initViews();
        setupBottomNav();
        setupDefaultDates();

        dateColumn = findDateColumn();

        loadAllServices();
        setupSpinners();

        edtFrom.setOnClickListener(v -> showDatePicker(edtFrom));
        edtTo.setOnClickListener(v -> showDatePicker(edtTo));

        btnLogoutServiceRevenue.setOnClickListener(v -> SessionManager.logout(this));
        btnExportExcel.setOnClickListener(v -> exportServiceRevenueExcel());
        btnExportPdf.setOnClickListener(v -> exportServiceRevenuePdf());
        btnApplyFilter.setOnClickListener(v -> loadData());
        btnResetFilter.setOnClickListener(v -> resetFilter());

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && db.isOpen()) {
            dateColumn = findDateColumn();
            loadAllServices();
            setupSpinners();
            loadData();
        }
    }

    private void initViews() {
        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnLogoutServiceRevenue = findViewById(R.id.btnLogoutServiceRevenue);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnResetFilter = findViewById(R.id.btnResetFilter);

        edtFrom = findViewById(R.id.edtFrom);
        edtTo = findViewById(R.id.edtTo);
        edtSearchService = findViewById(R.id.edtSearchService);
        edtAmountMin = findViewById(R.id.edtAmountMin);
        edtAmountMax = findViewById(R.id.edtAmountMax);

        spTypeFilter = findViewById(R.id.spTypeFilter);
        spGroupType = findViewById(R.id.spGroupType);

        layoutKpiList = findViewById(R.id.layoutKpiList);
        layoutServiceRevenueList = findViewById(R.id.layoutServiceRevenueList);

        layoutFilterHeader = findViewById(R.id.layoutFilterHeader);
        layoutFilterContent = findViewById(R.id.layoutFilterContent);
        tvFilterArrow = findViewById(R.id.tvFilterArrow);

        layoutPageNumbers = findViewById(R.id.layoutPageNumbers);
        tvPageInfo = findViewById(R.id.tvPageInfo);

        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvChartNote = findViewById(R.id.tvChartNote);

        donutChart = findViewById(R.id.donutChart);
        glLegend = findViewById(R.id.glLegend);

        btnExportExcel.setBackgroundTintList(null);
        btnExportPdf.setBackgroundTintList(null);
        btnLogoutServiceRevenue.setBackgroundTintList(null);
        btnApplyFilter.setBackgroundTintList(null);
        btnResetFilter.setBackgroundTintList(null);

        layoutFilterContent.setVisibility(View.GONE);
        tvFilterArrow.setText("▼");

        layoutFilterHeader.setOnClickListener(v -> toggleFilter());
        tvFilterArrow.setOnClickListener(v -> toggleFilter());

        addAutoFilter(edtSearchService);
        addAutoFilter(edtAmountMin);
        addAutoFilter(edtAmountMax);
    }

    private void setupBottomNav() {
        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setupFinance(this, bottomNavContainer, BottomNav.FN_SERVICE_REVENUE);
    }

    private void toggleFilter() {
        filterExpanded = !filterExpanded;

        if (filterExpanded) {
            layoutFilterContent.setVisibility(View.VISIBLE);
            tvFilterArrow.setText("▲");
        } else {
            layoutFilterContent.setVisibility(View.GONE);
            tvFilterArrow.setText("▼");
        }
    }

    private void setupDefaultDates() {
        Calendar cal = Calendar.getInstance();

        edtTo.setText(dateFormat.format(cal.getTime()));

        cal.add(Calendar.MONTH, -12);
        edtFrom.setText(dateFormat.format(cal.getTime()));
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(dateFormat.parse(editText.getText().toString().trim()));
        } catch (Exception ignored) {

        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    editText.setText(date);
                    loadData();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void addAutoFilter(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadData();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void resetFilter() {
        setupDefaultDates();

        edtSearchService.setText("");
        edtAmountMin.setText("");
        edtAmountMax.setText("");

        currentType = ALL;
        currentGroup = "Theo tháng";

        if (spTypeFilter.getAdapter() != null) {
            spTypeFilter.setSelection(0);
        }

        if (spGroupType.getAdapter() != null) {
            spGroupType.setSelection(1);
        }

        currentPage = 1;
        loadData();
    }

    private void setupSpinners() {
        ArrayList<String> types = new ArrayList<>();
        types.add(ALL);

        for (ServiceRevenueItem item : allServices) {
            if (item.type != null && !item.type.trim().isEmpty() && !types.contains(item.type)) {
                types.add(item.type);
            }
        }

        setupDarkSpinner(spTypeFilter, types);

        String[] groups = {
                "Theo ngày",
                "Theo tháng",
                "Theo năm"
        };

        setupDarkSpinner(spGroupType, groups);
        spGroupType.setSelection(1);

        spTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentType = parent.getItemAtPosition(position).toString();

                if (first) {
                    first = false;
                    return;
                }

                currentPage = 1;
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spGroupType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentGroup = parent.getItemAtPosition(position).toString();

                if (first) {
                    first = false;
                    return;
                }

                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupDarkSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                values
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(TEXT);
                tv.setTextSize(13);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setGravity(Gravity.CENTER_VERTICAL);
                tv.setPadding(dp(12), 0, dp(12), 0);
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(13);
                tv.setBackgroundColor(Color.parseColor("#111827"));
                tv.setPadding(dp(14), dp(12), dp(14), dp(12));
                return tv;
            }
        };

        spinner.setAdapter(adapter);
    }

    private void setupDarkSpinner(Spinner spinner, ArrayList<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                values
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(TEXT);
                tv.setTextSize(13);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setGravity(Gravity.CENTER_VERTICAL);
                tv.setPadding(dp(12), 0, dp(12), 0);
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(13);
                tv.setBackgroundColor(Color.parseColor("#111827"));
                tv.setPadding(dp(14), dp(12), dp(14), dp(12));
                return tv;
            }
        };

        spinner.setAdapter(adapter);
    }

    private String findDateColumn() {
        if (db == null || !db.isOpen()) {
            return "";
        }

        Cursor c = null;

        try {
            c = db.rawQuery("PRAGMA table_info(DTDICHVU)", null);

            while (c.moveToNext()) {
                String name = c.getString(1);

                if (name == null) {
                    continue;
                }

                String lower = name.toLowerCase(Locale.ROOT);

                if (lower.equals("thoigian")
                        || lower.equals("ngay")
                        || lower.equals("ngaytao")
                        || lower.equals("ngaylap")
                        || lower.equals("ngaygd")
                        || lower.equals("ngaythanhtoan")
                        || lower.contains("ngay")
                        || lower.contains("date")
                        || lower.contains("time")) {
                    return name;
                }
            }

        } catch (Exception ignored) {

        } finally {
            if (c != null) {
                c.close();
            }
        }

        return "";
    }

    private void loadAllServices() {
        allServices.clear();

        if (db == null || !db.isOpen()) {
            return;
        }

        Cursor c = null;

        try {
            String sql;

            if (!dateColumn.isEmpty()) {
                String safeDateColumn = "\"" + dateColumn.replace("\"", "\"\"") + "\"";

                sql = "SELECT " +
                        "IFNULL(LHDICHVU, 'Khác') AS LOAI_DICH_VU, " +
                        "IFNULL(GIACATONG, 0) AS DOANH_THU, " +
                        "IFNULL(" + safeDateColumn + ", '') AS NGAY_DICH_VU " +
                        "FROM DTDICHVU " +
                        "ORDER BY " + safeDateColumn + " DESC";
            } else {
                sql = "SELECT " +
                        "IFNULL(LHDICHVU, 'Khác') AS LOAI_DICH_VU, " +
                        "IFNULL(GIACATONG, 0) AS DOANH_THU, " +
                        "'' AS NGAY_DICH_VU " +
                        "FROM DTDICHVU";
            }

            c = db.rawQuery(sql, null);

            while (c.moveToNext()) {
                ServiceRevenueItem item = new ServiceRevenueItem();

                item.type = c.getString(0);
                item.amount = c.getDouble(1);
                item.date = normalizeDate(c.getString(2));

                if (item.type == null || item.type.trim().isEmpty()) {
                    item.type = "Khác";
                }

                allServices.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tải doanh thu dịch vụ.", Toast.LENGTH_SHORT).show();
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void loadData() {
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Không mở được cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }

        applyFilter();

        double totalRevenue = 0;

        for (ServiceRevenueItem item : filteredServices) {
            totalRevenue += item.amount;
        }

        int orderCount = filteredServices.size();
        double avgValue = orderCount > 0 ? totalRevenue / orderCount : 0;

        buildKpiCard(totalRevenue, orderCount, avgValue);

        currentChartData = buildChartData();
        currentChartCount = buildChartCount();

        if (currentChartData.isEmpty()) {
            currentChartData.put("No data", 0.0);
        }

        donutChart.setData(currentChartData);
        setupLegend(currentChartData);

        tvChartTitle.setText("REVENUE CHART - " + currentGroup.toUpperCase());
        tvChartNote.setText("Từ " + getText(edtFrom) + " đến " + getText(edtTo)
                + " • " + filteredServices.size() + " dòng sau lọc");

        buildServiceList();
        updatePaginationUI();
    }

    private void applyFilter() {
        filteredServices.clear();

        String from = getText(edtFrom);
        String to = getText(edtTo);
        String search = getText(edtSearchService).toLowerCase(Locale.ROOT);

        boolean hasAmountMin = !getText(edtAmountMin).isEmpty();
        boolean hasAmountMax = !getText(edtAmountMax).isEmpty();

        double amountMin = parseMoneyInput(edtAmountMin);
        double amountMax = parseMoneyInput(edtAmountMax);

        for (ServiceRevenueItem item : allServices) {
            String type = emptyText(item.type).toLowerCase(Locale.ROOT);
            String date = emptyText(item.date).toLowerCase(Locale.ROOT);

            boolean matchSearch = search.isEmpty()
                    || type.contains(search)
                    || date.contains(search);

            boolean matchType = currentType == null
                    || currentType.equals(ALL)
                    || emptyText(item.type).equalsIgnoreCase(currentType);

            boolean matchAmount = true;

            if (hasAmountMin && item.amount < amountMin) {
                matchAmount = false;
            }

            if (hasAmountMax && item.amount > amountMax) {
                matchAmount = false;
            }

            boolean matchDate = true;

            if (!dateColumn.isEmpty() && item.date != null && item.date.length() >= 10) {
                if (!from.isEmpty() && item.date.compareTo(from) < 0) {
                    matchDate = false;
                }

                if (!to.isEmpty() && item.date.compareTo(to) > 0) {
                    matchDate = false;
                }
            }

            if (matchSearch && matchType && matchAmount && matchDate) {
                filteredServices.add(item);
            }
        }

        if (currentPage < 1) {
            currentPage = 1;
        }

        totalPages = (int) Math.ceil((double) filteredServices.size() / PAGE_SIZE);

        if (totalPages == 0) {
            totalPages = 1;
        }

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
    }

    private Map<String, Double> buildChartData() {
        Map<String, Double> map = new LinkedHashMap<>();

        for (ServiceRevenueItem item : filteredServices) {
            String label = getGroupLabel(item.date);

            if (!map.containsKey(label)) {
                map.put(label, 0.0);
            }

            map.put(label, map.get(label) + item.amount);
        }

        return map;
    }

    private Map<String, Integer> buildChartCount() {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (ServiceRevenueItem item : filteredServices) {
            String label = getGroupLabel(item.date);

            if (!map.containsKey(label)) {
                map.put(label, 0);
            }

            map.put(label, map.get(label) + 1);
        }

        return map;
    }

    private String getGroupLabel(String date) {
        String cleanDate = normalizeDate(date);

        if (cleanDate.isEmpty()) {
            return "Không ngày";
        }

        if (currentGroup.equals("Theo ngày")) {
            return cleanDate.length() >= 10 ? cleanDate.substring(0, 10) : cleanDate;
        }

        if (currentGroup.equals("Theo năm")) {
            return cleanDate.length() >= 4 ? cleanDate.substring(0, 4) : cleanDate;
        }

        if (cleanDate.length() >= 7) {
            return cleanDate.substring(5, 7) + "/" + cleanDate.substring(0, 4);
        }

        return cleanDate;
    }

    private void buildKpiCard(double totalRevenue, int orderCount, double avgValue) {
        layoutKpiList.removeAllViews();

        LinearLayout card = makeCard();
        card.addView(makeCardTitle("TỔNG QUAN DOANH THU DỊCH VỤ"));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        row1.addView(
                makeStatBox(
                        "Total revenue",
                        formatCurrency(totalRevenue),
                        "Tổng doanh thu sau lọc",
                        PRIMARY
                ),
                halfLp(true)
        );

        row1.addView(
                makeStatBox(
                        "Service orders",
                        String.valueOf(orderCount),
                        "Số dòng sau lọc",
                        GREEN
                ),
                halfLp(false)
        );

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);

        row2.addView(
                makeStatBox(
                        "Avg value",
                        formatCurrency(avgValue),
                        "Trung bình / dòng",
                        BLUE
                ),
                halfLp(true)
        );

        row2.addView(
                makeStatBox(
                        "Chart",
                        currentGroup,
                        "Kiểu biểu đồ hiện tại",
                        ORANGE
                ),
                halfLp(false)
        );

        card.addView(row1);
        card.addView(row2);

        layoutKpiList.addView(card);
    }

    private void buildServiceList() {
        layoutServiceRevenueList.removeAllViews();

        LinearLayout card = makeCard();
        card.addView(makeCardTitle("DANH SÁCH DOANH THU DỊCH VỤ"));

        if (filteredServices.isEmpty()) {
            card.addView(makeEmptyText("Không có dữ liệu doanh thu dịch vụ theo bộ lọc."));
            layoutServiceRevenueList.addView(card);
            return;
        }

        totalPages = (int) Math.ceil((double) filteredServices.size() / PAGE_SIZE);

        if (totalPages == 0) {
            totalPages = 1;
        }

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredServices.size());

        TextView note = makeText(
                "Hiển thị " + (start + 1) + " - " + end + " / " + filteredServices.size() + " dòng sau lọc.",
                12,
                SUB,
                false
        );
        note.setPadding(0, 0, 0, dp(10));
        card.addView(note);

        for (int i = start; i < end; i++) {
            ServiceRevenueItem item = filteredServices.get(i);

            LinearLayout box = makeInnerBox();

            box.addView(makeText(emptyText(item.type), 15, TEXT, true));
            box.addView(makeSmallLine("Doanh thu", formatCurrencyFull(item.amount)));
            box.addView(makeSmallLine("Ngày", emptyText(item.date)));
            box.addView(makeSmallLine("Nhóm biểu đồ", getGroupLabel(item.date)));

            card.addView(box);
        }

        layoutServiceRevenueList.addView(card);
    }

    private void updatePaginationUI() {
        layoutPageNumbers.removeAllViews();

        totalPages = (int) Math.ceil((double) filteredServices.size() / PAGE_SIZE);

        if (totalPages == 0) {
            totalPages = 1;
        }

        tvPageInfo.setText("Page " + currentPage + " / " + totalPages);

        for (int i = 1; i <= totalPages; i++) {
            final int pageNum = i;

            TextView tv = new TextView(this);
            tv.setText(String.valueOf(i));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(12);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(dp(8), dp(6), dp(8), dp(6));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(34),
                    dp(34)
            );

            lp.setMargins(dp(4), 0, dp(4), 0);
            tv.setLayoutParams(lp);

            if (i == currentPage) {
                tv.setBackgroundResource(R.drawable.pagination_active);
                tv.setTextColor(Color.WHITE);
            } else {
                tv.setTextColor(SUB);
            }

            tv.setOnClickListener(v -> {
                currentPage = pageNum;
                buildServiceList();
                updatePaginationUI();
            });

            layoutPageNumbers.addView(tv);
        }
    }

    private void setupLegend(Map<String, Double> data) {
        glLegend.removeAllViews();

        int i = 0;

        for (String key : data.keySet()) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(0, 0, dp(14), dp(10));

            View dot = new View(this);
            dot.setBackgroundColor(Color.parseColor(colorPalette[i % colorPalette.length]));

            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(12), dp(12));
            dotLp.setMargins(0, 0, dp(6), 0);

            item.addView(dot, dotLp);

            double amount = data.containsKey(key) ? data.get(key) : 0;
            int count = currentChartCount.containsKey(key) ? currentChartCount.get(key) : 0;

            TextView tv = makeText(
                    emptyText(key).toUpperCase() + " • " + formatCurrencyShort(amount) + " • " + count,
                    10,
                    SUB,
                    false
            );

            item.addView(tv);

            glLegend.addView(item);
            i++;
        }
    }

    private void exportServiceRevenueExcel() {
        if (filteredServices.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để xuất Excel.", Toast.LENGTH_SHORT).show();
            return;
        }

        MatrixCursor cursor = null;

        try {
            String[] columnNames = {
                    "STT",
                    "Loại dịch vụ",
                    "Doanh thu",
                    "Ngày",
                    "Nhóm biểu đồ",
                    "Bộ lọc loại",
                    "Từ ngày",
                    "Đến ngày",
                    "Tìm kiếm",
                    "Khoảng doanh thu"
            };

            cursor = new MatrixCursor(columnNames);

            String search = getText(edtSearchService);
            String amountRange = getText(edtAmountMin) + " - " + getText(edtAmountMax);

            if (search.isEmpty()) {
                search = "Tất cả";
            }

            if (getText(edtAmountMin).isEmpty() && getText(edtAmountMax).isEmpty()) {
                amountRange = "Tất cả";
            }

            int stt = 1;

            for (ServiceRevenueItem item : filteredServices) {
                cursor.addRow(new Object[]{
                        stt,
                        emptyText(item.type),
                        item.amount,
                        emptyText(item.date),
                        getGroupLabel(item.date),
                        currentType == null ? ALL : currentType,
                        getText(edtFrom),
                        getText(edtTo),
                        search,
                        amountRange
                });

                stt++;
            }

            ExcelExporter.exportCursorToExcel(
                    this,
                    cursor,
                    "Service_Revenue_Report",
                    columnNames
            );

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xuất XLSX: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void exportServiceRevenuePdf() {
        if (filteredServices.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để xuất PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();

        int pageWidth = 842;
        int pageHeight = 595;
        int margin = 32;
        int y = 40;

        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();
        Paint textPaint = new Paint();

        titlePaint.setColor(Color.parseColor("#FFD700"));
        titlePaint.setTextSize(20);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        headerPaint.setColor(Color.parseColor("#111827"));

        textPaint.setColor(Color.parseColor("#111827"));
        textPaint.setTextSize(10);

        PdfDocument.Page page = createPdfPage(document, pageWidth, pageHeight);
        Canvas canvas = page.getCanvas();

        drawPdfHeader(canvas, titlePaint, textPaint, margin, y);
        y += 72;

        drawPdfFilter(canvas, textPaint, margin, y);
        y += 30;

        Bitmap chartBitmap = captureChartBitmap();

        if (chartBitmap != null) {
            Paint chartBg = new Paint();
            chartBg.setColor(Color.parseColor("#111827"));
            canvas.drawRect(margin, y, pageWidth - margin, y + 210, chartBg);

            canvas.drawBitmap(
                    chartBitmap,
                    null,
                    new android.graphics.Rect(margin + 20, y + 16, pageWidth - margin - 20, y + 198),
                    null
            );

            y += 230;
        }

        String[] headers = {
                "STT",
                "Loại dịch vụ",
                "Doanh thu",
                "Ngày",
                "Nhóm"
        };

        int[] widths = {
                34,
                250,
                150,
                130,
                160
        };

        y = drawPdfTableHeader(canvas, headerPaint, textPaint, margin, y, headers, widths);

        int stt = 1;

        for (ServiceRevenueItem item : filteredServices) {
            if (y > pageHeight - 50) {
                document.finishPage(page);
                page = createPdfPage(document, pageWidth, pageHeight);
                canvas = page.getCanvas();

                y = 40;
                y = drawPdfTableHeader(canvas, headerPaint, textPaint, margin, y, headers, widths);
            }

            int x = margin;

            paint.setColor(stt % 2 == 0 ? Color.parseColor("#F9FAFB") : Color.WHITE);
            canvas.drawRect(margin, y - 14, pageWidth - margin, y + 18, paint);

            textPaint.setColor(Color.parseColor("#111827"));
            textPaint.setTextSize(10);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

            canvas.drawText(String.valueOf(stt), x + 4, y + 5, textPaint);
            x += widths[0];

            canvas.drawText(cutText(emptyText(item.type), 34), x + 4, y + 5, textPaint);
            x += widths[1];

            canvas.drawText(formatCurrencyShort(item.amount), x + 4, y + 5, textPaint);
            x += widths[2];

            canvas.drawText(cutText(emptyText(item.date), 14), x + 4, y + 5, textPaint);
            x += widths[3];

            canvas.drawText(cutText(getGroupLabel(item.date), 18), x + 4, y + 5, textPaint);

            y += 32;
            stt++;
        }

        document.finishPage(page);

        OutputStream outputStream = null;

        try {
            String fileName = "Service_Revenue_Report_" + System.currentTimeMillis() + ".pdf";

            outputStream = openDownloadOutputStream(
                    fileName,
                    "application/pdf"
            );

            if (outputStream == null) {
                Toast.makeText(this, "Không tạo được file PDF trong Download.", Toast.LENGTH_SHORT).show();
                document.close();
                return;
            }

            document.writeTo(outputStream);

            Toast.makeText(this, "Đã xuất PDF vào Download: " + fileName, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xuất PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception ignored) {

            }

            document.close();
        }
    }

    private Bitmap captureChartBitmap() {
        try {
            if (donutChart.getWidth() <= 0 || donutChart.getHeight() <= 0) {
                return null;
            }

            Bitmap bitmap = Bitmap.createBitmap(
                    donutChart.getWidth(),
                    donutChart.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(bitmap);
            donutChart.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private OutputStream openDownloadOutputStream(String fileName, String mimeType) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri == null) {
                    return null;
                }

                return resolver.openOutputStream(uri);
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                return new FileOutputStream(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private PdfDocument.Page createPdfPage(PdfDocument document, int pageWidth, int pageHeight) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        return document.startPage(pageInfo);
    }

    private void drawPdfHeader(Canvas canvas,
                               Paint titlePaint,
                               Paint textPaint,
                               int margin,
                               int y) {

        Paint bg = new Paint();
        bg.setColor(Color.BLACK);
        canvas.drawRect(0, 0, 842, 86, bg);

        canvas.drawText("TBT CENTER", margin, y, titlePaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(12);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SERVICE REVENUE REPORT", margin, y + 22, textPaint);

        textPaint.setColor(Color.parseColor("#CBD5F5"));
        textPaint.setTextSize(9);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Bao cao doanh thu dich vu theo bo loc hien tai", margin, y + 42, textPaint);
    }

    private void drawPdfFilter(Canvas canvas, Paint textPaint, int margin, int y) {
        textPaint.setColor(Color.parseColor("#111827"));
        textPaint.setTextSize(10);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Bo loc: " + cutText(getFilterDescription(), 115), margin, y, textPaint);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
    }

    private int drawPdfTableHeader(Canvas canvas,
                                   Paint headerPaint,
                                   Paint textPaint,
                                   int margin,
                                   int y,
                                   String[] headers,
                                   int[] widths) {

        int x = margin;

        canvas.drawRect(margin, y - 18, 842 - margin, y + 12, headerPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], x + 4, y + 2, textPaint);
            x += widths[i];
        }

        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        return y + 34;
    }

    private String getFilterDescription() {
        return "From=" + getText(edtFrom)
                + ", To=" + getText(edtTo)
                + ", Type=" + currentType
                + ", Group=" + currentGroup
                + ", Search=" + getText(edtSearchService)
                + ", Amount=[" + getText(edtAmountMin) + "-" + getText(edtAmountMax) + "]";
    }

    private String normalizeDate(String raw) {
        if (raw == null) {
            return "";
        }

        String value = raw.trim();

        if (value.length() >= 10) {
            return value.substring(0, 10);
        }

        return value;
    }

    private double parseMoneyInput(EditText editText) {
        String value = getText(editText);

        if (value.isEmpty()) {
            return 0;
        }

        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getText(EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }

        return editText.getText().toString().trim();
    }

    private String cutText(String text, int max) {
        if (text == null) {
            return "";
        }

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, max - 3) + "...";
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

    private LinearLayout makeInnerBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackgroundResource(R.drawable.employee_input_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);

        return box;
    }

    private TextView makeCardTitle(String text) {
        TextView tv = makeText(text, 13, PRIMARY, true);
        tv.setPadding(0, 0, 0, dp(10));
        return tv;
    }

    private LinearLayout makeSmallLine(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, 0);

        TextView l = makeText(label, 11, SUB, false);
        TextView v = makeText(value, 11, TEXT, false);

        v.setGravity(Gravity.RIGHT);
        v.setSingleLine(false);

        row.addView(
                l,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        row.addView(
                v,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        return row;
    }

    private LinearLayout makeStatBox(String label, String value, String note, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackgroundResource(R.drawable.employee_input_bg);
        box.setClickable(true);
        box.setFocusable(true);

        box.addView(makeText(label, 11, SUB, false));

        TextView tvValue = makeText(value, 18, color, true);
        tvValue.setPadding(0, dp(6), 0, dp(4));
        box.addView(tvValue);

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

    private TextView makeEmptyText(String text) {
        TextView tv = makeText(text, 14, SUB, false);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(18), 0, dp(18));
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

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")
                ? "—"
                : value;
    }

    private String formatCurrency(double amount) {
        if (amount >= 1_000_000_000 || amount <= -1_000_000_000) {
            return String.format(Locale.US, "$%.1fB", amount / 1_000_000_000.0);
        }

        if (amount >= 1_000_000 || amount <= -1_000_000) {
            return String.format(Locale.US, "$%.1fM", amount / 1_000_000.0);
        }

        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    private String formatCurrencyFull(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    private String formatCurrencyShort(double amount) {
        if (amount >= 1_000_000_000 || amount <= -1_000_000_000) {
            return String.format(Locale.US, "$%.1fB", amount / 1_000_000_000.0);
        }

        if (amount >= 1_000_000 || amount <= -1_000_000) {
            return String.format(Locale.US, "$%.1fM", amount / 1_000_000.0);
        }

        if (amount >= 1_000 || amount <= -1_000) {
            return String.format(Locale.US, "$%.1fK", amount / 1_000.0);
        }

        return String.format(Locale.US, "$%.0f", amount);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class ServiceRevenueItem {
        String type;
        double amount;
        String date;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}