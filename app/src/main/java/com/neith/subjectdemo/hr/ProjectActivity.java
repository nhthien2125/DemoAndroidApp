package com.neith.subjectdemo.hr;

import android.app.DatePickerDialog;
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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ProjectActivity extends AppCompatActivity {

    SQLiteDatabase db;

    LinearLayout layoutProjectList;
    Button btnAddProject;

    ArrayList<OptionItem> customers = new ArrayList<>();
    ArrayList<OptionItem> areas = new ArrayList<>();
    ArrayList<OptionItem> projectTypes = new ArrayList<>();
    ArrayList<OptionItem> contractTypes = new ArrayList<>();

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");
    final int PURPLE = Color.parseColor("#A855F7");

    LinearLayout currentDialogRoot;

    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat viewDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

        db = DB.openDatabase(this);

        layoutProjectList = findViewById(R.id.layoutProjectList);
        btnAddProject = findViewById(R.id.btnAddProject);

        btnAddProject.setBackgroundTintList(null);
        btnAddProject.setOnClickListener(v -> showCreateProjectDialog());

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.PROJECT);

        loadOptionData();
        loadProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOptionData();
        loadProjects();
    }

    private void loadOptionData() {
        customers.clear();
        areas.clear();
        projectTypes.clear();
        contractTypes.clear();

        contractTypes.add(new OptionItem("", "-- Chọn loại --"));
        contractTypes.add(new OptionItem("1", "Loại 1"));
        contractTypes.add(new OptionItem("2", "Loại 2"));
        contractTypes.add(new OptionItem("3", "Loại 3"));
        contractTypes.add(new OptionItem("4", "Loại 4"));

        customers.add(new OptionItem("", "-- Chọn khách hàng --"));
        Cursor cCus = db.rawQuery(
                "SELECT MAKH, IFNULL(LOAIKH, MAKH) FROM KHACHHANG ORDER BY MAKH",
                null
        );
        while (cCus.moveToNext()) {
            customers.add(new OptionItem(cCus.getString(0), cCus.getString(1)));
        }
        cCus.close();

        areas.add(new OptionItem("", "-- Chọn khu vực --"));
        Cursor cArea = db.rawQuery(
                "SELECT MAKV, TENTINH FROM DTTHEOKV ORDER BY TENTINH",
                null
        );
        while (cArea.moveToNext()) {
            areas.add(new OptionItem(cArea.getString(0), cArea.getString(1)));
        }
        cArea.close();

        projectTypes.add(new OptionItem("", "-- Chọn loại hình --"));
        Cursor cType = db.rawQuery(
                "SELECT MALH, TENLH FROM DTTHEOLHCT ORDER BY TENLH",
                null
        );
        while (cType.moveToNext()) {
            projectTypes.add(new OptionItem(cType.getString(0), cType.getString(1)));
        }
        cType.close();
    }

    private void loadProjects() {
        layoutProjectList.removeAllViews();

        for (int i = 1; i <= 4; i++) {
            String typeCode = String.valueOf(i);
            ArrayList<ProjectItem> items = getProjectsByType(typeCode);
            layoutProjectList.addView(makeSection(typeCode, items));
        }
    }

    private ArrayList<ProjectItem> getProjectsByType(String typeCode) {
        ArrayList<ProjectItem> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT DTHD.MADAHD, DTHD.MADA, DTHD.MAHD, " +
                        "IFNULL(HD.TENHD, DTHD.MADAHD) AS TENHD, " +
                        "IFNULL(HD.LOAI, '') AS LOAI, " +
                        "IFNULL(HD.NGAYBD, '') AS NGAYBD, " +
                        "IFNULL(DTHD.NGAYKT, '') AS NGAYKT, " +
                        "IFNULL(KH.LOAIKH, '') AS KHACHHANG, " +
                        "IFNULL(KV.TENTINH, '') AS KHUVUC, " +
                        "(SELECT COUNT(*) FROM NVTHAMGIADA N WHERE N.MADAHD = DTHD.MADAHD) AS EMP_COUNT " +
                        "FROM DUANTHEOHOPDONG DTHD " +
                        "JOIN DUAN DA ON DTHD.MADA = DA.MADA " +
                        "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                        "LEFT JOIN KHACHHANG KH ON DA.MAKH = KH.MAKH " +
                        "LEFT JOIN DTDUAN DT ON DA.MADA = DT.MADA " +
                        "LEFT JOIN DTTHEOKV KV ON DT.MAKV = KV.MAKV " +
                        "WHERE TRIM(IFNULL(HD.LOAI, '')) = ? " +
                        "ORDER BY TENHD",
                new String[]{typeCode}
        );

        while (c.moveToNext()) {
            ProjectItem item = new ProjectItem();
            item.projectKey = c.getString(0);
            item.projectCode = c.getString(1);
            item.contractCode = c.getString(2);
            item.projectName = c.getString(3);
            item.typeCode = c.getString(4);
            item.startDate = c.getString(5);
            item.endDate = c.getString(6);
            item.customerName = c.getString(7);
            item.areaName = c.getString(8);
            item.employeeCount = c.getInt(9);
            item.statusCode = getStatusCode(item.startDate, item.endDate);
            item.statusLabel = getStatusLabel(item.statusCode);

            list.add(item);
        }

        c.close();
        return list;
    }

    private View makeSection(String typeCode, ArrayList<ProjectItem> items) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(14), dp(14), dp(14), dp(14));
        section.setBackgroundResource(R.drawable.hr_card_bg);

        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        sectionLp.setMargins(0, 0, 0, dp(14));
        section.setLayoutParams(sectionLp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(10));

        TextView title = makeText(getTypeName(typeCode), 17, PRIMARY, true);
        TextView count = makeText(items.size() + " dự án", 12, SUB, false);
        count.setGravity(Gravity.RIGHT);

        header.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        header.addView(count);

        section.addView(header);

        if (items.isEmpty()) {
            TextView empty = makeText("Chưa có dự án trong nhóm này.", 13, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(16), 0, dp(16));
            section.addView(empty);
        } else {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            for (ProjectItem item : items) {
                row.addView(makeProjectCard(item));
            }

            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            hsv.addView(row);

            section.addView(hsv);
        }

        return section;
    }

    private View makeProjectCard(ProjectItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.employee_input_bg);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(dp(270), LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, dp(10), 0);
        card.setLayoutParams(cardLp);

        card.setOnClickListener(v -> {
            animateClick(card);
            openDetail(item.projectKey);
        });

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView name = makeText(item.projectName, 15, TEXT, true);
        name.setSingleLine(false);
        titleBox.addView(name);

        TextView code = makeText(item.projectCode + " • HĐ: " + item.contractCode, 11, SUB, false);
        code.setPadding(0, dp(2), 0, 0);
        titleBox.addView(code);

        top.addView(titleBox, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView status = makeStatusPill(item.statusLabel, item.statusCode);
        top.addView(status);

        card.addView(top);

        card.addView(makeInfoLine("Khách hàng", emptyText(item.customerName)));
        card.addView(makeInfoLine("Khu vực", emptyText(item.areaName)));
        card.addView(makeInfoLine("Thời gian", formatDate(item.startDate) + " - " + formatDate(item.endDate)));
        card.addView(makeInfoLine("Nhân sự", item.employeeCount + " người"));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(10), 0, 0);

        actions.addView(makeSmallButton("Xóa", Color.parseColor("#991B1B"), Color.WHITE, v -> showDeleteProjectDialog(item)));

        card.addView(actions);

        return card;
    }

    private TextView makeStatusPill(String text, String code) {
        int color = getStatusColor(code);

        TextView tv = makeText(text, 10, color, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(3), dp(8), dp(3));
        tv.setBackgroundResource(R.drawable.employee_tag_bg);

        return tv;
    }

    private LinearLayout makeInfoLine(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, 0);

        TextView l = makeText(label, 11, SUB, false);
        TextView v = makeText(value, 11, TEXT, true);
        v.setGravity(Gravity.RIGHT);
        v.setSingleLine(false);

        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
    }

    private void openDetail(String projectKey) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        intent.putExtra("PROJECT_KEY", projectKey);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showCreateProjectDialog() {
        Dialog dialog = createStyledDialog("Dự án mới");

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        Spinner spnType = makeDarkSpinner();
        spnType.setAdapter(new SpinnerTextAdapter<>(contractTypes));

        EditText edtName = makeDarkInput("Tên dự án / hợp đồng");

        Spinner spnCustomer = makeDarkSpinner();
        spnCustomer.setAdapter(new SpinnerTextAdapter<>(customers));

        Spinner spnArea = makeDarkSpinner();
        spnArea.setAdapter(new SpinnerTextAdapter<>(areas));

        Spinner spnProjectType = makeDarkSpinner();
        spnProjectType.setAdapter(new SpinnerTextAdapter<>(projectTypes));

        EditText edtStart = makeDarkInput("Ngày bắt đầu yyyy-MM-dd");
        edtStart.setFocusable(false);
        edtStart.setOnClickListener(v -> pickDate(edtStart));

        EditText edtEnd = makeDarkInput("Ngày kết thúc yyyy-MM-dd");
        edtEnd.setFocusable(false);
        edtEnd.setOnClickListener(v -> pickDate(edtEnd));

        EditText edtDeposit = makeNumberInput("Tiền cọc");
        EditText edtExpected = makeNumberInput("Doanh thu dự kiến");
        EditText edtCoefficient = makeNumberInput("Hệ số điều chỉnh");
        EditText edtFinalTotal = makeNumberInput("Tổng nghiệm thu");

        content.addView(makeDialogLabel("Loại hợp đồng / dự án"));
        content.addView(spnType);

        content.addView(makeDialogLabel("Tên dự án"));
        content.addView(edtName);

        content.addView(makeDialogLabel("Khách hàng"));
        content.addView(spnCustomer);

        content.addView(makeDialogLabel("Khu vực"));
        content.addView(spnArea);

        content.addView(makeDialogLabel("Loại hình dự án"));
        content.addView(spnProjectType);

        content.addView(makeDialogLabel("Ngày bắt đầu"));
        content.addView(edtStart);

        content.addView(makeDialogLabel("Ngày kết thúc"));
        content.addView(edtEnd);

        content.addView(makeDialogLabel("Tài chính"));
        content.addView(edtDeposit);
        content.addView(edtExpected);
        content.addView(edtCoefficient);
        content.addView(edtFinalTotal);

        TextView hint = makeText("Mã dự án và mã hợp đồng sẽ tự sinh sau khi lưu.", 12, PRIMARY, true);
        hint.setPadding(0, dp(8), 0, dp(8));
        content.addView(hint);

        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(560)
        );
        scrollLp.setMargins(0, 0, 0, dp(8));

        currentDialogRoot.addView(scrollView, scrollLp);

        LinearLayout actions = makeDialogActions();
        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Tạo", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);
        currentDialogRoot.addView(actions);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            OptionItem type = (OptionItem) spnType.getSelectedItem();
            OptionItem customer = (OptionItem) spnCustomer.getSelectedItem();
            OptionItem area = (OptionItem) spnArea.getSelectedItem();
            OptionItem projectType = (OptionItem) spnProjectType.getSelectedItem();

            String name = edtName.getText().toString().trim();
            String start = edtStart.getText().toString().trim();
            String end = edtEnd.getText().toString().trim();

            if (type == null || type.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn loại dự án.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên dự án.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (customer == null || customer.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn khách hàng.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (area == null || area.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn khu vực.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (projectType == null || projectType.id.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn loại hình dự án.", Toast.LENGTH_SHORT).show();
                return;
            }

            Date startDate = parseDate(start);
            Date endDate = parseDate(end);

            if (!start.isEmpty() && startDate == null) {
                Toast.makeText(this, "Ngày bắt đầu không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!end.isEmpty() && endDate == null) {
                Toast.makeText(this, "Ngày kết thúc không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (startDate != null && endDate != null && endDate.before(startDate)) {
                Toast.makeText(this, "Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu.", Toast.LENGTH_SHORT).show();
                return;
            }

            String projectKey = createProject(
                    type.id,
                    name,
                    customer.id,
                    area.id,
                    projectType.id,
                    start,
                    end,
                    parseDoubleOrNull(edtDeposit.getText().toString()),
                    parseDoubleOrNull(edtExpected.getText().toString()),
                    parseDoubleOrNull(edtCoefficient.getText().toString()),
                    parseDoubleOrNull(edtFinalTotal.getText().toString())
            );

            dialog.dismiss();

            if (projectKey != null) {
                openDetail(projectKey);
            }
        });

        dialog.show();
    }

    private String createProject(
            String typeCode,
            String projectName,
            String customerId,
            String areaCode,
            String loaiHinhCode,
            String startDate,
            String endDate,
            Double deposit,
            Double expectedTotal,
            Double coefficient,
            Double finalTotal
    ) {
        String contractCode = generateContractCode();
        String projectCode = buildProjectCode(typeCode, contractCode);
        String projectKey = projectCode;

        db.beginTransaction();

        try {
            ContentValues hopDong = new ContentValues();
            hopDong.put("MAHD", contractCode);
            hopDong.put("TENHD", projectName);
            hopDong.put("LOAI", typeCode);
            putNullableString(hopDong, "NGAYBD", startDate);
            putNullableString(hopDong, "NGAYKT_DUTINH", endDate);

            if (db.insert("HOPDONG", null, hopDong) == -1) {
                Toast.makeText(this, "Thêm hợp đồng thất bại.", Toast.LENGTH_SHORT).show();
                return null;
            }

            ContentValues duAn = new ContentValues();
            duAn.put("MADA", projectCode);
            duAn.put("MAKH", customerId);
            putNullableDouble(duAn, "TIENCOC", deposit);
            putNullableDouble(duAn, "TIENNGHIEMTHU_DUTINH", expectedTotal);

            if (db.insert("DUAN", null, duAn) == -1) {
                Toast.makeText(this, "Thêm dự án thất bại.", Toast.LENGTH_SHORT).show();
                return null;
            }

            ContentValues dthd = new ContentValues();
            dthd.put("MADAHD", projectKey);
            dthd.put("MADA", projectCode);
            dthd.put("MAHD", contractCode);
            putNullableString(dthd, "NGAYKT", endDate);

            if (db.insert("DUANTHEOHOPDONG", null, dthd) == -1) {
                Toast.makeText(this, "Thêm dự án theo hợp đồng thất bại.", Toast.LENGTH_SHORT).show();
                return null;
            }

            ContentValues dt = new ContentValues();
            dt.put("MADA", projectCode);
            dt.put("MAKV", areaCode);
            dt.put("MALH", loaiHinhCode);
            putNullableDouble(dt, "HSTHAYDOI", coefficient);
            putNullableDouble(dt, "TIENNGHIEMTHU_TONG", finalTotal);

            if (db.insert("DTDUAN", null, dt) == -1) {
                Toast.makeText(this, "Thêm đối tượng dự án thất bại.", Toast.LENGTH_SHORT).show();
                return null;
            }

            db.setTransactionSuccessful();
            Toast.makeText(this, "Đã tạo dự án: " + projectKey, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi tạo dự án: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        } finally {
            db.endTransaction();
        }

        loadProjects();
        return projectKey;
    }

    private void showDeleteProjectDialog(ProjectItem item) {
        Dialog dialog = createStyledDialog("Xóa dự án");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText(
                "Bạn có chắc muốn xóa dự án " + item.projectName + "?\nDữ liệu nhân sự tham gia, chi phí, đối tượng dự án cũng sẽ bị xóa.",
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
            deleteProject(item.projectKey);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteProject(String projectKey) {
        Cursor c = db.rawQuery(
                "SELECT MADA, MAHD FROM DUANTHEOHOPDONG WHERE MADAHD = ?",
                new String[]{projectKey}
        );

        if (!c.moveToFirst()) {
            c.close();
            Toast.makeText(this, "Không tìm thấy dự án.", Toast.LENGTH_SHORT).show();
            return;
        }

        String maDa = c.getString(0);
        String maHd = c.getString(1);
        c.close();

        db.beginTransaction();

        try {
            db.delete("NVTHAMGIADA", "MADAHD = ?", new String[]{projectKey});
            db.delete("DTDUAN", "MADA = ?", new String[]{maDa});
            db.delete("DUANTHEOHOPDONG", "MADAHD = ?", new String[]{projectKey});
            db.delete("DUAN", "MADA = ?", new String[]{maDa});

            Cursor cOther = db.rawQuery(
                    "SELECT 1 FROM DUANTHEOHOPDONG WHERE MAHD = ?",
                    new String[]{maHd}
            );

            boolean hasOther = cOther.moveToFirst();
            cOther.close();

            if (!hasOther) {
                db.delete("HOPDONG", "MAHD = ?", new String[]{maHd});
            }

            db.setTransactionSuccessful();
            Toast.makeText(this, "Đã xóa dự án.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Không thể xóa dự án: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

        loadProjects();
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

    private EditText makeNumberInput(String hint) {
        EditText edt = makeDarkInput(hint);
        edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
            super(ProjectActivity.this, android.R.layout.simple_spinner_item, data);
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
            TextView tv = new TextView(ProjectActivity.this);
            tv.setText(item == null ? "" : item.toString());
            tv.setTextSize(15);
            tv.setSingleLine(false);
            tv.setTextColor(TEXT);
            return tv;
        }
    }

    private void pickDate(EditText target) {
        Calendar cal = Calendar.getInstance();

        Date current = parseDate(target.getText().toString().trim());
        if (current != null) {
            cal.setTime(current);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    target.setText(dbDateFormat.format(picked.getTime()));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private String getStatusCode(String startStr, String endStr) {
        Date today = clearTime(new Date());
        Date start = parseDate(startStr);
        Date end = parseDate(endStr);

        if (start != null && today.before(start)) {
            return "upcoming";
        }

        if (end != null && today.after(end)) {
            return "done";
        }

        if (start != null && end != null && !today.before(start) && !today.after(end)) {
            return "ongoing";
        }

        return "ontrack";
    }

    private String getStatusLabel(String code) {
        switch ((code == null ? "" : code).toLowerCase()) {
            case "upcoming":
                return "Sắp triển khai";
            case "ongoing":
                return "Đang thực hiện";
            case "done":
                return "Hoàn thành";
            case "overdue":
                return "Quá hạn";
            default:
                return "Đúng tiến độ";
        }
    }

    private int getStatusColor(String code) {
        switch ((code == null ? "" : code).toLowerCase()) {
            case "upcoming":
                return GREEN;
            case "ongoing":
                return BLUE;
            case "done":
                return PURPLE;
            case "overdue":
                return ORANGE;
            default:
                return PRIMARY;
        }
    }

    private String getTypeName(String typeCode) {
        switch ((typeCode == null ? "" : typeCode).trim()) {
            case "1":
                return "Loại 1";
            case "2":
                return "Loại 2";
            case "3":
                return "Loại 3";
            case "4":
                return "Loại 4";
            default:
                return "Khác";
        }
    }

    private String buildProjectCode(String typeCode, String contractCode) {
        typeCode = typeCode == null || typeCode.trim().isEmpty() ? "0" : typeCode.trim();
        if (typeCode.length() > 1) {
            typeCode = typeCode.substring(0, 1);
        }

        return typeCode + contractCode;
    }

    private String generateContractCode() {
        String code;

        do {
            code = randomLetters(3) + randomDigits(2);
        } while (contractCodeExists(code));

        return code;
    }

    private boolean contractCodeExists(String code) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM HOPDONG WHERE MAHD = ?",
                new String[]{code}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private String randomLetters(int length) {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }

        return sb.toString();
    }

    private String randomDigits(int length) {
        String digits = "0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(digits.charAt(random.nextInt(digits.length())));
        }

        return sb.toString();
    }

    private void putNullableString(ContentValues values, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            values.putNull(key);
        } else {
            values.put(key, value.trim());
        }
    }

    private void putNullableDouble(ContentValues values, String key, Double value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    private Double parseDoubleOrNull(String text) {
        try {
            text = text == null ? "" : text.trim();
            if (text.isEmpty()) return null;
            return Double.parseDouble(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseDate(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return null;
            return dbDateFormat.parse(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Date clearTime(Date date) {
        try {
            return dbDateFormat.parse(dbDateFormat.format(date));
        } catch (Exception e) {
            return date;
        }
    }

    private String formatDate(String text) {
        try {
            Date d = parseDate(text);
            if (d == null) return "?";
            return viewDateFormat.format(d);
        } catch (Exception e) {
            return "?";
        }
    }

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    private LinearLayout makeDialogActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.RIGHT);
        actions.setPadding(0, dp(14), 0, 0);
        return actions;
    }

    private TextView makeDialogLabel(String text) {
        TextView tv = makeText(text, 13, SUB, true);
        tv.setPadding(0, dp(8), 0, dp(4));
        return tv;
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

    private Button makeSmallButton(String text, int bgColor, int textColor, View.OnClickListener listener) {
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

    private String formatMoney(double number) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(number);
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

    static class ProjectItem {
        String projectKey;
        String projectCode;
        String contractCode;
        String projectName;
        String typeCode;
        String customerName;
        String areaName;
        String startDate;
        String endDate;
        int employeeCount;
        String statusCode;
        String statusLabel;
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
}