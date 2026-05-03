package com.neith.subjectdemo.hr;

import android.app.Dialog;
import android.content.ContentValues;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
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

    TextView txtWeekRange;
    EditText edtSearchWorkEmployee;
    Button btnPrevWeek, btnNextWeek;
    LinearLayout layoutWorkList;

    Calendar weekStart = Calendar.getInstance();

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat viewDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    HashMap<String, ScheduleCell> scheduleMap = new HashMap<>();
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
        edtSearchWorkEmployee = findViewById(R.id.edtSearchWorkEmployee);
        btnPrevWeek = findViewById(R.id.btnPrevWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);
        layoutWorkList = findViewById(R.id.layoutWorkList);

        btnPrevWeek.setBackgroundTintList(null);
        btnNextWeek.setBackgroundTintList(null);

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.WORK_SCHEDULE);

        setCurrentWeek();

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

        edtSearchWorkEmployee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadWeek();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadWeek();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWeek();
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

    private void loadWeek() {
        layoutWorkList.removeAllViews();

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);

        txtWeekRange.setText(
                "Tuần từ " + viewDateFormat.format(weekStart.getTime()) +
                        " đến " + viewDateFormat.format(weekEnd.getTime())
        );

        loadSchedulesAndHolidays();

        ArrayList<EmployeeItem> employees = loadEmployees();

        if (employees.isEmpty()) {
            TextView empty = makeText("Không có nhân viên phù hợp.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, dp(28));
            layoutWorkList.addView(empty);
            return;
        }

        for (EmployeeItem emp : employees) {
            layoutWorkList.addView(makeEmployeeWeekCard(emp));
        }
    }

    private ArrayList<EmployeeItem> loadEmployees() {
        ArrayList<EmployeeItem> list = new ArrayList<>();
        String keyword = edtSearchWorkEmployee.getText().toString().trim();

        String sql =
                "SELECT NV.MANV, " +
                        "IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "IFNULL(CV.TENCV, '') AS TENCV " +
                        "FROM NHANVIEN NV " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "WHERE 1 = 1 ";

        ArrayList<String> args = new ArrayList<>();

        if (!keyword.isEmpty()) {
            sql += "AND (IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') LIKE ? OR NV.MANV LIKE ?) ";
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }

        sql += "ORDER BY NV.HOLOT, NV.TENNV";

        Cursor c = db.rawQuery(sql, args.toArray(new String[0]));

        while (c.moveToNext()) {
            EmployeeItem item = new EmployeeItem();
            item.maNV = c.getString(0);
            item.hoTen = c.getString(1).trim();
            item.chucVu = c.getString(2);
            list.add(item);
        }

        c.close();

        return list;
    }

    private void loadSchedulesAndHolidays() {
        scheduleMap.clear();
        holidayMap.clear();

        String start = dbDateFormat.format(weekStart.getTime());

        Calendar endCal = (Calendar) weekStart.clone();
        endCal.add(Calendar.DAY_OF_MONTH, 6);
        String end = dbDateFormat.format(endCal.getTime());

        Cursor cSchedule = db.rawQuery(
                "SELECT MANV, NGAYLAMVIEC, " +
                        "IFNULL(NOIDUNGLAMVIEC, ''), " +
                        "IFNULL(HESOLUONG, ''), " +
                        "IFNULL(CHUNHAT, 0) " +
                        "FROM LICHLAMVIEC " +
                        "WHERE NGAYLAMVIEC >= ? AND NGAYLAMVIEC <= ?",
                new String[]{start, end}
        );

        while (cSchedule.moveToNext()) {
            ScheduleCell cell = new ScheduleCell();
            cell.maNV = cSchedule.getString(0);
            cell.date = cSchedule.getString(1);
            cell.content = cSchedule.getString(2);
            cell.factor = cSchedule.getString(3);
            cell.isSunday = cSchedule.getInt(4) != 0;

            scheduleMap.put(buildKey(cell.maNV, cell.date), cell);
        }

        cSchedule.close();

        Cursor cHoliday = db.rawQuery(
                "SELECT NGAYLE, IFNULL(NOIDUNG, '') " +
                        "FROM DSNGAYLE " +
                        "WHERE NGAYLE >= ? AND NGAYLE <= ?",
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

    private View makeEmployeeWeekCard(EmployeeItem emp) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.hr_card_bg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardLp);

        LinearLayout employeeHeader = new LinearLayout(this);
        employeeHeader.setOrientation(LinearLayout.HORIZONTAL);
        employeeHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = makeAvatar(emp.hoTen);
        employeeHeader.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);

        TextView name = makeText(emp.hoTen, 16, TEXT, true);
        info.addView(name);

        TextView sub = makeText(emp.maNV + " • " + emptyText(emp.chucVu), 12, SUB, false);
        sub.setPadding(0, dp(2), 0, 0);
        info.addView(sub);

        employeeHeader.addView(info, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        card.addView(employeeHeader);

        LinearLayout daysRow = new LinearLayout(this);
        daysRow.setOrientation(LinearLayout.HORIZONTAL);
        daysRow.setPadding(0, dp(12), 0, 0);

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            String date = dbDateFormat.format(day.getTime());
            String key = buildKey(emp.maNV, date);

            ScheduleCell schedule = scheduleMap.get(key);
            HolidayCell holiday = holidayMap.get(date);

            boolean isSunday = day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            View slot = makeDaySlot(emp, day, i, schedule, holiday, isSunday);
            daysRow.addView(slot);
        }

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(daysRow);

        card.addView(hsv);

        return card;
    }

    private View makeDaySlot(EmployeeItem emp, Calendar day, int index, ScheduleCell schedule, HolidayCell holiday, boolean isSunday) {
        String date = dbDateFormat.format(day.getTime());
        String dateLabel = dayNames[index] + ", " + viewDateFormat.format(day.getTime());

        LinearLayout slot = new LinearLayout(this);
        slot.setOrientation(LinearLayout.VERTICAL);
        slot.setGravity(Gravity.CENTER_VERTICAL);
        slot.setPadding(dp(10), dp(8), dp(10), dp(8));
        slot.setBackground(makeSlotBg(schedule != null, holiday != null, isSunday));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(154), dp(112));
        lp.setMargins(0, 0, dp(10), 0);
        slot.setLayoutParams(lp);

        TextView dayName = makeText(dayNames[index], 12, isSunday ? RED : PRIMARY, true);
        dayName.setSingleLine(true);
        slot.addView(dayName);

        TextView dateText = makeText(viewDateFormat.format(day.getTime()), 11, SUB, false);
        dateText.setPadding(0, dp(2), 0, dp(4));
        slot.addView(dateText);

        if (schedule != null) {
            TextView content = makeText(
                    schedule.content == null || schedule.content.trim().isEmpty()
                            ? "(Đã có lịch)"
                            : schedule.content,
                    12,
                    TEXT,
                    true
            );
            content.setMaxLines(2);
            slot.addView(content);

            if (schedule.factor != null && !schedule.factor.trim().isEmpty()) {
                TextView factor = makeText("Hệ số: " + schedule.factor, 11, GREEN, false);
                factor.setPadding(0, dp(3), 0, 0);
                slot.addView(factor);
            }
        } else {
            TextView add = makeText("+ Thêm", 13, SUB, true);
            add.setPadding(0, dp(4), 0, 0);
            slot.addView(add);
        }

        if (holiday != null) {
            TextView h = makeText(
                    holiday.name == null || holiday.name.trim().isEmpty()
                            ? "Ngày lễ"
                            : holiday.name,
                    10,
                    ORANGE,
                    true
            );
            h.setPadding(0, dp(3), 0, 0);
            h.setMaxLines(1);
            slot.addView(h);
        }

        slot.setOnClickListener(v -> {
            animateClick(slot);
            showScheduleDialog(emp, date, dateLabel, schedule, holiday, isSunday);
        });

        return slot;
    }

    private GradientDrawable makeSlotBg(boolean hasSchedule, boolean isHoliday, boolean isSunday) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));

        if (hasSchedule) {
            bg.setColor(Color.parseColor("#111827"));
            bg.setStroke(dp(1), isHoliday ? ORANGE : PRIMARY);
        } else if (isSunday) {
            bg.setColor(Color.parseColor("#2A1010"));
            bg.setStroke(dp(1), Color.parseColor("#7F1D1D"));
        } else {
            bg.setColor(Color.parseColor("#111827"));
            bg.setStroke(dp(1), Color.parseColor("#4B5563"), dp(6), dp(4));
        }

        return bg;
    }

    private void showScheduleDialog(EmployeeItem emp, String date, String dateLabel, ScheduleCell schedule, HolidayCell holiday, boolean isSunday) {
        Dialog dialog = createStyledDialog("Lịch làm việc");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView info = makeText(emp.hoTen + " • " + dateLabel, 13, SUB, false);
        info.setPadding(0, 0, 0, dp(10));
        content.addView(info);

        if (holiday != null) {
            TextView holidayText = makeText(
                    "Ngày lễ: " + (holiday.name == null || holiday.name.trim().isEmpty() ? "Không rõ tên" : holiday.name),
                    13,
                    ORANGE,
                    true
            );
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
                isSunday ? "Ô này là Chủ nhật. Khi lưu sẽ đánh dấu CHUNHAT = 1." : "Để trống hệ số nếu không có hệ số đặc biệt.",
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
            deleteSchedule(emp.maNV, date);
            dialog.dismiss();
        });

        save.setOnClickListener(v -> {
            String workContent = edtContent.getText().toString().trim();
            String factor = edtFactor.getText().toString().trim();

            if (workContent.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung công việc.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (parseDate(date).before(clearTime(new Date()))) {
                Toast.makeText(this, "Không thể thêm hoặc sửa lịch cho ngày quá khứ.", Toast.LENGTH_LONG).show();
                return;
            }

            saveSchedule(emp.maNV, date, workContent, factor, isSunday);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveSchedule(String maNV, String date, String content, String factor, boolean isSunday) {
        if (!employeeExists(maNV)) {
            Toast.makeText(this, "Không tìm thấy nhân viên.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean exists = scheduleExists(maNV, date);

        ContentValues values = new ContentValues();
        values.put("MANV", maNV);
        values.put("NGAYLAMVIEC", date);
        values.put("NOIDUNGLAMVIEC", content);
        values.put("CHUNHAT", isSunday ? 1 : 0);
        values.put("NGAYLE", 0);

        if (factor == null || factor.trim().isEmpty()) {
            values.putNull("HESOLUONG");
        } else {
            try {
                values.put("HESOLUONG", Double.parseDouble(factor.replace(",", ".")));
            } catch (Exception e) {
                Toast.makeText(this, "Hệ số lương không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }
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
            Toast.makeText(this, "Đã lưu lịch làm việc.", Toast.LENGTH_SHORT).show();
            loadWeek();
        } else {
            Toast.makeText(this, "Lưu lịch làm việc thất bại.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSchedule(String maNV, String date) {
        int rows = db.delete(
                "LICHLAMVIEC",
                "MANV = ? AND NGAYLAMVIEC = ?",
                new String[]{maNV, date}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã xóa lịch làm việc.", Toast.LENGTH_SHORT).show();
            loadWeek();
        } else {
            Toast.makeText(this, "Không tìm thấy lịch làm việc.", Toast.LENGTH_SHORT).show();
        }
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

    private Date parseDate(String text) {
        try {
            return dbDateFormat.parse(text);
        } catch (Exception e) {
            return new Date();
        }
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

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
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

    static class EmployeeItem {
        String maNV;
        String hoTen;
        String chucVu;
    }

    static class ScheduleCell {
        String maNV;
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