package com.neith.subjectdemo.admin;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.SessionManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class AdminHomeActivity extends AppCompatActivity {

    SQLiteDatabase db;

    EditText edtFrom, edtTo;
    Button btnThongKe, btnReset, btnLogout;
    LinearLayout layoutList;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int GREEN = Color.parseColor("#22C55E");
    final int RED = Color.parseColor("#EF4444");
    final int ORANGE = Color.parseColor("#F97316");
    final int BLUE = Color.parseColor("#3B82F6");
    final int PURPLE = Color.parseColor("#A855F7");

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        db = DB.openDatabase(this);

        edtFrom = findViewById(R.id.edtFrom);
        edtTo = findViewById(R.id.edtTo);
        btnThongKe = findViewById(R.id.btnThongKe);
        btnReset = findViewById(R.id.btnResetAdminHome);

        // XML vẫn dùng id cũ btnRefreshAdminHome, nhưng chức năng giờ là Đăng xuất
        btnLogout = findViewById(R.id.btnRefreshAdminHome);

        layoutList = findViewById(R.id.layoutAdminHomeList);

        btnThongKe.setBackgroundTintList(null);
        btnReset.setBackgroundTintList(null);
        btnLogout.setBackgroundTintList(null);

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setupAdmin(this, bottomNavContainer, BottomNav.ADMIN_HOME);

        setDefaultDateRange();

        edtFrom.setOnClickListener(v -> showDatePicker(edtFrom));
        edtTo.setOnClickListener(v -> showDatePicker(edtTo));

        btnThongKe.setOnClickListener(v -> loadThongKe());

        btnLogout.setOnClickListener(v -> SessionManager.logout(this));

        btnReset.setOnClickListener(v -> {
            setDefaultDateRange();
            loadThongKe();
        });

        loadThongKe();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && db.isOpen()) {
            loadThongKe();
        }
    }

    private void setDefaultDateRange() {
        Calendar cal = Calendar.getInstance();

        edtTo.setText(dateFormat.format(cal.getTime()));

        cal.add(Calendar.DAY_OF_MONTH, -30);
        edtFrom.setText(dateFormat.format(cal.getTime()));
    }

    private void showDatePicker(EditText edt) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(dateFormat.parse(edt.getText().toString().trim()));
        } catch (Exception ignored) {

        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    edt.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void loadThongKe() {
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Không mở được cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }

        String from = edtFrom.getText().toString().trim();
        String to = edtTo.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn đủ từ ngày và đến ngày.", Toast.LENGTH_SHORT).show();
            return;
        }

        layoutList.removeAllViews();

        buildSummaryCard(from, to);
        buildCandidateCard(from, to);
        buildNewEmployeeCard(from, to);
        buildPromotionEmployeeCard(from, to);
        buildProjectStartCard(from, to);
    }

    private void buildSummaryCard(String from, String to) {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("TỔNG QUAN THỐNG KÊ"));

        int candidateCount = getCandidateInRange(from, to).size();

        int newEmployeeCount = getInt(
                "SELECT COUNT(*) FROM ACTIVITY_LOG " +
                        "WHERE ActionCode = 'Add' " +
                        "AND ActionTime BETWEEN ? AND ?",
                new String[]{from + " 00:00:00", to + " 23:59:59"}
        );

        int promotionCount = getInt(
                "SELECT COUNT(*) FROM ACTIVITY_LOG " +
                        "WHERE ActionCode = 'Up' " +
                        "AND ActionTime BETWEEN ? AND ?",
                new String[]{from + " 00:00:00", to + " 23:59:59"}
        );

        int projectCount = getInt(
                "SELECT COUNT(*) FROM HOPDONG " +
                        "WHERE NGAYBD BETWEEN ? AND ?",
                new String[]{from, to}
        );

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(makeStatBox("Ứng viên", String.valueOf(candidateCount), "Trong khoảng lọc", BLUE), halfLp(true));
        row1.addView(makeStatBox("NV mới", String.valueOf(newEmployeeCount), "ActionCode = Add", GREEN), halfLp(false));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(10), 0, 0);
        row2.addView(makeStatBox("Thăng chức", String.valueOf(promotionCount), "ActionCode = Up", ORANGE), halfLp(true));
        row2.addView(makeStatBox("Dự án", String.valueOf(projectCount), "Theo HOPDONG.NGAYBD", PRIMARY), halfLp(false));

        card.addView(row1);
        card.addView(row2);

        layoutList.addView(card);
    }

    private void buildCandidateCard(String from, String to) {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("1. DANH SÁCH ỨNG VIÊN TRONG KHOẢNG LỌC"));

        ArrayList<CandidateItem> list = getCandidateInRange(from, to);

        if (list.isEmpty()) {
            card.addView(makeEmptyText("Không có ứng viên trong khoảng thời gian này."));
        } else {
            TextView note = makeText(
                    "Thời gian ứng viên được lấy từ cột thời gian nếu bảng có cột NGAYNOP / THOIGIANNOP / CreatedAt. Nếu chưa có, hệ thống tự lấy ngày từ tên file dạng _yyyyMMdd.",
                    12,
                    SUB,
                    false
            );
            note.setPadding(0, 0, 0, dp(10));
            card.addView(note);

            for (CandidateItem item : list) {
                LinearLayout box = makeInnerBox();

                box.addView(makeText("#" + item.id + " • " + emptyText(item.name), 15, TEXT, true));
                box.addView(makeSmallLine("Thời gian", emptyText(item.time)));
                box.addView(makeSmallLine("Email", emptyText(item.email)));
                box.addView(makeSmallLine("File thông tin", hasFile(item.fileInfo)));
                box.addView(makeSmallLine("File bằng cấp", hasFile(item.fileBangCap)));
                box.addView(makeSmallLine("File khác", hasFile(item.fileKhac)));

                card.addView(box);
            }
        }

        layoutList.addView(card);
    }

    private ArrayList<CandidateItem> getCandidateInRange(String from, String to) {
        ArrayList<CandidateItem> list = new ArrayList<>();

        String timeColumn = getCandidateTimeColumn();

        String sql;

        if (timeColumn.isEmpty()) {
            sql = "SELECT ID, IFNULL(TENUNGVIEN, ''), IFNULL(EMAIL, ''), " +
                    "IFNULL(FILETHONGTIN, ''), IFNULL(FILEBANGCAP, ''), IFNULL(FILEKHAC, '') " +
                    "FROM HOSOVIECLAM";
        } else {
            sql = "SELECT ID, IFNULL(TENUNGVIEN, ''), IFNULL(EMAIL, ''), " +
                    "IFNULL(FILETHONGTIN, ''), IFNULL(FILEBANGCAP, ''), IFNULL(FILEKHAC, ''), " +
                    "IFNULL(" + timeColumn + ", '') " +
                    "FROM HOSOVIECLAM " +
                    "WHERE substr(" + timeColumn + ", 1, 10) BETWEEN ? AND ? " +
                    "ORDER BY " + timeColumn + " DESC";
        }

        Cursor c;

        if (timeColumn.isEmpty()) {
            c = db.rawQuery(sql, null);
        } else {
            c = db.rawQuery(sql, new String[]{from, to});
        }

        while (c.moveToNext()) {
            CandidateItem item = new CandidateItem();

            item.id = c.getString(0);
            item.name = c.getString(1);
            item.email = c.getString(2);
            item.fileInfo = c.getString(3);
            item.fileBangCap = c.getString(4);
            item.fileKhac = c.getString(5);

            if (timeColumn.isEmpty()) {
                item.time = extractDateFromCandidateFiles(item.fileInfo, item.fileBangCap, item.fileKhac);
            } else {
                item.time = normalizeDateText(c.getString(6));
            }

            if (!item.time.isEmpty() && item.time.compareTo(from) >= 0 && item.time.compareTo(to) <= 0) {
                list.add(item);
            }
        }

        c.close();

        Collections.sort(list, (a, b) -> b.time.compareTo(a.time));

        return list;
    }

    private String getCandidateTimeColumn() {
        if (hasColumn("HOSOVIECLAM", "NGAYNOP")) {
            return "NGAYNOP";
        }

        if (hasColumn("HOSOVIECLAM", "THOIGIANNOP")) {
            return "THOIGIANNOP";
        }

        if (hasColumn("HOSOVIECLAM", "CreatedAt")) {
            return "CreatedAt";
        }

        if (hasColumn("HOSOVIECLAM", "CREATEDAT")) {
            return "CREATEDAT";
        }

        return "";
    }

    private void buildNewEmployeeCard(String from, String to) {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("2. DANH SÁCH NHÂN VIÊN MỚI VÀO TRONG KHOẢNG LỌC"));

        Cursor c = db.rawQuery(
                "SELECT LogID, ActionTime, PerformedBy, Description " +
                        "FROM ACTIVITY_LOG " +
                        "WHERE ActionCode = 'Add' " +
                        "AND ActionTime BETWEEN ? AND ? " +
                        "ORDER BY ActionTime DESC",
                new String[]{from + " 00:00:00", to + " 23:59:59"}
        );

        if (c.getCount() == 0) {
            card.addView(makeEmptyText("Không có log thêm nhân viên trong khoảng thời gian này."));
        } else {
            while (c.moveToNext()) {
                String logId = c.getString(0);
                String actionTime = c.getString(1);
                String performedBy = c.getString(2);
                String description = c.getString(3);
                String maNV = extractLastWord(description);

                EmployeeItem emp = getEmployeeByCode(maNV);

                LinearLayout box = makeInnerBox();

                box.addView(makeText("Log #" + logId + " • " + maNV, 15, GREEN, true));
                box.addView(makeSmallLine("Thời gian", emptyText(actionTime)));
                box.addView(makeSmallLine("Người thực hiện", emptyText(performedBy)));
                box.addView(makeSmallLine("Mô tả log", emptyText(description)));

                if (emp == null) {
                    box.addView(makeSmallLine("Thông tin NV", "Không tìm thấy trong bảng NHANVIEN"));
                } else {
                    box.addView(makeSmallLine("Họ tên", emptyText(emp.fullName)));
                    box.addView(makeSmallLine("Năm sinh", emptyText(emp.birthYear)));
                    box.addView(makeSmallLine("Giới tính", formatGender(emp.gender)));
                    box.addView(makeSmallLine("Phòng ban", emptyText(emp.departmentCode) + " • " + emptyText(emp.departmentName)));
                    box.addView(makeSmallLine("Chức vụ", emptyText(emp.positionCode) + " • " + emptyText(emp.positionName)));
                    box.addView(makeSmallLine("Doanh nghiệp", emptyText(emp.businessCode) + " • " + emptyText(emp.businessName)));
                }

                card.addView(box);
            }
        }

        c.close();
        layoutList.addView(card);
    }

    private void buildPromotionEmployeeCard(String from, String to) {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("3. DANH SÁCH NHÂN VIÊN THĂNG CHỨC TRONG KHOẢNG LỌC"));

        Cursor c = db.rawQuery(
                "SELECT LogID, ActionTime, PerformedBy, Description " +
                        "FROM ACTIVITY_LOG " +
                        "WHERE ActionCode = 'Up' " +
                        "AND ActionTime BETWEEN ? AND ? " +
                        "ORDER BY ActionTime DESC",
                new String[]{from + " 00:00:00", to + " 23:59:59"}
        );

        if (c.getCount() == 0) {
            card.addView(makeEmptyText("Không có log thăng chức trong khoảng thời gian này."));
        } else {
            while (c.moveToNext()) {
                String logId = c.getString(0);
                String actionTime = c.getString(1);
                String performedBy = c.getString(2);
                String description = c.getString(3);
                String maNV = extractLastWord(description);

                EmployeeItem emp = getEmployeeByCode(maNV);

                LinearLayout box = makeInnerBox();

                box.addView(makeText("Log #" + logId + " • " + maNV, 15, ORANGE, true));
                box.addView(makeSmallLine("Thời gian", emptyText(actionTime)));
                box.addView(makeSmallLine("Người thực hiện", emptyText(performedBy)));
                box.addView(makeSmallLine("Mô tả log", emptyText(description)));

                if (emp == null) {
                    box.addView(makeSmallLine("Thông tin NV", "Không tìm thấy trong bảng NHANVIEN"));
                } else {
                    box.addView(makeSmallLine("Họ tên", emptyText(emp.fullName)));
                    box.addView(makeSmallLine("Phòng ban hiện tại", emptyText(emp.departmentCode) + " • " + emptyText(emp.departmentName)));
                    box.addView(makeSmallLine("Mã trưởng phòng", emptyText(emp.managerCode)));
                    box.addView(makeSmallLine("Địa điểm phòng ban", emptyText(emp.departmentLocation)));
                    box.addView(makeSmallLine("Chức vụ sau khi thăng chức", emptyText(emp.positionCode) + " • " + emptyText(emp.positionName)));
                    box.addView(makeSmallLine("Hệ số lương chức vụ", emptyText(emp.salaryCoefficient)));
                }

                card.addView(box);
            }
        }

        c.close();
        layoutList.addView(card);
    }

    private EmployeeItem getEmployeeByCode(String maNV) {
        if (maNV == null || maNV.trim().isEmpty()) {
            return null;
        }

        Cursor c = db.rawQuery(
                "SELECT NV.MANV, " +
                        "IFNULL(NV.HOLOT, '') || ' ' || IFNULL(NV.TENNV, '') AS HOTEN, " +
                        "IFNULL(NV.NAMSINH, ''), IFNULL(NV.GIOITINH, ''), " +
                        "IFNULL(NV.MAPB, ''), IFNULL(PB.TENPB, ''), IFNULL(PB.MATRG_PHG, ''), IFNULL(PB.DIADIEM, ''), " +
                        "IFNULL(NV.MACV, ''), IFNULL(CV.TENCV, ''), IFNULL(CV.HESOLUONG, ''), " +
                        "IFNULL(NV.MADN, ''), IFNULL(DN.TENDN, '') " +
                        "FROM NHANVIEN NV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "LEFT JOIN VITRICONGVIEC CV ON NV.MACV = CV.MACV " +
                        "LEFT JOIN THONGTINDOANHNGHIEP DN ON NV.MADN = DN.MADN " +
                        "WHERE NV.MANV = ? " +
                        "LIMIT 1",
                new String[]{maNV}
        );

        EmployeeItem item = null;

        if (c.moveToFirst()) {
            item = new EmployeeItem();
            item.employeeCode = c.getString(0);
            item.fullName = c.getString(1).trim();
            item.birthYear = c.getString(2);
            item.gender = c.getString(3);
            item.departmentCode = c.getString(4);
            item.departmentName = c.getString(5);
            item.managerCode = c.getString(6);
            item.departmentLocation = c.getString(7);
            item.positionCode = c.getString(8);
            item.positionName = c.getString(9);
            item.salaryCoefficient = c.getString(10);
            item.businessCode = c.getString(11);
            item.businessName = c.getString(12);
        }

        c.close();

        return item;
    }

    private void buildProjectStartCard(String from, String to) {
        LinearLayout card = makeCard();
        card.addView(makeCardTitle("4. DỰ ÁN / HỢP ĐỒNG BẮT ĐẦU TRONG KHOẢNG LỌC"));

        Cursor c = db.rawQuery(
                "SELECT HD.MAHD, " +
                        "IFNULL(HD.TENHD, ''), " +
                        "IFNULL(HD.LOAI, ''), " +
                        "IFNULL(HD.NGAYBD, ''), " +
                        "IFNULL(HD.NGAYKT_DUTINH, ''), " +
                        "IFNULL(DTHD.MADAHD, ''), " +
                        "IFNULL(DTHD.MADA, ''), " +
                        "IFNULL(DTHD.NGAYKT, ''), " +
                        "IFNULL(DA.MAKH, ''), " +
                        "IFNULL(DA.TIENCOC, 0), " +
                        "IFNULL(DA.TIENNGHIEMTHU_DUTINH, 0), " +
                        "IFNULL(DT.TIENNGHIEMTHU_TONG, 0), " +
                        "IFNULL(DT.MALH, ''), " +
                        "IFNULL(DT.MAKV, ''), " +
                        "IFNULL(DT.MARR, ''), " +
                        "IFNULL(DT.HSTHAYDOI, 0) " +
                        "FROM HOPDONG HD " +
                        "LEFT JOIN DUANTHEOHOPDONG DTHD ON HD.MAHD = DTHD.MAHD " +
                        "LEFT JOIN DUAN DA ON DTHD.MADA = DA.MADA " +
                        "LEFT JOIN DTDUAN DT ON DTHD.MADA = DT.MADA " +
                        "WHERE HD.NGAYBD BETWEEN ? AND ? " +
                        "ORDER BY HD.NGAYBD DESC",
                new String[]{from, to}
        );

        if (c.getCount() == 0) {
            card.addView(makeEmptyText("Không có hợp đồng bắt đầu trong khoảng thời gian này."));
        } else {
            while (c.moveToNext()) {
                String maHD = c.getString(0);
                String tenHD = c.getString(1);
                String loai = c.getString(2);
                String ngayBD = c.getString(3);
                String ngayKTduTinh = c.getString(4);
                String maDAHD = c.getString(5);
                String maDA = c.getString(6);
                String ngayKTThucTe = c.getString(7);
                String maKH = c.getString(8);

                double tienCoc = c.getDouble(9);
                double tienNghiemThuDuTinh = c.getDouble(10);
                double tienNghiemThuTong = c.getDouble(11);

                String maLoaiHinh = c.getString(12);
                String maKhuVuc = c.getString(13);
                String maRuiRo = c.getString(14);
                double heSoThayDoi = c.getDouble(15);

                LinearLayout box = makeInnerBox();

                box.addView(makeText(maHD + " • " + emptyText(tenHD), 15, PRIMARY, true));

                box.addView(makeSmallLine("Loại hợp đồng", emptyText(loai)));
                box.addView(makeSmallLine("Ngày bắt đầu", emptyText(ngayBD)));
                box.addView(makeSmallLine("Ngày kết thúc dự tính", emptyText(ngayKTduTinh)));

                box.addView(makeSmallLine("Mã dự án hợp đồng", emptyText(maDAHD)));
                box.addView(makeSmallLine("Mã dự án", emptyText(maDA)));
                box.addView(makeSmallLine("Ngày kết thúc thực tế", emptyText(ngayKTThucTe)));
                box.addView(makeSmallLine("Mã khách hàng", emptyText(maKH)));

                box.addView(makeSmallLine("Tiền cọc", formatCurrencyFull(tienCoc)));
                box.addView(makeSmallLine("Nghiệm thu dự tính", formatCurrencyFull(tienNghiemThuDuTinh)));
                box.addView(makeSmallLine("Nghiệm thu tổng", formatCurrencyFull(tienNghiemThuTong)));

                box.addView(makeSmallLine("Mã loại hình", emptyText(maLoaiHinh)));
                box.addView(makeSmallLine("Mã khu vực", emptyText(maKhuVuc)));
                box.addView(makeSmallLine("Mã rủi ro", emptyText(maRuiRo)));
                box.addView(makeSmallLine("Hệ số thay đổi", formatNumber(heSoThayDoi)));

                card.addView(box);
            }
        }

        c.close();
        layoutList.addView(card);
    }

    private String extractLastWord(String text) {
        if (text == null) {
            return "";
        }

        text = text.trim();

        if (text.isEmpty()) {
            return "";
        }

        String[] arr = text.split("\\s+");
        return arr[arr.length - 1].trim();
    }

    private String extractDateFromCandidateFiles(String fileInfo, String fileBangCap, String fileKhac) {
        String date = extractDateFromFileName(fileInfo);

        if (!date.isEmpty()) {
            return date;
        }

        date = extractDateFromFileName(fileBangCap);

        if (!date.isEmpty()) {
            return date;
        }

        return extractDateFromFileName(fileKhac);
    }

    private String extractDateFromFileName(String fileName) {
        if (fileName == null) {
            return "";
        }

        String text = fileName.trim();

        if (text.isEmpty()) {
            return "";
        }

        for (int i = 0; i <= text.length() - 8; i++) {
            String part = text.substring(i, i + 8);

            if (part.matches("\\d{8}")) {
                String year = part.substring(0, 4);
                String month = part.substring(4, 6);
                String day = part.substring(6, 8);

                return year + "-" + month + "-" + day;
            }
        }

        return "";
    }

    private String normalizeDateText(String value) {
        if (value == null) {
            return "";
        }

        value = value.trim();

        if (value.length() >= 10) {
            return value.substring(0, 10);
        }

        return value;
    }

    private boolean hasColumn(String tableName, String columnName) {
        Cursor c = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);

        boolean exists = false;

        while (c.moveToNext()) {
            String name = c.getString(1);

            if (name != null && name.equalsIgnoreCase(columnName)) {
                exists = true;
                break;
            }
        }

        c.close();

        return exists;
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

    private LinearLayout makeInnerBox() {
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

        return box;
    }

    private TextView makeCardTitle(String text) {
        TextView tv = makeText(text, 13, PRIMARY, true);
        tv.setPadding(0, 0, 0, dp(10));
        return tv;
    }

    private LinearLayout makeSmallLine(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, 0);

        TextView l = makeText(label, 11, SUB, false);
        TextView v = makeText(value, 11, TEXT, false);
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
        box.addView(makeText(value, 20, color, true));
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

    private TextView makeEmptyText(String text) {
        TextView tv = makeText(text, 14, SUB, false);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(18), 0, dp(18));
        return tv;
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

    private String hasFile(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null") ? "Chưa có" : "Đã tải";
    }

    private String formatGender(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "—";
        }

        if (value.equals("1")) {
            return "Nam";
        }

        if (value.equals("0")) {
            return "Nữ";
        }

        return value;
    }

    private String formatNumber(double value) {
        return String.format(Locale.getDefault(), "%,.2f", value);
    }

    private String formatCurrencyFull(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class CandidateItem {
        String id;
        String name;
        String email;
        String fileInfo;
        String fileBangCap;
        String fileKhac;
        String time;
    }

    static class EmployeeItem {
        String employeeCode;
        String fullName;
        String birthYear;
        String gender;
        String departmentCode;
        String departmentName;
        String managerCode;
        String departmentLocation;
        String positionCode;
        String positionName;
        String salaryCoefficient;
        String businessCode;
        String businessName;
    }
}