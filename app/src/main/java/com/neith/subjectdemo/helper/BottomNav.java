package com.neith.subjectdemo.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.fn.FNActivity;
import com.neith.subjectdemo.admin.AdminActivityLogActivity;
import com.neith.subjectdemo.admin.AdminHomeActivity;
import com.neith.subjectdemo.admin.AdminRoleActivity;
import com.neith.subjectdemo.admin.AdminDataActivity;
import com.neith.subjectdemo.hr.DepartmentActivity;
import com.neith.subjectdemo.hr.EmployeeActivity;
import com.neith.subjectdemo.hr.HRActivity;
import com.neith.subjectdemo.hr.ProjectActivity;
import com.neith.subjectdemo.hr.WorkScheduleActivity;
import com.neith.subjectdemo.fn.FNActivity;

public class BottomNav {

    public static final int HOME = 0;
    public static final int EMPLOYEE = 1;
    public static final int DEPARTMENT = 2;
    public static final int PROJECT = 3;
    public static final int WORK_SCHEDULE = 4;

    public static final int ADMIN_HOME = 100;
    public static final int ADMIN_ROLE = 101;
    public static final int ADMIN_ACTIVITY_LOG = 102;
    public static final int ADMIN_DATA = 103;
    public static final int ADMIN_HR = 104;

    public static final int ADMIN_FN = 105;

    public static void setup(Activity activity, LinearLayout container, int selectedIndex) {
        container.removeAllViews();
        container.setClipChildren(false);
        container.setClipToPadding(false);

        // Xác định trang Home dựa trên quyền
        String auth = activity.getSharedPreferences("LOGIN_CACHE", Context.MODE_PRIVATE).getString("AUTH", "");
        Class<?> homeClass = HRActivity.class;
        if (auth != null && auth.startsWith("FN")) {
            homeClass = FNActivity.class;
        }

        addItem(activity, container, "Home", R.drawable.ic_home, HOME, selectedIndex, homeClass);
        addItem(activity, container, "Employee", R.drawable.ic_employee, EMPLOYEE, selectedIndex, EmployeeActivity.class);
        addItem(activity, container, "Department", R.drawable.ic_department, DEPARTMENT, selectedIndex, DepartmentActivity.class);
        addItem(activity, container, "Project", R.drawable.ic_project, PROJECT, selectedIndex, ProjectActivity.class);
        addItem(activity, container, "Work", R.drawable.ic_work_schedule, WORK_SCHEDULE, selectedIndex, WorkScheduleActivity.class);
    }

    public static void setupAdmin(Activity activity, LinearLayout container, int selectedIndex) {
        container.removeAllViews();
        container.setClipChildren(false);
        container.setClipToPadding(false);

        addItem(activity, container, "Admin", R.drawable.ic_home, ADMIN_HOME, selectedIndex, AdminHomeActivity.class);
        addItem(activity, container, "Roles", R.drawable.ic_employee, ADMIN_ROLE, selectedIndex, AdminRoleActivity.class);
        addItem(activity, container, "Logs", R.drawable.ic_project, ADMIN_ACTIVITY_LOG, selectedIndex, AdminActivityLogActivity.class);
        addItem(activity, container, "Data", R.drawable.ic_db, ADMIN_DATA, selectedIndex, AdminDataActivity.class);
        addItem(activity, container, "HR", R.drawable.ic_work_schedule, ADMIN_HR, selectedIndex, HRActivity.class);
        addItem(activity, container, "Finance", R.drawable.ic_finance, ADMIN_FN, selectedIndex, FNActivity.class);
    }

    private static void addItem(Activity activity,
                                LinearLayout container,
                                String title,
                                int iconRes,
                                int index,
                                int selectedIndex,
                                Class<?> targetActivity) {

        boolean selected = index == selectedIndex;

        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        item.setLayoutParams(itemLp);

        FrameLayout iconHolder = new FrameLayout(activity);
        int holderSize = selected ? dp(activity, 46) : dp(activity, 34);
        LinearLayout.LayoutParams holderLp = new LinearLayout.LayoutParams(holderSize, holderSize);
        if (selected) {
            iconHolder.setBackgroundResource(R.drawable.bottom_nav_selected_bg);
            holderLp.topMargin = dp(activity, 2);
        }

        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setColorFilter(selected ? Color.BLACK : Color.WHITE);
        icon.setAlpha(selected ? 1f : 0.6f);

        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(activity, selected ? 22 : 20), dp(activity, selected ? 22 : 20));
        iconLp.gravity = Gravity.CENTER;

        iconHolder.addView(icon, iconLp);
        item.addView(iconHolder, holderLp);

        TextView text = new TextView(activity);
        text.setText(title);
        text.setTextColor(Color.WHITE);
        text.setAlpha(selected ? 1f : 0.6f);
        text.setTextSize(selected ? 12 : 11);
        text.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        item.addView(text);

        item.setOnClickListener(v -> {
            animateClick(item);
            if (index == selectedIndex) return;
        text.setGravity(Gravity.CENTER);
        text.setSingleLine(true);

        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textLp.topMargin = dp(activity, 4);

        item.addView(text, textLp);

        item.setOnClickListener(v -> {
            animateClick(item);

            if (index == selectedIndex) {
                return;
            }

            Intent intent = new Intent(activity, targetActivity);
            
            // QUAN TRỌNG: Chuyển tiếp session để tránh lỗi dashboard
            String username = activity.getIntent().getStringExtra("USERNAME");
            String auth = activity.getIntent().getStringExtra("AUTH");
            intent.putExtra("USERNAME", username);
            intent.putExtra("AUTH", auth);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        });

        container.addView(item);
    }

    private static void animateClick(View view) {
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).withEndAction(() -> 
            view.animate().scaleX(1f).scaleY(1f).setInterpolator(new OvershootInterpolator()).setDuration(200).start()
        ).start();
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}