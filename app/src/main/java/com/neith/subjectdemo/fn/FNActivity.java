package com.neith.subjectdemo.fn;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
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
import com.neith.subjectdemo.fn.model.ProjectHighlight;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.SessionManager;
import com.neith.subjectdemo.hr.EmployeeProfileActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FNActivity extends AppCompatActivity {

    private RecyclerView rvProjects;
    private ProjectHighlightAdapter adapter;
    private Spinner spFilter;
    private Button btnExport;
    private ImageView ivMenu;
    private TextView tvTotalEarning, tvProjectRevenue, tvServiceRevenue, tvServiceCount;
    private LinearLayout cardServiceRevenue, cardProjectRevenue, cardTotalEarning;
    private SQLiteDatabase db;

    private String username = "";
    private String auth = "";
    private String maNV = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fn);

        db = DB.openDatabase(this);

        username = getIntent().getStringExtra("USERNAME");
        auth = getIntent().getStringExtra("AUTH");
        findMaNV();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_finance_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupSpinner();
        loadSummaryData();
        loadProjectHighlights("Highest revenue projects");

        cardProjectRevenue.setOnClickListener(v -> {
            Intent intent = new Intent(FNActivity.this, ProjectRevenueActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("AUTH", auth);
            startActivity(intent);
        });

        cardServiceRevenue.setOnClickListener(v -> {
            Intent intent = new Intent(FNActivity.this, ServiceRevenueActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("AUTH", auth);
            startActivity(intent);
        });

        ivMenu.setOnClickListener(this::showMenu);

        btnExport.setOnClickListener(v -> Toast.makeText(this, "Đang chuẩn bị báo cáo Excel...", Toast.LENGTH_SHORT).show());
    }

    private void findMaNV() {
        if (auth != null && !auth.isEmpty()) {
            try {
                Cursor c = db.rawQuery("SELECT CODE FROM CONFIRMAUTH WHERE AUTH = ?", new String[]{auth});
                if (c.moveToFirst()) {
                    maNV = c.getString(0);
                }
                c.close();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void initViews() {
        rvProjects = findViewById(R.id.rvProjects);
        spFilter = findViewById(R.id.spFilter);
        btnExport = findViewById(R.id.btnExport);
        ivMenu = findViewById(R.id.ivMenu);
        tvTotalEarning = findViewById(R.id.tvTotalEarning);
        tvProjectRevenue = findViewById(R.id.tvProjectRevenue);
        tvServiceRevenue = findViewById(R.id.tvServiceRevenue);
        tvServiceCount = findViewById(R.id.tvServiceCount);
        cardServiceRevenue = findViewById(R.id.cardServiceRevenue);
        cardProjectRevenue = findViewById(R.id.cardProjectRevenue);
        cardTotalEarning = findViewById(R.id.cardTotalEarning);

        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setNestedScrollingEnabled(false);
    }

    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.fn_main_menu, popup.getMenu());

        android.view.MenuItem profileItem = popup.getMenu().findItem(R.id.menu_user_profile);
        if (profileItem != null && username != null && !username.isEmpty()) {
            profileItem.setTitle("Hello, " + username);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_user_profile) {
                if (maNV != null && !maNV.isEmpty()) {
                    Intent intent = new Intent(this, EmployeeProfileActivity.class);
                    intent.putExtra("MANV", maNV);
                    startActivity(intent);
                }
                return true;
            } else if (id == R.id.menu_logout) {
                SessionManager.logout(this);
                return true;
            } else if (id == R.id.menu_expense_transport) {
                // Mở màn hình Chi phí vận chuyển vừa tạo
                Intent intent = new Intent(this, TransportExpenseActivity.class);
                intent.putExtra("USERNAME", username);
                intent.putExtra("AUTH", auth);
                startActivity(intent);
                return true;
            } else if (id == R.id.menu_revenue_project) {
                startActivity(new Intent(this, ProjectRevenueActivity.class));
                return true;
            } else if (id == R.id.menu_revenue_service) {
                startActivity(new Intent(this, ServiceRevenueActivity.class));
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void setupSpinner() {
        String[] filters = {"Highest revenue projects", "Lowest revenue projects", "Newest projects"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilter.setAdapter(spinnerAdapter);

        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                loadProjectHighlights(filters[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadSummaryData() {
        double projectRev = 0;
        try {
            Cursor cProject = db.rawQuery("SELECT SUM(TIENNGHIEMTHU_TONG) FROM DTDUAN", null);
            if (cProject.moveToFirst()) projectRev = cProject.getDouble(0);
            cProject.close();
        } catch (Exception e) { e.printStackTrace(); }

        double serviceRev = 0;
        int serviceCount = 0;
        try {
            Cursor cService = db.rawQuery("SELECT SUM(GIACATONG), COUNT(*) FROM DTDICHVU", null);
            if (cService.moveToFirst()) {
                serviceRev = cService.getDouble(0);
                serviceCount = cService.getInt(1);
            }
            cService.close();
        } catch (Exception e) { e.printStackTrace(); }

        double totalEarning = projectRev + serviceRev;
        tvTotalEarning.setText(formatCurrency(totalEarning));
        tvProjectRevenue.setText(formatCurrency(projectRev));
        tvServiceRevenue.setText(formatCurrency(serviceRev));
        tvServiceCount.setText(serviceCount + "\ntransactions");
    }

    private void loadProjectHighlights(String filter) {
        List<ProjectHighlight> data = new ArrayList<>();
        String orderBy = filter.equals("Lowest revenue projects") ? "REVENUE ASC" : (filter.equals("Newest projects") ? "DTHD.MADAHD DESC" : "REVENUE DESC");

        String query = "SELECT IFNULL(HD.TENHD, DTHD.MADAHD), DTHD.MADAHD, IFNULL(DT.TIENNGHIEMTHU_TONG, 0) AS REVENUE, " +
                "IFNULL((SELECT SUM(CHIPHITONG) FROM CPDUAN WHERE MADA = DTHD.MADA), 0) " +
                "FROM DUANTHEOHOPDONG DTHD JOIN DUAN DA ON DTHD.MADA = DA.MADA JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                "LEFT JOIN DTDUAN DT ON DA.MADA = DT.MADA ORDER BY " + orderBy + " LIMIT 10";

        try {
            Cursor c = db.rawQuery(query, null);
            while (c.moveToNext()) {
                data.add(new ProjectHighlight(c.getString(0), c.getString(1), formatCurrency(c.getDouble(2)), formatCurrency(c.getDouble(3))));
            }
            c.close();
        } catch (Exception e) { e.printStackTrace(); }

        adapter = new ProjectHighlightAdapter(data);
        rvProjects.setAdapter(adapter);
    }

    private String formatCurrency(double amount) {
        if (amount >= 1_000_000_000) return String.format(Locale.US, "$%.1fB", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format(Locale.US, "$%.1fM", amount / 1_000_000.0);
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }
}
