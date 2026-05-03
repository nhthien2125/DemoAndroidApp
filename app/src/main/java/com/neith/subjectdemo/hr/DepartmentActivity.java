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
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class DepartmentActivity extends AppCompatActivity {

    SQLiteDatabase db;

    LinearLayout layoutDepartmentList;
    Button btnAddDepartment;

    ArrayList<AreaItem> areas = new ArrayList<>();

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int BLUE = Color.parseColor("#3B82F6");

    LinearLayout currentDialogRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department);

        db = DB.openDatabase(this);

        layoutDepartmentList = findViewById(R.id.layoutDepartmentList);
        btnAddDepartment = findViewById(R.id.btnAddDepartment);

        btnAddDepartment.setBackgroundTintList(null);
        btnAddDepartment.setOnClickListener(v -> showCreateDepartmentDialog());

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.DEPARTMENT);

        loadAreas();
        loadDepartments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAreas();
        loadDepartments();
    }

    private void loadAreas() {
        areas.clear();
        areas.add(new AreaItem("", "-- Chọn khu vực --"));

        Cursor c = db.rawQuery(
                "SELECT MAKV, TENTINH FROM DTTHEOKV ORDER BY TENTINH",
                null
        );

        while (c.moveToNext()) {
            areas.add(new AreaItem(c.getString(0), c.getString(1)));
        }

        c.close();
    }

    private void loadDepartments() {
        layoutDepartmentList.removeAllViews();

        Cursor c = db.rawQuery(
                "SELECT PB.MAPB, PB.TENPB, IFNULL(PB.DIADIEM, ''), IFNULL(PB.MATRG_PHG, ''), " +
                        "IFNULL(KV.TENTINH, ''), " +
                        "IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HEAD_NAME, " +
                        "(SELECT COUNT(*) FROM NHANVIEN N WHERE N.MAPB = PB.MAPB) AS EMP_COUNT " +
                        "FROM PHONGBAN PB " +
                        "LEFT JOIN DTTHEOKV KV ON CAST(PB.DIADIEM AS TEXT) = CAST(KV.MAKV AS TEXT) " +
                        "LEFT JOIN NHANVIEN NV ON PB.MATRG_PHG = NV.MANV " +
                        "ORDER BY PB.MAPB",
                null
        );

        if (c.getCount() == 0) {
            TextView empty = makeText("Chưa có phòng ban.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            layoutDepartmentList.addView(empty);
        } else {
            while (c.moveToNext()) {
                DepartmentItem item = new DepartmentItem();

                item.maPB = c.getString(0);
                item.tenPB = c.getString(1);
                item.diaDiem = c.getString(2);
                item.maTruongPhong = c.getString(3);
                item.tenKhuVuc = c.getString(4);
                item.tenTruongPhong = c.getString(5).trim();
                item.soNhanVien = c.getInt(6);

                layoutDepartmentList.addView(makeDepartmentCard(item));
            }
        }

        c.close();
    }

    private View makeDepartmentCard(DepartmentItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.hr_card_bg);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardLp);

        card.setOnClickListener(v -> {
            animateClick(card);
            openDetail(item.maPB);
        });

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = makeDeptAvatar(item.tenPB);
        top.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);

        TextView name = makeText(item.tenPB, 17, TEXT, true);
        name.setSingleLine(false);
        info.addView(name);

        TextView code = makeText("Mã: " + item.maPB, 12, SUB, false);
        code.setPadding(0, dp(2), 0, 0);
        info.addView(code);

        top.addView(info, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        card.addView(top);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(0, dp(12), 0, dp(6));

        meta.addView(makeInfoRow(R.drawable.ic_people, "Nhân viên", String.valueOf(item.soNhanVien)));
        meta.addView(makeInfoRow(R.drawable.ic_manager, "Trưởng phòng", getHeadDisplay(item)));
        meta.addView(makeInfoRow(R.drawable.ic_location, "Địa điểm", getLocationDisplay(item)));

        card.addView(meta);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(10), 0, 0);

        actions.addView(makeSmallButton("Sửa", BLUE, Color.WHITE, v -> showEditDepartmentDialog(item)));
        actions.addView(makeSmallButton("Xóa", Color.parseColor("#991B1B"), Color.WHITE, v -> showDeleteDepartmentDialog(item)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(actions);

        card.addView(hsv);

        return card;
    }

    private String getHeadDisplay(DepartmentItem item) {
        if (item.tenTruongPhong != null && !item.tenTruongPhong.trim().isEmpty()) {
            return item.tenTruongPhong;
        }

        if (item.maTruongPhong != null && !item.maTruongPhong.trim().isEmpty()) {
            return item.maTruongPhong;
        }

        return "Chưa thiết lập";
    }

    private String getLocationDisplay(DepartmentItem item) {
        if (item.tenKhuVuc != null && !item.tenKhuVuc.trim().isEmpty()) {
            return item.tenKhuVuc;
        }

        if (item.diaDiem != null && !item.diaDiem.trim().isEmpty()) {
            return item.diaDiem;
        }

        return "N/A";
    }

    private void openDetail(String maPB) {
        Intent intent = new Intent(this, DepartmentDetailActivity.class);
        intent.putExtra("MAPB", maPB);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showCreateDepartmentDialog() {
        Dialog dialog = createStyledDialog("Thêm phòng ban mới");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtName = makeDarkInput("Tên phòng ban");

        Spinner spnArea = makeDarkSpinner();
        spnArea.setAdapter(new SpinnerTextAdapter<>(areas));

        content.addView(makeDialogLabel("Tên phòng ban"));
        content.addView(edtName);

        content.addView(makeDialogLabel("Địa điểm / khu vực"));
        content.addView(spnArea);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Tạo mới", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            AreaItem area = (AreaItem) spnArea.getSelectedItem();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên phòng ban.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (area == null || area.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn khu vực.", Toast.LENGTH_SHORT).show();
                return;
            }

            createDepartment(name, area.id);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createDepartment(String tenPB, String diaDiem) {
        String maPB = generateDeptCode();

        ContentValues values = new ContentValues();
        values.put("MAPB", maPB);
        values.put("TENPB", tenPB);
        values.put("DIADIEM", diaDiem);
        values.putNull("MATRG_PHG");

        long result = db.insert("PHONGBAN", null, values);

        if (result != -1) {
            Toast.makeText(this, "Đã tạo phòng ban: " + maPB, Toast.LENGTH_SHORT).show();
            loadDepartments();
        } else {
            Toast.makeText(this, "Không thể tạo phòng ban.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDepartmentDialog(DepartmentItem item) {
        Dialog dialog = createStyledDialog("Sửa thông tin phòng ban");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView code = makeText(item.maPB, 14, PRIMARY, true);
        code.setPadding(0, 0, 0, dp(8));

        EditText edtName = makeDarkInput("Tên phòng ban");
        edtName.setText(item.tenPB);
        edtName.setSelection(edtName.getText().length());

        Spinner spnArea = makeDarkSpinner();
        spnArea.setAdapter(new SpinnerTextAdapter<>(areas));
        spnArea.setSelection(findAreaIndex(item.diaDiem));

        content.addView(makeDialogLabel("Mã phòng ban"));
        content.addView(code);

        content.addView(makeDialogLabel("Tên phòng ban"));
        content.addView(edtName);

        content.addView(makeDialogLabel("Địa điểm / khu vực"));
        content.addView(spnArea);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            AreaItem area = (AreaItem) spnArea.getSelectedItem();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên phòng ban.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (area == null || area.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn khu vực.", Toast.LENGTH_SHORT).show();
                return;
            }

            updateDepartment(item.maPB, name, area.id);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateDepartment(String maPB, String tenPB, String diaDiem) {
        ContentValues values = new ContentValues();
        values.put("TENPB", tenPB);
        values.put("DIADIEM", diaDiem);

        int rows = db.update(
                "PHONGBAN",
                values,
                "MAPB = ?",
                new String[]{maPB}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã cập nhật phòng ban.", Toast.LENGTH_SHORT).show();
            loadDepartments();
        } else {
            Toast.makeText(this, "Không thể cập nhật phòng ban.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteDepartmentDialog(DepartmentItem item) {
        Dialog dialog = createStyledDialog("Xóa phòng ban");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText(
                "Bạn có chắc muốn xóa phòng ban " + item.tenPB + "?\nChỉ có thể xóa nếu phòng ban không còn nhân viên.",
                14,
                TEXT,
                false
        );
        msg.setPadding(0, 0, 0, dp(12));
        content.addView(msg);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button delete = makeDialogButton("Xóa", R.drawable.employee_button_red_bg, Color.WHITE);

        actions.addView(cancel);
        actions.addView(delete);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        delete.setOnClickListener(v -> {
            deleteDepartment(item.maPB);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteDepartment(String maPB) {
        int count = getInt(
                "SELECT COUNT(*) FROM NHANVIEN WHERE MAPB = ?",
                new String[]{maPB}
        );

        if (count > 0) {
            Toast.makeText(this, "Không thể xóa vì phòng ban vẫn còn nhân viên.", Toast.LENGTH_LONG).show();
            return;
        }

        int rows = db.delete(
                "PHONGBAN",
                "MAPB = ?",
                new String[]{maPB}
        );

        if (rows > 0) {
            Toast.makeText(this, "Đã xóa phòng ban.", Toast.LENGTH_SHORT).show();
            loadDepartments();
        } else {
            Toast.makeText(this, "Không tìm thấy phòng ban.", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateDeptCode() {
        Random random = new Random();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int attempt = 0; attempt < 500; attempt++) {
            StringBuilder sb = new StringBuilder("PB");

            for (int i = 0; i < 4; i++) {
                sb.append(letters.charAt(random.nextInt(letters.length())));
            }

            String code = sb.toString();

            if (!departmentCodeExists(code)) {
                return code;
            }
        }

        return "PB" + System.currentTimeMillis();
    }

    private boolean departmentCodeExists(String maPB) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM PHONGBAN WHERE MAPB = ?",
                new String[]{maPB}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private int findAreaIndex(String areaId) {
        if (areaId == null) {
            areaId = "";
        }

        for (int i = 0; i < areas.size(); i++) {
            if (areaId.equals(String.valueOf(areas.get(i).id))) {
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

    private TextView makeDeptAvatar(String name) {
        TextView avatar = makeText(getInitial(name), 18, Color.BLACK, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.employee_avatar_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatar.setLayoutParams(lp);

        return avatar;
    }

    private LinearLayout makeInfoRow(int iconRes, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(SUB);
        icon.setAlpha(0.85f);

        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        iconLp.setMargins(0, 0, dp(8), 0);
        row.addView(icon, iconLp);

        TextView l = makeText(label, 13, SUB, false);

        row.addView(l, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView v = makeText(
                value == null || value.trim().isEmpty() ? "—" : value,
                13,
                TEXT,
                true
        );
        v.setGravity(Gravity.RIGHT);

        row.addView(v, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        return row;
    }

    private Button makeSmallButton(String text, int bgColor, int textColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTextColor(textColor);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        btn.setOnClickListener(v -> listener.onClick(v));
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        btn.setPadding(dp(14), 0, dp(14), 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );
        lp.setMargins(0, 0, dp(7), 0);
        btn.setLayoutParams(lp);

        return btn;
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

    private Spinner makeDarkSpinner() {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(R.drawable.employee_input_bg);
        spinner.setPadding(dp(10), 0, dp(10), 0);
        spinner.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        lp.setMargins(0, 0, 0, dp(8));
        spinner.setLayoutParams(lp);

        return spinner;
    }

    class SpinnerTextAdapter<T> extends ArrayAdapter<T> {

        SpinnerTextAdapter(ArrayList<T> data) {
            super(DepartmentActivity.this, android.R.layout.simple_spinner_item, data);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(TEXT);
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView tv = makeSpinnerText(getItem(position));
            tv.setTextColor(TEXT);
            tv.setBackgroundColor(Color.parseColor("#111827"));
            tv.setPadding(dp(16), dp(16), dp(16), dp(16));
            tv.setGravity(Gravity.CENTER_VERTICAL);
            return tv;
        }

        private TextView makeSpinnerText(T item) {
            TextView tv = new TextView(DepartmentActivity.this);
            tv.setText(item == null ? "" : item.toString());
            tv.setTextSize(15);
            tv.setSingleLine(false);
            tv.setTextColor(TEXT);
            tv.setTypeface(null, Typeface.NORMAL);
            return tv;
        }
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

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(96), dp(44));
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

    private String getInitial(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "PB";
        }

        String[] parts = text.trim().split("\\s+");
        String last = parts[parts.length - 1];

        if (last.length() >= 1) {
            return last.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        return "PB";
    }

    private void animateClick(View view) {
        view.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .alpha(0.85f)
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class DepartmentItem {
        String maPB;
        String tenPB;
        String diaDiem;
        String maTruongPhong;
        String tenTruongPhong;
        String tenKhuVuc;
        int soNhanVien;
    }

    static class AreaItem {
        String id;
        String name;

        AreaItem(String id, String name) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}