package com.neith.subjectdemo.hr;

import android.app.Dialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.ActivityLogger;
import com.neith.subjectdemo.helper.DB;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class WorkSchedulePersonalActivity extends AppCompatActivity {

    SQLiteDatabase db;

    TextView txtPersonalWorkTitle, txtPersonalWorkSub, txtPersonalWeekRange;
    Button btnBackPersonalWork, btnPersonalPrevWeek, btnPersonalNextWeek;
    LinearLayout layoutPersonalWorkBody;

    String maNV = "";
    String hoTen = "";
    String chucVu = "";
    String phongBan = "";

    Calendar weekStart = Calendar.getInstance();

    HashMap<String, ScheduleCell> scheduleMap = new HashMap<>();
    HashMap<String, HolidayCell> holidayMap = new HashMap<>();
    HashMap<String, ProjectAllowInfo> allowMap = new HashMap<>();

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");
    final int PURPLE = Color.parseColor("#A855F7");

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat viewDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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
        setContentView(R.layout.activity_work_schedule_personal);

        db = DB.openDatabase(this);

        txtPersonalWorkTitle = findViewById(R.id.txtPersonalWorkTitle);
        txtPersonalWorkSub = findViewById(R.id.txtPersonalWorkSub);
        txtPersonalWeekRange = findViewById(R.id.txtPersonalWeekRange);

        btnBackPersonalWork = findViewById(R.id.btnBackPersonalWork);
        btnPersonalPrevWeek = findViewById(R.id.btnPersonalPrevWeek);
        btnPersonalNextWeek = findViewById(R.id.btnPersonalNextWeek);

        layoutPersonalWorkBody = findViewById(R.id.layoutPersonalWorkBody);

        btnBackPersonalWork.setBackgroundTintList(null);
        btnPersonalPrevWeek.setBackgroundTintList(null);
        btnPersonalNextWeek.setBackgroundTintList(null);

        maNV = getIntent().getStringExtra("MANV");

        if (maNV == null || maNV.trim().isEmpty()) {
            toast("Không tìm thấy mã nhân viên.");
            finish();
            return;
        }

        String passedWeek = getIntent().getStringExtra("WEEK_START");

        if (!isEmpty(passedWeek)) {
            try {
                Date d = dbDateFormat.parse(passedWeek);

                if (d != null) {
                    weekStart.setTime(d);
                }
            } catch (Exception e) {
                setCurrentWeek();
            }
        } else {
            setCurrentWeek();
        }

        clearTime(weekStart);

        btnBackPersonalWork.setOnClickListener(v -> finish());

        btnPersonalPrevWeek.setOnClickListener(v -> {
            weekStart.add(Calendar.DAY_OF_MONTH, -7);
            animateClick(btnPersonalPrevWeek);
            loadPersonalWeek();
        });

        btnPersonalNextWeek.setOnClickListener(v -> {
            weekStart.add(Calendar.DAY_OF_MONTH, 7);
            animateClick(btnPersonalNextWeek);
            loadPersonalWeek();
        });

        loadEmployeeInfo();
        loadPersonalWeek();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPersonalWeek();
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

    private void loadEmployeeInfo() {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "IFNULL(PB.TENPB, ''), IFNULL(CV.TENCV, '') " +
                        "FROM NHANVIEN NV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "WHERE NV.MANV = ?",
                new String[]{maNV}
        );

        if (c.moveToFirst()) {
            hoTen = c.getString(0).trim();
            phongBan = c.getString(1);
            chucVu = c.getString(2);
        } else {
            c.close();
            toast("Không tìm thấy nhân viên.");
            finish();
            return;
        }

        c.close();

        txtPersonalWorkTitle.setText(hoTen);
        txtPersonalWorkSub.setText(maNV + " • " + emptyText(phongBan) + " • " + emptyText(chucVu));
    }

    private void loadPersonalWeek() {
        layoutPersonalWorkBody.removeAllViews();

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);

        txtPersonalWeekRange.setText(
                "Tuần từ " + viewDateFormat.format(weekStart.getTime()) +
                        " đến " + viewDateFormat.format(weekEnd.getTime())
        );

        loadSchedulesAndHolidays();
        loadAllowedProjectDays();

        buildSummaryCard();
        buildWeekSlots();
    }

    private void loadSchedulesAndHolidays() {
        scheduleMap.clear();
        holidayMap.clear();

        String start = dbDateFormat.format(weekStart.getTime());

        Calendar endCal = (Calendar) weekStart.clone();
        endCal.add(Calendar.DAY_OF_MONTH, 6);
        String end = dbDateFormat.format(endCal.getTime());

        Cursor cSchedule = db.rawQuery(
                "SELECT MANV, NGAYLAMVIEC, IFNULL(NOIDUNGLAMVIEC, ''), IFNULL(HESOLUONG, ''), IFNULL(CHUNHAT, 0), IFNULL(NGAYLE, 0) " +
                        "FROM LICHLAMVIEC " +
                        "WHERE MANV = ? AND NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ?",
                new String[]{maNV, start, end}
        );

        while (cSchedule.moveToNext()) {
            ScheduleCell cell = new ScheduleCell();
            cell.maNV = cSchedule.getString(0);
            cell.date = cSchedule.getString(1);
            cell.content = cSchedule.getString(2);
            cell.factor = cSchedule.getString(3);
            cell.isSunday = cSchedule.getInt(4) != 0;
            cell.isHoliday = cSchedule.getInt(5) != 0;

            scheduleMap.put(cell.date, cell);
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

    private void loadAllowedProjectDays() {
        allowMap.clear();

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            String date = dbDateFormat.format(day.getTime());
            ProjectAllowInfo info = getProjectAllowInfo(date);

            allowMap.put(date, info);
        }
    }

    private ProjectAllowInfo getProjectAllowInfo(String date) {
        ProjectAllowInfo info = new ProjectAllowInfo();
        info.date = date;
        info.allowed = false;
        info.reason = "";

        if (isPastDate(date)) {
            info.reason = "Ngày cũ hơn hôm nay";
            return info;
        }

        Cursor c = db.rawQuery(
                "SELECT DTHD.MADAHD, IFNULL(HD.TENHD, DTHD.MADAHD), IFNULL(HD.NGAYBD, ''), " +
                        "IFNULL(NULLIF(DTHD.NGAYKT, ''), IFNULL(NULLIF(HD.NGAYKT_DUTINH, ''), '9999-12-31')) AS NGAYKT " +
                        "FROM NVTHAMGIADA TG " +
                        "JOIN DUANTHEOHOPDONG DTHD ON TG.MADAHD = DTHD.MADAHD " +
                        "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                        "WHERE TG.MANV = ? " +
                        "AND (IFNULL(HD.NGAYBD, '') = '' OR HD.NGAYBD <= ?) " +
                        "AND IFNULL(NULLIF(DTHD.NGAYKT, ''), IFNULL(NULLIF(HD.NGAYKT_DUTINH, ''), '9999-12-31')) >= ? " +
                        "ORDER BY HD.NGAYBD LIMIT 1",
                new String[]{maNV, date, date}
        );

        if (c.moveToFirst()) {
            info.allowed = true;
            info.projectKey = c.getString(0);
            info.projectName = c.getString(1);
            info.startDate = c.getString(2);
            info.endDate = c.getString(3);
            info.reason = "Thuộc dự án: " + info.projectName;
        } else {
            info.reason = "Nhân viên không thuộc dự án hoạt động trong ngày này";
        }

        c.close();

        return info;
    }

    private void buildSummaryCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("TỔNG QUAN CÁ NHÂN"));

        int count = scheduleMap.size();
        double maxFactor = 0;
        int sundayCount = 0;
        int holidayCount = 0;
        int allowedCount = 0;

        for (ScheduleCell s : scheduleMap.values()) {
            maxFactor = Math.max(maxFactor, parseFactorSafe(s.factor));

            if (s.isSunday) {
                sundayCount++;
            }

            if (s.isHoliday) {
                holidayCount++;
            }
        }

        for (ProjectAllowInfo info : allowMap.values()) {
            if (info.allowed) {
                allowedCount++;
            }
        }

        card.addView(makeInfoRow("Số ngày có lịch", count + "/7 ngày"));
        card.addView(makeInfoRow("Ngày được phép thêm/sửa", allowedCount + "/7 ngày"));
        card.addView(makeInfoRow("Hệ số cao nhất", maxFactor <= 0 ? "—" : formatFactor(maxFactor)));
        card.addView(makeInfoRow("Lịch Chủ nhật", sundayCount + " ngày"));
        card.addView(makeInfoRow("Lịch ngày lễ", holidayCount + " ngày"));

        layoutPersonalWorkBody.addView(card);
    }

    private void buildWeekSlots() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("LỊCH LÀM VIỆC TRONG TUẦN"));

        LinearLayout daysRow = new LinearLayout(this);
        daysRow.setOrientation(LinearLayout.HORIZONTAL);
        daysRow.setPadding(0, dp(10), 0, 0);

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            String date = dbDateFormat.format(day.getTime());
            ScheduleCell schedule = scheduleMap.get(date);
            HolidayCell holiday = holidayMap.get(date);
            ProjectAllowInfo allowInfo = allowMap.get(date);
            boolean isSunday = day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

            daysRow.addView(makeDaySlot(day, i, schedule, holiday, isSunday, allowInfo));
        }

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(daysRow);

        card.addView(hsv);

        layoutPersonalWorkBody.addView(card);
    }

    private View makeDaySlot(
            Calendar day,
            int index,
            ScheduleCell schedule,
            HolidayCell holiday,
            boolean isSunday,
            ProjectAllowInfo allowInfo
    ) {
        String date = dbDateFormat.format(day.getTime());
        double factor = schedule == null ? 0 : parseFactorSafe(schedule.factor);
        boolean allowed = allowInfo != null && allowInfo.allowed;

        LinearLayout slot = new LinearLayout(this);
        slot.setOrientation(LinearLayout.VERTICAL);
        slot.setGravity(Gravity.CENTER_VERTICAL);
        slot.setPadding(dp(10), dp(8), dp(10), dp(8));
        slot.setBackground(makeFactorBorderBg(schedule != null, holiday != null, isSunday, factor, allowed));
        slot.setClickable(true);
        slot.setFocusable(true);
        slot.setAlpha(allowed ? 1.0f : 0.42f);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(180), dp(160));
        lp.setMargins(0, 0, dp(10), 0);
        slot.setLayoutParams(lp);

        TextView dayName = makeText(dayNames[index], 12, isSunday ? RED : PRIMARY, true);
        slot.addView(dayName);

        TextView dateText = makeText(viewDateFormat.format(day.getTime()), 11, SUB, false);
        dateText.setPadding(0, dp(2), 0, dp(4));
        slot.addView(dateText);

        if (schedule != null) {
            TextView content = makeText(
                    isEmpty(schedule.content) ? "(Đã có lịch)" : schedule.content,
                    12,
                    TEXT,
                    true
            );
            content.setMaxLines(3);
            slot.addView(content);

            if (!isEmpty(schedule.factor)) {
                TextView factorText = makeText("Hệ số: " + schedule.factor, 11, getFactorColor(factor), true);
                factorText.setPadding(0, dp(3), 0, 0);
                slot.addView(factorText);
            }
        } else {
            TextView add = makeText(allowed ? "+ Thêm lịch" : "Không được thêm", 13, allowed ? SUB : RED, true);
            add.setPadding(0, dp(6), 0, 0);
            slot.addView(add);
        }

        if (holiday != null) {
            TextView h = makeText("Ngày lễ: " + emptyText(holiday.name), 10, ORANGE, true);
            h.setPadding(0, dp(3), 0, 0);
            h.setMaxLines(1);
            slot.addView(h);
        }

        TextView projectNote = makeText(
                allowInfo == null ? "" : allowInfo.reason,
                10,
                allowed ? GREEN : RED,
                true
        );
        projectNote.setPadding(0, dp(4), 0, 0);
        projectNote.setMaxLines(2);
        slot.addView(projectNote);

        slot.setOnClickListener(v -> {
            animateClick(slot);

            if (!allowed) {
                toast(allowInfo == null ? "Ngày này không được thêm lịch." : allowInfo.reason);
                return;
            }

            showScheduleDialog(date, dayNames[index] + ", " + viewDateFormat.format(day.getTime()), schedule, holiday, isSunday);
        });

        return slot;
    }

    private void showScheduleDialog(String date, String dateLabel, ScheduleCell schedule, HolidayCell holiday, boolean isSunday) {
        ProjectAllowInfo allowInfo = allowMap.get(date);

        if (allowInfo == null || !allowInfo.allowed) {
            toast(allowInfo == null ? "Ngày này không được thêm lịch." : allowInfo.reason);
            return;
        }

        Dialog dialog = createStyledDialog(schedule == null ? "Thêm lịch cá nhân" : "Sửa lịch cá nhân");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView info = makeText(hoTen + " • " + dateLabel, 13, SUB, false);
        info.setPadding(0, 0, 0, dp(8));
        content.addView(info);

        TextView projectText = makeText("Dự án hợp lệ: " + allowInfo.projectName, 13, GREEN, true);
        projectText.setPadding(0, 0, 0, dp(8));
        content.addView(projectText);

        if (holiday != null) {
            TextView holidayText = makeText("Ngày lễ: " + emptyText(holiday.name), 13, ORANGE, true);
            holidayText.setPadding(0, 0, 0, dp(8));
            content.addView(holidayText);
        }

        EditText edtContent = makeDarkInput("Nội dung công việc");
        edtContent.setMinLines(3);
        edtContent.setGravity(Gravity.TOP);
        edtContent.setSingleLine(false);

        EditText edtFactor = makeDarkInput("Hệ số lương");
        edtFactor.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        if (schedule != null) {
            edtContent.setText(schedule.content);
            edtContent.setSelection(edtContent.getText().length());
            edtFactor.setText(schedule.factor == null ? "" : schedule.factor);
            edtFactor.setSelection(edtFactor.getText().length());
        }

        content.addView(makeDialogLabel("Nội dung công việc"));
        content.addView(edtContent);

        content.addView(makeDialogLabel("Hệ số lương"));
        content.addView(edtFactor);

        TextView note = makeText(
                isSunday ? "Ngày này là Chủ nhật, khi lưu sẽ đánh dấu CHUNHAT = 1." : "Chỉ được lưu khi nhân viên thuộc dự án hợp lệ trong ngày này.",
                12,
                SUB,
                false
        );
        note.setPadding(0, dp(4), 0, 0);
        content.addView(note);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button delete = makeDialogButton("Xóa", R.drawable.employee_button_red_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);

        if (schedule != null) {
            actions.addView(delete);
        }

        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        delete.setOnClickListener(v -> {
            deleteSchedule(date);
            dialog.dismiss();
        });

        save.setOnClickListener(v -> {
            String workContent = edtContent.getText().toString().trim();
            String factor = edtFactor.getText().toString().trim();

            if (workContent.isEmpty()) {
                toast("Vui lòng nhập nội dung công việc.");
                return;
            }

            if (!isValidFactor(factor)) {
                toast("Hệ số lương không hợp lệ.");
                return;
            }

            saveSchedule(date, workContent, factor, isSunday, holiday != null);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveSchedule(String date, String content, String factor, boolean isSunday, boolean isHoliday) {
        ProjectAllowInfo allowInfo = allowMap.get(date);

        if (allowInfo == null || !allowInfo.allowed) {
            toast(allowInfo == null ? "Ngày này không được thêm lịch." : allowInfo.reason);
            return;
        }

        boolean exists = scheduleExists(date);

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

        if (result != -1 && result != 0) {
            toast("Đã lưu lịch làm việc.");
            ActivityLogger.log(db, "SAVE_SCHEDULE", "HR",
                    (exists ? "Cập nhật" : "Thêm") + " lịch cho nhân viên " + maNV +
                            " ngày " + date + ", nội dung: " + content + ", hệ số: " + factor +
                            (isSunday ? ", Chủ nhật" : "") + (isHoliday ? ", Ngày lễ" : ""));
            loadPersonalWeek();
        } else {
            toast("Lưu lịch làm việc thất bại.");
        }
    }

    private void deleteSchedule(String date) {
        ProjectAllowInfo allowInfo = allowMap.get(date);

        if (allowInfo == null || !allowInfo.allowed) {
            toast("Không thể xóa/sửa lịch ở ngày không còn hợp lệ.");
            return;
        }

        int rows = db.delete(
                "LICHLAMVIEC",
                "MANV = ? AND NGAYLAMVIEC = ?",
                new String[]{maNV, date}
        );

        if (rows > 0) {
            ActivityLogger.log(db, "DELETE_SCHEDULE", "HR",
                    "Đã xóa lịch của nhân viên " + maNV + " ngày " + date);
            toast("Đã xóa lịch làm việc.");
            loadPersonalWeek();
        } else {
            toast("Không tìm thấy lịch làm việc.");
        }
    }

    private boolean scheduleExists(String date) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM LICHLAMVIEC WHERE MANV = ? AND NGAYLAMVIEC = ?",
                new String[]{maNV, date}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private GradientDrawable makeFactorBorderBg(boolean hasSchedule, boolean isHoliday, boolean isSunday, double factor, boolean allowed) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));

        if (!allowed) {
            bg.setColor(Color.parseColor("#080A0F"));
            bg.setStroke(dp(1), Color.parseColor("#374151"));
            return bg;
        }

        bg.setColor(Color.parseColor("#111827"));

        int stroke;

        if (isHoliday) {
            stroke = ORANGE;
        } else if (factor >= 3) {
            stroke = PURPLE;
        } else if (factor >= 2) {
            stroke = RED;
        } else if (factor > 1) {
            stroke = GREEN;
        } else if (hasSchedule) {
            stroke = BLUE;
        } else if (isSunday) {
            stroke = Color.parseColor("#7F1D1D");
        } else {
            stroke = Color.parseColor("#4B5563");
        }

        bg.setStroke(dp(2), stroke);

        return bg;
    }

    private int getFactorColor(double factor) {
        if (factor >= 3) return PURPLE;
        if (factor >= 2) return RED;
        if (factor > 1) return GREEN;
        return BLUE;
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

    private double parseFactorSafe(String factor) {
        try {
            if (isEmpty(factor)) {
                return 0;
            }

            return Double.parseDouble(factor.replace(",", "."));
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatFactor(double factor) {
        if (Math.abs(factor - Math.round(factor)) < 0.0001) {
            return String.valueOf((long) Math.round(factor));
        }

        return String.format(Locale.getDefault(), "%.1f", factor);
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

    private LinearLayout makeInfoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(7), 0, dp(7));

        TextView l = makeText(label, 13, SUB, false);
        TextView v = makeText(value, 13, TEXT, true);
        v.setGravity(Gravity.RIGHT);

        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
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

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null");
    }

    private String emptyText(String value) {
        return isEmpty(value) ? "—" : value;
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

    static class ScheduleCell {
        String maNV;
        String date;
        String content;
        String factor;
        boolean isSunday;
        boolean isHoliday;
    }

    static class HolidayCell {
        String date;
        String name;
    }

    static class ProjectAllowInfo {
        String date;
        boolean allowed;
        String reason;
        String projectKey;
        String projectName;
        String startDate;
        String endDate;
    }
}