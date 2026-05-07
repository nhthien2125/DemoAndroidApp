package com.neith.subjectdemo.hr;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ProjectDetailActivity extends AppCompatActivity {

    SQLiteDatabase db;

    LinearLayout layoutHeader, layoutBody;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");
    final int PURPLE = Color.parseColor("#A855F7");

    String projectKey = "";
    ProjectInfo projectInfo;

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat viewDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        db = DB.openDatabase(this);

        layoutHeader = findViewById(R.id.layoutProjectDetailHeader);
        layoutBody = findViewById(R.id.layoutProjectDetailBody);

        projectKey = getIntent().getStringExtra("PROJECT_KEY");

        if (projectKey == null || projectKey.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã dự án.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (projectKey != null && !projectKey.trim().isEmpty()) {
            loadDetail();
        }
    }

    private void loadDetail() {
        layoutHeader.removeAllViews();
        layoutBody.removeAllViews();

        projectInfo = getProjectInfo(projectKey);

        if (projectInfo == null) {
            Toast.makeText(this, "Không tìm thấy dự án.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        projectInfo.statusCode = getStatusCode(projectInfo.startDate, projectInfo.endDate);
        projectInfo.statusLabel = getStatusLabel(projectInfo.statusCode);

        buildHeader();
        buildInfoCard();
        buildFinanceCard();
        buildFormulaCard();
        buildCurrentEmployeeCard();

        if (!"done".equals(projectInfo.statusCode)) {
            buildAvailableEmployeeCard();
        }
    }

    private ProjectInfo getProjectInfo(String key) {
        Cursor c = db.rawQuery(
                "SELECT DTHD.MADAHD, DTHD.MADA, DTHD.MAHD, " +
                        "IFNULL(HD.TENHD, DTHD.MADAHD) AS TENHD, " +
                        "IFNULL(HD.LOAI, '') AS LOAIHD, " +
                        "IFNULL(HD.NGAYBD, '') AS NGAYBD, " +
                        "IFNULL(HD.NGAYKT_DUTINH, '') AS NGAYKT_DUTINH, " +
                        "IFNULL(DTHD.NGAYKT, '') AS NGAYKT, " +
                        "IFNULL(DA.MAKH, ''), " +
                        "IFNULL(KH.LOAIKH, ''), " +
                        "IFNULL(DT.MAKV, ''), " +
                        "IFNULL(KV.TENTINH, ''), " +
                        "IFNULL(KV.HESOKV, 0), " +
                        "IFNULL(DT.MALH, ''), " +
                        "IFNULL(LH.TENLH, ''), " +
                        "IFNULL(DA.TIENCOC, 0), " +
                        "IFNULL(DA.TIENNGHIEMTHU_DUTINH, 0), " +
                        "IFNULL(DT.HSTHAYDOI, 0), " +
                        "IFNULL(DT.TIENNGHIEMTHU_TONG, 0), " +
                        "(SELECT COUNT(*) FROM NVTHAMGIADA TG WHERE TG.MADAHD = DTHD.MADAHD) " +
                        "FROM DUANTHEOHOPDONG DTHD " +
                        "JOIN DUAN DA ON DTHD.MADA = DA.MADA " +
                        "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                        "LEFT JOIN KHACHHANG KH ON DA.MAKH = KH.MAKH " +
                        "LEFT JOIN DTDUAN DT ON DA.MADA = DT.MADA " +
                        "LEFT JOIN DTTHEOKV KV ON DT.MAKV = KV.MAKV " +
                        "LEFT JOIN DTTHEOLHCT LH ON DT.MALH = LH.MALH " +
                        "WHERE DTHD.MADAHD = ?",
                new String[]{key}
        );

        ProjectInfo info = null;

        if (c.moveToFirst()) {
            info = new ProjectInfo();
            info.projectKey = c.getString(0);
            info.projectCode = c.getString(1);
            info.contractCode = c.getString(2);
            info.projectName = c.getString(3);
            info.contractType = c.getString(4);
            info.startDate = c.getString(5);
            info.contractExpectedEnd = c.getString(6);
            info.endDate = c.getString(7);
            info.customerCode = c.getString(8);
            info.customerName = c.getString(9);
            info.areaCode = c.getString(10);
            info.areaName = c.getString(11);
            info.areaFactor = c.getDouble(12);
            info.projectTypeCode = c.getString(13);
            info.projectTypeName = c.getString(14);
            info.deposit = c.getDouble(15);
            info.expectedTotal = c.getDouble(16);
            info.coefficient = c.getDouble(17);
            info.finalTotal = c.getDouble(18);
            info.employeeCount = c.getInt(19);
        }

        c.close();
        return info;
    }

    private void buildHeader() {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(6));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = makeText(projectInfo.projectName, 24, PRIMARY, true);
        title.setSingleLine(false);

        TextView sub = makeText(
                "Mã: " + projectInfo.projectCode + " • HĐ: " + projectInfo.contractCode,
                12,
                SUB,
                false
        );

        titleBox.addView(title);
        titleBox.addView(sub);

        Button back = new Button(this);
        back.setText("Quay lại");
        back.setAllCaps(false);
        back.setTextColor(Color.BLACK);
        back.setTextSize(12);
        back.setTypeface(null, Typeface.BOLD);
        back.setBackgroundTintList(null);
        back.setBackgroundResource(R.drawable.hr_logout_bg);
        back.setOnClickListener(v -> finish());

        topBar.addView(titleBox, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(96), dp(46));
        backLp.setMargins(dp(10), 0, 0, 0);
        topBar.addView(back, backLp);

        layoutHeader.addView(topBar);
    }

    private void buildInfoCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("THÔNG TIN DỰ ÁN"));
        card.addView(makeInfoRow("Mã dự án theo HĐ", projectInfo.projectKey));
        card.addView(makeInfoRow("Mã dự án", projectInfo.projectCode));
        card.addView(makeInfoRow("Mã hợp đồng", projectInfo.contractCode));
        card.addView(makeInfoRow("Loại hợp đồng", getTypeName(projectInfo.contractType)));
        card.addView(makeInfoRow("Mã khách hàng", emptyText(projectInfo.customerCode)));
        card.addView(makeInfoRow("Khách hàng", emptyText(projectInfo.customerName)));
        card.addView(makeInfoRow("Mã khu vực", emptyText(projectInfo.areaCode)));
        card.addView(makeInfoRow("Khu vực", emptyText(projectInfo.areaName)));
        card.addView(makeInfoRow("Mã loại hình", emptyText(projectInfo.projectTypeCode)));
        card.addView(makeInfoRow("Loại hình công trình", emptyText(projectInfo.projectTypeName)));
        card.addView(makeInfoRow("Ngày bắt đầu", formatDate(projectInfo.startDate)));
        card.addView(makeInfoRow("Ngày KT dự tính HĐ", formatDate(projectInfo.contractExpectedEnd)));
        card.addView(makeInfoRow("Ngày kết thúc dự án", formatDate(projectInfo.endDate)));
        card.addView(makeInfoRow("Trạng thái", projectInfo.statusLabel));
        card.addView(makeInfoRow("Nhân sự tham gia", projectInfo.employeeCount + " người"));

        if ("done".equals(projectInfo.statusCode)) {
            TextView note = makeText(
                    "Dự án đã hoàn thành nên không thể thêm nhân viên mới.",
                    13,
                    ORANGE,
                    true
            );
            note.setPadding(0, dp(10), 0, 0);
            card.addView(note);
        }

        layoutBody.addView(card);
    }

    private void buildFinanceCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("TÀI CHÍNH DỰ ÁN"));
        card.addView(makeBigValue("Tiền cọc", formatCompactMoney(projectInfo.deposit), PRIMARY));
        card.addView(makeBigValue("Dự toán cơ sở / doanh thu dự kiến", formatCompactMoney(projectInfo.expectedTotal), BLUE));
        card.addView(makeBigValue("Hệ số khu vực", String.valueOf(projectInfo.areaFactor <= 0 ? 1 : projectInfo.areaFactor), ORANGE));
        card.addView(makeBigValue("Hệ số điều chỉnh", String.valueOf(projectInfo.coefficient <= 0 ? 1 : projectInfo.coefficient), ORANGE));
        card.addView(makeBigValue("Tổng nghiệm thu dự kiến", formatCompactMoney(projectInfo.finalTotal), GREEN));

        layoutBody.addView(card);
    }

    private void buildFormulaCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("CÔNG THỨC TÍNH NGHIỆM THU"));

        String formula =
                "Tổng nghiệm thu dự kiến =\n" +
                        "MAX(Dự toán cơ sở, Tiền cọc / Tỷ lệ cọc theo loại)\n" +
                        "× Hệ số loại hợp đồng\n" +
                        "× Hệ số thời gian\n" +
                        "× Hệ số khu vực\n" +
                        "× Hệ số điều chỉnh\n\n" +
                        "Dự án này đang dùng:\n" +
                        "Tỷ lệ cọc: " + formatPercent(getDepositRate(projectInfo.contractType)) + "\n" +
                        "Hệ số loại: " + getContractTypeFactor(projectInfo.contractType) + "\n" +
                        "Hệ số thời gian: " + getDurationFactor(projectInfo.startDate, projectInfo.endDate) + "\n" +
                        "Hệ số khu vực: " + (projectInfo.areaFactor <= 0 ? 1 : projectInfo.areaFactor) + "\n" +
                        "Hệ số điều chỉnh: " + (projectInfo.coefficient <= 0 ? 1 : projectInfo.coefficient);

        TextView tv = makeText(formula, 13, TEXT, false);
        tv.setPadding(0, dp(4), 0, 0);
        card.addView(tv);

        layoutBody.addView(card);
    }

    private void buildCurrentEmployeeCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("NHÂN SỰ TRONG DỰ ÁN"));

        TextView sub = makeText("Danh sách nhân viên đang tham gia dự án này.", 12, SUB, false);
        sub.setPadding(0, 0, 0, dp(8));
        card.addView(sub);

        ArrayList<EmployeeRow> list = getCurrentEmployees();

        if (list.isEmpty()) {
            TextView empty = makeText("Chưa có nhân sự nào trong dự án.", 13, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(12), 0, dp(12));
            card.addView(empty);
        } else {
            for (EmployeeRow item : list) {
                card.addView(makeCurrentEmployeeRow(item));
            }
        }

        layoutBody.addView(card);
    }

    private void buildAvailableEmployeeCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("NHÂN SỰ CÓ THỂ THÊM"));

        TextView sub = makeText(
                "Nhân viên đang thuộc dự án khác còn hạn sẽ không hiển thị ở đây.",
                12,
                SUB,
                false
        );
        sub.setPadding(0, 0, 0, dp(8));
        card.addView(sub);

        ArrayList<EmployeeRow> list = getAvailableEmployees();

        if (list.isEmpty()) {
            TextView empty = makeText("Không còn nhân sự hợp lệ để thêm.", 13, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(12), 0, dp(12));
            card.addView(empty);
        } else {
            for (EmployeeRow item : list) {
                card.addView(makeAvailableEmployeeRow(item));
            }
        }

        layoutBody.addView(card);
    }

    private ArrayList<EmployeeRow> getCurrentEmployees() {
        ArrayList<EmployeeRow> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT NV.MANV, IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "IFNULL(PB.TENPB, ''), IFNULL(CV.TENCV, ''), IFNULL(L.LUONGCOBAN, 0) " +
                        "FROM NVTHAMGIADA TG " +
                        "JOIN NHANVIEN NV ON TG.MANV = NV.MANV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "LEFT JOIN LUONG L ON NV.MANV = L.MANV " +
                        "WHERE TG.MADAHD = ? " +
                        "ORDER BY NV.HOLOT, NV.TENNV",
                new String[]{projectKey}
        );

        while (c.moveToNext()) {
            EmployeeRow item = new EmployeeRow();
            item.maNV = c.getString(0);
            item.hoTen = c.getString(1).trim();
            item.phongBan = c.getString(2);
            item.chucVu = c.getString(3);
            item.luongCoBan = c.getDouble(4);
            list.add(item);
        }

        c.close();

        return list;
    }

    private ArrayList<EmployeeRow> getAvailableEmployees() {
        ArrayList<EmployeeRow> list = new ArrayList<>();
        String today = dbDateFormat.format(new Date());

        Cursor c = db.rawQuery(
                "SELECT NV.MANV, IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "IFNULL(PB.TENPB, ''), IFNULL(CV.TENCV, ''), IFNULL(L.LUONGCOBAN, 0) " +
                        "FROM NHANVIEN NV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "LEFT JOIN LUONG L ON NV.MANV = L.MANV " +
                        "WHERE NV.MANV NOT IN (SELECT MANV FROM NVTHAMGIADA WHERE MADAHD = ?) " +
                        "AND NV.MANV NOT IN ( " +
                        "   SELECT TG.MANV " +
                        "   FROM NVTHAMGIADA TG " +
                        "   JOIN DUANTHEOHOPDONG DTHD ON TG.MADAHD = DTHD.MADAHD " +
                        "   WHERE IFNULL(DTHD.NGAYKT, '') >= ? " +
                        ") " +
                        "ORDER BY NV.HOLOT, NV.TENNV",
                new String[]{projectKey, today}
        );

        while (c.moveToNext()) {
            EmployeeRow item = new EmployeeRow();
            item.maNV = c.getString(0);
            item.hoTen = c.getString(1).trim();
            item.phongBan = c.getString(2);
            item.chucVu = c.getString(3);
            item.luongCoBan = c.getDouble(4);
            list.add(item);
        }

        c.close();

        return list;
    }

    private View makeCurrentEmployeeRow(EmployeeRow item) {
        LinearLayout box = makeEmployeeRowBase();

        LinearLayout info = makeEmployeeInfo(
                item.maNV,
                item.hoTen,
                "Đang tham gia • " + emptyText(item.phongBan) + " • " + emptyText(item.chucVu) + " • " + formatCompactMoney(item.luongCoBan)
        );

        box.addView(info);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        actions.addView(makeMiniButton("Hồ sơ", PRIMARY, Color.BLACK, v -> openEmployeeProfile(item.maNV)));
        actions.addView(makeMiniButton("Xóa khỏi DA", RED, Color.WHITE, v -> removeEmployee(item.maNV)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(actions);

        box.addView(hsv);

        return box;
    }

    private View makeAvailableEmployeeRow(EmployeeRow item) {
        LinearLayout box = makeEmployeeRowBase();

        LinearLayout info = makeEmployeeInfo(
                item.maNV,
                item.hoTen,
                "Có thể thêm • " + emptyText(item.phongBan) + " • " + emptyText(item.chucVu) + " • " + formatCompactMoney(item.luongCoBan)
        );

        box.addView(info);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        actions.addView(makeMiniButton("Thêm vào DA", GREEN, Color.WHITE, v -> addEmployee(item.maNV)));
        actions.addView(makeMiniButton("Hồ sơ", PRIMARY, Color.BLACK, v -> openEmployeeProfile(item.maNV)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(actions);

        box.addView(hsv);

        return box;
    }

    private void addEmployee(String maNV) {
        if ("done".equals(projectInfo.statusCode)) {
            Toast.makeText(this, "Dự án đã hoàn thành, không thể thêm nhân viên.", Toast.LENGTH_LONG).show();
            return;
        }

        if (employeeExistsInProject(maNV)) {
            Toast.makeText(this, "Nhân viên đã có trong dự án.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (employeeBusy(maNV)) {
            Toast.makeText(this, "Nhân viên đang thuộc dự án khác còn hạn.", Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("MADAHD", projectKey);
        values.put("MANV", maNV);
        values.putNull("TONGTGLAMVIEC");
        values.putNull("LUONGTHEODUAN");

        long result = db.insert("NVTHAMGIADA", null, values);

        if (result != -1) {
            Toast.makeText(this, "Đã thêm nhân sự vào dự án.", Toast.LENGTH_SHORT).show();
            loadDetail();
        } else {
            Toast.makeText(this, "Không thể thêm nhân sự.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeEmployee(String maNV) {
        int rows = db.delete(
                "NVTHAMGIADA",
                "MADAHD = ? AND MANV = ?",
                new String[]{projectKey, maNV}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã xóa nhân sự khỏi dự án.", Toast.LENGTH_SHORT).show();
            loadDetail();
        } else {
            Toast.makeText(this, "Không tìm thấy bản ghi.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean employeeExistsInProject(String maNV) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM NVTHAMGIADA WHERE MADAHD = ? AND MANV = ?",
                new String[]{projectKey, maNV}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private boolean employeeBusy(String maNV) {
        String today = dbDateFormat.format(new Date());

        Cursor c = db.rawQuery(
                "SELECT 1 " +
                        "FROM NVTHAMGIADA TG " +
                        "JOIN DUANTHEOHOPDONG DTHD ON TG.MADAHD = DTHD.MADAHD " +
                        "WHERE TG.MANV = ? " +
                        "AND TG.MADAHD <> ? " +
                        "AND IFNULL(DTHD.NGAYKT, '') >= ?",
                new String[]{maNV, projectKey, today}
        );

        boolean busy = c.moveToFirst();
        c.close();

        return busy;
    }

    private void openEmployeeProfile(String maNV) {
        Intent intent = new Intent(this, EmployeeProfileActivity.class);
        intent.putExtra("MANV", maNV);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

    private String formatPercent(double value) {
        return Math.round(value * 100) + "%";
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

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.hr_card_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        return card;
    }

    private TextView makeCardTitle(String text) {
        TextView tv = makeText(text, 13, PRIMARY, true);
        tv.setPadding(0, 0, 0, dp(10));
        return tv;
    }

    private LinearLayout makeInfoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(7), 0, dp(7));

        TextView l = makeText(label, 13, SUB, false);

        TextView v = makeText(
                value == null || value.trim().isEmpty() ? "—" : value,
                13,
                TEXT,
                true
        );
        v.setGravity(Gravity.RIGHT);
        v.setSingleLine(false);

        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
    }

    private LinearLayout makeBigValue(String label, String value, int color) {
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

        box.addView(makeText(label, 12, SUB, false));

        TextView valueText = makeText(value, 17, color, true);
        valueText.setSingleLine(false);
        box.addView(valueText);

        return box;
    }

    private LinearLayout makeEmployeeRowBase() {
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

    private LinearLayout makeEmployeeInfo(String maNV, String hoTen, String note) {
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, 0, 0, dp(8));

        TextView name = makeText(hoTen, 15, TEXT, true);
        name.setSingleLine(false);
        info.addView(name);

        TextView code = makeText(maNV + " • " + note, 12, SUB, false);
        code.setPadding(0, dp(2), 0, 0);
        code.setSingleLine(false);
        info.addView(code);

        return info;
    }

    private Button makeMiniButton(String text, int bgColor, int textColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(10);
        btn.setTextColor(textColor);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        btn.setOnClickListener(listener);
        btn.setMinHeight(0);
        btn.setMinWidth(0);
        btn.setPadding(dp(12), 0, dp(12), 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );
        lp.setMargins(0, 0, dp(7), 0);
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

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class ProjectInfo {
        String projectKey;
        String projectCode;
        String contractCode;
        String projectName;
        String contractType;
        String customerCode;
        String customerName;
        String areaCode;
        String areaName;
        double areaFactor;
        String projectTypeCode;
        String projectTypeName;
        String startDate;
        String contractExpectedEnd;
        String endDate;
        String statusCode;
        String statusLabel;
        double deposit;
        double expectedTotal;
        double coefficient;
        double finalTotal;
        int employeeCount;
    }

    static class EmployeeRow {
        String maNV;
        String hoTen;
        String phongBan;
        String chucVu;
        double luongCoBan;
    }
}