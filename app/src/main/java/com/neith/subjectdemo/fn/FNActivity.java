package com.neith.subjectdemo.fn;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
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
import com.neith.subjectdemo.admin.AdminHomeActivity;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.ExcelExporter;
import com.neith.subjectdemo.helper.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FNActivity extends AppCompatActivity {

    SQLiteDatabase db;

    Spinner spTypeFilter, spSortFilter;
    EditText edtSearchProject, edtRevenueMin, edtRevenueMax, edtCostMin, edtCostMax;
    Button btnExportExcel, btnExportPdf, btnLogoutFinance, btnApplyFilter, btnResetFilter, btnBackAdmin;

    LinearLayout layoutSummaryList, layoutProjectHighlights;
    LinearLayout layoutFilterHeader, layoutFilterContent;
    TextView tvFilterArrow;

    LinearLayout cardServiceRevenue, cardProjectRevenue, cardTotalEarning;

    String username = "";
    String auth = "";

    boolean filterExpanded = false;
    boolean openedFromAdmin = false;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int BLUE = Color.parseColor("#3B82F6");
    final int ORANGE = Color.parseColor("#F97316");
    final int RED = Color.parseColor("#EF4444");

    final String ALL = "Tất cả";

    List<ProjectItem> allProjects = new ArrayList<>();
    List<ProjectItem> filteredProjects = new ArrayList<>();

    String currentType = ALL;
    String currentSort = "Doanh thu cao nhất";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fn);

        db = DB.openDatabase(this);

        username = getIntent().getStringExtra("USERNAME");
        auth = getIntent().getStringExtra("AUTH");

        if (username == null || username.trim().isEmpty()) {
            username = getSharedPreferences("LOGIN_CACHE", MODE_PRIVATE).getString("USERNAME", "");
        }

        if (auth == null || auth.trim().isEmpty()) {
            auth = getSharedPreferences("LOGIN_CACHE", MODE_PRIVATE).getString("AUTH", "");
        }

        openedFromAdmin = getIntent().getBooleanExtra("FROM_ADMIN", false)
                || "admin".equalsIgnoreCase(auth);

        initViews();
        setupBottomNav();

        btnLogoutFinance.setOnClickListener(v -> SessionManager.logout(this));
        btnExportExcel.setOnClickListener(v -> exportFinanceExcel());
        btnExportPdf.setOnClickListener(v -> exportFinancePdf());
        btnApplyFilter.setOnClickListener(v -> applyProjectFilters());
        btnResetFilter.setOnClickListener(v -> resetFilters());

        if (btnBackAdmin != null) {
            btnBackAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(FNActivity.this, AdminHomeActivity.class);
                intent.putExtra("USERNAME", username);
                intent.putExtra("AUTH", auth);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        loadAllProjects();
        setupSpinners();
        applyProjectFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && db.isOpen()) {
            loadAllProjects();
            setupSpinners();
            applyProjectFilters();
        }
    }

    private void initViews() {
        spTypeFilter = findViewById(R.id.spTypeFilter);
        spSortFilter = findViewById(R.id.spSortFilter);

        edtSearchProject = findViewById(R.id.edtSearchProject);
        edtRevenueMin = findViewById(R.id.edtRevenueMin);
        edtRevenueMax = findViewById(R.id.edtRevenueMax);
        edtCostMin = findViewById(R.id.edtCostMin);
        edtCostMax = findViewById(R.id.edtCostMax);

        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnLogoutFinance = findViewById(R.id.btnLogoutFinance);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnResetFilter = findViewById(R.id.btnResetFilter);
        btnBackAdmin = findViewById(R.id.btnBackAdmin);

        layoutSummaryList = findViewById(R.id.layoutSummaryList);
        layoutProjectHighlights = findViewById(R.id.layoutProjectHighlights);

        layoutFilterHeader = findViewById(R.id.layoutFilterHeader);
        layoutFilterContent = findViewById(R.id.layoutFilterContent);
        tvFilterArrow = findViewById(R.id.tvFilterArrow);

        btnExportExcel.setBackgroundTintList(null);
        btnExportPdf.setBackgroundTintList(null);
        btnLogoutFinance.setBackgroundTintList(null);
        btnApplyFilter.setBackgroundTintList(null);
        btnResetFilter.setBackgroundTintList(null);

        if (btnBackAdmin != null) {
            btnBackAdmin.setBackgroundTintList(null);

            if (openedFromAdmin) {
                btnBackAdmin.setVisibility(View.VISIBLE);
            } else {
                btnBackAdmin.setVisibility(View.GONE);
            }
        }

        layoutFilterContent.setVisibility(View.GONE);
        tvFilterArrow.setText("▼");

        layoutFilterHeader.setOnClickListener(v -> toggleFilter());
        tvFilterArrow.setOnClickListener(v -> toggleFilter());

        addAutoFilter(edtSearchProject);
        addAutoFilter(edtRevenueMin);
        addAutoFilter(edtRevenueMax);
        addAutoFilter(edtCostMin);
        addAutoFilter(edtCostMax);
    }

    private void setupBottomNav() {
        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setupFinance(this, bottomNavContainer, BottomNav.FN_HOME);
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

    private void setupSpinners() {
        ArrayList<String> types = new ArrayList<>();
        types.add(ALL);

        for (ProjectItem item : allProjects) {
            if (item.type != null && !item.type.trim().isEmpty() && !types.contains(item.type)) {
                types.add(item.type);
            }
        }

        setupDarkSpinner(spTypeFilter, types);

        String[] sortValues = {
                "Doanh thu cao nhất",
                "Doanh thu thấp nhất",
                "Chi phí cao nhất",
                "Chi phí thấp nhất",
                "Lợi nhuận cao nhất",
                "Lợi nhuận thấp nhất",
                "Mới nhất"
        };

        setupDarkSpinner(spSortFilter, sortValues);

        spTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentType = parent.getItemAtPosition(position).toString();

                if (first) {
                    first = false;
                    return;
                }

                applyProjectFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spSortFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = parent.getItemAtPosition(position).toString();

                if (first) {
                    first = false;
                    return;
                }

                applyProjectFilters();
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

    private void addAutoFilter(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyProjectFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void loadAllProjects() {
        allProjects.clear();

        if (db == null || !db.isOpen()) {
            return;
        }

        Cursor c = null;

        try {
            String query =
                    "SELECT IFNULL(HD.TENHD, DTHD.MADAHD), " +
                            "DTHD.MADAHD, " +
                            "IFNULL(HD.LOAI, ''), " +
                            "IFNULL(DT.TIENNGHIEMTHU_TONG, 0) AS REVENUE, " +
                            "IFNULL((SELECT SUM(CHIPHITONG) FROM CPDUAN WHERE MADA = DTHD.MADA), 0) AS COST, " +
                            "IFNULL(HD.NGAYBD, ''), " +
                            "IFNULL(HD.NGAYKT_DUTINH, '') " +
                            "FROM DUANTHEOHOPDONG DTHD " +
                            "JOIN DUAN DA ON DTHD.MADA = DA.MADA " +
                            "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                            "LEFT JOIN DTDUAN DT ON DA.MADA = DT.MADA";

            c = db.rawQuery(query, null);

            while (c.moveToNext()) {
                ProjectItem item = new ProjectItem();

                item.projectName = c.getString(0);
                item.projectCode = c.getString(1);
                item.type = c.getString(2);
                item.revenue = c.getDouble(3);
                item.cost = c.getDouble(4);
                item.startDate = c.getString(5);
                item.endDate = c.getString(6);

                allProjects.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tải dữ liệu dự án.", Toast.LENGTH_SHORT).show();
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void applyProjectFilters() {
        filteredProjects.clear();

        if (allProjects == null) {
            return;
        }

        String search = getText(edtSearchProject).toLowerCase();

        double revenueMin = parseMoneyInput(edtRevenueMin);
        double revenueMax = parseMoneyInput(edtRevenueMax);
        double costMin = parseMoneyInput(edtCostMin);
        double costMax = parseMoneyInput(edtCostMax);

        boolean hasRevenueMin = !getText(edtRevenueMin).isEmpty();
        boolean hasRevenueMax = !getText(edtRevenueMax).isEmpty();
        boolean hasCostMin = !getText(edtCostMin).isEmpty();
        boolean hasCostMax = !getText(edtCostMax).isEmpty();

        for (ProjectItem item : allProjects) {
            String name = emptyText(item.projectName).toLowerCase();
            String code = emptyText(item.projectCode).toLowerCase();
            String type = emptyText(item.type).toLowerCase();

            boolean matchSearch = search.isEmpty()
                    || name.contains(search)
                    || code.contains(search)
                    || type.contains(search);

            boolean matchType = currentType == null
                    || currentType.equals(ALL)
                    || emptyText(item.type).equalsIgnoreCase(currentType);

            boolean matchRevenue = true;
            boolean matchCost = true;

            if (hasRevenueMin && item.revenue < revenueMin) {
                matchRevenue = false;
            }

            if (hasRevenueMax && item.revenue > revenueMax) {
                matchRevenue = false;
            }

            if (hasCostMin && item.cost < costMin) {
                matchCost = false;
            }

            if (hasCostMax && item.cost > costMax) {
                matchCost = false;
            }

            if (matchSearch && matchType && matchRevenue && matchCost) {
                filteredProjects.add(item);
            }
        }

        sortFilteredProjects();
        buildSummaryData();
        buildProjectHighlights();
    }

    private void sortFilteredProjects() {
        if (currentSort == null) {
            currentSort = "Doanh thu cao nhất";
        }

        if (currentSort.equals("Doanh thu thấp nhất")) {
            Collections.sort(filteredProjects, (a, b) -> Double.compare(a.revenue, b.revenue));
        } else if (currentSort.equals("Chi phí cao nhất")) {
            Collections.sort(filteredProjects, (a, b) -> Double.compare(b.cost, a.cost));
        } else if (currentSort.equals("Chi phí thấp nhất")) {
            Collections.sort(filteredProjects, (a, b) -> Double.compare(a.cost, b.cost));
        } else if (currentSort.equals("Lợi nhuận cao nhất")) {
            Collections.sort(filteredProjects, (a, b) -> Double.compare(b.getProfit(), a.getProfit()));
        } else if (currentSort.equals("Lợi nhuận thấp nhất")) {
            Collections.sort(filteredProjects, (a, b) -> Double.compare(a.getProfit(), b.getProfit()));
        } else if (currentSort.equals("Mới nhất")) {
            Collections.sort(filteredProjects, (a, b) -> emptyText(b.startDate).compareTo(emptyText(a.startDate)));
        } else {
            Collections.sort(filteredProjects, (a, b) -> Double.compare(b.revenue, a.revenue));
        }
    }

    private void resetFilters() {
        edtSearchProject.setText("");
        edtRevenueMin.setText("");
        edtRevenueMax.setText("");
        edtCostMin.setText("");
        edtCostMax.setText("");

        currentType = ALL;
        currentSort = "Doanh thu cao nhất";

        if (spTypeFilter.getAdapter() != null) {
            spTypeFilter.setSelection(0);
        }

        if (spSortFilter.getAdapter() != null) {
            spSortFilter.setSelection(0);
        }

        applyProjectFilters();
    }

    private void buildSummaryData() {
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Không mở được cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }

        layoutSummaryList.removeAllViews();

        double projectRev = getDouble(
                "SELECT IFNULL(SUM(TIENNGHIEMTHU_TONG), 0) FROM DTDUAN",
                null
        );

        SummaryService service = getServiceSummary();

        double serviceRev = service.totalRevenue;
        int serviceCount = service.count;

        double totalEarning = projectRev + serviceRev;

        LinearLayout card = makeCard();
        card.addView(makeCardTitle("TỔNG QUAN TÀI CHÍNH"));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        cardTotalEarning = makeStatBox(
                "Total earning",
                formatCurrency(totalEarning),
                "Tổng doanh thu",
                PRIMARY
        );

        cardProjectRevenue = makeStatBox(
                "From projects",
                formatCurrency(projectRev),
                "Project revenue",
                BLUE
        );

        row1.addView(cardTotalEarning, halfLp(true));
        row1.addView(cardProjectRevenue, halfLp(false));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);

        cardServiceRevenue = makeStatBox(
                "From services",
                formatCurrency(serviceRev),
                serviceCount + " transactions",
                GREEN
        );

        LinearLayout countBox = makeStatBox(
                "Projects",
                String.valueOf(filteredProjects.size()),
                "Sau bộ lọc",
                ORANGE
        );

        row2.addView(cardServiceRevenue, halfLp(true));
        row2.addView(countBox, halfLp(false));

        card.addView(row1);
        card.addView(row2);

        layoutSummaryList.addView(card);

        cardProjectRevenue.setOnClickListener(v -> {
            Intent intent = new Intent(FNActivity.this, ProjectRevenueActivity.class);
            putSession(intent);
            intent.putExtra("FROM_ADMIN", openedFromAdmin);
            startActivity(intent);
        });

        cardServiceRevenue.setOnClickListener(v -> {
            Intent intent = new Intent(FNActivity.this, ServiceRevenueActivity.class);
            putSession(intent);
            intent.putExtra("FROM_ADMIN", openedFromAdmin);
            startActivity(intent);
        });

        cardTotalEarning.setOnClickListener(v ->
                Toast.makeText(this, "Tổng doanh thu = dự án + dịch vụ", Toast.LENGTH_SHORT).show()
        );
    }

    private SummaryService getServiceSummary() {
        SummaryService item = new SummaryService();

        Cursor c = null;

        try {
            c = db.rawQuery(
                    "SELECT IFNULL(SUM(GIACATONG), 0), COUNT(*) FROM DTDICHVU",
                    null
            );

            if (c.moveToFirst()) {
                item.totalRevenue = c.getDouble(0);
                item.count = c.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return item;
    }

    private void buildProjectHighlights() {
        layoutProjectHighlights.removeAllViews();

        LinearLayout card = makeCard();
        card.addView(makeCardTitle("DANH SÁCH / PROJECT HIGHLIGHTS"));

        TextView note = makeText(
                "Hiển thị " + filteredProjects.size() + " / " + allProjects.size() + " dự án theo bộ lọc hiện tại.",
                12,
                SUB,
                false
        );
        note.setPadding(0, 0, 0, dp(10));
        card.addView(note);

        if (filteredProjects.isEmpty()) {
            card.addView(makeEmptyText("Không tìm thấy dự án phù hợp với bộ lọc."));
        } else {
            for (ProjectItem item : filteredProjects) {
                LinearLayout box = makeInnerBox();

                int profitColor = item.getProfit() >= 0 ? GREEN : RED;

                box.addView(makeText(emptyText(item.projectName), 15, TEXT, true));
                box.addView(makeSmallLine("Mã dự án", emptyText(item.projectCode)));
                box.addView(makeSmallLine("Loại hợp đồng", emptyText(item.type)));
                box.addView(makeSmallLine("Ngày bắt đầu", emptyText(item.startDate)));
                box.addView(makeSmallLine("Ngày kết thúc dự tính", emptyText(item.endDate)));
                box.addView(makeSmallLine("Doanh thu", formatCurrency(item.revenue)));
                box.addView(makeSmallLine("Chi phí", formatCurrency(item.cost)));

                LinearLayout profitRow = makeSmallLine("Lợi nhuận", formatCurrency(item.getProfit()));
                TextView profitValue = (TextView) profitRow.getChildAt(1);
                profitValue.setTextColor(profitColor);
                profitValue.setTypeface(null, Typeface.BOLD);
                box.addView(profitRow);

                card.addView(box);
            }
        }

        layoutProjectHighlights.addView(card);
    }

    private void exportFinanceExcel() {
        if (filteredProjects.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để xuất Excel.", Toast.LENGTH_SHORT).show();
            return;
        }

        MatrixCursor cursor = null;

        try {
            String[] columnNames = {
                    "STT",
                    "Tên dự án",
                    "Mã dự án",
                    "Loại",
                    "Ngày bắt đầu",
                    "Ngày kết thúc dự tính",
                    "Doanh thu",
                    "Chi phí",
                    "Lợi nhuận",
                    "Bộ lọc loại",
                    "Sắp xếp",
                    "Tìm kiếm",
                    "Khoảng doanh thu",
                    "Khoảng chi phí"
            };

            cursor = new MatrixCursor(columnNames);

            String search = getText(edtSearchProject);
            String revenueRange = getText(edtRevenueMin) + " - " + getText(edtRevenueMax);
            String costRange = getText(edtCostMin) + " - " + getText(edtCostMax);

            if (search.isEmpty()) {
                search = "Tất cả";
            }

            if (getText(edtRevenueMin).isEmpty() && getText(edtRevenueMax).isEmpty()) {
                revenueRange = "Tất cả";
            }

            if (getText(edtCostMin).isEmpty() && getText(edtCostMax).isEmpty()) {
                costRange = "Tất cả";
            }

            int stt = 1;

            for (ProjectItem item : filteredProjects) {
                cursor.addRow(new Object[]{
                        stt,
                        emptyText(item.projectName),
                        emptyText(item.projectCode),
                        emptyText(item.type),
                        emptyText(item.startDate),
                        emptyText(item.endDate),
                        item.revenue,
                        item.cost,
                        item.getProfit(),
                        currentType == null ? ALL : currentType,
                        currentSort == null ? "Doanh thu cao nhất" : currentSort,
                        search,
                        revenueRange,
                        costRange
                });

                stt++;
            }

            ExcelExporter.exportCursorToExcel(
                    this,
                    cursor,
                    "Finance_Project_Report",
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

    private void exportFinancePdf() {
        if (filteredProjects.isEmpty()) {
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
        y += 34;

        String[] headers = {
                "STT",
                "Tên dự án",
                "Loại",
                "Doanh thu",
                "Chi phí",
                "Lợi nhuận"
        };

        int[] widths = {
                34,
                250,
                110,
                120,
                120,
                120
        };

        y = drawPdfTableHeader(canvas, headerPaint, textPaint, margin, y, headers, widths);

        int stt = 1;

        for (ProjectItem item : filteredProjects) {
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

            canvas.drawText(cutText(emptyText(item.projectName), 34), x + 4, y + 5, textPaint);
            x += widths[1];

            canvas.drawText(cutText(emptyText(item.type), 15), x + 4, y + 5, textPaint);
            x += widths[2];

            canvas.drawText(formatCurrencyShort(item.revenue), x + 4, y + 5, textPaint);
            x += widths[3];

            canvas.drawText(formatCurrencyShort(item.cost), x + 4, y + 5, textPaint);
            x += widths[4];

            textPaint.setColor(item.getProfit() >= 0 ? Color.parseColor("#15803D") : Color.parseColor("#DC2626"));
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(formatCurrencyShort(item.getProfit()), x + 4, y + 5, textPaint);

            y += 32;
            stt++;
        }

        document.finishPage(page);

        OutputStream outputStream = null;

        try {
            String fileName = "Finance_Project_Report_" + System.currentTimeMillis() + ".pdf";

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
        canvas.drawText("FINANCE PROJECT REPORT", margin, y + 22, textPaint);

        textPaint.setColor(Color.parseColor("#CBD5F5"));
        textPaint.setTextSize(9);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Bao cao du an tai chinh theo bo loc hien tai", margin, y + 42, textPaint);
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
        return "Type=" + currentType
                + ", Sort=" + currentSort
                + ", Search=" + getText(edtSearchProject)
                + ", Revenue=[" + getText(edtRevenueMin) + "-" + getText(edtRevenueMax) + "]"
                + ", Cost=[" + getText(edtCostMin) + "-" + getText(edtCostMax) + "]";
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

    private void putSession(Intent intent) {
        intent.putExtra("USERNAME", username);
        intent.putExtra("AUTH", auth);
    }

    private double getDouble(String sql, String[] args) {
        Cursor c = null;
        double value = 0;

        try {
            c = db.rawQuery(sql, args);

            if (c.moveToFirst()) {
                value = c.getDouble(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
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
        if (amount >= 1_000_000_000) {
            return String.format(Locale.US, "$%.1fB", amount / 1_000_000_000.0);
        }

        if (amount >= 1_000_000) {
            return String.format(Locale.US, "$%.1fM", amount / 1_000_000.0);
        }

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

    static class ProjectItem {
        String projectName;
        String projectCode;
        String type;
        String startDate;
        String endDate;
        double revenue;
        double cost;

        double getProfit() {
            return revenue - cost;
        }
    }

    static class SummaryService {
        double totalRevenue;
        int count;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}