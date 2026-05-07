package com.neith.subjectdemo.hr;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.ActivityLogger;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class WorkScheduleActivity extends AppCompatActivity {

    SQLiteDatabase db;

    TextView txtWeekRange, txtWorkFilterArrow;
    EditText edtSearchWorkEmployee;
    Button btnPrevWeek, btnNextWeek, btnApplyWorkFilter, btnResetWorkFilter;
    Spinner spnWorkDepartment, spnWorkPosition, spnWorkStatus, spnWorkFactor;
    LinearLayout layoutWeekOverview, layoutEmployeeList;
    LinearLayout layoutWorkFilterToggle, layoutWorkFilterContent;

    Calendar weekStart = Calendar.getInstance();

    ArrayList<OptionItem> departments = new ArrayList<>();
    ArrayList<OptionItem> positions = new ArrayList<>();
    ArrayList<OptionItem> statuses = new ArrayList<>();
    ArrayList<OptionItem> factors = new ArrayList<>();

    String selectedDepartment = "";
    String selectedPosition = "";
    String selectedStatus = "ALL";
    String selectedFactor = "ALL";

    boolean isFilterExpanded = false;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat viewDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    HashMap<String, ArrayList<ScheduleCell>> dayScheduleMap = new HashMap<>();
    HashMap<String, ScheduleCell> employeeDateMap = new HashMap<>();
    HashMap<String, HolidayCell> holidayMap = new HashMap<>();

    LinearLayout currentDialogRoot;

    String[] dayNames = {
            "Thứ hai",
            "Thứ ba",
            "Thứ tư",
            "Thứ năm",
            "Thứ sáu",
            "Thứ bảy",
            "Chủ nhật"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_schedule);

        db = DB.openDatabase(this);

        txtWeekRange = findViewById(R.id.txtWeekRange);
        txtWorkFilterArrow = findViewById(R.id.txtWorkFilterArrow);

        edtSearchWorkEmployee = findViewById(R.id.edtSearchWorkEmployee);

        btnPrevWeek = findViewById(R.id.btnPrevWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);
        btnApplyWorkFilter = findViewById(R.id.btnApplyWorkFilter);
        btnResetWorkFilter = findViewById(R.id.btnResetWorkFilter);

        spnWorkDepartment = findViewById(R.id.spnWorkDepartment);
        spnWorkPosition = findViewById(R.id.spnWorkPosition);
        spnWorkStatus = findViewById(R.id.spnWorkStatus);
        spnWorkFactor = findViewById(R.id.spnWorkFactor);

        layoutWeekOverview = findViewById(R.id.layoutWeekOverview);
        layoutEmployeeList = findViewById(R.id.layoutEmployeeList);
        layoutWorkFilterToggle = findViewById(R.id.layoutWorkFilterToggle);
        layoutWorkFilterContent = findViewById(R.id.layoutWorkFilterContent);

        btnPrevWeek.setBackgroundTintList(null);
        btnNextWeek.setBackgroundTintList(null);
        btnApplyWorkFilter.setBackgroundTintList(null);
        btnResetWorkFilter.setBackgroundTintList(null);

        layoutWorkFilterContent.setVisibility(View.GONE);
        txtWorkFilterArrow.setText("▼");

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.WORK_SCHEDULE);

        setCurrentWeek();
        loadFilterData();
        setupFilterSpinners();

        layoutWorkFilterToggle.setOnClickListener(v -> toggleFilter());

        btnPrevWeek.setOnClickListener(v -> {
            weekStart.add(Calendar.DAY_OF_MONTH, -7);
            animateClick(btnPrevWeek);
            loadWeek();
        });

        btnNextWeek.setOnClickListener(v -> {
            weekStart.add(Calendar.DAY_OF_MONTH, 7);
            animateClick(btnNextWeek);
            loadWeek();
        });

        btnApplyWorkFilter.setOnClickListener(v -> loadWeek());

        btnResetWorkFilter.setOnClickListener(v -> {
            edtSearchWorkEmployee.setText("");
            spnWorkDepartment.setSelection(0);
            spnWorkPosition.setSelection(0);
            spnWorkStatus.setSelection(0);
            spnWorkFactor.setSelection(0);

            selectedDepartment = "";
            selectedPosition = "";
            selectedStatus = "ALL";
            selectedFactor = "ALL";

            loadWeek();
        });

        edtSearchWorkEmployee.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                loadWeek();
            }
        });

        loadWeek();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFilterData();
        refreshFilterSpinnersKeepSelection();
        loadWeek();
    }

    private void toggleFilter() {
        if (isFilterExpanded) {
            isFilterExpanded = false;
            txtWorkFilterArrow.setText("▼");

            layoutWorkFilterContent.animate()
                    .alpha(0f)
                    .translationY(-dp(8))
                    .setDuration(160)
                    .withEndAction(() -> {
                        layoutWorkFilterContent.setVisibility(View.GONE);
                        layoutWorkFilterContent.setAlpha(1f);
                        layoutWorkFilterContent.setTranslationY(0);
                    })
                    .start();
        } else {
            isFilterExpanded = true;
            txtWorkFilterArrow.setText("▲");

            layoutWorkFilterContent.setVisibility(View.VISIBLE);
            layoutWorkFilterContent.setAlpha(0f);
            layoutWorkFilterContent.setTranslationY(-dp(8));

            layoutWorkFilterContent.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setInterpolator(new OvershootInterpolator())
                    .setDuration(220)
                    .start();
        }
    }

    private void setCurrentWeek() {
        Calendar today = Calendar.getInstance();
        int day = today.get(Calendar.DAY_OF_WEEK);

        int diff;

        if (day == Calendar.SUNDAY) {
            diff = -6;
        } else {
            diff = Calendar.MONDAY - day;
        }

        weekStart.setTime(today.getTime());
        weekStart.add(Calendar.DAY_OF_MONTH, diff);
        clearTime(weekStart);
    }

    private void loadFilterData() {
        departments.clear();
        positions.clear();
        statuses.clear();
        factors.clear();

        departments.add(new OptionItem("", "Tất cả phòng ban"));

        Cursor cDept = db.rawQuery(
                "SELECT MAPB, TENPB FROM PHONGBAN ORDER BY TENPB",
                null
        );

        while (cDept.moveToNext()) {
            departments.add(new OptionItem(cDept.getString(0), cDept.getString(1)));
        }

        cDept.close();

        positions.add(new OptionItem("", "Tất cả chức vụ"));

        Cursor cPos = db.rawQuery(
                "SELECT MACV, TENCV FROM VITRICONGVIEC ORDER BY TENCV",
                null
        );

        while (cPos.moveToNext()) {
            positions.add(new OptionItem(cPos.getString(0), cPos.getString(1)));
        }

        cPos.close();

        statuses.add(new OptionItem("ALL", "Tất cả trạng thái"));
        statuses.add(new OptionItem("HAS_SCHEDULE", "Có lịch trong tuần"));
        statuses.add(new OptionItem("NO_SCHEDULE", "Chưa có lịch trong tuần"));
        statuses.add(new OptionItem("FULL_WEEK", "Đủ 7 ngày"));
        statuses.add(new OptionItem("SUNDAY", "Có lịch Chủ nhật"));
        statuses.add(new OptionItem("HOLIDAY", "Có lịch ngày lễ"));

        factors.add(new OptionItem("ALL", "Tất cả hệ số"));
        factors.add(new OptionItem("NORMAL", "Hệ số thường / trống"));
        factors.add(new OptionItem("GT1", "Hệ số > 1"));
        factors.add(new OptionItem("GTE2", "Hệ số >= 2"));
        factors.add(new OptionItem("GTE3", "Hệ số >= 3"));
    }

    private void setupFilterSpinners() {
        spnWorkDepartment.setAdapter(new WhiteSpinnerAdapter<>(departments));
        spnWorkPosition.setAdapter(new WhiteSpinnerAdapter<>(positions));
        spnWorkStatus.setAdapter(new WhiteSpinnerAdapter<>(statuses));
        spnWorkFactor.setAdapter(new WhiteSpinnerAdapter<>(factors));

        spnWorkDepartment.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnWorkPosition.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnWorkStatus.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnWorkFactor.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);

        spnWorkDepartment.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDepartment = departments.get(position).id;
            }
        });

        spnWorkPosition.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = positions.get(position).id;
            }
        });

        spnWorkStatus.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statuses.get(position).id;
            }
        });

        spnWorkFactor.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFactor = factors.get(position).id;
            }
        });
    }

    private void refreshFilterSpinnersKeepSelection() {
        String oldDept = selectedDepartment;
        String oldPos = selectedPosition;
        String oldStatus = selectedStatus;
        String oldFactor = selectedFactor;

        setupFilterSpinners();

        spnWorkDepartment.setSelection(findOptionIndex(departments, oldDept));
        spnWorkPosition.setSelection(findOptionIndex(positions, oldPos));
        spnWorkStatus.setSelection(findOptionIndex(statuses, oldStatus));
        spnWorkFactor.setSelection(findOptionIndex(factors, oldFactor));

        selectedDepartment = oldDept == null ? "" : oldDept;
        selectedPosition = oldPos == null ? "" : oldPos;
        selectedStatus = oldStatus == null ? "ALL" : oldStatus;
        selectedFactor = oldFactor == null ? "ALL" : oldFactor;
    }

    private void loadWeek() {
        layoutWeekOverview.removeAllViews();
        layoutEmployeeList.removeAllViews();

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);

        txtWeekRange.setText(
                "Tuần từ " + viewDateFormat.format(weekStart.getTime()) +
                        " đến " + viewDateFormat.format(weekEnd.getTime())
        );

        loadSchedulesAndHolidays();

        buildWeekOverview();

        ArrayList<EmployeeItem> employees = loadEmployees();

        buildEmployeeList(employees);
    }

    private void buildWeekOverview() {
        LinearLayout card = makeCard();

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = makeText("TỔNG QUAN TUẦN", 13, PRIMARY, true);
        TextView note = makeText("Bấm ngày để thêm nhiều nhân viên", 11, SUB, false);
        note.setGravity(Gravity.RIGHT);

        titleRow.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleRow.addView(note);

        card.addView(titleRow);

        LinearLayout daysRow = new LinearLayout(this);
        daysRow.setOrientation(LinearLayout.HORIZONTAL);
        daysRow.setPadding(0, dp(12), 0, 0);

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            String date = dbDateFormat.format(day.getTime());
            ArrayList<ScheduleCell> schedules = dayScheduleMap.get(date);
            HolidayCell holiday = holidayMap.get(date);
            boolean isSunday = day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

            daysRow.addView(makeOverviewDayCard(day, i, schedules, holiday, isSunday));
        }

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(daysRow);

        card.addView(hsv);
        layoutWeekOverview.addView(card);
    }

    private View makeOverviewDayCard(
            Calendar day,
            int index,
            ArrayList<ScheduleCell> schedules,
            HolidayCell holiday,
            boolean isSunday
    ) {
        String date = dbDateFormat.format(day.getTime());
        int count = schedules == null ? 0 : schedules.size();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(makeOverviewBg(isSunday, holiday != null));
        box.setClickable(true);
        box.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(210), LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(10), 0);
        box.setLayoutParams(lp);

        TextView dayText = makeText(dayNames[index], 14, isSunday ? RED : PRIMARY, true);
        box.addView(dayText);

        TextView dateText = makeText(viewDateFormat.format(day.getTime()), 12, SUB, false);
        dateText.setPadding(0, dp(2), 0, dp(6));
        box.addView(dateText);

        TextView countText = makeText(count + " nhân viên có lịch", 13, count > 0 ? GREEN : SUB, true);
        box.addView(countText);

        if (holiday != null) {
            TextView h = makeText("Ngày lễ: " + emptyText(holiday.name), 11, ORANGE, true);
            h.setPadding(0, dp(4), 0, 0);
            h.setMaxLines(1);
            box.addView(h);
        }

        TextView names = makeText(getScheduleNames(schedules), 11, TEXT, false);
        names.setPadding(0, dp(6), 0, dp(4));
        names.setMaxLines(5);
        box.addView(names);

        TextView add = makeText("+ Thêm lịch ngày này", 12, PRIMARY, true);
        add.setPadding(0, dp(6), 0, 0);
        box.addView(add);

        box.setOnClickListener(v -> {
            animateClick(box);

            if (isPastDate(date)) {
                toast("Không thể thêm lịch cho ngày cũ hơn hôm nay.");
                return;
            }

            showBulkAddDialog(date, dayNames[index] + ", " + viewDateFormat.format(day.getTime()), holiday, isSunday);

        });

        return box;
    }

    private GradientDrawable makeOverviewBg(boolean isSunday, boolean isHoliday) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));

        if (isHoliday) {
            bg.setColor(Color.parseColor("#241A08"));
        } else if (isSunday) {
            bg.setColor(Color.parseColor("#2A1010"));
        } else {
            bg.setColor(Color.parseColor("#111827"));
        }

        return bg;
    }

    private String getScheduleNames(ArrayList<ScheduleCell> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return "Chưa có ai.";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(schedules.size(), 5);

        for (int i = 0; i < limit; i++) {
            ScheduleCell s = schedules.get(i);
            sb.append("• ").append(emptyText(s.hoTen));

            if (!isEmpty(s.content)) {
                sb.append(": ").append(s.content);
            }

            if (i < limit - 1) {
                sb.append("\n");
            }
        }

        if (schedules.size() > limit) {
            sb.append("\n+ ").append(schedules.size() - limit).append(" người khác");
        }

        return sb.toString();
    }

    private void buildEmployeeList(ArrayList<EmployeeItem> employees) {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("DANH SÁCH NHÂN VIÊN"));

        TextView sub = makeText(
                "Bấm nhân viên để mở WorkSchedulePersonal và thêm lịch riêng theo ràng buộc dự án.",
                12,
                SUB,
                false
        );
        sub.setPadding(0, 0, 0, dp(8));
        card.addView(sub);

        if (employees.isEmpty()) {
            TextView empty = makeText("Không có nhân viên phù hợp bộ lọc.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, dp(28));
            card.addView(empty);
        } else {
            for (EmployeeItem emp : employees) {
                card.addView(makeEmployeeCard(emp));
            }
        }

        layoutEmployeeList.addView(card);
    }

    private View makeEmployeeCard(EmployeeItem emp) {
        int days = countEmployeeSchedules(emp.maNV);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackgroundResource(R.drawable.employee_input_bg);
        box.setClickable(true);
        box.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = makeAvatar(emp.hoTen);
        row.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);

        info.addView(makeText(emp.hoTen, 15, TEXT, true));
        info.addView(makeText(emp.maNV + " • " + emptyText(emp.phongBan) + " • " + emptyText(emp.chucVu), 12, SUB, false));

        TextView scheduleInfo = makeText(days + "/7 ngày có lịch", 12, days > 0 ? GREEN : ORANGE, true);
        scheduleInfo.setPadding(0, dp(4), 0, 0);
        info.addView(scheduleInfo);

        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView next = makeText("›", 28, PRIMARY, true);
        next.setGravity(Gravity.CENTER);
        row.addView(next, new LinearLayout.LayoutParams(dp(32), dp(48)));

        box.addView(row);

        box.setOnClickListener(v -> {
            animateClick(box);

            Intent intent = new Intent(this, WorkSchedulePersonalActivity.class);
            intent.putExtra("MANV", emp.maNV);
            intent.putExtra("WEEK_START", dbDateFormat.format(weekStart.getTime()));
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        return box;
    }

    private ArrayList<EmployeeItem> loadEmployees() {
        ArrayList<EmployeeItem> list = new ArrayList<>();
        String keyword = edtSearchWorkEmployee.getText().toString().trim();

        String start = dbDateFormat.format(weekStart.getTime());
        Calendar endCal = (Calendar) weekStart.clone();
        endCal.add(Calendar.DAY_OF_MONTH, 6);
        String end = dbDateFormat.format(endCal.getTime());

        StringBuilder sql = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        sql.append("SELECT NV.MANV, ");
        sql.append("IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, ");
        sql.append("IFNULL(PB.TENPB, ''), IFNULL(CV.TENCV, ''), ");
        sql.append("IFNULL(NV.MAPB, ''), IFNULL(NV.MACV, '') ");
        sql.append("FROM NHANVIEN NV ");
        sql.append("LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB ");
        sql.append("LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV ");
        sql.append("WHERE 1 = 1 ");

        if (!keyword.isEmpty()) {
            sql.append("AND (NV.MANV LIKE ? OR IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') LIKE ?) ");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }

        if (!isEmpty(selectedDepartment)) {
            sql.append("AND NV.MAPB = ? ");
            args.add(selectedDepartment);
        }

        if (!isEmpty(selectedPosition)) {
            sql.append("AND NV.MACV = ? ");
            args.add(selectedPosition);
        }

        if ("HAS_SCHEDULE".equals(selectedStatus)) {
            sql.append("AND NV.MANV IN (SELECT MANV FROM LICHLAMVIEC WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ?) ");
            args.add(start);
            args.add(end);
        } else if ("NO_SCHEDULE".equals(selectedStatus)) {
            sql.append("AND NV.MANV NOT IN (SELECT MANV FROM LICHLAMVIEC WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ?) ");
            args.add(start);
            args.add(end);
        } else if ("FULL_WEEK".equals(selectedStatus)) {
            sql.append("AND NV.MANV IN (SELECT MANV FROM LICHLAMVIEC WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ? GROUP BY MANV HAVING COUNT(DISTINCT NGAYLAMVIEC) >= 7) ");
            args.add(start);
            args.add(end);
        } else if ("SUNDAY".equals(selectedStatus)) {
            sql.append("AND NV.MANV IN (SELECT MANV FROM LICHLAMVIEC WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ? AND IFNULL(CHUNHAT, 0) = 1) ");
            args.add(start);
            args.add(end);
        } else if ("HOLIDAY".equals(selectedStatus)) {
            sql.append("AND NV.MANV IN (SELECT L.MANV FROM LICHLAMVIEC L JOIN DSNGAYLE H ON L.NGAYLAMVIEC = H.NGAYLE WHERE L.NGAYLAMVIEC >= ? AND L.NGAYLAMVIEC <= ?) ");
            args.add(start);
            args.add(end);
        }

        if ("NORMAL".equals(selectedFactor)) {
            sql.append("AND NV.MANV IN (SELECT MANV FROM LICHLAMVIEC WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ? AND (HESOLUONG IS NULL OR HESOLUONG = '' OR CAST(HESOLUONG AS REAL) <= 1)) ");
            args.add(start);
            args.add(end);
        } else if ("GT1".equals(selectedFactor)) {
            sql.append("AND NV.MANV IN (SELECT MANV FROM LICHLAMVIEC WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ? AND CAST(HESOLUONG AS REAL) > 1) ");
            args.add(start);
            args.add(end);
        } else if ("GTE2".equals(selectedFactor)) {
            sql.append("AND NV.MANV IN (SELECT MANV FROM LICHLAMVIEC WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ? AND CAST(HESOLUONG AS REAL) >= 2) ");
            args.add(start);
            args.add(end);
        } else if ("GTE3".equals(selectedFactor)) {
            sql.append("AND NV.MANV IN (SELECT MANV FROM LICHLAMVIEC WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ? AND CAST(HESOLUONG AS REAL) >= 3) ");
            args.add(start);
            args.add(end);
        }

        sql.append("ORDER BY NV.HOLOT, NV.TENNV");

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));

        while (c.moveToNext()) {
            EmployeeItem item = new EmployeeItem();
            item.maNV = c.getString(0);
            item.hoTen = c.getString(1).trim();
            item.phongBan = c.getString(2);
            item.chucVu = c.getString(3);
            item.maPB = c.getString(4);
            item.maCV = c.getString(5);
            list.add(item);
        }

        c.close();

        return list;
    }

    private void loadSchedulesAndHolidays() {
        dayScheduleMap.clear();
        employeeDateMap.clear();
        holidayMap.clear();

        String start = dbDateFormat.format(weekStart.getTime());

        Calendar endCal = (Calendar) weekStart.clone();
        endCal.add(Calendar.DAY_OF_MONTH, 6);
        String end = dbDateFormat.format(endCal.getTime());

        Cursor cSchedule = db.rawQuery(
                "SELECT L.MANV, L.NGAYLAMVIEC, " +
                        "IFNULL(L.NOIDUNGLAMVIEC, ''), " +
                        "IFNULL(L.HESOLUONG, ''), " +
                        "IFNULL(L.CHUNHAT, 0), " +
                        "IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN " +
                        "FROM LICHLAMVIEC L " +
                        "JOIN NHANVIEN NV ON L.MANV = NV.MANV " +
                        "WHERE L.NGAYLAMVIEC >= ? AND L.NGAYLAMVIEC <= ? " +
                        "ORDER BY L.NGAYLAMVIEC, NV.HOLOT, NV.TENNV",
                new String[]{start, end}
        );

        while (cSchedule.moveToNext()) {
            ScheduleCell cell = new ScheduleCell();
            cell.maNV = cSchedule.getString(0);
            cell.date = cSchedule.getString(1);
            cell.content = cSchedule.getString(2);
            cell.factor = cSchedule.getString(3);
            cell.isSunday = cSchedule.getInt(4) != 0;
            cell.hoTen = cSchedule.getString(5).trim();

            if (!dayScheduleMap.containsKey(cell.date)) {
                dayScheduleMap.put(cell.date, new ArrayList<>());
            }

            dayScheduleMap.get(cell.date).add(cell);
            employeeDateMap.put(buildKey(cell.maNV, cell.date), cell);
        }

        cSchedule.close();

        Cursor cHoliday = db.rawQuery(
                "SELECT NGAYLE, IFNULL(NOIDUNG, '') FROM DSNGAYLE WHERE NGAYLE >= ? AND NGAYLE <= ?",
                new String[]{start, end}
        );

        while (cHoliday.moveToNext()) {
            HolidayCell h = new HolidayCell();
            h.date = cHoliday.getString(0);
            h.name = cHoliday.getString(1);
            holidayMap.put(h.date, h);
        }

        cHoliday.close();
    }

    private void showBulkAddDialog(String date, String dateLabel, HolidayCell holiday, boolean isSunday) {
        if (isPastDate(date)) {
            toast("Không thể thêm lịch cho ngày cũ hơn hôm nay.");
            return;
        }
        ActivityLogger.log(db, "BULK_ADD_SCHEDULE", "HR",
                "Đã lưu lịch ngày " + date + " cho " + " nhân viên. Nội dung: ");
        Dialog dialog = createStyledDialog("Thêm lịch theo ngày");

        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView info = makeText(dateLabel, 14, PRIMARY, true);
        info.setPadding(0, 0, 0, dp(8));
        content.addView(info);

        if (holiday != null) {
            TextView h = makeText("Ngày lễ: " + emptyText(holiday.name), 13, ORANGE, true);
            h.setPadding(0, 0, 0, dp(8));
            content.addView(h);
        }

        EditText edtContent = makeDarkInput("Nội dung công việc áp dụng cho các nhân viên đã chọn");
        edtContent.setMinLines(3);
        edtContent.setGravity(Gravity.TOP);
        edtContent.setSingleLine(false);

        EditText edtFactor = makeDarkInput("Hệ số lương, ví dụ 1.5 hoặc 2");
        edtFactor.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText edtSearch = makeDarkInput("Tìm nhân viên đủ điều kiện");
        edtSearch.setSingleLine(true);

        LinearLayout employeeBox = new LinearLayout(this);
        employeeBox.setOrientation(LinearLayout.VERTICAL);

        ArrayList<EmployeeItem> allEmployees = loadEligibleEmployeesForDate(date);
        ArrayList<CheckBox> checkBoxes = new ArrayList<>();

        Runnable[] render = new Runnable[1];

        render[0] = () -> {
            employeeBox.removeAllViews();
            checkBoxes.clear();

            String keyword = edtSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

            for (EmployeeItem emp : allEmployees) {
                String source = (emp.maNV + " " + emp.hoTen + " " + emp.phongBan + " " + emp.chucVu).toLowerCase(Locale.getDefault());

                if (!keyword.isEmpty() && !source.contains(keyword)) {
                    continue;
                }

                CheckBox cb = new CheckBox(this);
                cb.setText(emp.hoTen + "\n" + emp.maNV + " • " + emptyText(emp.phongBan) + " • " + emptyText(emp.chucVu));
                cb.setTextColor(TEXT);
                cb.setTextSize(13);
                cb.setButtonTintList(ColorStateList.valueOf(PRIMARY));
                cb.setPadding(0, dp(8), 0, dp(8));
                cb.setTag(emp.maNV);
                cb.setChecked(employeeDateMap.containsKey(buildKey(emp.maNV, date)));

                employeeBox.addView(cb);
                checkBoxes.add(cb);
            }

            if (employeeBox.getChildCount() == 0) {
                TextView empty = makeText(
                        "Không có nhân viên đủ điều kiện.\nNhân viên phải thuộc dự án có ngày này nằm trong thời gian dự án.",
                        13,
                        SUB,
                        false
                );
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, dp(14), 0, dp(14));
                employeeBox.addView(empty);
            }
        };

        edtSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                render[0].run();
            }
        });

        render[0].run();

        content.addView(makeDialogLabel("Nội dung công việc"));
        content.addView(edtContent);
        content.addView(makeDialogLabel("Hệ số lương"));
        content.addView(edtFactor);
        content.addView(makeDialogLabel("Tìm và chọn nhiều nhân viên"));
        content.addView(edtSearch);

        TextView pickHint = makeText(
                "Chỉ hiển thị nhân viên đang nằm trong dự án hoạt động ở ngày này.",
                12,
                SUB,
                false
        );
        pickHint.setPadding(0, dp(4), 0, dp(8));
        content.addView(pickHint);

        content.addView(employeeBox);

        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        currentDialogRoot.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(560)
        ));

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        currentDialogRoot.addView(actions);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String workContent = edtContent.getText().toString().trim();
            String factor = edtFactor.getText().toString().trim();

            if (workContent.isEmpty()) {
                toast("Vui lòng nhập nội dung công việc.");
                return;
            }

            ArrayList<String> selected = new ArrayList<>();

            for (CheckBox cb : checkBoxes) {
                if (cb.isChecked()) {
                    selected.add(String.valueOf(cb.getTag()));
                }
            }

            if (selected.isEmpty()) {
                toast("Vui lòng chọn ít nhất một nhân viên.");
                return;
            }

            if (!isValidFactor(factor)) {
                toast("Hệ số lương không hợp lệ.");
                return;
            }

            int ok = 0;

            for (String maNV : selected) {
                if (isEmployeeEligibleForWorkDate(maNV, date)) {
                    if (saveSchedule(maNV, date, workContent, factor, isSunday, holiday != null, false)) {
                        ok++;
                    }
                }
            }

            toast("Đã lưu lịch cho " + ok + " nhân viên.");
            dialog.dismiss();
            loadWeek();
        });

        dialog.show();
    }

    private ArrayList<EmployeeItem> loadEligibleEmployeesForDate(String date) {
        ArrayList<EmployeeItem> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT DISTINCT NV.MANV, " +
                        "IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "IFNULL(PB.TENPB, ''), IFNULL(CV.TENCV, '') " +
                        "FROM NHANVIEN NV " +
                        "JOIN NVTHAMGIADA TG ON NV.MANV = TG.MANV " +
                        "JOIN DUANTHEOHOPDONG DTHD ON TG.MADAHD = DTHD.MADAHD " +
                        "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "WHERE (IFNULL(HD.NGAYBD, '') = '' OR HD.NGAYBD <= ?) " +
                        "AND ( " +
                        "   IFNULL(NULLIF(DTHD.NGAYKT, ''), IFNULL(NULLIF(HD.NGAYKT_DUTINH, ''), '9999-12-31')) >= ? " +
                        ") " +
                        "ORDER BY NV.HOLOT, NV.TENNV",
                new String[]{date, date}
        );

        while (c.moveToNext()) {
            EmployeeItem item = new EmployeeItem();
            item.maNV = c.getString(0);
            item.hoTen = c.getString(1).trim();
            item.phongBan = c.getString(2);
            item.chucVu = c.getString(3);
            list.add(item);
        }

        c.close();

        return list;
    }

    private boolean saveSchedule(
            String maNV,
            String date,
            String content,
            String factor,
            boolean isSunday,
            boolean isHoliday,
            boolean showToast
    ) {
        if (isPastDate(date)) {
            if (showToast) {
                toast("Không thể thêm lịch cho ngày cũ hơn hôm nay.");
            }
            return false;
        }

        if (!employeeExists(maNV)) {
            if (showToast) {
                toast("Không tìm thấy nhân viên.");
            }
            return false;
        }

        if (!isEmployeeEligibleForWorkDate(maNV, date)) {
            if (showToast) {
                toast("Nhân viên không thuộc dự án hoạt động ở ngày này.");
            }
            return false;
        }

        boolean exists = scheduleExists(maNV, date);

        ContentValues values = new ContentValues();
        values.put("MANV", maNV);
        values.put("NGAYLAMVIEC", date);
        values.put("NOIDUNGLAMVIEC", content);
        values.put("CHUNHAT", isSunday ? 1 : 0);
        values.put("NGAYLE", isHoliday ? 1 : 0);

        if (isEmpty(factor)) {
            values.putNull("HESOLUONG");
        } else {
            values.put("HESOLUONG", parseFactor(factor));
        }

        long result;

        if (exists) {
            result = db.update(
                    "LICHLAMVIEC",
                    values,
                    "MANV = ? AND NGAYLAMVIEC = ?",
                    new String[]{maNV, date}
            );
        } else {
            result = db.insert("LICHLAMVIEC", null, values);
        }

        if (showToast) {
            if (result != -1 && result != 0) {
                ActivityLogger.log(db, "SAVE_SCHEDULE", "HR",
                        (exists ? "Cập nhật" : "Thêm") + " lịch cho nhân viên " + maNV +
                                " ngày " + date + ", nội dung: " + content + ", hệ số: " + factor +
                                (isSunday ? ", Chủ nhật" : "") + (isHoliday ? ", Ngày lễ" : ""));
                toast("Đã lưu lịch làm việc.");
            } else {
                toast("Lưu lịch làm việc thất bại.");
            }
        }

        return result != -1 && result != 0;
    }

    private boolean isEmployeeEligibleForWorkDate(String maNV, String date) {
        if (isPastDate(date)) {
            return false;
        }

        Cursor c = db.rawQuery(
                "SELECT 1 " +
                        "FROM NVTHAMGIADA TG " +
                        "JOIN DUANTHEOHOPDONG DTHD ON TG.MADAHD = DTHD.MADAHD " +
                        "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                        "WHERE TG.MANV = ? " +
                        "AND (IFNULL(HD.NGAYBD, '') = '' OR HD.NGAYBD <= ?) " +
                        "AND IFNULL(NULLIF(DTHD.NGAYKT, ''), IFNULL(NULLIF(HD.NGAYKT_DUTINH, ''), '9999-12-31')) >= ? " +
                        "LIMIT 1",
                new String[]{maNV, date, date}
        );

        boolean ok = c.moveToFirst();
        c.close();

        return ok;
    }

    private int countEmployeeSchedules(String maNV) {
        int count = 0;

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            String date = dbDateFormat.format(day.getTime());

            if (employeeDateMap.containsKey(buildKey(maNV, date))) {
                count++;
            }
        }

        return count;
    }

    private boolean scheduleExists(String maNV, String date) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM LICHLAMVIEC WHERE MANV = ? AND NGAYLAMVIEC = ?",
                new String[]{maNV, date}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private boolean employeeExists(String maNV) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM NHANVIEN WHERE MANV = ?",
                new String[]{maNV}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private String buildKey(String maNV, String date) {
        return (maNV == null ? "" : maNV.trim()) + "|" + date;
    }

    private Calendar clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private Date clearTime(Date date) {
        try {
            return dbDateFormat.parse(dbDateFormat.format(date));
        } catch (Exception e) {
            return date;
        }
    }

    private boolean isPastDate(String date) {
        try {
            Date d = dbDateFormat.parse(date);
            Date today = clearTime(new Date());

            return d != null && d.before(today);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isValidFactor(String factor) {
        if (isEmpty(factor)) {
            return true;
        }

        try {
            return Double.parseDouble(factor.replace(",", ".")) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private double parseFactor(String factor) {
        if (isEmpty(factor)) {
            return 0;
        }

        return Double.parseDouble(factor.replace(",", "."));
    }

    private TextView makeAvatar(String fullName) {
        TextView avatar = makeText(getInitial(fullName), 18, Color.BLACK, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.employee_avatar_bg);

        avatar.setLayoutParams(new LinearLayout.LayoutParams(
                dp(48),
                dp(48)
        ));

        return avatar;
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

    private Dialog createStyledDialog(String title) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        currentDialogRoot = new LinearLayout(this);
        currentDialogRoot.setOrientation(LinearLayout.VERTICAL);
        currentDialogRoot.setPadding(dp(18), dp(16), dp(18), dp(16));
        currentDialogRoot.setBackgroundResource(R.drawable.employee_dialog_bg);

        TextView tvTitle = makeText(title, 20, PRIMARY, true);
        tvTitle.setPadding(0, 0, 0, dp(12));
        currentDialogRoot.addView(tvTitle);

        dialog.setContentView(currentDialogRoot);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();

            if (w != null) {
                w.setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        return dialog;
    }

    private EditText makeDarkInput(String hint) {
        EditText edt = new EditText(this);
        edt.setHint(hint);
        edt.setTextColor(TEXT);
        edt.setHintTextColor(Color.parseColor("#8B90A0"));
        edt.setBackgroundResource(R.drawable.employee_input_bg);
        edt.setPadding(dp(14), 0, dp(14), 0);
        edt.setSingleLine(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        lp.setMargins(0, 0, 0, dp(8));
        edt.setLayoutParams(lp);

        return edt;
    }

    private TextView makeDialogLabel(String text) {
        TextView tv = makeText(text, 13, SUB, true);
        tv.setPadding(0, dp(8), 0, dp(4));
        return tv;
    }

    private LinearLayout makeDialogActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.RIGHT);
        actions.setPadding(0, dp(14), 0, 0);
        return actions;
    }

    private Button makeDialogButton(String text, int bgRes, int textColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextColor(textColor);
        btn.setTextSize(13);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(null);
        btn.setBackgroundResource(bgRes);
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        btn.setPadding(dp(14), 0, dp(14), 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        );
        lp.setMargins(dp(8), 0, 0, 0);
        btn.setLayoutParams(lp);

        return btn;
    }

    class WhiteSpinnerAdapter<T> extends ArrayAdapter<T> {
        WhiteSpinnerAdapter(ArrayList<T> data) {
            super(WorkScheduleActivity.this, android.R.layout.simple_spinner_item, data);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(Color.parseColor("#111827"));
            tv.setPadding(dp(16), dp(16), dp(16), dp(16));
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        private TextView makeSpinnerText(T item) {
            TextView tv = new TextView(WorkScheduleActivity.this);
            tv.setText(item == null ? "" : item.toString());
            tv.setTextSize(15);
            tv.setSingleLine(false);
            tv.setTextColor(Color.WHITE);
            return tv;
        }
    }

    private int findOptionIndex(ArrayList<OptionItem> list, String id) {
        if (id == null) {
            id = "";
        }

        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).id)) {
                return i;
            }
        }

        return 0;
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

    private String getInitial(String fullName) {
        if (isEmpty(fullName)) {
            return "NV";
        }

        String[] parts = fullName.trim().split("\\s+");
        String last = parts[parts.length - 1];

        if (last.length() >= 1) {
            return last.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        return "NV";
    }

    private String emptyText(String value) {
        return isEmpty(value) ? "—" : value;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null");
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

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    abstract class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    static class OptionItem {
        String id;
        String name;

        OptionItem(String id, String name) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class EmployeeItem {
        String maNV;
        String hoTen;
        String phongBan;
        String chucVu;
        String maPB;
        String maCV;
    }

    static class ScheduleCell {
        String maNV;
        String hoTen;
        String date;
        String content;
        String factor;
        boolean isSunday;
    }

    static class HolidayCell {
        String date;
        String name;
    }
}