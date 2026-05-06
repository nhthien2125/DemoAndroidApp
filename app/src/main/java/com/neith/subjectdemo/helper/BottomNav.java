package com.neith.subjectdemo.helper;

import android.app.Activity;
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
import com.neith.subjectdemo.hr.DepartmentActivity;
import com.neith.subjectdemo.hr.EmployeeActivity;
import com.neith.subjectdemo.hr.HRActivity;
import com.neith.subjectdemo.hr.ProjectActivity;
import com.neith.subjectdemo.hr.WorkScheduleActivity;

public class BottomNav{

    public static final int HOME = 0;
    public static final int EMPLOYEE = 1;
    public static final int DEPARTMENT = 2;
    public static final int PROJECT = 3;
    public static final int WORK_SCHEDULE = 4;

    public static void setup(Activity activity, LinearLayout container, int selectedIndex) {
        container.removeAllViews();
        container.setClipChildren(false);
        container.setClipToPadding(false);

        addItem(activity, container, "Home", R.drawable.ic_home, HOME, selectedIndex, HRActivity.class);
        addItem(activity, container, "Employee", R.drawable.ic_employee, EMPLOYEE, selectedIndex, EmployeeActivity.class);
        addItem(activity, container, "Department", R.drawable.ic_department, DEPARTMENT, selectedIndex, DepartmentActivity.class);
        addItem(activity, container, "Project", R.drawable.ic_project, PROJECT, selectedIndex, ProjectActivity.class);
        addItem(activity, container, "Work", R.drawable.ic_work_schedule, WORK_SCHEDULE, selectedIndex, WorkScheduleActivity.class);
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
        item.setClipChildren(false);
        item.setClipToPadding(false);

        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        );
        item.setLayoutParams(itemLp);

        FrameLayout iconHolder = new FrameLayout(activity);
        iconHolder.setForegroundGravity(Gravity.CENTER);

        int holderSize = selected ? dp(activity, 46) : dp(activity, 34);

        LinearLayout.LayoutParams holderLp = new LinearLayout.LayoutParams(
                holderSize,
                holderSize
        );

        if (selected) {
            iconHolder.setBackgroundResource(R.drawable.bottom_nav_selected_bg);
            holderLp.topMargin = dp(activity, 2);
        }

        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setColorFilter(selected ? Color.BLACK : Color.WHITE);
        icon.setAlpha(selected ? 1f : 0.6f);

        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(
                selected ? dp(activity, 22) : dp(activity, 20),
                selected ? dp(activity, 22) : dp(activity, 20)
        );
        iconLp.gravity = Gravity.CENTER;

        iconHolder.addView(icon, iconLp);
        item.addView(iconHolder, holderLp);

        TextView text = new TextView(activity);
        text.setText(title);
        text.setTextColor(Color.WHITE);
        text.setAlpha(selected ? 1f : 0.6f);
        text.setTextSize(selected ? 12 : 11);
        text.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        text.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textLp.topMargin = dp(activity, 4);

        item.addView(text, textLp);

        item.setOnClickListener(v -> {
            animateClick(item);

            if (index == selectedIndex) return;

            Intent intent = new Intent(activity, targetActivity);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        container.addView(item);
    }

    private static void animateClick(View view) {
        view.animate()
                .scaleX(0.90f)
                .scaleY(0.90f)
                .alpha(0.75f)
                .setDuration(90)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setInterpolator(new OvershootInterpolator())
                        .setDuration(220)
                        .start())
                .start();
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}