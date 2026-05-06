package com.neith.subjectdemo.admin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.hr.HRActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class AdminHomeActivity extends AppCompatActivity {

    SQLiteDatabase db;
    LinearLayout layoutBody;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");
    final int PURPLE = Color.parseColor("#A855F7");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        db = DB.openDatabase(this);
        layoutBody = findViewById(R.id.layoutAdminHomeBody);

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setupAdmin(this, bottomNavContainer, BottomNav.ADMIN_HOME);

        loadHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHome();
    }

    private void loadHome() {
        layoutBody.removeAllViews();

        buildSummaryCard();
        buildRoleSummaryCard();
        buildQuickActionCard();
        buildLatestLogCard();
    }

    private void buildSummaryCard() {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("TỔNG QUAN HỆ THỐNG"));

        int userCount = getInt("SELECT COUNT(*) FROM USERS", null);
        int authCount = getInt("SELECT COUNT(*) FROM CONFIRMAUTH", null);
        int employeeCount = getInt("SELECT COUNT(*) FROM NHANVIEN", null);
        int unassigned = getInt(
                "SELECT COUNT(*) FROM NHANVIEN WHERE MANV NOT IN (SELECT CODE FROM CONFIRMAUTH WHERE CODE IS NOT NULL)",
                null
        );

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(makeStatBox("Tài khoản", String.valueOf(userCount), "USERS", PRIMARY), halfLp(true));
        row1.addView(makeStatBox("Mã quyền", String.valueOf(authCount), "CONFIRMAUTH", BLUE), halfLp(false));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);
        row2.addView(makeStatBox("Nhân viên", String.valueOf(employeeCount), "Toàn hệ thống", GREEN), halfLp(true));
        row2.addView(makeStatBox("Chưa có quyền", String.valueOf(unassigned), "Có thể tạo account", ORANGE), halfLp(false));

        card.addView(row1);
        card.addView(row2);

        layoutBody.addView(card);
    }

    private void buildRoleSummaryCard() {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("PHÂN BỔ QUYỀN"));

        card.addView(makeRoleRow("AD", "Admin", getRoleCount("AD"), PRIMARY));
        card.addView(makeRoleRow("HR", "Quản lý nhân sự", getRoleCount("HR"), GREEN));
        card.addView(makeRoleRow("FN", "Quản lý tài chính", getRoleCount("FN"), BLUE));
        card.addView(makeRoleRow("OF", "Quản lý văn phòng", getRoleCount("OF"), PURPLE));
        card.addView(makeRoleRow("EM", "Nhân viên", getRoleCount("EM"), ORANGE));

        layoutBody.addView(card);
    }

    private void buildQuickActionCard() {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("ĐIỀU HƯỚNG NHANH"));

        card.addView(makeActionButton("Quản lý tài khoản & phân quyền", "Tạo quyền, đổi role, reset mật khẩu, xóa tài khoản.", BLUE, v -> {
            startActivity(new Intent(this, AdminRoleActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }));

        card.addView(makeActionButton("Nhật ký hoạt động", "Xem ACTIVITY_LOG theo action, user và khoảng ngày.", GREEN, v -> {
            startActivity(new Intent(this, AdminActivityLogActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }));

        card.addView(makeActionButton("Về trang HR", "Mở lại trang quản lý nhân sự hiện có.", PRIMARY, v -> {
            startActivity(new Intent(this, HRActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }));

        layoutBody.addView(card);
    }

    private void buildLatestLogCard() {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("HOẠT ĐỘNG GẦN ĐÂY"));

        Cursor c = db.rawQuery(
                "SELECT IFNULL(ActionCode, ''), IFNULL(ActionTime, ''), IFNULL(PerformedBy, ''), IFNULL(Description, '') " +
                        "FROM ACTIVITY_LOG ORDER BY ActionTime DESC, LogID DESC LIMIT 5",
                null
        );

        if (c.getCount() == 0) {
            TextView empty = makeText("Chưa có nhật ký hoạt động.", 13, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(16), 0, dp(16));
            card.addView(empty);
        } else {
            while (c.moveToNext()) {
                card.addView(makeLogMiniRow(
                        c.getString(0),
                        c.getString(1),
                        c.getString(2),
                        c.getString(3)
                ));
            }
        }

        c.close();
        layoutBody.addView(card);
    }

    private LinearLayout makeRoleRow(String code, String name, int count, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView left = makeText(code + " • " + name, 13, color, true);
        TextView right = makeText(count + " tài khoản", 13, TEXT, true);
        right.setGravity(Gravity.RIGHT);

        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
    }

    private View makeActionButton(String title, String sub, int color, View.OnClickListener listener) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackgroundResource(R.drawable.employee_input_bg);
        box.setClickable(true);
        box.setFocusable(true);
        box.setOnClickListener(v -> {
            animateClick(box);
            listener.onClick(v);
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);

        box.addView(makeText(title, 15, color, true));
        TextView s = makeText(sub, 12, SUB, false);
        s.setPadding(0, dp(3), 0, 0);
        box.addView(s);

        return box;
    }

    private View makeLogMiniRow(String action, String time, String user, String desc) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(9), dp(12), dp(9));
        box.setBackgroundResource(R.drawable.employee_input_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(8));
        box.setLayoutParams(lp);

        box.addView(makeText(action + " • " + user, 13, PRIMARY, true));
        box.addView(makeText(time, 11, SUB, false));

        TextView d = makeText(desc, 12, TEXT, false);
        d.setPadding(0, dp(3), 0, 0);
        box.addView(d);

        return box;
    }

    private int getRoleCount(String prefix) {
        return getInt(
                "SELECT COUNT(*) FROM USERS WHERE UPPER(SUBSTR(IFNULL(AUTH, ''), 1, 2)) = ?",
                new String[]{prefix}
        );
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

    private TextView makeCardTitle(String text) {
        TextView tv = makeText(text, 13, PRIMARY, true);
        tv.setPadding(0, 0, 0, dp(10));
        return tv;
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

    private int getInt(String sql, String[] args) {
        Cursor c = db.rawQuery(sql, args);
        int value = 0;

        if (c.moveToFirst()) {
            value = c.getInt(0);
        }

        c.close();
        return value;
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

    private void animateClick(View view) {
        view.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .alpha(0.85f)
                .setDuration(80)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setInterpolator(new OvershootInterpolator())
                        .setDuration(190)
                        .start())
                .start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}