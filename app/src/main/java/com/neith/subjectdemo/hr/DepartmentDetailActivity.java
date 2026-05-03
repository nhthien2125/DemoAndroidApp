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

public class DepartmentDetailActivity extends AppCompatActivity {

    SQLiteDatabase db;

    LinearLayout layoutHeader, layoutBody;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int BLUE = Color.parseColor("#3B82F6");

    String maPB = "";
    DepartmentInfo departmentInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_detail);

        db = DB.openDatabase(this);

        layoutHeader = findViewById(R.id.layoutDepartmentDetailHeader);
        layoutBody = findViewById(R.id.layoutDepartmentDetailBody);

        maPB = getIntent().getStringExtra("MAPB");

        if (maPB == null || maPB.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã phòng ban.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (maPB != null && !maPB.trim().isEmpty()) {
            loadDetail();
        }
    }

    private void loadDetail() {
        layoutHeader.removeAllViews();
        layoutBody.removeAllViews();

        departmentInfo = getDepartmentInfo(maPB);

        if (departmentInfo == null) {
            Toast.makeText(this, "Không tìm thấy phòng ban.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buildHeader();
        buildInfoCard();
        buildEmployeeInCard();
        buildEmployeeOutCard();
    }

    private DepartmentInfo getDepartmentInfo(String maPB) {
        Cursor c = db.rawQuery(
                "SELECT PB.MAPB, PB.TENPB, IFNULL(PB.DIADIEM, ''), IFNULL(PB.MATRG_PHG, ''), " +
                        "IFNULL(KV.TENTINH, ''), " +
                        "IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HEAD_NAME " +
                        "FROM PHONGBAN PB " +
                        "LEFT JOIN DTTHEOKV KV ON CAST(PB.DIADIEM AS TEXT) = CAST(KV.MAKV AS TEXT) " +
                        "LEFT JOIN NHANVIEN NV ON PB.MATRG_PHG = NV.MANV " +
                        "WHERE PB.MAPB = ?",
                new String[]{maPB}
        );

        DepartmentInfo info = null;

        if (c.moveToFirst()) {
            info = new DepartmentInfo();
            info.maPB = c.getString(0);
            info.tenPB = c.getString(1);
            info.diaDiem = c.getString(2);
            info.maTruongPhong = c.getString(3);
            info.tenKhuVuc = c.getString(4);
            info.tenTruongPhong = c.getString(5).trim();
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

        TextView title = makeText(departmentInfo.tenPB, 24, PRIMARY, true);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView sub = makeText(
                "Mã: " + departmentInfo.maPB + " • Địa điểm: " + getLocationDisplay(),
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

        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                dp(96),
                dp(46)
        );
        backLp.setMargins(dp(10), 0, 0, 0);
        topBar.addView(back, backLp);

        layoutHeader.addView(topBar);
    }

    private void buildInfoCard() {
        int totalIn = getInt("SELECT COUNT(*) FROM NHANVIEN WHERE MAPB = ?", new String[]{maPB});
        int totalOut = getOutEmployeeCount();

        LinearLayout card = makeCard();

        card.addView(makeCardTitle("THÔNG TIN PHÒNG BAN"));
        card.addView(makeInfoRow("Mã phòng ban", departmentInfo.maPB));
        card.addView(makeInfoRow("Tên phòng ban", departmentInfo.tenPB));
        card.addView(makeInfoRow("Mã trưởng phòng", emptyText(departmentInfo.maTruongPhong)));
        card.addView(makeInfoRow("Trưởng phòng", getHeadDisplay()));
        card.addView(makeInfoRow("Địa điểm", getLocationDisplay()));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, dp(12), 0, 0);

        LinearLayout inBox = makeStatBox("Trong phòng", String.valueOf(totalIn), "Nhân viên hiện hữu", PRIMARY);
        LinearLayout outBox = makeStatBox("Ngoài phòng", String.valueOf(totalOut), "Có thể thêm vào phòng", BLUE);

        LinearLayout.LayoutParams statLp1 = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        statLp1.setMargins(0, 0, dp(6), 0);

        LinearLayout.LayoutParams statLp2 = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        statLp2.setMargins(dp(6), 0, 0, 0);

        stats.addView(inBox, statLp1);
        stats.addView(outBox, statLp2);

        card.addView(stats);
        layoutBody.addView(card);
    }

    private void buildEmployeeInCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("NHÂN VIÊN TRONG PHÒNG"));

        TextView sub = makeText(
                "Trưởng phòng luôn hiển thị đầu danh sách. Nhân viên thường có thể thăng chức hoặc xóa khỏi phòng.",
                12,
                SUB,
                false
        );
        sub.setPadding(0, 0, 0, dp(8));
        card.addView(sub);

        Cursor c = db.rawQuery(
                "SELECT MANV, IFNULL(HOLOT, '') || ' ' || IFNULL(TENNV, '') AS HOTEN " +
                        "FROM NHANVIEN " +
                        "WHERE MAPB = ? " +
                        "ORDER BY " +
                        "CASE WHEN MANV = (SELECT MATRG_PHG FROM PHONGBAN WHERE MAPB = ?) THEN 0 ELSE 1 END, " +
                        "MANV",
                new String[]{maPB, maPB}
        );

        if (c.getCount() == 0) {
            TextView empty = makeText("Phòng ban chưa có nhân viên.", 13, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(12), 0, dp(12));
            card.addView(empty);
        } else {
            while (c.moveToNext()) {
                EmployeeRow item = new EmployeeRow();
                item.maNV = c.getString(0);
                item.hoTen = c.getString(1).trim();

                card.addView(makeEmployeeInRow(item));
            }
        }

        c.close();
        layoutBody.addView(card);
    }

    private void buildEmployeeOutCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("NHÂN VIÊN KHÔNG THUỘC PHÒNG NÀY"));

        TextView sub = makeText(
                "Nhân viên đang là trưởng phòng ở bất kỳ phòng ban nào sẽ không hiển thị ở đây.",
                12,
                SUB,
                false
        );
        sub.setPadding(0, 0, 0, dp(8));
        card.addView(sub);

        Cursor c = db.rawQuery(
                "SELECT NV.MANV, IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "IFNULL(PB.TENPB, '') AS CURRENT_DEPT " +
                        "FROM NHANVIEN NV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "WHERE (NV.MAPB IS NULL OR NV.MAPB <> ?) " +
                        "AND NV.MANV NOT IN (SELECT MATRG_PHG FROM PHONGBAN WHERE MATRG_PHG IS NOT NULL) " +
                        "ORDER BY NV.MANV",
                new String[]{maPB}
        );

        if (c.getCount() == 0) {
            TextView empty = makeText("Không có nhân viên ngoài phòng ban này.", 13, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(12), 0, dp(12));
            card.addView(empty);
        } else {
            while (c.moveToNext()) {
                EmployeeRow item = new EmployeeRow();
                item.maNV = c.getString(0);
                item.hoTen = c.getString(1).trim();
                item.currentDept = c.getString(2);

                card.addView(makeEmployeeOutRow(item));
            }
        }

        c.close();
        layoutBody.addView(card);
    }

    private View makeEmployeeInRow(EmployeeRow item) {
        LinearLayout box = makeEmployeeRowBase();

        boolean head = isHead(item.maNV);
        String note = head ? "Trưởng phòng" : "Nhân viên";

        LinearLayout info = makeEmployeeInfo(item.maNV, item.hoTen, note, head);

        box.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        actions.addView(makeMiniButton("Hồ sơ", PRIMARY, Color.BLACK, v -> openEmployeeProfile(item.maNV)));

        if (!head) {
            actions.addView(makeMiniButton("Thăng chức", GREEN, Color.WHITE, v -> promoteToHead(item.maNV)));
            actions.addView(makeMiniButton("Xóa khỏi phòng", RED, Color.WHITE, v -> removeEmployee(item.maNV)));
        }

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(actions);

        box.addView(hsv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return box;
    }

    private View makeEmployeeOutRow(EmployeeRow item) {
        LinearLayout box = makeEmployeeRowBase();

        String dept = item.currentDept == null || item.currentDept.trim().isEmpty()
                ? "Chưa thuộc phòng ban"
                : "PB hiện tại: " + item.currentDept;

        LinearLayout info = makeEmployeeInfo(item.maNV, item.hoTen, dept, false);

        box.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        actions.addView(makeMiniButton("Thêm vào phòng", GREEN, Color.WHITE, v -> addEmployee(item.maNV)));
        actions.addView(makeMiniButton("Hồ sơ", PRIMARY, Color.BLACK, v -> openEmployeeProfile(item.maNV)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(actions);

        box.addView(hsv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

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

    private LinearLayout makeEmployeeInfo(String maNV, String hoTen, String note, boolean isHead) {
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, 0, 0, dp(8));

        TextView name = makeText(hoTen, 15, isHead ? PRIMARY : TEXT, true);
        name.setSingleLine(false);
        info.addView(name);

        TextView code = makeText(maNV + " • " + note, 12, isHead ? PRIMARY : SUB, false);
        code.setPadding(0, dp(2), 0, 0);
        info.addView(code);

        return info;
    }

    private void addEmployee(String maNV) {
        if (isHeadSomewhere(maNV)) {
            Toast.makeText(this, "Nhân viên này đang là trưởng phòng, không thể chuyển.", Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("MAPB", maPB);

        int rows = db.update(
                "NHANVIEN",
                values,
                "MANV = ?",
                new String[]{maNV}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã thêm nhân viên vào phòng ban.", Toast.LENGTH_SHORT).show();
            loadDetail();
        } else {
            Toast.makeText(this, "Không thể thêm nhân viên.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeEmployee(String maNV) {
        if (isHead(maNV)) {
            Toast.makeText(this, "Không thể xóa trưởng phòng khỏi phòng ban.", Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.putNull("MAPB");

        int rows = db.update(
                "NHANVIEN",
                values,
                "MANV = ? AND MAPB = ?",
                new String[]{maNV, maPB}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã xóa nhân viên khỏi phòng ban.", Toast.LENGTH_SHORT).show();
            loadDetail();
        } else {
            Toast.makeText(this, "Nhân viên không thuộc phòng ban này.", Toast.LENGTH_SHORT).show();
        }
    }

    private void promoteToHead(String maNV) {
        if (!employeeBelongsToDept(maNV)) {
            Toast.makeText(this, "Nhân viên không thuộc phòng ban này.", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("MATRG_PHG", maNV);

        int rows = db.update(
                "PHONGBAN",
                values,
                "MAPB = ?",
                new String[]{maPB}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã thăng chức trưởng phòng.", Toast.LENGTH_SHORT).show();
            loadDetail();
        } else {
            Toast.makeText(this, "Không thể thăng chức.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEmployeeProfile(String maNV) {
        Intent intent = new Intent(this, EmployeeProfileActivity.class);
        intent.putExtra("MANV", maNV);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private boolean isHead(String maNV) {
        return departmentInfo.maTruongPhong != null && departmentInfo.maTruongPhong.equals(maNV);
    }

    private boolean isHeadSomewhere(String maNV) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM PHONGBAN WHERE MATRG_PHG = ?",
                new String[]{maNV}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private boolean employeeBelongsToDept(String maNV) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM NHANVIEN WHERE MANV = ? AND MAPB = ?",
                new String[]{maNV, maPB}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private int getOutEmployeeCount() {
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM NHANVIEN NV " +
                        "WHERE (NV.MAPB IS NULL OR NV.MAPB <> ?) " +
                        "AND NV.MANV NOT IN (SELECT MATRG_PHG FROM PHONGBAN WHERE MATRG_PHG IS NOT NULL)",
                new String[]{maPB}
        );

        int value = 0;

        if (c.moveToFirst()) {
            value = c.getInt(0);
        }

        c.close();

        return value;
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

    private String getLocationDisplay() {
        if (departmentInfo.tenKhuVuc != null && !departmentInfo.tenKhuVuc.trim().isEmpty()) {
            return departmentInfo.tenKhuVuc;
        }

        if (departmentInfo.diaDiem != null && !departmentInfo.diaDiem.trim().isEmpty()) {
            return departmentInfo.diaDiem;
        }

        return "N/A";
    }

    private String getHeadDisplay() {
        if (departmentInfo.tenTruongPhong != null && !departmentInfo.tenTruongPhong.trim().isEmpty()) {
            return departmentInfo.tenTruongPhong;
        }

        if (departmentInfo.maTruongPhong != null && !departmentInfo.maTruongPhong.trim().isEmpty()) {
            return departmentInfo.maTruongPhong;
        }

        return "Chưa thiết lập";
    }

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class DepartmentInfo {
        String maPB;
        String tenPB;
        String diaDiem;
        String maTruongPhong;
        String tenKhuVuc;
        String tenTruongPhong;
    }

    static class EmployeeRow {
        String maNV;
        String hoTen;
        String currentDept;
    }
}