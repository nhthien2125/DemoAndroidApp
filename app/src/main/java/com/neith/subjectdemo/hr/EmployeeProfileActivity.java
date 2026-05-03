package com.neith.subjectdemo.hr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;

import java.text.NumberFormat;
import java.util.Locale;

public class EmployeeProfileActivity extends AppCompatActivity {

    SQLiteDatabase db;
    LinearLayout layoutProfileHeader, layoutProfileBody;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");

    String maNV = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_profile);

        db = DB.openDatabase(this);

        layoutProfileHeader = findViewById(R.id.layoutProfileHeader);
        layoutProfileBody = findViewById(R.id.layoutProfileBody);

        maNV = getIntent().getStringExtra("MANV");

        if (maNV == null || maNV.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã nhân viên", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProfile();
    }

    private void loadProfile() {
        layoutProfileHeader.removeAllViews();
        layoutProfileBody.removeAllViews();

        Cursor c = db.rawQuery(
                "SELECT NV.MANV, " +
                        "IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "NV.GIOITINH, NV.NAMSINH, " +
                        "IFNULL(PB.TENPB, '(Chưa gán)') AS TENPB, " +
                        "IFNULL(CV.TENCV, '(Chưa gán)') AS TENCV, " +
                        "IFNULL(L.LOAINV, '') AS LOAINV, " +
                        "IFNULL(L.LUONGCOBAN, 0) AS LUONGCOBAN, " +
                        "IFNULL(NV.MAPB, '') AS MAPB, " +
                        "IFNULL(NV.MACV, '') AS MACV " +
                        "FROM NHANVIEN NV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "LEFT JOIN LUONG L ON NV.MANV = L.MANV " +
                        "WHERE NV.MANV = ?",
                new String[]{maNV}
        );

        if (!c.moveToFirst()) {
            c.close();
            Toast.makeText(this, "Không tìm thấy nhân viên", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String id = c.getString(0);
        String hoTen = c.getString(1).trim();
        boolean gioiTinh = c.getInt(2) == 1;
        int namSinh = c.isNull(3) ? 0 : c.getInt(3);
        String tenPB = c.getString(4);
        String tenCV = c.getString(5);
        String loaiNV = c.getString(6);
        double luongCoBan = c.getDouble(7);
        String maPB = c.getString(8);
        String maCV = c.getString(9);

        c.close();

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(10));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = makeText("Hồ sơ nhân viên", 26, PRIMARY, true);
        TextView subtitle = makeText("Thông tin chi tiết nhân viên", 13, SUB, false);

        titleBox.addView(title);
        titleBox.addView(subtitle);

        Button btnBack = new Button(this);
        btnBack.setText("Quay lại");
        btnBack.setAllCaps(false);
        btnBack.setTextColor(Color.BLACK);
        btnBack.setTextSize(13);
        btnBack.setTypeface(null, Typeface.BOLD);
        btnBack.setBackgroundTintList(null);
        btnBack.setBackgroundResource(R.drawable.hr_logout_bg);
        btnBack.setOnClickListener(v -> finish());

        topBar.addView(titleBox, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        topBar.addView(btnBack, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        ));

        layoutProfileHeader.addView(topBar);

        LinearLayout headerCard = makeCard();

        LinearLayout avatarRow = new LinearLayout(this);
        avatarRow.setOrientation(LinearLayout.HORIZONTAL);
        avatarRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = makeAvatar(hoTen);
        avatarRow.addView(avatar);

        LinearLayout nameBox = new LinearLayout(this);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        nameBox.setPadding(dp(14), 0, 0, 0);

        TextView name = makeText(hoTen, 22, PRIMARY, true);
        nameBox.addView(name);

        TextView idLine = makeText(
                id + " • " + (gioiTinh ? "Nam" : "Nữ") + " • " + (namSinh == 0 ? "—" : namSinh),
                13,
                SUB,
                false
        );
        idLine.setPadding(0, dp(3), 0, 0);
        nameBox.addView(idLine);

        avatarRow.addView(nameBox, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        headerCard.addView(avatarRow);

        LinearLayout tagRow = new LinearLayout(this);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setPadding(0, dp(14), 0, 0);

        tagRow.addView(makeTag(tenPB));
        tagRow.addView(makeTag(tenCV));

        if (loaiNV != null && !loaiNV.trim().isEmpty()) {
            tagRow.addView(makeTag(loaiNV));
        }

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(tagRow);

        headerCard.addView(hsv);

        Button copy = new Button(this);
        copy.setText("Copy mã nhân viên");
        copy.setAllCaps(false);
        copy.setTextColor(Color.BLACK);
        copy.setTypeface(null, Typeface.BOLD);
        copy.setBackgroundTintList(null);
        copy.setBackgroundResource(R.drawable.hr_logout_bg);
        copy.setOnClickListener(v -> copyText(id));

        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        copyLp.setMargins(0, dp(16), 0, 0);

        headerCard.addView(copy, copyLp);
        layoutProfileHeader.addView(headerCard);

        LinearLayout personalCard = makeCard();
        personalCard.addView(makeCardTitle("THÔNG TIN CÁ NHÂN"));
        personalCard.addView(makeInfoRow("Mã nhân viên", id));
        personalCard.addView(makeInfoRow("Họ và tên", hoTen));
        personalCard.addView(makeInfoRow("Giới tính", gioiTinh ? "Nam" : "Nữ"));
        personalCard.addView(makeInfoRow("Năm sinh", namSinh == 0 ? "—" : String.valueOf(namSinh)));
        layoutProfileBody.addView(personalCard);

        LinearLayout jobCard = makeCard();
        jobCard.addView(makeCardTitle("CÔNG VIỆC"));
        jobCard.addView(makeInfoRow("Mã phòng ban", emptyText(maPB)));
        jobCard.addView(makeInfoRow("Phòng ban", emptyText(tenPB)));
        jobCard.addView(makeInfoRow("Mã chức vụ", emptyText(maCV)));
        jobCard.addView(makeInfoRow("Chức vụ", emptyText(tenCV)));
        jobCard.addView(makeInfoRow("Loại nhân viên", emptyText(loaiNV)));
        layoutProfileBody.addView(jobCard);

        LinearLayout salaryCard = makeCard();
        salaryCard.addView(makeCardTitle("LƯƠNG & THỐNG KÊ"));
        salaryCard.addView(makeBigValue("Lương cơ bản", formatNumber(luongCoBan) + " VND", PRIMARY));
        salaryCard.addView(makeBigValue("Tổng thưởng", formatNumber(getRewardPenalty(id, "KT")) + " VND", GREEN));
        salaryCard.addView(makeBigValue("Tổng phạt", formatNumber(getRewardPenalty(id, "KL")) + " VND", RED));
        salaryCard.addView(makeBigValue("Tổng ca làm", formatNumber(getTotalShift(id)) + " ca", ORANGE));
        layoutProfileBody.addView(salaryCard);

        LinearLayout rewardCard = makeCard();
        rewardCard.addView(makeCardTitle("THƯỞNG / PHẠT GẦN ĐÂY"));
        addRewardPenaltyList(rewardCard, id);
        layoutProfileBody.addView(rewardCard);
    }

    private void addRewardPenaltyList(LinearLayout parent, String id) {
        Cursor c = db.rawQuery(
                "SELECT HINHTHUC, " +
                        "IFNULL(TONG, IFNULL(SOTIEN, 0)) AS SOTIEN_TONG, " +
                        "IFNULL(NGAY, '') AS NGAY_TP " +
                        "FROM DSTHUONGPHAT " +
                        "WHERE MANV = ? " +
                        "ORDER BY NGAY DESC " +
                        "LIMIT 8",
                new String[]{id}
        );

        if (c.getCount() == 0) {
            TextView empty = makeText("Chưa có dữ liệu thưởng/phạt.", 13, SUB, false);
            empty.setPadding(0, dp(8), 0, 0);
            parent.addView(empty);
        } else {
            while (c.moveToNext()) {
                String type = c.getString(0);
                double amount = c.getDouble(1);
                String date = c.getString(2);

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setPadding(0, dp(10), 0, dp(10));

                String label = "KT".equals(type) ? "Thưởng" : "Phạt";
                int color = "KT".equals(type) ? GREEN : RED;

                TextView line1 = makeText(
                        label + ": " + formatNumber(amount) + " VND",
                        14,
                        color,
                        true
                );
                item.addView(line1);

                TextView line2 = makeText(
                        date == null || date.isEmpty() ? "Không rõ ngày" : date,
                        12,
                        SUB,
                        false
                );
                line2.setPadding(0, dp(2), 0, 0);
                item.addView(line2);

                parent.addView(item);
            }
        }

        c.close();
    }

    private double getRewardPenalty(String id, String type) {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(SUM(IFNULL(TONG, IFNULL(SOTIEN, 0))), 0) " +
                        "FROM DSTHUONGPHAT " +
                        "WHERE MANV = ? AND HINHTHUC = ?",
                new String[]{id, type}
        );

        double value = 0;

        if (c.moveToFirst()) {
            value = c.getDouble(0);
        }

        c.close();
        return value;
    }

    private double getTotalShift(String id) {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(SUM(TONGCA), 0) FROM CHAMCONG WHERE MANV = ?",
                new String[]{id}
        );

        double value = 0;

        if (c.moveToFirst()) {
            value = c.getDouble(0);
        }

        c.close();
        return value;
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.hr_card_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        return card;
    }

    private TextView makeCardTitle(String title) {
        TextView tv = makeText(title, 13, PRIMARY, true);
        tv.setPadding(0, 0, 0, dp(10));
        return tv;
    }

    private LinearLayout makeInfoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(7), 0, dp(7));

        TextView l = makeText(label, 13, SUB, false);
        TextView v = makeText(value == null || value.isEmpty() ? "—" : value, 13, TEXT, true);
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
        box.addView(makeText(value, 19, color, true));

        return box;
    }

    private TextView makeAvatar(String fullName) {
        TextView avatar = makeText(getInitial(fullName), 24, Color.BLACK, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.employee_avatar_bg);

        avatar.setLayoutParams(new LinearLayout.LayoutParams(
                dp(72),
                dp(72)
        ));

        return avatar;
    }

    private TextView makeTag(String text) {
        TextView tag = makeText(text, 11, SUB, false);
        tag.setPadding(dp(9), dp(4), dp(9), dp(4));
        tag.setBackgroundResource(R.drawable.employee_tag_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, dp(6), 0);
        tag.setLayoutParams(lp);

        return tag;
    }

    private void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Employee code", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Đã copy mã nhân viên", Toast.LENGTH_SHORT).show();
    }

    private String getInitial(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "NV";
        }

        String[] parts = fullName.trim().split("\\s+");
        String last = parts[parts.length - 1];

        if (last.length() >= 1) {
            return last.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        return "NV";
    }

    private String formatNumber(double number) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(number);
    }

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
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
}