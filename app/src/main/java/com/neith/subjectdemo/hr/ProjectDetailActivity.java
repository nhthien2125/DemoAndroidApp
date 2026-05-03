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
        buildCurrentEmployeeCard();
        buildAvailableEmployeeCard();
    }

    private ProjectInfo getProjectInfo(String key) {
        Cursor c = db.rawQuery(
                "SELECT DTHD.MADAHD, DTHD.MADA, DTHD.MAHD, " +
                        "IFNULL(HD.TENHD, DTHD.MADAHD) AS TENHD, " +
                        "IFNULL(HD.NGAYBD, '') AS NGAYBD, " +
                        "IFNULL(DTHD.NGAYKT, '') AS NGAYKT, " +
                        "IFNULL(KH.LOAIKH, '') AS KHACHHANG, " +
                        "IFNULL(KV.TENTINH, '') AS KHUVUC, " +
                        "IFNULL(LH.TENLH, '') AS LOAIHINH, " +
                        "IFNULL(DA.TIENCOC, 0), " +
                        "IFNULL(DA.TIENNGHIEMTHU_DUTINH, 0), " +
                        "IFNULL(DT.HSTHAYDOI, 0), " +
                        "IFNULL(DT.TIENNGHIEMTHU_TONG, 0) " +
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
            info.startDate = c.getString(4);
            info.endDate = c.getString(5);
            info.customerName = c.getString(6);
            info.areaName = c.getString(7);
            info.projectTypeName = c.getString(8);
            info.deposit = c.getDouble(9);
            info.expectedTotal = c.getDouble(10);
            info.coefficient = c.getDouble(11);
            info.finalTotal = c.getDouble(12);
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
        card.addView(makeInfoRow("Mã dự án", projectInfo.projectCode));
        card.addView(makeInfoRow("Mã hợp đồng", projectInfo.contractCode));
        card.addView(makeInfoRow("Khách hàng", emptyText(projectInfo.customerName)));
        card.addView(makeInfoRow("Khu vực", emptyText(projectInfo.areaName)));
        card.addView(makeInfoRow("Loại hình", emptyText(projectInfo.projectTypeName)));
        card.addView(makeInfoRow("Thời gian", formatDate(projectInfo.startDate) + " - " + formatDate(projectInfo.endDate)));
        card.addView(makeInfoRow("Trạng thái", projectInfo.statusLabel));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setPadding(0, dp(12), 0, 0);

        stats.addView(makeBigValue("Tiền cọc", projectInfo.deposit + " VND", PRIMARY));
        stats.addView(makeBigValue("Doanh thu dự kiến", projectInfo.expectedTotal + " VND", BLUE));
        stats.addView(makeBigValue("Hệ số điều chỉnh", String.valueOf(projectInfo.coefficient), ORANGE));
        stats.addView(makeBigValue("Tổng nghiệm thu", projectInfo.finalTotal + " VND", GREEN));

        card.addView(stats);
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
                "SELECT NV.MANV, IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN " +
                        "FROM NVTHAMGIADA TG " +
                        "JOIN NHANVIEN NV ON TG.MANV = NV.MANV " +
                        "WHERE TG.MADAHD = ? " +
                        "ORDER BY NV.HOLOT, NV.TENNV",
                new String[]{projectKey}
        );

        while (c.moveToNext()) {
            EmployeeRow item = new EmployeeRow();
            item.maNV = c.getString(0);
            item.hoTen = c.getString(1).trim();
            list.add(item);
        }

        c.close();

        return list;
    }

    private ArrayList<EmployeeRow> getAvailableEmployees() {
        ArrayList<EmployeeRow> list = new ArrayList<>();
        String today = dbDateFormat.format(new Date());

        Cursor c = db.rawQuery(
                "SELECT NV.MANV, IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN " +
                        "FROM NHANVIEN NV " +
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
            list.add(item);
        }

        c.close();

        return list;
    }

    private View makeCurrentEmployeeRow(EmployeeRow item) {
        LinearLayout box = makeEmployeeRowBase();

        LinearLayout info = makeEmployeeInfo(item.maNV, item.hoTen, "Đang tham gia dự án");

        box.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

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

        LinearLayout info = makeEmployeeInfo(item.maNV, item.hoTen, "Có thể thêm vào dự án");

        box.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

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
        info.addView(code);

        return info;
    }

    private void addEmployee(String maNV) {
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
            case "overdue":
                return "Quá hạn";
            default:
                return "Đúng tiến độ";
        }
    }

    private Date parseDate(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return null;
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
            if (d == null) return "?";
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

        row.addView(l, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        row.addView(v, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

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
        box.addView(makeText(value, 17, color, true));

        return box;
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class ProjectInfo {
        String projectKey;
        String projectCode;
        String contractCode;
        String projectName;
        String customerName;
        String areaName;
        String projectTypeName;
        String startDate;
        String endDate;
        String statusCode;
        String statusLabel;
        double deposit;
        double expectedTotal;
        double coefficient;
        double finalTotal;
    }

    static class EmployeeRow {
        String maNV;
        String hoTen;
    }
}