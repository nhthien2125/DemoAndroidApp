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

public class ProjectRevenueActivity extends AppCompatActivity {

    SQLiteDatabase db;

    Button btnExportExcel, btnExportPdf, btnLogoutProjectRevenue, btnApplyFilter, btnResetFilter;
    EditText edtFrom, edtTo, edtSearch;
    Spinner spGroupType;

    LinearLayout layoutKpiList, layoutProjectRevenueList;
    LinearLayout layoutFilterHeader, layoutFilterContent;
    LinearLayout layoutPageNumbers;

    TextView tvFilterArrow, tvPageInfo, tvChartTitle, tvChartNote;

    CombinedChartView combinedChart;

    String username = "";
    String auth = "";

    boolean filterExpanded = false;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int BLUE = Color.parseColor("#3B82F6");
    final int ORANGE = Color.parseColor("#F97316");

    final int PAGE_SIZE = 6;
    int currentPage = 1;
    int totalPages = 1;

    String currentGroup = "Theo tháng";

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    List<ProjectRevenueItem> filteredList = new ArrayList<>();
    Map<String, Double> currentChartRevenue = new LinkedHashMap<>();
    List<Double> currentMarketIndices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_revenue);

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
        setupGroupSpinner();

        edtFrom.setOnClickListener(v -> showDatePicker(edtFrom));
        edtTo.setOnClickListener(v -> showDatePicker(edtTo));

        btnLogoutProjectRevenue.setOnClickListener(v -> SessionManager.logout(this));
        btnApplyFilter.setOnClickListener(v -> loadData());
        btnResetFilter.setOnClickListener(v -> resetFilter());
        btnExportExcel.setOnClickListener(v -> exportExcel());
        btnExportPdf.setOnClickListener(v -> exportPdf());

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && db.isOpen()) {
            loadData();
        }
    }

    private void initViews() {
        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnLogoutProjectRevenue = findViewById(R.id.btnLogoutProjectRevenue);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnResetFilter = findViewById(R.id.btnResetFilter);

        edtFrom = findViewById(R.id.edtFrom);
        edtTo = findViewById(R.id.edtTo);
        edtSearch = findViewById(R.id.edtSearch);

        spGroupType = findViewById(R.id.spGroupType);

        layoutKpiList = findViewById(R.id.layoutKpiList);
        layoutProjectRevenueList = findViewById(R.id.layoutProjectRevenueList);

        layoutFilterHeader = findViewById(R.id.layoutFilterHeader);
        layoutFilterContent = findViewById(R.id.layoutFilterContent);
        tvFilterArrow = findViewById(R.id.tvFilterArrow);

        tvPageInfo = findViewById(R.id.tvPageInfo);
        layoutPageNumbers = findViewById(R.id.layoutPageNumbers);

        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvChartNote = findViewById(R.id.tvChartNote);

        combinedChart = findViewById(R.id.combinedChart);

        btnExportExcel.setBackgroundTintList(null);
        btnExportPdf.setBackgroundTintList(null);
        btnLogoutProjectRevenue.setBackgroundTintList(null);
        btnApplyFilter.setBackgroundTintList(null);
        btnResetFilter.setBackgroundTintList(null);

        layoutFilterContent.setVisibility(View.GONE);
        tvFilterArrow.setText("▼");

        layoutFilterHeader.setOnClickListener(v -> toggleFilter());
        tvFilterArrow.setOnClickListener(v -> toggleFilter());

        edtSearch.addTextChangedListener(new TextWatcher() {
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

    private void setupBottomNav() {
        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setupFinance(this, bottomNavContainer, BottomNav.FN_PROJECT_REVENUE);
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

    private void resetFilter() {
        setupDefaultDates();
        edtSearch.setText("");
        currentGroup = "Theo tháng";

        if (spGroupType.getAdapter() != null) {
            spGroupType.setSelection(1);
        }

        loadData();
    }

    private void setupGroupSpinner() {
        String[] groups = {
                "Theo ngày",
                "Theo tháng",
                "Theo năm"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                groups
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

        spGroupType.setAdapter(adapter);
        spGroupType.setSelection(1);

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

    private void showDatePicker(EditText edt) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(dateFormat.parse(edt.getText().toString().trim()));
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

                    edt.setText(date);
                    loadData();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void loadData() {
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Không mở được cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }

        String from = edtFrom.getText().toString().trim();
        String to = edtTo.getText().toString().trim();
        String search = edtSearch.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn đủ từ ngày và đến ngày.", Toast.LENGTH_SHORT).show();
            return;
        }

        filteredList = getProjectRevenueList(from, to, search);

        double totalRevenue = 0;

        for (ProjectRevenueItem item : filteredList) {
            totalRevenue += item.revenue;
        }

        int completedProjects = filteredList.size();
        double avgRevenue = completedProjects > 0 ? totalRevenue / completedProjects : 0;

        buildKpiCard(totalRevenue, completedProjects, avgRevenue);

        currentChartRevenue = getChartRevenue(from, to, search);
        currentMarketIndices = getMarketIndices(from, to);

        if (currentChartRevenue.isEmpty()) {
            currentChartRevenue.put("No data", 0.0);
        }

        fixMarketLineSize();

        if (currentMarketIndices.isEmpty()) {
            currentMarketIndices.add(1.0);
        }

        combinedChart.setData(currentChartRevenue, currentMarketIndices);

        tvChartTitle.setText("REVENUE CHART - " + currentGroup.toUpperCase());
        tvChartNote.setText("Từ " + from + " đến " + to + " • " + filteredList.size() + " dự án");

        currentPage = 1;
        buildProjectList();
        updatePaginationUI();
    }

    private void fixMarketLineSize() {
        while (currentMarketIndices.size() < currentChartRevenue.size()) {
            currentMarketIndices.add(1.0);
        }

        while (currentMarketIndices.size() > currentChartRevenue.size()) {
            currentMarketIndices.remove(currentMarketIndices.size() - 1);
        }
    }

    private List<ProjectRevenueItem> getProjectRevenueList(String from, String to, String search) {
        List<ProjectRevenueItem> list = new ArrayList<>();

        String where = "WHERE substr(IFNULL(DTHD.NGAYKT, ''), 1, 10) BETWEEN ? AND ?";
        ArrayList<String> args = new ArrayList<>();
        args.add(from);
        args.add(to);

        if (!search.isEmpty()) {
            where += " AND (HD.TENHD LIKE ? OR DTHD.MADAHD LIKE ?)";
            args.add("%" + search + "%");
            args.add("%" + search + "%");
        }

        Cursor c = null;

        try {
            String sql =
                    "SELECT IFNULL(HD.TENHD, DTHD.MADAHD), " +
                            "DTHD.MADAHD, " +
                            "IFNULL(DT.TIENNGHIEMTHU_TONG, 0), " +
                            "IFNULL(DTHD.NGAYKT, '') " +
                            "FROM DUANTHEOHOPDONG DTHD " +
                            "JOIN DTDUAN DT ON DTHD.MADA = DT.MADA " +
                            "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                            where + " " +
                            "ORDER BY DTHD.NGAYKT DESC";

            c = db.rawQuery(sql, args.toArray(new String[0]));

            while (c.moveToNext()) {
                ProjectRevenueItem item = new ProjectRevenueItem();

                item.projectName = c.getString(0);
                item.projectCode = c.getString(1);
                item.revenue = c.getDouble(2);
                item.endDate = c.getString(3);

                list.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tải danh sách doanh thu dự án.", Toast.LENGTH_SHORT).show();
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return list;
    }

    private Map<String, Double> getChartRevenue(String from, String to, String search) {
        Map<String, Double> data = new LinkedHashMap<>();

        String groupSelect;
        String groupOrder;

        if (currentGroup.equals("Theo ngày")) {
            groupSelect = "substr(DTHD.NGAYKT, 1, 10)";
            groupOrder = "substr(DTHD.NGAYKT, 1, 10)";
        } else if (currentGroup.equals("Theo năm")) {
            groupSelect = "strftime('%Y', DTHD.NGAYKT)";
            groupOrder = "strftime('%Y', DTHD.NGAYKT)";
        } else {
            groupSelect = "strftime('%m/%Y', DTHD.NGAYKT)";
            groupOrder = "strftime('%Y-%m', DTHD.NGAYKT)";
        }

        String where = "WHERE substr(IFNULL(DTHD.NGAYKT, ''), 1, 10) BETWEEN ? AND ?";
        ArrayList<String> args = new ArrayList<>();
        args.add(from);
        args.add(to);

        if (!search.isEmpty()) {
            where += " AND (HD.TENHD LIKE ? OR DTHD.MADAHD LIKE ?)";
            args.add("%" + search + "%");
            args.add("%" + search + "%");
        }

        Cursor c = null;

        try {
            String sql =
                    "SELECT " + groupSelect + " AS label, " +
                            "SUM(IFNULL(DT.TIENNGHIEMTHU_TONG, 0)) " +
                            "FROM DUANTHEOHOPDONG DTHD " +
                            "JOIN DTDUAN DT ON DTHD.MADA = DT.MADA " +
                            "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                            where + " " +
                            "GROUP BY label " +
                            "ORDER BY " + groupOrder;

            c = db.rawQuery(sql, args.toArray(new String[0]));

            while (c.moveToNext()) {
                String label = c.getString(0);
                double revenueBillion = c.getDouble(1) / 1_000_000_000.0;

                if (label != null && !label.trim().isEmpty()) {
                    data.put(label, revenueBillion);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return data;
    }

    private List<Double> getMarketIndices(String from, String to) {
        List<Double> list = new ArrayList<>();

        String groupSelect;
        String groupOrder;

        if (currentGroup.equals("Theo ngày")) {
            groupSelect = "substr(THOIGIAN, 1, 10)";
            groupOrder = "substr(THOIGIAN, 1, 10)";
        } else if (currentGroup.equals("Theo năm")) {
            groupSelect = "strftime('%Y', THOIGIAN)";
            groupOrder = "strftime('%Y', THOIGIAN)";
        } else {
            groupSelect = "strftime('%m/%Y', THOIGIAN)";
            groupOrder = "strftime('%Y-%m', THOIGIAN)";
        }

        Cursor c = null;

        try {
            String sql =
                    "SELECT " + groupSelect + " AS label, " +
                            "AVG(IFNULL(HSTHITRUONG, 0)) " +
                            "FROM BIENDONGTHITRUONG " +
                            "WHERE substr(IFNULL(THOIGIAN, ''), 1, 10) BETWEEN ? AND ? " +
                            "GROUP BY label " +
                            "ORDER BY " + groupOrder;

            c = db.rawQuery(sql, new String[]{from, to});

            while (c.moveToNext()) {
                list.add(c.getDouble(1));
            }

        } catch (Exception e) {
            e.printStackTrace();

            try {
                if (c != null) {
                    c.close();
                    c = null;
                }

                c = db.rawQuery(
                        "SELECT HSTHITRUONG FROM BIENDONGTHITRUONG ORDER BY THOIGIAN",
                        null
                );

                while (c.moveToNext()) {
                    list.add(c.getDouble(0));
                }

            } catch (Exception ignored) {

            }

        } finally {
            if (c != null) {
                c.close();
            }
        }

        return list;
    }

    private void buildKpiCard(double totalRevenue, int completedProjects, double avgRevenue) {
        layoutKpiList.removeAllViews();

        LinearLayout card = makeCard();
        card.addView(makeCardTitle("TỔNG QUAN DOANH THU DỰ ÁN"));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        row1.addView(
                makeStatBox(
                        "Total revenue",
                        formatCurrencyB(totalRevenue),
                        "Tổng doanh thu theo lọc",
                        PRIMARY
                ),
                halfLp(true)
        );

        row1.addView(
                makeStatBox(
                        "Projects",
                        String.valueOf(completedProjects),
                        "Số dự án trong lọc",
                        GREEN
                ),
                halfLp(false)
        );

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);

        row2.addView(
                makeStatBox(
                        "Average",
                        formatCurrencyB(avgRevenue),
                        "Trung bình / dự án",
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

    private void buildProjectList() {
        layoutProjectRevenueList.removeAllViews();

        LinearLayout card = makeCard();
        card.addView(makeCardTitle("DANH SÁCH DOANH THU DỰ ÁN"));

        if (filteredList.isEmpty()) {
            card.addView(makeEmptyText("Không có dữ liệu doanh thu dự án theo bộ lọc."));
            layoutProjectRevenueList.addView(card);
            return;
        }

        totalPages = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);

        if (totalPages == 0) {
            totalPages = 1;
        }

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredList.size());

        TextView note = makeText(
                "Hiển thị " + (start + 1) + " - " + end + " / " + filteredList.size() + " dự án.",
                12,
                SUB,
                false
        );
        note.setPadding(0, 0, 0, dp(10));
        card.addView(note);

        for (int i = start; i < end; i++) {
            ProjectRevenueItem item = filteredList.get(i);

            LinearLayout box = makeInnerBox();

            box.addView(makeText(emptyText(item.projectName), 15, TEXT, true));
            box.addView(makeSmallLine("Mã dự án", emptyText(item.projectCode)));
            box.addView(makeSmallLine("Doanh thu thực tế", formatCurrencyFull(item.revenue)));
            box.addView(makeSmallLine("Ngày kết thúc", emptyText(item.endDate)));

            card.addView(box);
        }

        layoutProjectRevenueList.addView(card);
    }

    private void updatePaginationUI() {
        layoutPageNumbers.removeAllViews();

        totalPages = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);

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
                buildProjectList();
                updatePaginationUI();
            });

            layoutPageNumbers.addView(tv);
        }
    }

    private void exportExcel() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để xuất Excel.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String[] columnNames = {
                    "STT",
                    "Tên dự án",
                    "Mã dự án",
                    "Doanh thu thực tế",
                    "Ngày kết thúc",
                    "Nhóm biểu đồ",
                    "Từ ngày",
                    "Đến ngày",
                    "Tìm kiếm"
            };

            MatrixCursor cursor = new MatrixCursor(columnNames);

            String from = edtFrom.getText().toString().trim();
            String to = edtTo.getText().toString().trim();
            String search = edtSearch.getText().toString().trim();

            int stt = 1;

            for (ProjectRevenueItem item : filteredList) {
                cursor.addRow(new Object[]{
                        stt,
                        emptyText(item.projectName),
                        emptyText(item.projectCode),
                        item.revenue,
                        emptyText(item.endDate),
                        currentGroup,
                        from,
                        to,
                        search.isEmpty() ? "Tất cả" : search
                });

                stt++;
            }

            ExcelExporter.exportCursorToExcel(
                    this,
                    cursor,
                    "Project_Revenue_Report",
                    columnNames
            );

            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xuất Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportPdf() {
        if (filteredList.isEmpty()) {
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
        y += 28;

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
                "Tên dự án",
                "Mã dự án",
                "Doanh thu",
                "Ngày KT"
        };

        int[] widths = {
                34,
                310,
                110,
                150,
                120
        };

        y = drawPdfTableHeader(canvas, headerPaint, textPaint, margin, y, headers, widths);

        int stt = 1;

        for (ProjectRevenueItem item : filteredList) {
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

            canvas.drawText(cutText(emptyText(item.projectName), 42), x + 4, y + 5, textPaint);
            x += widths[1];

            canvas.drawText(cutText(emptyText(item.projectCode), 14), x + 4, y + 5, textPaint);
            x += widths[2];

            canvas.drawText(formatCurrencyShort(item.revenue), x + 4, y + 5, textPaint);
            x += widths[3];

            canvas.drawText(cutText(emptyText(item.endDate), 12), x + 4, y + 5, textPaint);

            y += 32;
            stt++;
        }

        document.finishPage(page);

        OutputStream outputStream = null;

        try {
            String fileName = "Project_Revenue_Report_" + System.currentTimeMillis() + ".pdf";

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
            if (combinedChart.getWidth() <= 0 || combinedChart.getHeight() <= 0) {
                return null;
            }

            Bitmap bitmap = Bitmap.createBitmap(
                    combinedChart.getWidth(),
                    combinedChart.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(bitmap);
            combinedChart.draw(canvas);

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
        canvas.drawText("PROJECT REVENUE REPORT", margin, y + 22, textPaint);

        textPaint.setColor(Color.parseColor("#CBD5F5"));
        textPaint.setTextSize(9);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Bao cao doanh thu du an theo bo loc hien tai", margin, y + 42, textPaint);
    }

    private void drawPdfFilter(Canvas canvas, Paint textPaint, int margin, int y) {
        textPaint.setColor(Color.parseColor("#111827"));
        textPaint.setTextSize(10);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Bo loc: " + cutText(getFilterDescription(), 110), margin, y, textPaint);
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
        return "From=" + edtFrom.getText().toString().trim()
                + ", To=" + edtTo.getText().toString().trim()
                + ", Group=" + currentGroup
                + ", Search=" + edtSearch.getText().toString().trim();
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

    private String formatCurrencyB(double amount) {
        return String.format(Locale.US, "$%.1fB", amount / 1_000_000_000.0);
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

    static class ProjectRevenueItem {
        String projectName;
        String projectCode;
        double revenue;
        String endDate;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}