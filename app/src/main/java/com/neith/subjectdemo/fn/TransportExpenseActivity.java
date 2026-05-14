package com.neith.subjectdemo.fn;

import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
import android.widget.FrameLayout;
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

public class TransportExpenseActivity extends AppCompatActivity {

    SQLiteDatabase db;

    EditText etSearch, edtFrom, edtTo, edtCostMin, edtCostMax;
    Spinner spFilter;
    Button btnExportExcel, btnExportPdf, btnLogoutTransport, btnApplyFilter, btnResetFilter;

    TextView tvTotalExpense, tvPageInfo, tvFilterArrow, tvChartTitle, tvChartNote;
    LinearLayout layoutExpenseList, layoutPageNumbers;
    LinearLayout layoutFilterHeader, layoutFilterContent;
    FrameLayout layoutChartContainer;

    WaterfallChartView waterfallChart;

    String username = "";
    String auth = "";

    boolean filterExpanded = false;

    int currentPage = 1;
    final int PAGE_SIZE = 6;
    int totalPages = 1;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int BLUE = Color.parseColor("#3B82F6");
    final int ORANGE = Color.parseColor("#F97316");
    final int RED = Color.parseColor("#EF4444");

    final String ALL = "Tất cả";

    String currentMaterial = ALL;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    List<TransportItem> allExpenses = new ArrayList<>();
    List<TransportItem> filteredExpenses = new ArrayList<>();
    Map<String, Double> currentChartData = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_expense);

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

        loadAllExpenses();
        setupSpinner();
        setupSearchAndFilter();

        btnLogoutTransport.setOnClickListener(v -> SessionManager.logout(this));
        btnExportExcel.setOnClickListener(v -> exportTransportExcel());
        btnExportPdf.setOnClickListener(v -> exportTransportPdf());
        btnApplyFilter.setOnClickListener(v -> applyAndRender(true));
        btnResetFilter.setOnClickListener(v -> resetFilter());

        applyAndRender(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && db.isOpen()) {
            loadAllExpenses();
            setupSpinner();
            applyAndRender(true);
        }
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        edtFrom = findViewById(R.id.edtFrom);
        edtTo = findViewById(R.id.edtTo);
        edtCostMin = findViewById(R.id.edtCostMin);
        edtCostMax = findViewById(R.id.edtCostMax);

        spFilter = findViewById(R.id.spFilter);

        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnLogoutTransport = findViewById(R.id.btnLogoutTransport);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnResetFilter = findViewById(R.id.btnResetFilter);

        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        tvFilterArrow = findViewById(R.id.tvFilterArrow);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvChartNote = findViewById(R.id.tvChartNote);

        layoutExpenseList = findViewById(R.id.layoutExpenseList);
        layoutPageNumbers = findViewById(R.id.layoutPageNumbers);

        layoutFilterHeader = findViewById(R.id.layoutFilterHeader);
        layoutFilterContent = findViewById(R.id.layoutFilterContent);

        layoutChartContainer = findViewById(R.id.layoutChartContainer);

        btnExportExcel.setBackgroundTintList(null);
        btnExportPdf.setBackgroundTintList(null);
        btnLogoutTransport.setBackgroundTintList(null);
        btnApplyFilter.setBackgroundTintList(null);
        btnResetFilter.setBackgroundTintList(null);

        layoutFilterContent.setVisibility(View.GONE);
        tvFilterArrow.setText("▼");

        layoutFilterHeader.setOnClickListener(v -> toggleFilter());
        tvFilterArrow.setOnClickListener(v -> toggleFilter());

        waterfallChart = new WaterfallChartView(this);
        layoutChartContainer.removeAllViews();
        layoutChartContainer.addView(
                waterfallChart,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
    }

    private void setupBottomNav() {
        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setupFinance(this, bottomNavContainer, BottomNav.FN_TRANSPORT_EXPENSE);
    }

    private void setupDefaultDates() {
        Calendar cal = Calendar.getInstance();

        edtTo.setText(dateFormat.format(cal.getTime()));

        cal.add(Calendar.MONTH, -24);
        edtFrom.setText(dateFormat.format(cal.getTime()));
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

    private void setupSpinner() {
        ArrayList<String> materials = new ArrayList<>();
        materials.add(ALL);

        for (TransportItem item : allExpenses) {
            if (item.material != null
                    && !item.material.trim().isEmpty()
                    && !materials.contains(item.material)) {
                materials.add(item.material);
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                materials
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

        spFilter.setAdapter(spinnerAdapter);

        if (currentMaterial == null || currentMaterial.trim().isEmpty()) {
            currentMaterial = ALL;
        }

        int selectedIndex = materials.indexOf(currentMaterial);

        if (selectedIndex >= 0) {
            spFilter.setSelection(selectedIndex);
        } else {
            currentMaterial = ALL;
            spFilter.setSelection(0);
        }
    }

    private void setupSearchAndFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyAndRender(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        edtCostMin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyAndRender(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        edtCostMax.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyAndRender(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        edtFrom.setOnClickListener(v -> showDatePicker(edtFrom));
        edtTo.setOnClickListener(v -> showDatePicker(edtTo));

        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMaterial = parent.getItemAtPosition(position).toString();

                if (first) {
                    first = false;
                    return;
                }

                applyAndRender(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
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
                    applyAndRender(true);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void resetFilter() {
        etSearch.setText("");
        edtCostMin.setText("");
        edtCostMax.setText("");
        setupDefaultDates();

        currentMaterial = ALL;

        if (spFilter.getAdapter() != null) {
            spFilter.setSelection(0);
        }

        applyAndRender(true);
    }

    private void loadAllExpenses() {
        allExpenses.clear();

        if (db == null || !db.isOpen()) {
            return;
        }

        Cursor c = null;

        try {
            String sql =
                    "SELECT IFNULL(V.NGAYVC, ''), " +
                            "IFNULL(V.MAVC, ''), " +
                            "IFNULL(N.TENNVL, 'N/A'), " +
                            "IFNULL(CP.MADA, 'N/A'), " +
                            "IFNULL(V.CPVC, 0) " +
                            "FROM VANCHUYENNVL V " +
                            "LEFT JOIN CPDUAN CP ON V.MAVC = CP.MAVC " +
                            "LEFT JOIN NHAPNVL N ON CP.MANHAP = N.MANHAP " +
                            "ORDER BY V.NGAYVC DESC";

            c = db.rawQuery(sql, null);

            while (c.moveToNext()) {
                TransportItem item = new TransportItem();

                item.date = normalizeDate(c.getString(0));
                item.transportCode = c.getString(1);
                item.material = c.getString(2);
                item.projectCode = c.getString(3);
                item.cost = c.getDouble(4);

                allExpenses.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tải dữ liệu vận chuyển.", Toast.LENGTH_SHORT).show();
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void applyAndRender(boolean resetPage) {
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Không mở được cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resetPage) {
            currentPage = 1;
        }

        applyFilter();

        double total = 0;

        for (TransportItem item : filteredExpenses) {
            total += item.cost;
        }

        tvTotalExpense.setText(formatCurrencyM(total));

        currentChartData = buildWaterfallData();
        waterfallChart.setData(currentChartData);

        tvChartTitle.setText("BIỂU ĐỒ THÁC NƯỚC - CHI PHÍ VẬN CHUYỂN");
        tvChartNote.setText(
                "Dữ liệu sau lọc: " + filteredExpenses.size()
                        + " dòng • Tổng: " + formatCurrencyFull(total)
        );

        buildExpenseList();
        updatePaginationUI();
    }

    private void applyFilter() {
        filteredExpenses.clear();

        String searchText = getText(etSearch).toLowerCase(Locale.ROOT);
        String from = getText(edtFrom);
        String to = getText(edtTo);

        boolean hasCostMin = !getText(edtCostMin).isEmpty();
        boolean hasCostMax = !getText(edtCostMax).isEmpty();

        double costMin = parseMoneyInput(edtCostMin);
        double costMax = parseMoneyInput(edtCostMax);

        for (TransportItem item : allExpenses) {
            String code = emptyText(item.transportCode).toLowerCase(Locale.ROOT);
            String material = emptyText(item.material).toLowerCase(Locale.ROOT);
            String project = emptyText(item.projectCode).toLowerCase(Locale.ROOT);
            String date = emptyText(item.date);

            boolean matchSearch = searchText.isEmpty()
                    || code.contains(searchText)
                    || material.contains(searchText)
                    || project.contains(searchText)
                    || date.toLowerCase(Locale.ROOT).contains(searchText);

            boolean matchMaterial = currentMaterial == null
                    || currentMaterial.equals(ALL)
                    || emptyText(item.material).equalsIgnoreCase(currentMaterial);

            boolean matchCost = true;

            if (hasCostMin && item.cost < costMin) {
                matchCost = false;
            }

            if (hasCostMax && item.cost > costMax) {
                matchCost = false;
            }

            boolean matchDate = true;

            if (date.length() >= 10) {
                if (!from.isEmpty() && date.compareTo(from) < 0) {
                    matchDate = false;
                }

                if (!to.isEmpty() && date.compareTo(to) > 0) {
                    matchDate = false;
                }
            }

            if (matchSearch && matchMaterial && matchCost && matchDate) {
                filteredExpenses.add(item);
            }
        }

        totalPages = (int) Math.ceil((double) filteredExpenses.size() / PAGE_SIZE);

        if (totalPages == 0) {
            totalPages = 1;
        }

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        if (currentPage < 1) {
            currentPage = 1;
        }
    }

    private Map<String, Double> buildWaterfallData() {
        Map<String, Double> map = new LinkedHashMap<>();

        for (TransportItem item : filteredExpenses) {
            String key = emptyText(item.material);

            if (!map.containsKey(key)) {
                map.put(key, 0.0);
            }

            map.put(key, map.get(key) + item.cost);
        }

        if (map.isEmpty()) {
            map.put("No data", 0.0);
        }

        return map;
    }

    private void buildExpenseList() {
        layoutExpenseList.removeAllViews();

        LinearLayout card = makeCard();
        card.addView(makeCardTitle("DANH SÁCH CHI PHÍ VẬN CHUYỂN"));

        if (filteredExpenses.isEmpty()) {
            card.addView(makeEmptyText("Không có dữ liệu chi phí vận chuyển theo bộ lọc."));
            layoutExpenseList.addView(card);
            return;
        }

        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredExpenses.size());

        TextView note = makeText(
                "Hiển thị " + (start + 1) + " - " + end + " / " + filteredExpenses.size() + " dòng sau lọc.",
                12,
                SUB,
                false
        );
        note.setPadding(0, 0, 0, dp(10));
        card.addView(note);

        for (int i = start; i < end; i++) {
            TransportItem item = filteredExpenses.get(i);

            LinearLayout box = makeInnerBox();

            box.addView(makeText("Mã VC: " + emptyText(item.transportCode), 15, TEXT, true));
            box.addView(makeSmallLine("Ngày vận chuyển", emptyText(item.date)));
            box.addView(makeSmallLine("Vật liệu", emptyText(item.material)));
            box.addView(makeSmallLine("Mã dự án", emptyText(item.projectCode)));
            box.addView(makeSmallLine("Chi phí", formatCurrencyFull(item.cost)));

            card.addView(box);
        }

        layoutExpenseList.addView(card);
    }

    private void updatePaginationUI() {
        layoutPageNumbers.removeAllViews();

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
                buildExpenseList();
                updatePaginationUI();
            });

            layoutPageNumbers.addView(tv);
        }
    }

    private void exportTransportExcel() {
        if (filteredExpenses.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để xuất Excel.", Toast.LENGTH_SHORT).show();
            return;
        }

        MatrixCursor cursor = null;

        try {
            String[] columns = {
                    "STT",
                    "Ngày vận chuyển",
                    "Mã VC",
                    "Vật liệu",
                    "Mã dự án",
                    "Chi phí",
                    "Bộ lọc vật liệu",
                    "Từ ngày",
                    "Đến ngày",
                    "Tìm kiếm",
                    "Khoảng chi phí",
                    "Biểu đồ"
            };

            cursor = new MatrixCursor(columns);

            String search = getText(etSearch);
            String costRange = getText(edtCostMin) + " - " + getText(edtCostMax);

            if (search.isEmpty()) {
                search = "Tất cả";
            }

            if (getText(edtCostMin).isEmpty() && getText(edtCostMax).isEmpty()) {
                costRange = "Tất cả";
            }

            int stt = 1;

            for (TransportItem item : filteredExpenses) {
                cursor.addRow(new Object[]{
                        stt,
                        emptyText(item.date),
                        emptyText(item.transportCode),
                        emptyText(item.material),
                        emptyText(item.projectCode),
                        item.cost,
                        currentMaterial == null ? ALL : currentMaterial,
                        getText(edtFrom),
                        getText(edtTo),
                        search,
                        costRange,
                        "Biểu đồ Thác nước"
                });

                stt++;
            }

            ExcelExporter.exportCursorToExcel(
                    this,
                    cursor,
                    "Report_Transport_Expenses",
                    columns
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

    private void exportTransportPdf() {
        if (filteredExpenses.isEmpty()) {
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
                "Ngày VC",
                "Mã VC",
                "Vật liệu",
                "Mã DA",
                "Chi phí"
        };

        int[] widths = {
                34,
                110,
                100,
                220,
                120,
                140
        };

        y = drawPdfTableHeader(canvas, headerPaint, textPaint, margin, y, headers, widths);

        int stt = 1;

        for (TransportItem item : filteredExpenses) {
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

            canvas.drawText(cutText(emptyText(item.date), 12), x + 4, y + 5, textPaint);
            x += widths[1];

            canvas.drawText(cutText(emptyText(item.transportCode), 12), x + 4, y + 5, textPaint);
            x += widths[2];

            canvas.drawText(cutText(emptyText(item.material), 30), x + 4, y + 5, textPaint);
            x += widths[3];

            canvas.drawText(cutText(emptyText(item.projectCode), 14), x + 4, y + 5, textPaint);
            x += widths[4];

            canvas.drawText(formatCurrencyShort(item.cost), x + 4, y + 5, textPaint);

            y += 32;
            stt++;
        }

        document.finishPage(page);

        OutputStream outputStream = null;

        try {
            String fileName = "Report_Transport_Expenses_" + System.currentTimeMillis() + ".pdf";

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
            if (waterfallChart.getWidth() <= 0 || waterfallChart.getHeight() <= 0) {
                return null;
            }

            Bitmap bitmap = Bitmap.createBitmap(
                    waterfallChart.getWidth(),
                    waterfallChart.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(bitmap);
            waterfallChart.draw(canvas);

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
        canvas.drawText("TRANSPORT EXPENSE REPORT", margin, y + 22, textPaint);

        textPaint.setColor(Color.parseColor("#CBD5F5"));
        textPaint.setTextSize(9);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Bao cao chi phi van chuyen theo bo loc hien tai", margin, y + 42, textPaint);
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
        return "Material=" + currentMaterial
                + ", From=" + getText(edtFrom)
                + ", To=" + getText(edtTo)
                + ", Search=" + getText(etSearch)
                + ", Cost=[" + getText(edtCostMin) + "-" + getText(edtCostMax) + "]"
                + ", Chart=Waterfall";
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

    private String formatCurrencyM(double amount) {
        return String.format(Locale.US, "$%.0fM", amount / 1_000_000.0);
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

    static class TransportItem {
        String date;
        String transportCode;
        String material;
        String projectCode;
        double cost;
    }

    public static class WaterfallChartView extends View {

        private Map<String, Double> data = new LinkedHashMap<>();

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final int green = Color.parseColor("#22C55E");
        private final int blue = Color.parseColor("#3B82F6");
        private final int yellow = Color.parseColor("#FFD700");
        private final int white = Color.parseColor("#E5E7EB");
        private final int sub = Color.parseColor("#CBD5F5");
        private final int grid = Color.parseColor("#334155");

        public WaterfallChartView(Context context) {
            super(context);
            setupPaints();
        }

        private void setupPaints() {
            textPaint.setColor(white);
            textPaint.setTextSize(24f);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            linePaint.setColor(grid);
            linePaint.setStrokeWidth(2f);
        }

        public void setData(Map<String, Double> data) {
            this.data = data == null ? new LinkedHashMap<>() : data;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int w = getWidth();
            int h = getHeight();

            paint.setColor(Color.parseColor("#111827"));
            canvas.drawRect(0, 0, w, h, paint);

            if (data == null || data.isEmpty()) {
                textPaint.setColor(sub);
                textPaint.setTextSize(26f);
                canvas.drawText("No data", 40, h / 2f, textPaint);
                return;
            }

            float left = 52f;
            float right = w - 28f;
            float top = 34f;
            float bottom = h - 54f;

            canvas.drawLine(left, bottom, right, bottom, linePaint);
            canvas.drawLine(left, top, left, bottom, linePaint);

            List<String> labels = new ArrayList<>(data.keySet());
            List<Double> values = new ArrayList<>();

            double cumulative = 0;
            double maxCumulative = 0;

            for (String label : labels) {
                double value = data.containsKey(label) ? data.get(label) : 0;
                values.add(value);
                cumulative += value;

                if (cumulative > maxCumulative) {
                    maxCumulative = cumulative;
                }
            }

            if (maxCumulative <= 0) {
                maxCumulative = 1;
            }

            int n = labels.size();
            float availableWidth = right - left;
            float slot = availableWidth / Math.max(n, 1);
            float barWidth = Math.min(slot * 0.55f, 58f);

            cumulative = 0;

            for (int i = 0; i < n; i++) {
                double value = values.get(i);
                double before = cumulative;
                cumulative += value;

                float xCenter = left + slot * i + slot / 2f;
                float barLeft = xCenter - barWidth / 2f;
                float barRight = xCenter + barWidth / 2f;

                float yBefore = bottom - (float) (before / maxCumulative) * (bottom - top);
                float yAfter = bottom - (float) (cumulative / maxCumulative) * (bottom - top);

                float rectTop = Math.min(yBefore, yAfter);
                float rectBottom = Math.max(yBefore, yAfter);

                paint.setColor(value >= 0 ? green : Color.parseColor("#EF4444"));
                RectF bar = new RectF(barLeft, rectTop, barRight, rectBottom);
                canvas.drawRoundRect(bar, 10f, 10f, paint);

                if (i < n - 1) {
                    float nextX = left + slot * (i + 1) + slot / 2f - barWidth / 2f;
                    linePaint.setColor(yellow);
                    linePaint.setStrokeWidth(2f);
                    canvas.drawLine(barRight, yAfter, nextX, yAfter, linePaint);
                    linePaint.setColor(grid);
                }

                textPaint.setColor(white);
                textPaint.setTextSize(18f);
                textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText(shortMoney(value), barLeft, rectTop - 8f, textPaint);

                textPaint.setColor(sub);
                textPaint.setTextSize(16f);
                textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                canvas.save();
                canvas.rotate(-25, xCenter - 8f, bottom + 28f);
                canvas.drawText(shortLabel(labels.get(i)), xCenter - 26f, bottom + 30f, textPaint);
                canvas.restore();
            }

            textPaint.setColor(yellow);
            textPaint.setTextSize(22f);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("Waterfall", left, top - 10f, textPaint);

            textPaint.setColor(blue);
            textPaint.setTextSize(20f);
            canvas.drawText("Total: " + shortMoney(cumulative), right - 170f, top - 10f, textPaint);
        }

        private String shortLabel(String label) {
            if (label == null) {
                return "";
            }

            if (label.length() <= 10) {
                return label;
            }

            return label.substring(0, 9) + "...";
        }

        private String shortMoney(double amount) {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}