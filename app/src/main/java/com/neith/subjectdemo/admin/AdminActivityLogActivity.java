package com.neith.subjectdemo.admin;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AdminActivityLogActivity extends AppCompatActivity {

    SQLiteDatabase db;

    EditText edtSearch, edtFrom, edtTo;
    Spinner spnAction;
    Button btnApply, btnReset;
    LinearLayout layoutList;

    ArrayList<OptionItem> actions = new ArrayList<>();
    String selectedAction = "";

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat viewDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_activity_log);

        db = DB.openDatabase(this);

        edtSearch = findViewById(R.id.edtSearchAdminLog);
        edtFrom = findViewById(R.id.edtAdminLogFrom);
        edtTo = findViewById(R.id.edtAdminLogTo);
        spnAction = findViewById(R.id.spnAdminLogAction);
        btnApply = findViewById(R.id.btnApplyAdminLogFilter);
        btnReset = findViewById(R.id.btnResetAdminLogFilter);
        layoutList = findViewById(R.id.layoutAdminLogList);

        btnApply.setBackgroundTintList(null);
        btnReset.setBackgroundTintList(null);

        setupDateInput(edtFrom);
        setupDateInput(edtTo);

        edtFrom.setOnClickListener(v -> pickDate(edtFrom));
        edtTo.setOnClickListener(v -> pickDate(edtTo));

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setupAdmin(this, bottomNavContainer, BottomNav.ADMIN_ACTIVITY_LOG);

        loadActionOptions();
        setupActionSpinner();

        btnApply.setOnClickListener(v -> loadLogs());

        btnReset.setOnClickListener(v -> {
            edtSearch.setText("");
            edtFrom.setText("");
            edtTo.setText("");
            spnAction.setSelection(0);
            selectedAction = "";
            loadLogs();
        });

        edtSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                loadLogs();
            }
        });

        loadLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActionOptions();
        setupActionSpinner();
        loadLogs();
    }

    private void loadActionOptions() {
        actions.clear();
        actions.add(new OptionItem("", "Tất cả action"));

        Cursor c = db.rawQuery(
                "SELECT DISTINCT IFNULL(ActionCode, '') FROM ACTIVITY_LOG WHERE IFNULL(ActionCode, '') <> '' ORDER BY ActionCode",
                null
        );

        while (c.moveToNext()) {
            String code = c.getString(0);
            actions.add(new OptionItem(code, code));
        }

        c.close();
    }

    private void setupActionSpinner() {
        String old = selectedAction;

        spnAction.setAdapter(new WhiteSpinnerAdapter<>(actions));
        spnAction.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);
        spnAction.setSelection(findActionIndex(old));

        spnAction.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedAction = actions.get(position).id;
            }
        });
    }

    private void loadLogs() {
        layoutList.removeAllViews();

        buildSummaryCard();

        String keyword = edtSearch.getText().toString().trim();
        String from = edtFrom.getText().toString().trim();
        String to = edtTo.getText().toString().trim();

        StringBuilder sql = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        sql.append("SELECT LogID, IFNULL(ActionCode, ''), IFNULL(ActionTime, ''), IFNULL(PerformedBy, ''), IFNULL(Description, '') ");
        sql.append("FROM ACTIVITY_LOG WHERE 1 = 1 ");

        if (!selectedAction.isEmpty()) {
            sql.append("AND ActionCode = ? ");
            args.add(selectedAction);
        }

        if (!keyword.isEmpty()) {
            sql.append("AND (ActionCode LIKE ? OR PerformedBy LIKE ? OR Description LIKE ?) ");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }

        if (!from.isEmpty()) {
            sql.append("AND ActionTime >= ? ");
            args.add(from + " 00:00:00");
        }

        if (!to.isEmpty()) {
            sql.append("AND ActionTime <= ? ");
            args.add(to + " 23:59:59");
        }

        sql.append("ORDER BY ActionTime DESC, LogID DESC LIMIT 300");

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));

        if (c.getCount() == 0) {
            TextView empty = makeText("Không có log phù hợp bộ lọc.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, dp(28));
            layoutList.addView(empty);
        } else {
            while (c.moveToNext()) {
                LogItem item = new LogItem();
                item.logId = c.getInt(0);
                item.actionCode = c.getString(1);
                item.actionTime = c.getString(2);
                item.performedBy = c.getString(3);
                item.description = c.getString(4);

                layoutList.addView(makeLogCard(item));
            }
        }

        c.close();
    }

    private void buildSummaryCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("TỔNG QUAN LOG"));

        int total = getInt("SELECT COUNT(*) FROM ACTIVITY_LOG", null);
        int today = getInt(
                "SELECT COUNT(*) FROM ACTIVITY_LOG WHERE SUBSTR(IFNULL(ActionTime, ''), 1, 10) = ?",
                new String[]{dbDateFormat.format(Calendar.getInstance().getTime())}
        );
        int adminAction = getInt(
                "SELECT COUNT(*) FROM ACTIVITY_LOG WHERE LOWER(IFNULL(PerformedBy, '')) LIKE ?",
                new String[]{"%admin%"}
        );
        int actionTypes = getInt(
                "SELECT COUNT(DISTINCT ActionCode) FROM ACTIVITY_LOG",
                null
        );

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(makeStatBox("Tổng log", String.valueOf(total), "ACTIVITY_LOG", PRIMARY), halfLp(true));
        row1.addView(makeStatBox("Hôm nay", String.valueOf(today), "Trong ngày", GREEN), halfLp(false));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);
        row2.addView(makeStatBox("Admin", String.valueOf(adminAction), "Thao tác admin", BLUE), halfLp(true));
        row2.addView(makeStatBox("Loại action", String.valueOf(actionTypes), "Distinct code", ORANGE), halfLp(false));

        card.addView(row1);
        card.addView(row2);

        layoutList.addView(card);
    }

    private android.view.View makeLogCard(LogItem item) {
        LinearLayout card = makeCard();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView action = makeText("#" + item.logId + " • " + item.actionCode, 15, getActionColor(item.actionCode), true);
        TextView user = makeText(item.performedBy, 13, PRIMARY, true);
        user.setGravity(Gravity.RIGHT);

        top.addView(action, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(user, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        card.addView(top);

        TextView time = makeText(item.actionTime, 12, SUB, false);
        time.setPadding(0, dp(4), 0, dp(6));
        card.addView(time);

        TextView desc = makeText(item.description, 13, TEXT, false);
        desc.setSingleLine(false);
        card.addView(desc);

        return card;
    }

    private int getActionColor(String action) {
        if (action == null) {
            return SUB;
        }

        String a = action.toLowerCase(Locale.getDefault());

        if (a.contains("delete") || a.contains("xóa")) {
            return RED;
        }

        if (a.contains("add") || a.contains("create")) {
            return GREEN;
        }

        if (a.contains("set") || a.contains("update") || a.contains("change")) {
            return BLUE;
        }

        if (a.contains("reset")) {
            return ORANGE;
        }

        return PRIMARY;
    }

    private void setupDateInput(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(true);
        editText.setInputType(InputType.TYPE_NULL);
    }

    private void pickDate(EditText target) {
        Calendar cal = Calendar.getInstance();

        String value = target.getText().toString().trim();

        if (!value.isEmpty()) {
            try {
                cal.setTime(dbDateFormat.parse(value));
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    target.setText(dbDateFormat.format(picked.getTime()));
                    loadLogs();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private int findActionIndex(String id) {
        if (id == null) {
            id = "";
        }

        for (int i = 0; i < actions.size(); i++) {
            if (id.equals(actions.get(i).id)) {
                return i;
            }
        }

        return 0;
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
        box.addView(makeText(value, 19, color, true));
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

    class WhiteSpinnerAdapter<T> extends ArrayAdapter<T> {
        WhiteSpinnerAdapter(ArrayList<T> data) {
            super(AdminActivityLogActivity.this, android.R.layout.simple_spinner_item, data);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        @Override
        public android.view.View getDropDownView(int position, android.view.View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(Color.parseColor("#111827"));
            tv.setPadding(dp(16), dp(16), dp(16), dp(16));
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        private TextView makeSpinnerText(T item) {
            TextView tv = new TextView(AdminActivityLogActivity.this);
            tv.setText(item == null ? "" : item.toString());
            tv.setTextSize(15);
            tv.setSingleLine(false);
            tv.setTextColor(Color.WHITE);
            return tv;
        }
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

    static class LogItem {
        int logId;
        String actionCode;
        String actionTime;
        String performedBy;
        String description;
    }
}