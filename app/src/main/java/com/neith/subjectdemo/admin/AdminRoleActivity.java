package com.neith.subjectdemo.admin;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class AdminRoleActivity extends AppCompatActivity {

    SQLiteDatabase db;

    EditText edtSearch;
    Spinner spnRoleFilter;
    Button btnApply, btnReset, btnAdd;
    LinearLayout layoutList;

    ArrayList<RoleOption> roleOptions = new ArrayList<>();
    String selectedPrefix = "";

    LinearLayout currentDialogRoot;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");
    final int PURPLE = Color.parseColor("#A855F7");

    SimpleDateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_roles);

        db = DB.openDatabase(this);

        edtSearch = findViewById(R.id.edtSearchAdminRole);
        spnRoleFilter = findViewById(R.id.spnAdminRoleFilter);
        btnApply = findViewById(R.id.btnApplyAdminRoleFilter);
        btnReset = findViewById(R.id.btnResetAdminRoleFilter);
        btnAdd = findViewById(R.id.btnAddAdminAccount);
        layoutList = findViewById(R.id.layoutAdminRoleList);

        btnApply.setBackgroundTintList(null);
        btnReset.setBackgroundTintList(null);
        btnAdd.setBackgroundTintList(null);

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setupAdmin(this, bottomNavContainer, BottomNav.ADMIN_ROLE);

        setupRoles();
        setupSpinner();

        btnApply.setOnClickListener(v -> loadRoles());

        btnReset.setOnClickListener(v -> {
            edtSearch.setText("");
            spnRoleFilter.setSelection(0);
            selectedPrefix = "";
            loadRoles();
        });

        btnAdd.setOnClickListener(v -> showAddAccountDialog());

        edtSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                loadRoles();
            }
        });

        loadRoles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRoles();
    }

    private void setupRoles() {
        roleOptions.clear();
        roleOptions.add(new RoleOption("", "Tất cả quyền"));
        roleOptions.add(new RoleOption("AD", "AD - Admin"));
        roleOptions.add(new RoleOption("HR", "HR - Quản lý nhân sự"));
        roleOptions.add(new RoleOption("FN", "FN - Quản lý tài chính"));
        roleOptions.add(new RoleOption("OF", "OF - Quản lý văn phòng"));
        roleOptions.add(new RoleOption("EM", "EM - Nhân viên"));
    }

    private void setupSpinner() {
        spnRoleFilter.setAdapter(new WhiteSpinnerAdapter<>(roleOptions));
        spnRoleFilter.setPopupBackgroundResource(R.drawable.spinner_dropdown_bg);

        spnRoleFilter.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPrefix = roleOptions.get(position).prefix;
            }
        });
    }

    private void loadRoles() {
        layoutList.removeAllViews();

        buildSummaryCard();

        String keyword = edtSearch.getText().toString().trim();

        StringBuilder sql = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        sql.append("SELECT U.USERNAME, IFNULL(U.AUTH, ''), IFNULL(C.NAMEAUTH, ''), ");
        sql.append("IFNULL(C.CODE, ''), IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, ");
        sql.append("IFNULL(CV.TENCV, ''), IFNULL(C.CODEBUS, ''), IFNULL(DN.TENDN, '') ");
        sql.append("FROM USERS U ");
        sql.append("LEFT JOIN CONFIRMAUTH C ON U.AUTH = C.AUTH ");
        sql.append("LEFT JOIN NHANVIEN NV ON C.CODE = NV.MANV ");
        sql.append("LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV ");
        sql.append("LEFT JOIN THONGTINDOANHNGHIEP DN ON C.CODEBUS = DN.MADN ");
        sql.append("WHERE 1 = 1 ");

        if (!selectedPrefix.isEmpty()) {
            sql.append("AND UPPER(SUBSTR(IFNULL(U.AUTH, ''), 1, 2)) = ? ");
            args.add(selectedPrefix);
        }

        if (!keyword.isEmpty()) {
            sql.append("AND (U.USERNAME LIKE ? OR U.AUTH LIKE ? OR C.NAMEAUTH LIKE ? OR C.CODE LIKE ? ");
            sql.append("OR IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') LIKE ? ");
            sql.append("OR IFNULL(CV.TENCV, '') LIKE ? OR IFNULL(DN.TENDN, '') LIKE ?) ");

            for (int i = 0; i < 7; i++) {
                args.add("%" + keyword + "%");
            }
        }

        sql.append("ORDER BY UPPER(SUBSTR(IFNULL(U.AUTH, ''), 1, 2)), U.USERNAME");

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));

        if (c.getCount() == 0) {
            TextView empty = makeText("Không có tài khoản phù hợp.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, dp(28));
            layoutList.addView(empty);
        } else {
            while (c.moveToNext()) {
                AccountItem item = new AccountItem();
                item.username = c.getString(0);
                item.auth = c.getString(1);
                item.nameAuth = c.getString(2);
                item.employeeCode = c.getString(3);
                item.employeeName = c.getString(4).trim();
                item.positionName = c.getString(5);
                item.businessCode = c.getString(6);
                item.businessName = c.getString(7);

                layoutList.addView(makeAccountCard(item));
            }
        }

        c.close();
    }

    private void buildSummaryCard() {
        LinearLayout card = makeCard();

        card.addView(makeCardTitle("TỔNG QUAN TÀI KHOẢN"));

        int totalUser = getInt("SELECT COUNT(*) FROM USERS", null);
        int totalAuth = getInt("SELECT COUNT(*) FROM CONFIRMAUTH", null);
        int unassigned = getInt(
                "SELECT COUNT(*) FROM NHANVIEN WHERE MANV NOT IN (SELECT CODE FROM CONFIRMAUTH WHERE CODE IS NOT NULL)",
                null
        );
        int admin = getRoleCount("AD");

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(makeStatBox("USERS", String.valueOf(totalUser), "Tài khoản đăng nhập", PRIMARY), halfLp(true));
        row1.addView(makeStatBox("AUTH", String.valueOf(totalAuth), "Mã phân quyền", BLUE), halfLp(false));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);
        row2.addView(makeStatBox("Admin", String.valueOf(admin), "Quyền AD", RED), halfLp(true));
        row2.addView(makeStatBox("Chưa gán", String.valueOf(unassigned), "Nhân viên chưa có quyền", ORANGE), halfLp(false));

        card.addView(row1);
        card.addView(row2);

        layoutList.addView(card);
    }

    private View makeAccountCard(AccountItem item) {
        LinearLayout card = makeCard();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);

        TextView title = makeText(item.username, 17, TEXT, true);
        info.addView(title);

        TextView auth = makeText(item.auth + " • " + getRoleName(getPrefix(item.auth)), 12, getRoleColor(getPrefix(item.auth)), true);
        auth.setPadding(0, dp(3), 0, 0);
        info.addView(auth);

        top.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView badge = makeText(getPrefix(item.auth), 13, Color.BLACK, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.employee_avatar_bg);
        top.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(48)));

        card.addView(top);

        card.addView(makeInfoRow("Tên quyền", emptyText(item.nameAuth)));
        card.addView(makeInfoRow("Mã nhân viên", emptyText(item.employeeCode)));
        card.addView(makeInfoRow("Nhân viên", emptyText(item.employeeName)));
        card.addView(makeInfoRow("Chức vụ", emptyText(item.positionName)));
        card.addView(makeInfoRow("Doanh nghiệp", emptyText(item.businessCode) + " • " + emptyText(item.businessName)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(10), 0, 0);

        actions.addView(makeMiniButton("Đổi quyền", BLUE, Color.WHITE, v -> showChangeRoleDialog(item)));
        actions.addView(makeMiniButton("Reset pass", ORANGE, Color.WHITE, v -> showResetPasswordDialog(item)));

        if (!"admin".equalsIgnoreCase(item.username)) {
            actions.addView(makeMiniButton("Xóa", Color.parseColor("#991B1B"), Color.WHITE, v -> showDeleteDialog(item)));
        }

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(actions);

        card.addView(hsv);

        return card;
    }

    private void showAddAccountDialog() {
        ArrayList<EmployeeOption> employees = loadUnassignedEmployees();

        if (employees.isEmpty()) {
            Toast.makeText(this, "Không còn nhân viên chưa gán quyền.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = createStyledDialog("Tạo tài khoản nhân viên");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        Spinner spnEmployee = makeDarkSpinner();
        spnEmployee.setAdapter(new WhiteSpinnerAdapter<>(employees));

        Spinner spnRole = makeDarkSpinner();
        ArrayList<RoleOption> createRoles = new ArrayList<>();
        createRoles.add(new RoleOption("HR", "HR - Quản lý nhân sự"));
        createRoles.add(new RoleOption("FN", "FN - Quản lý tài chính"));
        createRoles.add(new RoleOption("OF", "OF - Quản lý văn phòng"));
        createRoles.add(new RoleOption("EM", "EM - Nhân viên"));
        createRoles.add(new RoleOption("AD", "AD - Admin"));
        spnRole.setAdapter(new WhiteSpinnerAdapter<>(createRoles));

        EditText edtRoleName = makeDarkInput("Tên quyền, bỏ trống sẽ tự lấy theo role");
        edtRoleName.setInputType(InputType.TYPE_CLASS_TEXT);

        TextView note = makeText("Username mặc định = mã nhân viên viết thường. Mật khẩu mặc định = Abc@1234.", 12, SUB, false);
        note.setPadding(0, dp(6), 0, dp(8));

        content.addView(makeDialogLabel("Nhân viên chưa có tài khoản"));
        content.addView(spnEmployee);
        content.addView(makeDialogLabel("Nhóm quyền"));
        content.addView(spnRole);
        content.addView(makeDialogLabel("Tên quyền"));
        content.addView(edtRoleName);
        content.addView(note);

        LinearLayout actions = makeDialogActions();
        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Tạo", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            EmployeeOption emp = (EmployeeOption) spnEmployee.getSelectedItem();
            RoleOption role = (RoleOption) spnRole.getSelectedItem();
            String roleName = edtRoleName.getText().toString().trim();

            if (emp == null || role == null) {
                Toast.makeText(this, "Dữ liệu không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            createAccount(emp, role.prefix, roleName);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createAccount(EmployeeOption emp, String prefix, String roleName) {
        String username = emp.employeeCode.toLowerCase(Locale.getDefault());

        if (exists("SELECT 1 FROM USERS WHERE USERNAME = ?", new String[]{username})) {
            Toast.makeText(this, "Username đã tồn tại.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (exists("SELECT 1 FROM CONFIRMAUTH WHERE CODE = ?", new String[]{emp.employeeCode})) {
            Toast.makeText(this, "Nhân viên này đã có quyền.", Toast.LENGTH_SHORT).show();
            return;
        }

        String auth = generateAuth(prefix, emp.businessCode);

        db.beginTransaction();

        try {
            ContentValues user = new ContentValues();
            user.put("USERNAME", username);
            user.put("PASS", sha256("Abc@1234"));
            user.put("AUTH", auth);

            if (db.insert("USERS", null, user) == -1) {
                Toast.makeText(this, "Tạo USERS thất bại.", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues cf = new ContentValues();
            cf.put("AUTH", auth);
            cf.put("NAMEAUTH", roleName.isEmpty() ? getRoleName(prefix) : roleName);
            cf.put("CODE", emp.employeeCode);
            cf.put("CODEBUS", emp.businessCode);

            if (db.insert("CONFIRMAUTH", null, cf) == -1) {
                Toast.makeText(this, "Tạo CONFIRMAUTH thất bại.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.setTransactionSuccessful();

            logAction("CreateAuth", "admin", "Tạo tài khoản " + username + " với quyền " + auth + " cho nhân viên " + emp.employeeCode + ".");
            Toast.makeText(this, "Đã tạo tài khoản. Pass mặc định: Abc@1234", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

        loadRoles();
    }

    private void showChangeRoleDialog(AccountItem item) {
        Dialog dialog = createStyledDialog("Đổi quyền tài khoản");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView user = makeText(item.username + " • " + item.auth, 14, PRIMARY, true);
        user.setPadding(0, 0, 0, dp(10));
        content.addView(user);

        Spinner spnRole = makeDarkSpinner();
        ArrayList<RoleOption> roles = new ArrayList<>();
        roles.add(new RoleOption("AD", "AD - Admin"));
        roles.add(new RoleOption("HR", "HR - Quản lý nhân sự"));
        roles.add(new RoleOption("FN", "FN - Quản lý tài chính"));
        roles.add(new RoleOption("OF", "OF - Quản lý văn phòng"));
        roles.add(new RoleOption("EM", "EM - Nhân viên"));
        spnRole.setAdapter(new WhiteSpinnerAdapter<>(roles));
        spnRole.setSelection(findRoleIndex(roles, getPrefix(item.auth)));

        content.addView(makeDialogLabel("Nhóm quyền mới"));
        content.addView(spnRole);

        TextView note = makeText("Mã AUTH mới sẽ giữ phần đuôi cũ và chỉ thay prefix.", 12, SUB, false);
        note.setPadding(0, dp(6), 0, dp(8));
        content.addView(note);

        LinearLayout actions = makeDialogActions();
        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            RoleOption role = (RoleOption) spnRole.getSelectedItem();

            if (role == null) {
                Toast.makeText(this, "Vui lòng chọn quyền.", Toast.LENGTH_SHORT).show();
                return;
            }

            changeRole(item, role.prefix);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void changeRole(AccountItem item, String newPrefix) {
        String oldAuth = item.auth;
        String tail = oldAuth.length() > 2 ? oldAuth.substring(2) : "";
        String newAuth = newPrefix + tail;

        if (oldAuth.equals(newAuth)) {
            Toast.makeText(this, "Tài khoản đã thuộc quyền này.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (exists("SELECT 1 FROM USERS WHERE AUTH = ?", new String[]{newAuth}) ||
                exists("SELECT 1 FROM CONFIRMAUTH WHERE AUTH = ?", new String[]{newAuth})) {
            Toast.makeText(this, "AUTH mới đã tồn tại: " + newAuth, Toast.LENGTH_SHORT).show();
            return;
        }

        db.beginTransaction();

        try {
            ContentValues userValues = new ContentValues();
            userValues.put("AUTH", newAuth);

            db.update("USERS", userValues, "USERNAME = ?", new String[]{item.username});

            ContentValues cfValues = new ContentValues();
            cfValues.put("AUTH", newAuth);
            cfValues.put("NAMEAUTH", getRoleName(newPrefix));

            db.update("CONFIRMAUTH", cfValues, "AUTH = ?", new String[]{oldAuth});

            db.setTransactionSuccessful();

            logAction("ChangeRole", item.username, "Đổi quyền từ " + oldAuth + " sang " + newAuth + ".");
            Toast.makeText(this, "Đã đổi quyền thành " + newAuth, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đổi quyền: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

        loadRoles();
    }

    private void showResetPasswordDialog(AccountItem item) {
        Dialog dialog = createStyledDialog("Reset mật khẩu");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText("Reset mật khẩu tài khoản " + item.username + " về mặc định Abc@1234?", 14, TEXT, false);
        msg.setPadding(0, 0, 0, dp(12));
        content.addView(msg);

        LinearLayout actions = makeDialogActions();
        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button reset = makeDialogButton("Reset", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(reset);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        reset.setOnClickListener(v -> {
            resetPassword(item.username);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void resetPassword(String username) {
        ContentValues values = new ContentValues();
        values.put("PASS", sha256("Abc@1234"));

        int rows = db.update("USERS", values, "USERNAME = ?", new String[]{username});

        if (rows > 0) {
            logAction("ResetPass", username, "Reset mật khẩu tài khoản " + username + " về mặc định.");
            Toast.makeText(this, "Đã reset mật khẩu: Abc@1234", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Không tìm thấy tài khoản.", Toast.LENGTH_SHORT).show();
        }

        loadRoles();
    }

    private void showDeleteDialog(AccountItem item) {
        Dialog dialog = createStyledDialog("Xóa tài khoản");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText("Xóa tài khoản " + item.username + " và CONFIRMAUTH " + item.auth + "?", 14, TEXT, false);
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
            deleteAccount(item);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteAccount(AccountItem item) {
        if ("admin".equalsIgnoreCase(item.username)) {
            Toast.makeText(this, "Không thể xóa tài khoản admin.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.beginTransaction();

        try {
            db.delete("USERS", "USERNAME = ?", new String[]{item.username});
            db.delete("CONFIRMAUTH", "AUTH = ?", new String[]{item.auth});

            db.setTransactionSuccessful();

            logAction("DeleteAuth", "admin", "Xóa tài khoản " + item.username + " và quyền " + item.auth + ".");
            Toast.makeText(this, "Đã xóa tài khoản.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xóa tài khoản: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

        loadRoles();
    }

    private ArrayList<EmployeeOption> loadUnassignedEmployees() {
        ArrayList<EmployeeOption> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT NV.MANV, IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "IFNULL(CV.TENCV, ''), IFNULL(NV.MADN, ''), IFNULL(DN.TENDN, '') " +
                        "FROM NHANVIEN NV " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "LEFT JOIN THONGTINDOANHNGHIEP DN ON NV.MADN = DN.MADN " +
                        "WHERE NV.MANV NOT IN (SELECT CODE FROM CONFIRMAUTH WHERE CODE IS NOT NULL) " +
                        "ORDER BY NV.MANV",
                null
        );

        while (c.moveToNext()) {
            EmployeeOption item = new EmployeeOption();
            item.employeeCode = c.getString(0);
            item.employeeName = c.getString(1).trim();
            item.positionName = c.getString(2);
            item.businessCode = c.getString(3);
            item.businessName = c.getString(4);
            list.add(item);
        }

        c.close();

        return list;
    }

    private String generateAuth(String prefix, String businessCode) {
        String base = prefix + (businessCode == null || businessCode.trim().isEmpty() ? "SYS" : businessCode.trim());

        if (!exists("SELECT 1 FROM USERS WHERE AUTH = ?", new String[]{base}) &&
                !exists("SELECT 1 FROM CONFIRMAUTH WHERE AUTH = ?", new String[]{base})) {
            return base;
        }

        Random rnd = new Random();
        String auth;

        do {
            auth = base + rnd.nextInt(900) + 100;
        } while (exists("SELECT 1 FROM USERS WHERE AUTH = ?", new String[]{auth}) ||
                exists("SELECT 1 FROM CONFIRMAUTH WHERE AUTH = ?", new String[]{auth}));

        return auth;
    }

    private void logAction(String actionCode, String performedBy, String description) {
        ContentValues values = new ContentValues();
        values.put("ActionCode", actionCode);
        values.put("ActionTime", logFormat.format(new Date()));
        values.put("PerformedBy", performedBy);
        values.put("Description", description);

        db.insert("ACTIVITY_LOG", null, values);
    }

    private boolean exists(String sql, String[] args) {
        Cursor c = db.rawQuery(sql, args);
        boolean ok = c.moveToFirst();
        c.close();
        return ok;
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

    private int getRoleCount(String prefix) {
        return getInt(
                "SELECT COUNT(*) FROM USERS WHERE UPPER(SUBSTR(IFNULL(AUTH, ''), 1, 2)) = ?",
                new String[]{prefix}
        );
    }

    private String getPrefix(String auth) {
        if (auth == null || auth.length() < 2) {
            return auth == null ? "" : auth;
        }

        return auth.substring(0, 2).toUpperCase(Locale.getDefault());
    }

    private String getRoleName(String prefix) {
        prefix = prefix == null ? "" : prefix.toUpperCase(Locale.getDefault());

        switch (prefix) {
            case "AD":
                return "Admin";
            case "HR":
                return "Quản lý nhân sự";
            case "FN":
                return "Quản lý tài chính";
            case "OF":
                return "Quản lý văn phòng";
            case "EM":
                return "Nhân viên";
            default:
                return "Không xác định";
        }
    }

    private int getRoleColor(String prefix) {
        prefix = prefix == null ? "" : prefix.toUpperCase(Locale.getDefault());

        switch (prefix) {
            case "AD":
                return RED;
            case "HR":
                return GREEN;
            case "FN":
                return BLUE;
            case "OF":
                return PURPLE;
            case "EM":
                return ORANGE;
            default:
                return SUB;
        }
    }

    private int findRoleIndex(ArrayList<RoleOption> list, String prefix) {
        if (prefix == null) {
            prefix = "";
        }

        for (int i = 0; i < list.size(); i++) {
            if (prefix.equals(list.get(i).prefix)) {
                return i;
            }
        }

        return 0;
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);

                if (h.length() == 1) {
                    hex.append('0');
                }

                hex.append(h);
            }

            return hex.toString();

        } catch (Exception e) {
            return text;
        }
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
        row.setPadding(0, dp(6), 0, dp(6));

        TextView l = makeText(label, 12, SUB, false);
        TextView v = makeText(value, 12, TEXT, true);
        v.setGravity(Gravity.RIGHT);
        v.setSingleLine(false);

        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
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

    private Button makeMiniButton(String text, int bgColor, int textColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTextColor(textColor);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        btn.setOnClickListener(listener);
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
            super(AdminRoleActivity.this, android.R.layout.simple_spinner_item, data);
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
            TextView tv = new TextView(AdminRoleActivity.this);
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

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null") ? "—" : value;
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

    static class RoleOption {
        String prefix;
        String name;

        RoleOption(String prefix, String name) {
            this.prefix = prefix == null ? "" : prefix;
            this.name = name == null ? "" : name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class AccountItem {
        String username;
        String auth;
        String nameAuth;
        String employeeCode;
        String employeeName;
        String positionName;
        String businessCode;
        String businessName;
    }

    static class EmployeeOption {
        String employeeCode;
        String employeeName;
        String positionName;
        String businessCode;
        String businessName;

        @Override
        public String toString() {
            return employeeCode + " • " + employeeName + " • " + businessCode;
        }
    }
}