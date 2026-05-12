package com.neith.subjectdemo.fn;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.fn.model.TransportExpense;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.ExcelExporter;
import com.neith.subjectdemo.helper.SessionManager;
import com.neith.subjectdemo.hr.EmployeeProfileActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransportExpenseActivity extends AppCompatActivity {

    private RecyclerView rvExpenses;
    private TransportExpenseAdapter adapter;
    private List<TransportExpense> expenseList = new ArrayList<>();
    private SQLiteDatabase db;
    private String username, auth, maNV;
    private EditText etSearch;
    private Spinner spFilter;
    private TextView tvTotalExpense, tvPageInfo;
    private LinearLayout layoutPageNumbers;

    private int currentPage = 1;
    private final int PAGE_SIZE = 10;
    private int totalPages = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_expense);

        db = DB.openDatabase(this);
        username = getIntent().getStringExtra("USERNAME");
        auth = getIntent().getStringExtra("AUTH");
        findMaNV();

        initViews();
        setupSearchAndFilter();
        loadTotalAndData();

        findViewById(R.id.ivMenu).setOnClickListener(this::showMenu);
        findViewById(R.id.btnExport).setOnClickListener(v -> exportTransportExpenses());
    }

    private void findMaNV() {
        if (auth != null) {
            try {
                Cursor c = db.rawQuery("SELECT CODE FROM CONFIRMAUTH WHERE AUTH = ?", new String[]{auth});
                if (c.moveToFirst()) maNV = c.getString(0);
                c.close();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void initViews() {
        rvExpenses = findViewById(R.id.rvExpenses);
        etSearch = findViewById(R.id.etSearch);
        spFilter = findViewById(R.id.spFilter);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        layoutPageNumbers = findViewById(R.id.layoutPageNumbers);

        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransportExpenseAdapter(expenseList);
        rvExpenses.setAdapter(adapter);

        String[] filters = {"Tất cả", "Thép", "Xi măng", "Gạch", "Ống PVC", "Cát"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilter.setAdapter(spinnerAdapter);
    }

    private void loadTotalAndData() {
        try {
            // Sửa lỗi: Sử dụng CPVC thay vì CPVANCHUYEN
            Cursor cSum = db.rawQuery("SELECT SUM(CPVC) FROM VANCHUYENNVL", null);
            if (cSum.moveToFirst()) {
                double total = cSum.getDouble(0);
                tvTotalExpense.setText(formatCurrencyM(total));
            }
            cSum.close();
        } catch (Exception e) { e.printStackTrace(); }

        loadPage(1);
    }

    private void loadPage(int page) {
        this.currentPage = page;
        String query = etSearch.getText().toString().trim();
        String filter = spFilter.getSelectedItem() != null ? spFilter.getSelectedItem().toString() : "Tất cả";

        String whereClause = "1=1";
        List<String> args = new ArrayList<>();
        if (!query.isEmpty()) {
            whereClause += " AND (V.MAVC LIKE ? OR N.TENNVL LIKE ?)";
            args.add("%" + query + "%");
            args.add("%" + query + "%");
        }
        if (!filter.equals("Tất cả")) {
            whereClause += " AND N.TENNVL LIKE ?";
            args.add("%" + filter + "%");
        }

        try {
            // Đếm tổng để phân trang
            String countSql = "SELECT COUNT(*) FROM VANCHUYENNVL V " +
                    "LEFT JOIN CPDUAN CP ON V.MAVC = CP.MAVC " +
                    "LEFT JOIN NHAPNVL N ON CP.MANHAP = N.MANHAP " +
                    "WHERE " + whereClause;

            Cursor cCount = db.rawQuery(countSql, args.toArray(new String[0]));
            int totalCount = 0;
            if (cCount.moveToFirst()) totalCount = cCount.getInt(0);
            cCount.close();

            totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            if (totalPages == 0) totalPages = 1;

            tvPageInfo.setText("Page " + currentPage + " / " + totalPages);
            updatePaginationUI();

            // Lấy dữ liệu JOIN để có MATERIAL và PROJECT
            int offset = (currentPage - 1) * PAGE_SIZE;
            String sql = "SELECT V.NGAYVC, V.MAVC, N.TENNVL, CP.MADA, V.CPVC " +
                    "FROM VANCHUYENNVL V " +
                    "LEFT JOIN CPDUAN CP ON V.MAVC = CP.MAVC " +
                    "LEFT JOIN NHAPNVL N ON CP.MANHAP = N.MANHAP " +
                    "WHERE " + whereClause + " LIMIT ? OFFSET ?";

            args.add(String.valueOf(PAGE_SIZE));
            args.add(String.valueOf(offset));

            expenseList.clear();
            Cursor c = db.rawQuery(sql, args.toArray(new String[0]));
            while (c.moveToNext()) {
                expenseList.add(new TransportExpense(
                        c.getString(0),
                        c.getString(1),
                        c.getString(2) != null ? c.getString(2) : "N/A",
                        c.getString(3) != null ? c.getString(3) : "N/A",
                        formatCurrencyK(c.getDouble(4))
                ));
            }
            c.close();
            adapter.notifyDataSetChanged();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updatePaginationUI() {
        layoutPageNumbers.removeAllViews();
        for (int i = 1; i <= totalPages; i++) {
            final int pageNum = i;
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(i));
            tv.setPadding(20, 10, 20, 10);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
            lp.setMargins(5, 0, 5, 0);
            tv.setLayoutParams(lp);

            if (i == currentPage) {
                tv.setBackgroundResource(R.drawable.pagination_active);
                tv.setTextColor(Color.WHITE);
            } else {
                tv.setTextColor(Color.parseColor("#8B949E"));
            }

            tv.setOnClickListener(v -> loadPage(pageNum));
            layoutPageNumbers.addView(tv);
        }
    }

    private void setupSearchAndFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { loadPage(1); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { loadPage(1); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.fn_main_menu, popup.getMenu());
        android.view.MenuItem profileItem = popup.getMenu().findItem(R.id.menu_user_profile);
        if (profileItem != null && username != null) profileItem.setTitle("Hello, " + username);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_user_profile) {
                if (maNV != null) {
                    Intent intent = new Intent(this, EmployeeProfileActivity.class);
                    intent.putExtra("MANV", maNV);
                    startActivity(intent);
                }
                return true;
            } else if (id == R.id.menu_logout) {
                SessionManager.logout(this);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void exportTransportExpenses() {
        String query = "SELECT V.NGAYVC, V.MAVC, N.TENNVL, CP.MADA, V.CPVC " +
                "FROM VANCHUYENNVL V " +
                "LEFT JOIN CPDUAN CP ON V.MAVC = CP.MAVC " +
                "LEFT JOIN NHAPNVL N ON CP.MANHAP = N.MANHAP";
        
        Cursor cursor = db.rawQuery(query, null);
        String[] columns = {"Ngày VC", "Mã VC", "Vật Liệu", "Mã Dự Án", "Chi Phí"};
        ExcelExporter.exportCursorToExcel(this, cursor, "Report_Transport_Expenses", columns);
        cursor.close();
    }

    private String formatCurrencyM(double amount) {
        return String.format(Locale.US, "$%.0fM", amount / 1000000.0);
    }

    private String formatCurrencyK(double amount) {
        return String.format(Locale.US, "$%.0fK", amount / 1000.0);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
