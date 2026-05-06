package com.neith.subjectdemo.hr;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Patterns;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class EmployeeProfileActivity extends AppCompatActivity {

    SQLiteDatabase db;
    LinearLayout layoutProfileHeader, layoutProfileBody;

    String maNV = "";

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");

    LinearLayout currentDialogRoot;

    ArrayList<OptionItem> departments = new ArrayList<>();
    ArrayList<OptionItem> positions = new ArrayList<>();
    ArrayList<OptionItem> businesses = new ArrayList<>();
    ArrayList<String> workDates = new ArrayList<>();

    EmployeeProfile profile = new EmployeeProfile();

    ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_profile);

        db = DB.openDatabase(this);

        layoutProfileHeader = findViewById(R.id.layoutProfileHeader);
        layoutProfileBody = findViewById(R.id.layoutProfileBody);

        maNV = getIntent().getStringExtra("MANV");

        if (maNV == null || maNV.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã nhân viên.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupImagePicker();
        loadOptions();
        loadProfile();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }

                    Uri uri = result.getData().getData();

                    if (uri == null) {
                        return;
                    }

                    try {
                        String imagePath = copyImageToEmployeeFolder(uri);

                        ContentValues values = new ContentValues();
                        values.put("ANH", imagePath);

                        int rows = db.update(
                                "NHANVIEN",
                                values,
                                "MANV = ?",
                                new String[]{profile.maNV}
                        );

                        if (rows > 0) {
                            toast("Đã cập nhật ảnh nhân viên.");
                            loadProfile();
                        } else {
                            toast("Cập nhật ảnh thất bại.");
                        }

                    } catch (Exception e) {
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private String copyImageToEmployeeFolder(Uri uri) throws Exception {
        String originalName = getFileName(uri);
        String ext = getExtension(originalName);

        if (ext.isEmpty()) {
            ext = ".jpg";
        }

        String safeName = removeDiacritics(profile.getFullName())
                .toLowerCase(Locale.getDefault())
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9]", "");

        if (safeName.isEmpty()) {
            safeName = profile.maNV.toLowerCase(Locale.getDefault());
        }

        String fileName = safeName + "_" + profile.maNV + "_" + System.currentTimeMillis() + ext;
        File folder = getExternalFilesDir("AnhNhanVien");

        if (folder == null) {
            throw new Exception("Không mở được thư mục lưu ảnh.");
        }

        if (!folder.exists()) {
            boolean created = folder.mkdirs();

            if (!created) {
                throw new Exception("Không tạo được thư mục AnhNhanVien.");
            }
        }

        File outFile = new File(folder, fileName);

        InputStream inputStream = getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            throw new Exception("Không đọc được ảnh.");
        }

        FileOutputStream outputStream = new FileOutputStream(outFile);

        byte[] buffer = new byte[8192];
        int len;

        while ((len = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
        }

        outputStream.flush();
        outputStream.close();
        inputStream.close();

        return "AnhNhanVien/" + fileName;
    }

    private String getFileName(Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (result == null) {
            result = uri.getLastPathSegment();
        }

        return result == null ? "image.jpg" : result;
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }

        int dot = fileName.lastIndexOf(".");

        if (dot < 0) {
            return "";
        }

        return fileName.substring(dot).toLowerCase(Locale.getDefault());
    }

    private void loadOptions() {
        departments.clear();
        positions.clear();
        businesses.clear();

        departments.add(new OptionItem("", "(Chưa gán phòng ban)"));
        positions.add(new OptionItem("", "(Chưa gán chức vụ)"));
        businesses.add(new OptionItem("", "(Chưa gán doanh nghiệp)"));

        Cursor cDept = db.rawQuery("SELECT MAPB, TENPB FROM PHONGBAN ORDER BY TENPB", null);

        while (cDept.moveToNext()) {
            departments.add(new OptionItem(cDept.getString(0), cDept.getString(1)));
        }

        cDept.close();

        Cursor cPos = db.rawQuery("SELECT MACV, TENCV FROM VITRICONGVIEC ORDER BY TENCV", null);

        while (cPos.moveToNext()) {
            positions.add(new OptionItem(cPos.getString(0), cPos.getString(1)));
        }

        cPos.close();

        Cursor cBus = db.rawQuery("SELECT MADN, TENDN FROM THONGTINDOANHNGHIEP ORDER BY TENDN", null);

        while (cBus.moveToNext()) {
            businesses.add(new OptionItem(cBus.getString(0), cBus.getString(1)));
        }

        cBus.close();
    }

    private void loadProfile() {
        layoutProfileHeader.removeAllViews();
        layoutProfileBody.removeAllViews();

        Cursor c = db.rawQuery(
                "SELECT NV.MANV, IFNULL(NV.HOLOT, ''), IFNULL(NV.TENNV, ''), " +
                        "IFNULL(NV.NAMSINH, 0), IFNULL(NV.GIOITINH, 1), " +
                        "IFNULL(NV.MAPB, ''), IFNULL(PB.TENPB, '(Chưa gán)'), " +
                        "IFNULL(NV.MACV, ''), IFNULL(CV.TENCV, '(Chưa gán)'), " +
                        "IFNULL(NV.ANH, ''), IFNULL(NV.HDLD, ''), IFNULL(NV.MADN, ''), " +
                        "IFNULL(DN.TENDN, '(Chưa gán)'), " +
                        "IFNULL(L.LOAINV, ''), IFNULL(L.LUONGCOBAN, 0) " +
                        "FROM NHANVIEN NV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "LEFT JOIN THONGTINDOANHNGHIEP DN ON NV.MADN = DN.MADN " +
                        "LEFT JOIN LUONG L ON NV.MANV = L.MANV " +
                        "WHERE NV.MANV = ?",
                new String[]{maNV}
        );

        if (!c.moveToFirst()) {
            c.close();
            Toast.makeText(this, "Không tìm thấy nhân viên.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        profile.maNV = c.getString(0);
        profile.hoLot = c.getString(1);
        profile.tenNV = c.getString(2);
        profile.namSinh = c.getInt(3);
        profile.gioiTinh = c.getInt(4) == 1;
        profile.maPB = c.getString(5);
        profile.tenPB = c.getString(6);
        profile.maCV = c.getString(7);
        profile.tenCV = c.getString(8);
        profile.anh = c.getString(9);
        profile.hdld = c.getString(10);
        profile.maDN = c.getString(11);
        profile.tenDN = c.getString(12);
        profile.loaiNV = c.getString(13);
        profile.luongCoBan = c.getDouble(14);

        c.close();

        loadContact();
        loadHealth();
        loadInsurance();
        loadWorkDates();

        buildHeader();
        buildBody();
    }

    private void loadContact() {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(SODT, ''), IFNULL(DIACHI, ''), IFNULL(FB, ''), IFNULL(X, ''), IFNULL(QUEQUAN, '') " +
                        "FROM THONGTINLIENHE WHERE MANV = ?",
                new String[]{maNV}
        );

        profile.sdt = "";
        profile.diaChi = "";
        profile.fb = "";
        profile.email = "";
        profile.queQuan = "";

        if (c.moveToFirst()) {
            profile.sdt = c.getString(0);
            profile.diaChi = c.getString(1);
            profile.fb = c.getString(2);
            profile.email = c.getString(3);
            profile.queQuan = c.getString(4);
        }

        c.close();
    }

    private void loadHealth() {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(CHIEUCAO, 0), IFNULL(CANNANG, 0), IFNULL(TIENSUBENH, ''), IFNULL(THILUCTREN10, 0), IFNULL(NGAYCAPNHAT, '') " +
                        "FROM THONGTINSUCKHOE WHERE MANV = ?",
                new String[]{maNV}
        );

        profile.chieuCao = 0;
        profile.canNang = 0;
        profile.tienSuBenh = "";
        profile.thiLuc = 0;
        profile.ngayCapNhatSucKhoe = "";

        if (c.moveToFirst()) {
            profile.chieuCao = c.getInt(0);
            profile.canNang = c.getInt(1);
            profile.tienSuBenh = c.getString(2);
            profile.thiLuc = c.getInt(3);
            profile.ngayCapNhatSucKhoe = c.getString(4);
        }

        c.close();
    }

    private void loadInsurance() {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(LOAIBAOHIEM, ''), IFNULL(SOBAOHIEM, ''), IFNULL(THOIHAN, '') " +
                        "FROM THONGTINBAOHIEM WHERE MANV = ?",
                new String[]{maNV}
        );

        profile.loaiBaoHiem = "";
        profile.soBaoHiem = "";
        profile.thoiHanBaoHiem = "";

        if (c.moveToFirst()) {
            profile.loaiBaoHiem = c.getString(0);
            profile.soBaoHiem = c.getString(1);
            profile.thoiHanBaoHiem = c.getString(2);
        }

        c.close();
    }

    private void loadWorkDates() {
        workDates.clear();

        Cursor c = db.rawQuery(
                "SELECT NGAYLAMVIEC FROM LICHLAMVIEC WHERE MANV = ? ORDER BY NGAYLAMVIEC DESC",
                new String[]{profile.maNV}
        );

        while (c.moveToNext()) {
            workDates.add(c.getString(0));
        }

        c.close();
    }

    private void buildHeader() {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(10));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        titleBox.addView(makeText("Hồ sơ nhân viên", 26, PRIMARY, true));
        titleBox.addView(makeText("Xem và chỉnh sửa toàn bộ thông tin nhân viên", 13, SUB, false));

        Button btnBack = makeHeaderButton("Quay lại", R.drawable.hr_logout_bg, Color.BLACK);
        btnBack.setOnClickListener(v -> finish());

        topBar.addView(titleBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        topBar.addView(btnBack, new LinearLayout.LayoutParams(dp(96), dp(44)));

        layoutProfileHeader.addView(topBar);

        LinearLayout headerCard = makeCard();

        LinearLayout avatarRow = new LinearLayout(this);
        avatarRow.setOrientation(LinearLayout.HORIZONTAL);
        avatarRow.setGravity(Gravity.CENTER_VERTICAL);

        View photo = makeEmployeePhoto(profile.anh, profile.getFullName());
        avatarRow.addView(photo);

        LinearLayout nameBox = new LinearLayout(this);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        nameBox.setPadding(dp(14), 0, 0, 0);

        nameBox.addView(makeText(profile.getFullName(), 22, PRIMARY, true));

        TextView idLine = makeText(
                profile.maNV + " • " + (profile.gioiTinh ? "Nam" : "Nữ") + " • " + (profile.namSinh == 0 ? "—" : profile.namSinh),
                13,
                SUB,
                false
        );
        idLine.setPadding(0, dp(3), 0, 0);
        nameBox.addView(idLine);

        avatarRow.addView(nameBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        headerCard.addView(avatarRow);

        LinearLayout tagRow = new LinearLayout(this);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setPadding(0, dp(14), 0, 0);

        tagRow.addView(makeTag(profile.tenPB));
        tagRow.addView(makeTag(profile.tenCV));

        if (!isEmpty(profile.loaiNV)) {
            tagRow.addView(makeTag(profile.loaiNV));
        }

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(tagRow);

        headerCard.addView(hsv);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(16), 0, 0);

        actions.addView(makeSmallButton("Đổi ảnh", ORANGE, Color.WHITE, v -> openImagePicker()));
        actions.addView(makeSmallButton("Copy mã", PRIMARY, Color.BLACK, v -> copyText(profile.maNV)));
        actions.addView(makeSmallButton("Làm mới", BLUE, Color.WHITE, v -> loadProfile()));

        HorizontalScrollView actionScroll = new HorizontalScrollView(this);
        actionScroll.setHorizontalScrollBarEnabled(false);
        actionScroll.addView(actions);

        headerCard.addView(actionScroll);

        layoutProfileHeader.addView(headerCard);
    }

    private View makeEmployeePhoto(String path, String fullName) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundResource(R.drawable.employee_avatar_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(76), dp(76));
        image.setLayoutParams(lp);

        Uri uri = getEmployeeImageUri(path);

        if (uri != null) {
            image.setImageURI(uri);
            return image;
        }

        TextView avatar = makeText(getInitial(fullName), 24, Color.BLACK, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.employee_avatar_bg);
        avatar.setLayoutParams(lp);

        return avatar;
    }

    private Uri getEmployeeImageUri(String path) {
        if (isEmpty(path)) {
            return null;
        }

        String fileName = path;

        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }

        File folder = getExternalFilesDir("AnhNhanVien");

        if (folder == null) {
            return null;
        }

        File file = new File(folder, fileName);

        if (file.exists()) {
            return Uri.fromFile(file);
        }

        return null;
    }

    private void buildBody() {
        addPersonalCard();
        addContactCard();
        addHealthCard();
        addInsuranceCard();
        addSalaryCard();
        addAttendanceSummaryCard();
        addRecentAttendanceCard();
        addRewardPenaltyCard();
        addRelativeCard();
    }

    private void addPersonalCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("THÔNG TIN CÁ NHÂN", "Sửa", v -> showEditPersonalDialog()));

        card.addView(makeInfoRow("Mã nhân viên", profile.maNV));
        card.addView(makeInfoRow("Họ lót", profile.hoLot));
        card.addView(makeInfoRow("Tên", profile.tenNV));
        card.addView(makeInfoRow("Họ và tên", profile.getFullName()));
        card.addView(makeInfoRow("Giới tính", profile.gioiTinh ? "Nam" : "Nữ"));
        card.addView(makeInfoRow("Năm sinh", profile.namSinh == 0 ? "—" : String.valueOf(profile.namSinh)));
        card.addView(makeInfoRow("Mã phòng ban", emptyText(profile.maPB)));
        card.addView(makeInfoRow("Phòng ban", emptyText(profile.tenPB)));
        card.addView(makeInfoRow("Mã chức vụ", emptyText(profile.maCV)));
        card.addView(makeInfoRow("Chức vụ", emptyText(profile.tenCV)));
        card.addView(makeInfoRow("Doanh nghiệp", emptyText(profile.tenDN)));
        card.addView(makeInfoRow("Ảnh", emptyText(profile.anh)));
        card.addView(makeInfoRow("HĐLĐ", emptyText(profile.hdld)));

        layoutProfileBody.addView(card);
    }

    private void addContactCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("THÔNG TIN LIÊN HỆ", "Sửa", v -> showEditContactDialog()));

        card.addView(makeInfoRow("Số điện thoại", emptyText(profile.sdt)));
        card.addView(makeInfoRow("Địa chỉ", emptyText(profile.diaChi)));
        card.addView(makeInfoRow("Facebook", emptyText(profile.fb)));
        card.addView(makeInfoRow("Email / X", emptyText(profile.email)));
        card.addView(makeInfoRow("Quê quán", emptyText(profile.queQuan)));

        layoutProfileBody.addView(card);
    }

    private void addHealthCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("THÔNG TIN SỨC KHỎE", "Sửa", v -> showEditHealthDialog()));

        card.addView(makeInfoRow("Chiều cao", profile.chieuCao == 0 ? "—" : profile.chieuCao + " cm"));
        card.addView(makeInfoRow("Cân nặng", profile.canNang == 0 ? "—" : profile.canNang + " kg"));
        card.addView(makeInfoRow("Tiền sử bệnh", emptyText(profile.tienSuBenh)));
        card.addView(makeInfoRow("Thị lực / 10", profile.thiLuc == 0 ? "—" : String.valueOf(profile.thiLuc)));
        card.addView(makeInfoRow("Ngày cập nhật", emptyText(profile.ngayCapNhatSucKhoe)));

        layoutProfileBody.addView(card);
    }

    private void addInsuranceCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("THÔNG TIN BẢO HIỂM", "Sửa", v -> showEditInsuranceDialog()));

        card.addView(makeInfoRow("Loại bảo hiểm", emptyText(profile.loaiBaoHiem)));
        card.addView(makeInfoRow("Số bảo hiểm", emptyText(profile.soBaoHiem)));
        card.addView(makeInfoRow("Thời hạn", emptyText(profile.thoiHanBaoHiem)));

        layoutProfileBody.addView(card);
    }

    private void addSalaryCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("LƯƠNG & TỔNG HỢP", "Sửa", v -> showEditSalaryDialog()));

        double rewardMonth = getRewardPenaltyMonth(profile.maNV, "KT");
        double penaltyMonth = getRewardPenaltyMonth(profile.maNV, "KL");
        double rewardAll = getRewardPenaltyAll(profile.maNV, "KT");
        double penaltyAll = getRewardPenaltyAll(profile.maNV, "KL");

        card.addView(makeBigValue("Lương cơ bản", formatNumber(profile.luongCoBan) + " VND", PRIMARY));
        card.addView(makeBigValue("Thưởng tháng này", formatNumber(rewardMonth) + " VND", GREEN));
        card.addView(makeBigValue("Phạt tháng này", formatNumber(penaltyMonth) + " VND", RED));
        card.addView(makeBigValue("Tổng thưởng toàn bộ", formatNumber(rewardAll) + " VND", GREEN));
        card.addView(makeBigValue("Tổng phạt toàn bộ", formatNumber(penaltyAll) + " VND", RED));

        layoutProfileBody.addView(card);
    }

    private void addAttendanceSummaryCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("CHẤM CÔNG THÁNG NÀY", "Thêm", v -> showEditAttendanceDialog("", "", "", "", "", "", true)));

        MonthSummary s = getMonthSummary(profile.maNV);

        card.addView(makeBigValue("Số ngày đã chấm công", formatNumber(s.days) + " ngày", PRIMARY));
        card.addView(makeBigValue("Giờ công", formatNumber(s.gioCong) + " giờ", BLUE));
        card.addView(makeBigValue("Tăng ca", formatNumber(s.tangCa) + " giờ", ORANGE));
        card.addView(makeBigValue("Tổng ca", formatNumber(s.tongCa), GREEN));

        layoutProfileBody.addView(card);
    }

    private void addRecentAttendanceCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("CHẤM CÔNG GẦN ĐÂY", "Thêm", v -> showEditAttendanceDialog("", "", "", "", "", "", true)));

        Cursor c = db.rawQuery(
                "SELECT NGAYCC, IFNULL(GIOVAO, ''), IFNULL(GIORA, ''), IFNULL(GIOCONG, 0), IFNULL(TANGCA, 0), IFNULL(TONGCA, 0) " +
                        "FROM CHAMCONG WHERE MANV = ? ORDER BY NGAYCC DESC LIMIT 12",
                new String[]{profile.maNV}
        );

        if (c.getCount() == 0) {
            card.addView(makeEmptyText("Chưa có dữ liệu chấm công."));
        } else {
            while (c.moveToNext()) {
                String ngay = c.getString(0);
                String gioVao = c.getString(1);
                String gioRa = c.getString(2);
                String gioCong = formatHour(c.getDouble(3));
                String tangCa = formatHour(c.getDouble(4));
                String tongCa = formatHour(c.getDouble(5));

                card.addView(makeEditableItem(
                        ngay,
                        "Vào: " + emptyText(gioVao) + " • Ra: " + emptyText(gioRa)
                                + "\nGiờ công: " + gioCong + " • Tăng ca: " + tangCa + " • Tổng ca: " + tongCa,
                        BLUE,
                        v -> showEditAttendanceDialog(ngay, gioVao, gioRa, gioCong, tangCa, tongCa, false),
                        v -> deleteAttendance(ngay)
                ));
            }
        }

        c.close();
        layoutProfileBody.addView(card);
    }

    private void addRewardPenaltyCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("THƯỞNG / PHẠT", "Thêm", v -> showEditRewardPenaltyDialog("", "KT", "", true)));

        Cursor c = db.rawQuery(
                "SELECT NGAY, IFNULL(HINHTHUC, ''), IFNULL(SOTIEN, 0) " +
                        "FROM DSTHUONGPHAT WHERE MANV = ? ORDER BY NGAY DESC LIMIT 12",
                new String[]{profile.maNV}
        );

        if (c.getCount() == 0) {
            card.addView(makeEmptyText("Chưa có dữ liệu thưởng/phạt."));
        } else {
            while (c.moveToNext()) {
                String ngay = c.getString(0);
                String type = c.getString(1);
                String sotien = String.valueOf(c.getDouble(2));

                boolean reward = "KT".equalsIgnoreCase(type);
                int color = reward ? GREEN : RED;

                card.addView(makeEditableItem(
                        reward ? "Thưởng" : "Phạt",
                        "Ngày: " + emptyText(ngay) + "\nSố tiền: " + formatNumber(parseDoubleSafe(sotien)) + " VND",
                        color,
                        v -> showEditRewardPenaltyDialog(ngay, type, sotien, false),
                        v -> deleteRewardPenalty(ngay)
                ));
            }
        }

        c.close();
        layoutProfileBody.addView(card);
    }

    private void addRelativeCard() {
        LinearLayout card = makeCard();
        card.addView(makeTitleRow("NHÂN THÂN", "Thêm", v -> showEditRelativeDialog("", "", "", "", "", true)));

        Cursor c = db.rawQuery(
                "SELECT IFNULL(TENNT, ''), IFNULL(DIENTHOAI, ''), IFNULL(DIACHI, ''), IFNULL(QUANHE, '') " +
                        "FROM NHANTHAN WHERE MANV = ? ORDER BY TENNT",
                new String[]{profile.maNV}
        );

        if (c.getCount() == 0) {
            card.addView(makeEmptyText("Chưa có dữ liệu nhân thân."));
        } else {
            while (c.moveToNext()) {
                String ten = c.getString(0);
                String phone = c.getString(1);
                String address = c.getString(2);
                String relation = c.getString(3);

                card.addView(makeEditableItem(
                        emptyText(ten),
                        "Quan hệ: " + emptyText(relation) + "\nSĐT: " + emptyText(phone) + "\nĐịa chỉ: " + emptyText(address),
                        PRIMARY,
                        v -> showEditRelativeDialog(ten, phone, address, relation, ten, false),
                        v -> deleteRelative(ten)
                ));
            }
        }

        c.close();
        layoutProfileBody.addView(card);
    }

    private void showEditPersonalDialog() {
        Dialog dialog = createStyledDialog("Sửa thông tin cá nhân");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtHoLot = makeDarkInput("Họ lót");
        edtHoLot.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtHoLot.setText(profile.hoLot);

        EditText edtTen = makeDarkInput("Tên");
        edtTen.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtTen.setText(profile.tenNV);

        EditText edtNamSinh = makeDarkInput("Chọn năm sinh");
        setupDateInput(edtNamSinh);
        edtNamSinh.setText(profile.namSinh == 0 ? "" : String.valueOf(profile.namSinh));
        edtNamSinh.setOnClickListener(v -> showBirthYearPicker(edtNamSinh));

        Spinner spnGender = makeDarkSpinner();
        ArrayList<String> genders = new ArrayList<>();
        genders.add("Nam");
        genders.add("Nữ");
        spnGender.setAdapter(new WhiteSpinnerAdapter<>(genders));
        spnGender.setSelection(profile.gioiTinh ? 0 : 1);

        Spinner spnDept = makeDarkSpinner();
        spnDept.setAdapter(new WhiteSpinnerAdapter<>(departments));
        spnDept.setSelection(findOptionIndex(departments, profile.maPB));

        Spinner spnPos = makeDarkSpinner();
        spnPos.setAdapter(new WhiteSpinnerAdapter<>(positions));
        spnPos.setSelection(findOptionIndex(positions, profile.maCV));

        Spinner spnBusiness = makeDarkSpinner();
        spnBusiness.setAdapter(new WhiteSpinnerAdapter<>(businesses));
        spnBusiness.setSelection(findOptionIndex(businesses, profile.maDN));

        EditText edtHDLD = makeDarkInput("HĐLĐ");
        edtHDLD.setText(profile.hdld);

        content.addView(makeDialogLabel("Họ lót"));
        content.addView(edtHoLot);
        content.addView(makeDialogLabel("Tên"));
        content.addView(edtTen);
        content.addView(makeDialogLabel("Giới tính"));
        content.addView(spnGender);
        content.addView(makeDialogLabel("Năm sinh"));
        content.addView(edtNamSinh);
        content.addView(makeDialogLabel("Phòng ban"));
        content.addView(spnDept);
        content.addView(makeDialogLabel("Chức vụ"));
        content.addView(spnPos);
        content.addView(makeDialogLabel("Doanh nghiệp"));
        content.addView(spnBusiness);
        content.addView(makeDialogLabel("HĐLĐ"));
        content.addView(edtHDLD);

        addDialogActions(content, dialog, () -> {
            String hoLot = edtHoLot.getText().toString().trim();
            String ten = edtTen.getText().toString().trim();
            String namSinhStr = edtNamSinh.getText().toString().trim();

            if (!isValidVietnameseName(hoLot) || !isValidVietnameseName(ten)) {
                toast("Họ tên chỉ được chứa chữ cái tiếng Việt và khoảng trắng.");
                return false;
            }

            int namSinh = parseIntSafe(namSinhStr);

            if (!isValidBirthYear(namSinh)) {
                toast("Năm sinh phải từ 1950 đến " + getMaxBirthYear() + ".");
                return false;
            }

            OptionItem dept = (OptionItem) spnDept.getSelectedItem();
            OptionItem pos = (OptionItem) spnPos.getSelectedItem();
            OptionItem bus = (OptionItem) spnBusiness.getSelectedItem();

            ContentValues values = new ContentValues();
            values.put("HOLOT", hoLot);
            values.put("TENNV", ten);
            values.put("GIOITINH", spnGender.getSelectedItemPosition() == 0 ? 1 : 0);
            values.put("NAMSINH", namSinh);
            values.put("MAPB", dept.id);
            values.put("MACV", pos.id);
            values.put("MADN", bus.id);
            values.put("HDLD", edtHDLD.getText().toString().trim());

            int rows = db.update("NHANVIEN", values, "MANV = ?", new String[]{profile.maNV});

            if (rows > 0) {
                toast("Đã cập nhật thông tin cá nhân.");
                loadOptions();
                loadProfile();
                return true;
            }

            toast("Cập nhật thất bại.");
            return false;
        });

        currentDialogRoot.addView(content);
        dialog.show();
    }

    private void showEditContactDialog() {
        Dialog dialog = createStyledDialog("Sửa thông tin liên hệ");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtPhone = makeDarkInput("Số điện thoại");
        edtPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        edtPhone.setText(profile.sdt);

        EditText edtAddress = makeDarkInput("Địa chỉ");
        edtAddress.setText(profile.diaChi);

        EditText edtFb = makeDarkInput("Facebook");
        edtFb.setText(profile.fb);

        EditText edtEmail = makeDarkInput("Email / X");
        edtEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edtEmail.setText(profile.email);

        EditText edtQueQuan = makeDarkInput("Quê quán");
        edtQueQuan.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtQueQuan.setText(profile.queQuan);

        content.addView(makeDialogLabel("Số điện thoại"));
        content.addView(edtPhone);
        content.addView(makeDialogLabel("Địa chỉ"));
        content.addView(edtAddress);
        content.addView(makeDialogLabel("Facebook"));
        content.addView(edtFb);
        content.addView(makeDialogLabel("Email / X"));
        content.addView(edtEmail);
        content.addView(makeDialogLabel("Quê quán"));
        content.addView(edtQueQuan);

        addDialogActions(content, dialog, () -> {
            String email = edtEmail.getText().toString().trim();

            if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("Email không hợp lệ.");
                return false;
            }

            ContentValues values = new ContentValues();
            values.put("MANV", profile.maNV);
            values.put("SODT", edtPhone.getText().toString().trim());
            values.put("DIACHI", edtAddress.getText().toString().trim());
            values.put("FB", edtFb.getText().toString().trim());
            values.put("X", email);
            values.put("QUEQUAN", edtQueQuan.getText().toString().trim());

            upsertByManv("THONGTINLIENHE", values);
            toast("Đã cập nhật thông tin liên hệ.");
            loadProfile();
            return true;
        });

        currentDialogRoot.addView(content);
        dialog.show();
    }

    private void showEditHealthDialog() {
        Dialog dialog = createStyledDialog("Sửa thông tin sức khỏe");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtHeight = makeDarkInput("Chiều cao cm");
        edtHeight.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtHeight.setText(profile.chieuCao == 0 ? "" : String.valueOf(profile.chieuCao));

        EditText edtWeight = makeDarkInput("Cân nặng kg");
        edtWeight.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtWeight.setText(profile.canNang == 0 ? "" : String.valueOf(profile.canNang));

        EditText edtHistory = makeDarkInput("Tiền sử bệnh");
        edtHistory.setMinLines(3);
        edtHistory.setGravity(Gravity.TOP);
        edtHistory.setText(profile.tienSuBenh);

        EditText edtVision = makeDarkInput("Thị lực / 10");
        edtVision.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtVision.setText(profile.thiLuc == 0 ? "" : String.valueOf(profile.thiLuc));

        EditText edtDate = makeDarkInput("Chọn ngày cập nhật");
        setupDateInput(edtDate);
        edtDate.setText(isEmpty(profile.ngayCapNhatSucKhoe) ? today() : profile.ngayCapNhatSucKhoe);
        edtDate.setOnClickListener(v -> showNormalDatePicker(edtDate));

        content.addView(makeDialogLabel("Chiều cao"));
        content.addView(edtHeight);
        content.addView(makeDialogLabel("Cân nặng"));
        content.addView(edtWeight);
        content.addView(makeDialogLabel("Tiền sử bệnh"));
        content.addView(edtHistory);
        content.addView(makeDialogLabel("Thị lực / 10"));
        content.addView(edtVision);
        content.addView(makeDialogLabel("Ngày cập nhật"));
        content.addView(edtDate);

        addDialogActions(content, dialog, () -> {
            int height = parseIntSafe(edtHeight.getText().toString().trim());
            int weight = parseIntSafe(edtWeight.getText().toString().trim());
            int vision = parseIntSafe(edtVision.getText().toString().trim());

            if (height < 0 || weight < 0 || vision < 0 || vision > 10) {
                toast("Thông tin sức khỏe không hợp lệ.");
                return false;
            }

            ContentValues values = new ContentValues();
            values.put("MANV", profile.maNV);
            values.put("CHIEUCAO", height);
            values.put("CANNANG", weight);
            values.put("TIENSUBENH", edtHistory.getText().toString().trim());
            values.put("THILUCTREN10", vision);
            values.put("NGAYCAPNHAT", edtDate.getText().toString().trim());

            upsertByManv("THONGTINSUCKHOE", values);
            toast("Đã cập nhật thông tin sức khỏe.");
            loadProfile();
            return true;
        });

        currentDialogRoot.addView(content);
        dialog.show();
    }

    private void showEditInsuranceDialog() {
        Dialog dialog = createStyledDialog("Sửa thông tin bảo hiểm");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtType = makeDarkInput("Loại bảo hiểm");
        edtType.setText(profile.loaiBaoHiem);

        EditText edtNo = makeDarkInput("Số bảo hiểm");
        edtNo.setText(profile.soBaoHiem);

        EditText edtExpire = makeDarkInput("Chọn thời hạn");
        setupDateInput(edtExpire);
        edtExpire.setText(profile.thoiHanBaoHiem);
        edtExpire.setOnClickListener(v -> showFutureDatePicker(edtExpire));

        content.addView(makeDialogLabel("Loại bảo hiểm"));
        content.addView(edtType);
        content.addView(makeDialogLabel("Số bảo hiểm"));
        content.addView(edtNo);
        content.addView(makeDialogLabel("Thời hạn"));
        content.addView(edtExpire);

        addDialogActions(content, dialog, () -> {
            ContentValues values = new ContentValues();
            values.put("MANV", profile.maNV);
            values.put("LOAIBAOHIEM", edtType.getText().toString().trim());
            values.put("SOBAOHIEM", edtNo.getText().toString().trim());
            values.put("THOIHAN", edtExpire.getText().toString().trim());

            upsertByManv("THONGTINBAOHIEM", values);
            toast("Đã cập nhật thông tin bảo hiểm.");
            loadProfile();
            return true;
        });

        currentDialogRoot.addView(content);
        dialog.show();
    }

    private void showEditSalaryDialog() {
        Dialog dialog = createStyledDialog("Sửa lương");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtType = makeDarkInput("Loại nhân viên");
        edtType.setText(isEmpty(profile.loaiNV) ? "FT" : profile.loaiNV);

        EditText edtSalary = makeDarkInput("Lương cơ bản");
        edtSalary.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edtSalary.setText(String.valueOf((long) profile.luongCoBan));

        content.addView(makeDialogLabel("Loại nhân viên"));
        content.addView(edtType);
        content.addView(makeDialogLabel("Lương cơ bản"));
        content.addView(edtSalary);

        addDialogActions(content, dialog, () -> {
            double salary = parseDoubleSafe(edtSalary.getText().toString().trim());

            if (salary < 0) {
                toast("Lương không được âm.");
                return false;
            }

            ContentValues values = new ContentValues();
            values.put("MANV", profile.maNV);
            values.put("LOAINV", edtType.getText().toString().trim());
            values.put("LUONGCOBAN", salary);

            upsertByManv("LUONG", values);
            toast("Đã cập nhật lương.");
            loadProfile();
            return true;
        });

        currentDialogRoot.addView(content);
        dialog.show();
    }

    private void showEditAttendanceDialog(
            String ngay,
            String gioVao,
            String gioRa,
            String gioCong,
            String tangCa,
            String tongCa,
            boolean isAdd
    ) {
        Dialog dialog = createStyledDialog(isAdd ? "Thêm chấm công" : "Sửa chấm công");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        Spinner spnDate = makeDarkSpinner();

        ArrayList<String> dates = new ArrayList<>();

        if (!isAdd && !isEmpty(ngay)) {
            dates.add(ngay);
        }

        for (String d : workDates) {
            if (!dates.contains(d)) {
                dates.add(d);
            }
        }

        if (dates.isEmpty()) {
            dates.add("(Chưa có lịch làm việc)");
        }

        spnDate.setAdapter(new WhiteSpinnerAdapter<>(dates));

        if (!isEmpty(ngay)) {
            int index = dates.indexOf(ngay);

            if (index >= 0) {
                spnDate.setSelection(index);
            }
        }

        EditText edtIn = makeDarkInput("Chọn giờ vào");
        setupDateInput(edtIn);
        edtIn.setText(gioVao);

        EditText edtOut = makeDarkInput("Chọn giờ ra");
        setupDateInput(edtOut);
        edtOut.setText(gioRa);

        EditText edtWork = makeDarkInput("Giờ công tự tính");
        setupReadOnlyInput(edtWork);
        edtWork.setText(isEmpty(gioCong) ? "" : gioCong);

        EditText edtOver = makeDarkInput("Tăng ca tự tính");
        setupReadOnlyInput(edtOver);
        edtOver.setText(isEmpty(tangCa) ? "" : tangCa);

        EditText edtTotal = makeDarkInput("Tổng ca tự tính");
        setupReadOnlyInput(edtTotal);
        edtTotal.setText(isEmpty(tongCa) ? "" : tongCa);

        edtIn.setOnClickListener(v -> showTimePicker(
                edtIn,
                () -> updateAttendanceAutoFields(edtIn, edtOut, edtWork, edtOver, edtTotal)
        ));

        edtOut.setOnClickListener(v -> showTimePicker(
                edtOut,
                () -> updateAttendanceAutoFields(edtIn, edtOut, edtWork, edtOver, edtTotal)
        ));

        updateAttendanceAutoFields(edtIn, edtOut, edtWork, edtOver, edtTotal);

        content.addView(makeDialogLabel("Ngày chấm công trong lịch làm việc"));
        content.addView(spnDate);
        content.addView(makeDialogLabel("Giờ vào"));
        content.addView(edtIn);
        content.addView(makeDialogLabel("Giờ ra"));
        content.addView(edtOut);
        content.addView(makeDialogLabel("Giờ công"));
        content.addView(edtWork);
        content.addView(makeDialogLabel("Tăng ca"));
        content.addView(edtOver);
        content.addView(makeDialogLabel("Tổng ca"));
        content.addView(edtTotal);

        addDialogActions(content, dialog, () -> {
            String newNgay = spnDate.getSelectedItem().toString();

            if (isEmpty(newNgay) || newNgay.equals("(Chưa có lịch làm việc)")) {
                toast("Nhân viên này chưa có lịch làm việc để chấm công.");
                return false;
            }

            if (!isDateInWorkSchedule(newNgay)) {
                toast("Ngày chấm công phải nằm trong lịch làm việc.");
                return false;
            }

            String in = edtIn.getText().toString().trim();
            String out = edtOut.getText().toString().trim();

            if (in.isEmpty() || out.isEmpty()) {
                toast("Vui lòng chọn giờ vào và giờ ra.");
                return false;
            }

            double duration = calculateWorkingHours(in, out);

            if (duration <= 0) {
                toast("Giờ ra phải lớn hơn giờ vào.");
                return false;
            }

            updateAttendanceAutoFields(edtIn, edtOut, edtWork, edtOver, edtTotal);

            if (!isAdd && !ngay.equals(newNgay)) {
                db.delete("CHAMCONG", "MANV = ? AND NGAYCC = ?", new String[]{profile.maNV, ngay});
            }

            ContentValues values = new ContentValues();
            values.put("MANV", profile.maNV);
            values.put("NGAYCC", newNgay);
            values.put("GIOVAO", in);
            values.put("GIORA", out);
            values.put("GIOCONG", parseDoubleSafe(edtWork.getText().toString().trim()));
            values.put("TANGCA", parseDoubleSafe(edtOver.getText().toString().trim()));
            values.put("TONGCA", parseDoubleSafe(edtTotal.getText().toString().trim()));

            db.insertWithOnConflict("CHAMCONG", null, values, SQLiteDatabase.CONFLICT_REPLACE);

            toast("Đã lưu chấm công.");
            loadProfile();
            return true;
        });

        currentDialogRoot.addView(content);
        dialog.show();
    }

    private void updateAttendanceAutoFields(
            EditText edtIn,
            EditText edtOut,
            EditText edtWork,
            EditText edtOver,
            EditText edtTotal
    ) {
        String in = edtIn.getText().toString().trim();
        String out = edtOut.getText().toString().trim();

        if (in.isEmpty() || out.isEmpty()) {
            return;
        }

        double duration = calculateWorkingHours(in, out);

        if (duration <= 0) {
            edtWork.setText("");
            edtOver.setText("");
            edtTotal.setText("");
            return;
        }

        double work;
        double overtime;

        if (duration <= 8.0) {
            work = duration;
            overtime = 0.0;
        } else {
            work = 8.0;
            overtime = duration - 8.0;
        }

        double total = duration;

        edtWork.setText(formatHour(work));
        edtOver.setText(formatHour(overtime));
        edtTotal.setText(formatHour(total));
    }

    private double calculateWorkingHours(String in, String out) {
        try {
            String[] start = in.split(":");
            String[] end = out.split(":");

            int startMinute = Integer.parseInt(start[0]) * 60 + Integer.parseInt(start[1]);
            int endMinute = Integer.parseInt(end[0]) * 60 + Integer.parseInt(end[1]);

            int diff = endMinute - startMinute;

            if (diff <= 0) {
                return -1;
            }

            return diff / 60.0;

        } catch (Exception e) {
            return -1;
        }
    }

    private String formatHour(double value) {
        if (Math.abs(value - Math.round(value)) < 0.0001) {
            return String.valueOf((long) Math.round(value));
        }

        return String.format(Locale.US, "%.2f", value);
    }

    private void showEditRewardPenaltyDialog(String ngay, String type, String amount, boolean isAdd) {
        Dialog dialog = createStyledDialog(isAdd ? "Thêm thưởng/phạt" : "Sửa thưởng/phạt");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtDate = makeDarkInput("Chọn ngày");
        setupDateInput(edtDate);
        edtDate.setText(isEmpty(ngay) ? nowDateTime() : ngay);
        edtDate.setOnClickListener(v -> showDateTimePicker(edtDate));

        Spinner spnType = makeDarkSpinner();
        ArrayList<String> types = new ArrayList<>();
        types.add("KT - Thưởng");
        types.add("KL - Phạt");
        spnType.setAdapter(new WhiteSpinnerAdapter<>(types));
        spnType.setSelection("KL".equalsIgnoreCase(type) ? 1 : 0);

        EditText edtAmount = makeDarkInput("Số tiền");
        edtAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edtAmount.setText(isEmpty(amount) ? "" : amount);

        content.addView(makeDialogLabel("Ngày"));
        content.addView(edtDate);
        content.addView(makeDialogLabel("Hình thức"));
        content.addView(spnType);
        content.addView(makeDialogLabel("Số tiền"));
        content.addView(edtAmount);

        addDialogActions(content, dialog, () -> {
            String newNgay = edtDate.getText().toString().trim();

            if (newNgay.isEmpty()) {
                toast("Vui lòng chọn ngày.");
                return false;
            }

            if (!isAdd && !ngay.equals(newNgay)) {
                db.delete("DSTHUONGPHAT", "MANV = ? AND NGAY = ?", new String[]{profile.maNV, ngay});
            }

            double sotien = parseDoubleSafe(edtAmount.getText().toString().trim());

            if (sotien <= 0) {
                toast("Số tiền phải lớn hơn 0.");
                return false;
            }

            ContentValues values = new ContentValues();
            values.put("MANV", profile.maNV);
            values.put("NGAY", newNgay);
            values.put("HINHTHUC", spnType.getSelectedItemPosition() == 0 ? "KT" : "KL");
            values.put("SOTIEN", sotien);
            values.put("HESOTIEN", 1);
            values.put("TONG", sotien);

            db.insertWithOnConflict("DSTHUONGPHAT", null, values, SQLiteDatabase.CONFLICT_REPLACE);

            toast("Đã lưu thưởng/phạt.");
            loadProfile();
            return true;
        });

        currentDialogRoot.addView(content);
        dialog.show();
    }

    private void showEditRelativeDialog(
            String ten,
            String phone,
            String address,
            String relation,
            String oldTen,
            boolean isAdd
    ) {
        Dialog dialog = createStyledDialog(isAdd ? "Thêm nhân thân" : "Sửa nhân thân");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        EditText edtName = makeDarkInput("Tên nhân thân");
        edtName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtName.setText(ten);

        EditText edtPhone = makeDarkInput("Điện thoại");
        edtPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        edtPhone.setText(phone);

        EditText edtAddress = makeDarkInput("Địa chỉ");
        edtAddress.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtAddress.setText(address);

        EditText edtRelation = makeDarkInput("Quan hệ");
        edtRelation.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtRelation.setText(relation);

        content.addView(makeDialogLabel("Tên"));
        content.addView(edtName);
        content.addView(makeDialogLabel("Điện thoại"));
        content.addView(edtPhone);
        content.addView(makeDialogLabel("Địa chỉ"));
        content.addView(edtAddress);
        content.addView(makeDialogLabel("Quan hệ"));
        content.addView(edtRelation);

        addDialogActions(content, dialog, () -> {
            String newTen = edtName.getText().toString().trim();
            String newRelation = edtRelation.getText().toString().trim();

            if (!isValidVietnameseName(newTen)) {
                toast("Tên nhân thân chỉ được chứa chữ cái tiếng Việt và khoảng trắng.");
                return false;
            }

            if (!newRelation.isEmpty() && !isValidVietnameseName(newRelation)) {
                toast("Quan hệ chỉ được chứa chữ cái tiếng Việt và khoảng trắng.");
                return false;
            }

            if (!isAdd && !oldTen.equals(newTen)) {
                db.delete("NHANTHAN", "MANV = ? AND TENNT = ?", new String[]{profile.maNV, oldTen});
            }

            ContentValues values = new ContentValues();
            values.put("MANV", profile.maNV);
            values.put("TENNT", newTen);
            values.put("DIENTHOAI", edtPhone.getText().toString().trim());
            values.put("DIACHI", edtAddress.getText().toString().trim());
            values.put("QUANHE", newRelation);

            db.insertWithOnConflict("NHANTHAN", null, values, SQLiteDatabase.CONFLICT_REPLACE);

            toast("Đã lưu nhân thân.");
            loadProfile();
            return true;
        });

        currentDialogRoot.addView(content);
        dialog.show();
    }

    private void deleteAttendance(String ngay) {
        db.delete("CHAMCONG", "MANV = ? AND NGAYCC = ?", new String[]{profile.maNV, ngay});
        toast("Đã xóa chấm công.");
        loadProfile();
    }

    private void deleteRewardPenalty(String ngay) {
        db.delete("DSTHUONGPHAT", "MANV = ? AND NGAY = ?", new String[]{profile.maNV, ngay});
        toast("Đã xóa thưởng/phạt.");
        loadProfile();
    }

    private void deleteRelative(String ten) {
        db.delete("NHANTHAN", "MANV = ? AND TENNT = ?", new String[]{profile.maNV, ten});
        toast("Đã xóa nhân thân.");
        loadProfile();
    }

    private void upsertByManv(String tableName, ContentValues values) {
        Cursor c = db.rawQuery("SELECT 1 FROM " + tableName + " WHERE MANV = ?", new String[]{profile.maNV});

        boolean exists = c.moveToFirst();
        c.close();

        if (exists) {
            db.update(tableName, values, "MANV = ?", new String[]{profile.maNV});
        } else {
            db.insert(tableName, null, values);
        }
    }

    private boolean isDateInWorkSchedule(String date) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM LICHLAMVIEC WHERE MANV = ? AND NGAYLAMVIEC = ?",
                new String[]{profile.maNV, date}
        );

        boolean exists = c.moveToFirst();
        c.close();

        return exists;
    }

    private double getRewardPenaltyMonth(String id, String type) {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(SUM(IFNULL(SOTIEN, 0)), 0) " +
                        "FROM DSTHUONGPHAT " +
                        "WHERE MANV = ? AND HINHTHUC = ? AND strftime('%Y-%m', NGAY) = strftime('%Y-%m', 'now')",
                new String[]{id, type}
        );

        double value = 0;

        if (c.moveToFirst()) {
            value = c.getDouble(0);
        }

        c.close();
        return value;
    }

    private double getRewardPenaltyAll(String id, String type) {
        Cursor c = db.rawQuery(
                "SELECT IFNULL(SUM(IFNULL(SOTIEN, 0)), 0) " +
                        "FROM DSTHUONGPHAT " +
                        "WHERE MANV = ? AND HINHTHUC = ?",
                new String[]{id, type}
        );

        double value = 0;

        if (c.moveToFirst()) {
            value = c.getDouble(0);
        }

        c.close();
        return value;
    }

    private MonthSummary getMonthSummary(String id) {
        MonthSummary s = new MonthSummary();

        Cursor c = db.rawQuery(
                "SELECT COUNT(*), IFNULL(SUM(GIOCONG), 0), IFNULL(SUM(TANGCA), 0), IFNULL(SUM(TONGCA), 0) " +
                        "FROM CHAMCONG " +
                        "WHERE MANV = ? AND strftime('%Y-%m', NGAYCC) = strftime('%Y-%m', 'now')",
                new String[]{id}
        );

        if (c.moveToFirst()) {
            s.days = c.getDouble(0);
            s.gioCong = c.getDouble(1);
            s.tangCa = c.getDouble(2);
            s.tongCa = c.getDouble(3);
        }

        c.close();
        return s;
    }

    private void setupDateInput(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(true);
        editText.setInputType(InputType.TYPE_NULL);
    }

    private void setupReadOnlyInput(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(false);
        editText.setInputType(InputType.TYPE_NULL);
    }

    private void showBirthYearPicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        int year = parseIntSafe(target.getText().toString().trim());

        if (year >= 1950 && year <= getMaxBirthYear()) {
            calendar.set(Calendar.YEAR, year);
        } else {
            calendar.set(Calendar.YEAR, getMaxBirthYear());
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, selectedYear, month, dayOfMonth) -> target.setText(String.valueOf(selectedYear)),
                calendar.get(Calendar.YEAR),
                Calendar.JANUARY,
                1
        );

        Calendar min = Calendar.getInstance();
        min.set(1950, Calendar.JANUARY, 1);

        Calendar max = Calendar.getInstance();
        max.set(getMaxBirthYear(), Calendar.DECEMBER, 31);

        dialog.getDatePicker().setMinDate(min.getTimeInMillis());
        dialog.getDatePicker().setMaxDate(max.getTimeInMillis());

        dialog.show();
    }

    private void showNormalDatePicker(EditText target) {
        Calendar calendar = getCalendarFromText(target.getText().toString().trim());

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> target.setText(formatDate(year, month, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        Calendar max = Calendar.getInstance();
        dialog.getDatePicker().setMaxDate(max.getTimeInMillis());

        dialog.show();
    }

    private void showFutureDatePicker(EditText target) {
        Calendar calendar = getCalendarFromText(target.getText().toString().trim());

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> target.setText(formatDate(year, month, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        Calendar min = Calendar.getInstance();
        dialog.getDatePicker().setMinDate(min.getTimeInMillis());

        dialog.show();
    }

    private void showDateTimePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = formatDate(year, month, dayOfMonth);

                    TimePickerDialog timeDialog = new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                String time = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute);
                                target.setText(date + " " + time);
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );

                    timeDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dateDialog.show();
    }

    private void showTimePicker(EditText target, Runnable afterPick) {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    target.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));

                    if (afterPick != null) {
                        afterPick.run();
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );

        dialog.show();
    }

    private Calendar getCalendarFromText(String text) {
        Calendar calendar = Calendar.getInstance();

        if (!isEmpty(text)) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(text.substring(0, 10));

                if (date != null) {
                    calendar.setTime(date);
                }

            } catch (Exception ignored) {
            }
        }

        return calendar;
    }

    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
    }

    private int getMaxBirthYear() {
        return getCurrentYear() - 18;
    }

    private boolean isValidBirthYear(int year) {
        return year >= 1950 && year <= getMaxBirthYear();
    }

    private boolean isValidVietnameseName(String name) {
        if (name == null) {
            return false;
        }

        String value = name.trim();

        if (value.isEmpty()) {
            return false;
        }

        return value.matches("^[\\p{L}]+([\\s.'-][\\p{L}]+)*$");
    }

    class WhiteSpinnerAdapter<T> extends ArrayAdapter<T> {

        WhiteSpinnerAdapter(ArrayList<T> data) {
            super(EmployeeProfileActivity.this, android.R.layout.simple_spinner_item, data);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(EmployeeProfileActivity.this);
            tv.setText(getItem(position) == null ? "" : getItem(position).toString());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(15);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setSingleLine(false);
            tv.setBackgroundColor(Color.TRANSPARENT);
            return tv;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(EmployeeProfileActivity.this);
            tv.setText(getItem(position) == null ? "" : getItem(position).toString());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(15);
            tv.setPadding(dp(16), dp(16), dp(16), dp(16));
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setSingleLine(false);
            tv.setBackgroundColor(Color.parseColor("#111827"));
            return tv;
        }
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.hr_card_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        return card;
    }

    private LinearLayout makeTitleRow(String title, String buttonText, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(10));

        TextView tv = makeText(title, 13, PRIMARY, true);

        row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button btn = makeSmallButton(buttonText, PRIMARY, Color.BLACK, listener);
        row.addView(btn);

        return row;
    }

    private LinearLayout makeInfoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(7), 0, dp(7));

        TextView l = makeText(label, 13, SUB, false);
        TextView v = makeText(value == null || value.trim().isEmpty() ? "—" : value, 13, TEXT, true);
        v.setGravity(Gravity.RIGHT);

        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
    }

    private LinearLayout makeBigValue(String label, String value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackgroundResource(R.drawable.employee_input_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);

        box.addView(makeText(label, 12, SUB, false));
        box.addView(makeText(value, 19, color, true));

        return box;
    }

    private LinearLayout makeEditableItem(
            String title,
            String subtitle,
            int color,
            View.OnClickListener editListener,
            View.OnClickListener deleteListener
    ) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackgroundResource(R.drawable.employee_input_bg);

        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        boxLp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(boxLp);

        box.addView(makeText(title, 15, color, true));

        TextView s = makeText(subtitle, 12, SUB, false);
        s.setPadding(0, dp(4), 0, dp(8));
        box.addView(s);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        actions.addView(makeSmallButton("Sửa", BLUE, Color.WHITE, editListener));
        actions.addView(makeSmallButton("Xóa", RED, Color.WHITE, deleteListener));

        box.addView(actions);

        return box;
    }

    private TextView makeEmptyText(String text) {
        TextView tv = makeText(text, 13, SUB, false);
        tv.setPadding(0, dp(8), 0, dp(8));
        return tv;
    }

    private TextView makeTag(String text) {
        TextView tag = makeText(text, 11, SUB, false);
        tag.setPadding(dp(9), dp(4), dp(9), dp(4));
        tag.setBackgroundResource(R.drawable.employee_tag_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, dp(6), 0);
        tag.setLayoutParams(lp);

        return tag;
    }

    private Button makeHeaderButton(String text, int bgRes, int textColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextColor(textColor);
        btn.setTextSize(13);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundTintList(null);
        btn.setBackgroundResource(bgRes);
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

    private void addDialogActions(LinearLayout content, Dialog dialog, SaveAction saveAction) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.RIGHT);
        actions.setPadding(0, dp(14), 0, 0);

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button save = makeDialogButton("Lưu", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(save);
        content.addView(actions);

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            boolean ok = saveAction.save();

            if (ok) {
                dialog.dismiss();
            }
        });
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

    private void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Employee code", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Đã copy mã nhân viên.", Toast.LENGTH_SHORT).show();
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

    private String formatNumber(double number) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(number);
    }

    private String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null");
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

    private int parseIntSafe(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0;
            }

            return Integer.parseInt(value.trim());

        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0;
            }

            return Double.parseDouble(value.trim());

        } catch (Exception e) {
            return 0;
        }
    }

    private int getCurrentYear() {
        return Integer.parseInt(new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date()));
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String nowDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String removeDiacritics(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        return pattern.matcher(normalized).replaceAll("")
                .replace("Đ", "D")
                .replace("đ", "d");
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    interface SaveAction {
        boolean save();
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

    static class MonthSummary {
        double days;
        double gioCong;
        double tangCa;
        double tongCa;
    }

    static class EmployeeProfile {
        String maNV;
        String hoLot;
        String tenNV;
        int namSinh;
        boolean gioiTinh;
        String maPB;
        String tenPB;
        String maCV;
        String tenCV;
        String anh;
        String hdld;
        String maDN;
        String tenDN;
        String loaiNV;
        double luongCoBan;

        String sdt;
        String diaChi;
        String fb;
        String email;
        String queQuan;

        int chieuCao;
        int canNang;
        String tienSuBenh;
        int thiLuc;
        String ngayCapNhatSucKhoe;

        String loaiBaoHiem;
        String soBaoHiem;
        String thoiHanBaoHiem;

        String getFullName() {
            String fullName = (hoLot == null ? "" : hoLot.trim()) + " " + (tenNV == null ? "" : tenNV.trim());
            return fullName.trim();
        }
    }
}