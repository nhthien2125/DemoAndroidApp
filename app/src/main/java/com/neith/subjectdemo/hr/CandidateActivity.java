package com.neith.subjectdemo.hr;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.ActivityLogger;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.EmailSender;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CandidateActivity extends AppCompatActivity {

    SQLiteDatabase db;

    LinearLayout layoutCandidateList;
    LinearLayout layoutCandidateFilterToggle, layoutCandidateFilterContent;
    TextView txtCandidateFilterArrow;

    EditText edtSearchCandidate, edtFromDate, edtToDate;
    Button btnFilterCandidate, btnResetCandidate, btnExportDeleteCandidate, btnBackEmployee, btnDownloadSelectedFiles;

    ArrayList<CandidateItem> candidates = new ArrayList<>();
    HashSet<Integer> selectedIds = new HashSet<>();

    boolean isFilterExpanded = false;

    String candidateTableName = "";

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int RED = Color.parseColor("#EF4444");
    final int BLUE = Color.parseColor("#3B82F6");
    final int GREEN = Color.parseColor("#22C55E");
    final int ORANGE = Color.parseColor("#F97316");

    LinearLayout currentDialogRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate);

        db = DB.openDatabase(this);

        layoutCandidateList = findViewById(R.id.layoutCandidateList);
        layoutCandidateFilterToggle = findViewById(R.id.layoutCandidateFilterToggle);
        layoutCandidateFilterContent = findViewById(R.id.layoutCandidateFilterContent);
        txtCandidateFilterArrow = findViewById(R.id.txtCandidateFilterArrow);

        edtSearchCandidate = findViewById(R.id.edtSearchCandidate);
        edtFromDate = findViewById(R.id.edtFromDate);
        edtToDate = findViewById(R.id.edtToDate);

        setupDateInput(edtFromDate);
        setupDateInput(edtToDate);

        edtFromDate.setOnClickListener(v -> showDatePicker(edtFromDate));
        edtToDate.setOnClickListener(v -> showDatePicker(edtToDate));

        btnFilterCandidate = findViewById(R.id.btnFilterCandidate);
        btnResetCandidate = findViewById(R.id.btnResetCandidate);
        btnExportDeleteCandidate = findViewById(R.id.btnExportDeleteCandidate);
        btnBackEmployee = findViewById(R.id.btnBackEmployee);
        btnDownloadSelectedFiles = findViewById(R.id.btnDownloadSelectedFiles);

        btnFilterCandidate.setBackgroundTintList(null);
        btnResetCandidate.setBackgroundTintList(null);
        btnExportDeleteCandidate.setBackgroundTintList(null);
        btnBackEmployee.setBackgroundTintList(null);
        btnDownloadSelectedFiles.setBackgroundTintList(null);

        layoutCandidateFilterContent.setVisibility(View.GONE);
        txtCandidateFilterArrow.setText("▼");

        layoutCandidateFilterToggle.setOnClickListener(v -> toggleFilter());

        btnBackEmployee.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnFilterCandidate.setOnClickListener(v -> loadCandidates());

        btnResetCandidate.setOnClickListener(v -> {
            edtSearchCandidate.setText("");
            edtFromDate.setText("");
            edtToDate.setText("");
            selectedIds.clear();
            loadCandidates();
        });

        btnExportDeleteCandidate.setOnClickListener(v -> showExportDeleteDialog());

        btnDownloadSelectedFiles.setOnClickListener(v -> downloadSelectedFiles());

        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.EMPLOYEE);

        candidateTableName = findCandidateTableName();

        loadCandidates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCandidates();
    }

    private void toggleFilter() {
        if (isFilterExpanded) {
            isFilterExpanded = false;
            txtCandidateFilterArrow.setText("▼");

            layoutCandidateFilterContent.animate()
                    .alpha(0f)
                    .translationY(-dp(8))
                    .setDuration(160)
                    .withEndAction(() -> {
                        layoutCandidateFilterContent.setVisibility(View.GONE);
                        layoutCandidateFilterContent.setAlpha(1f);
                        layoutCandidateFilterContent.setTranslationY(0);
                    })
                    .start();

        } else {
            isFilterExpanded = true;
            txtCandidateFilterArrow.setText("▲");

            layoutCandidateFilterContent.setVisibility(View.VISIBLE);
            layoutCandidateFilterContent.setAlpha(0f);
            layoutCandidateFilterContent.setTranslationY(-dp(8));

            layoutCandidateFilterContent.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setInterpolator(new OvershootInterpolator())
                    .setDuration(220)
                    .start();
        }
    }

    private String findCandidateTableName() {
        String[] possibleNames = {
                "HOSOVIECLAM",
                "HOSOVIELAM",
                "HOSO_UNGVIEN",
                "UNGVIEN",
                "CANDIDATE"
        };

        for (String name : possibleNames) {
            if (tableExists(name)) {
                return name;
            }
        }

        Cursor c = null;

        try {
            c = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND UPPER(name) LIKE '%HOSO%'",
                    null
            );

            if (c.moveToFirst()) {
                return c.getString(0);
            }

        } catch (Exception ignored) {
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return "";
    }

    private boolean tableExists(String tableName) {
        Cursor c = null;

        try {
            c = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND UPPER(name)=UPPER(?)",
                    new String[]{tableName}
            );

            return c.moveToFirst();

        } catch (Exception e) {
            return false;

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Cursor c = null;

        try {
            c = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);

            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndexOrThrow("name"));

                if (name != null && name.equalsIgnoreCase(columnName)) {
                    return true;
                }
            }

        } catch (Exception ignored) {
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return false;
    }

    private void loadCandidates() {
        layoutCandidateList.removeAllViews();
        candidates.clear();

        if (candidateTableName == null || candidateTableName.trim().isEmpty()) {
            TextView error = makeText(
                    "Không tìm thấy bảng hồ sơ ứng viên trong database.",
                    14,
                    RED,
                    true
            );
            error.setGravity(Gravity.CENTER);
            error.setPadding(0, dp(30), 0, dp(30));
            layoutCandidateList.addView(error);
            return;
        }

        boolean hasId = columnExists(candidateTableName, "ID");
        boolean hasName = columnExists(candidateTableName, "TENUNGVIEN");
        boolean hasEmail = columnExists(candidateTableName, "EMAIL");
        boolean hasFileInfo = columnExists(candidateTableName, "FILETHONGTIN");
        boolean hasFileDegree = columnExists(candidateTableName, "FILEBANGCAP");
        boolean hasFileOther = columnExists(candidateTableName, "FILEKHAC");

        if (!hasId || !hasName) {
            TextView error = makeText(
                    "Bảng " + candidateTableName + " thiếu cột ID hoặc TENUNGVIEN.",
                    14,
                    RED,
                    true
            );
            error.setGravity(Gravity.CENTER);
            error.setPadding(0, dp(30), 0, dp(30));
            layoutCandidateList.addView(error);
            return;
        }

        String keyword = edtSearchCandidate.getText().toString().trim();
        Date from = parseDate(edtFromDate.getText().toString().trim());
        Date to = parseDate(edtToDate.getText().toString().trim());

        StringBuilder sql = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        sql.append("SELECT * FROM ").append(candidateTableName).append(" WHERE 1 = 1 ");

        if (!keyword.isEmpty()) {
            if (hasEmail) {
                sql.append("AND (TENUNGVIEN LIKE ? OR EMAIL LIKE ?) ");
                args.add("%" + keyword + "%");
                args.add("%" + keyword + "%");
            } else {
                sql.append("AND TENUNGVIEN LIKE ? ");
                args.add("%" + keyword + "%");
            }
        }

        sql.append("ORDER BY ID DESC");

        Cursor c = null;

        try {
            c = db.rawQuery(sql.toString(), args.toArray(new String[0]));

            while (c.moveToNext()) {
                CandidateItem item = new CandidateItem();

                item.id = getIntSafe(c, "ID");
                item.tenUngVien = getStringSafe(c, "TENUNGVIEN");
                item.email = hasEmail ? getStringSafe(c, "EMAIL") : "";
                item.fileThongTin = hasFileInfo ? getStringSafe(c, "FILETHONGTIN") : "";
                item.fileBangCap = hasFileDegree ? getStringSafe(c, "FILEBANGCAP") : "";
                item.fileKhac = hasFileOther ? getStringSafe(c, "FILEKHAC") : "";

                item.submittedAt = extractDateFromCandidate(item);

                if (from != null && item.submittedAt != null && item.submittedAt.before(startOfDay(from))) {
                    continue;
                }

                if (to != null && item.submittedAt != null && item.submittedAt.after(endOfDay(to))) {
                    continue;
                }

                candidates.add(item);
            }

        } catch (Exception e) {
            TextView error = makeText(
                    "Lỗi đọc bảng " + candidateTableName + ": " + e.getMessage(),
                    14,
                    RED,
                    true
            );
            error.setGravity(Gravity.CENTER);
            error.setPadding(0, dp(30), 0, dp(30));
            layoutCandidateList.addView(error);
            return;

        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (candidates.isEmpty()) {
            TextView empty = makeText("Không có ứng viên nào.", 14, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            layoutCandidateList.addView(empty);
            return;
        }

        for (CandidateItem item : candidates) {
            layoutCandidateList.addView(makeCandidateCard(item));
        }
    }

    private int getIntSafe(Cursor c, String columnName) {
        try {
            int index = c.getColumnIndex(columnName);

            if (index >= 0 && !c.isNull(index)) {
                return c.getInt(index);
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private String getStringSafe(Cursor c, String columnName) {
        try {
            int index = c.getColumnIndex(columnName);

            if (index >= 0 && !c.isNull(index)) {
                return c.getString(index);
            }

        } catch (Exception ignored) {
        }

        return "";
    }

    private View makeCandidateCard(CandidateItem item) {
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

        card.setOnClickListener(v -> animateClick(card));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox chk = new CheckBox(this);
        chk.setButtonTintList(ColorStateList.valueOf(PRIMARY));
        chk.setChecked(selectedIds.contains(item.id));
        chk.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                selectedIds.add(item.id);
            } else {
                selectedIds.remove(item.id);
            }
        });
        top.addView(chk);

        TextView avatar = makeAvatar(item);
        top.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);

        TextView name = makeText(item.tenUngVien, 17, TEXT, true);
        info.addView(name);

        String emailText = isEmpty(item.email) ? "(Chưa có email)" : item.email;

        TextView meta = makeText(
                "#" + item.id + " • " + emailText,
                12,
                SUB,
                false
        );
        meta.setPadding(0, dp(2), 0, 0);
        info.addView(meta);

        TextView date = makeText(
                "Ngày nộp: " + formatDate(item.submittedAt),
                12,
                PRIMARY,
                true
        );
        date.setPadding(0, dp(4), 0, 0);
        info.addView(date);

        top.addView(info, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        card.addView(top);

        LinearLayout files = new LinearLayout(this);
        files.setOrientation(LinearLayout.HORIZONTAL);
        files.setPadding(0, dp(12), 0, 0);

        if (!isEmpty(item.fileThongTin)) {
            files.addView(makeTag("INFO: " + item.fileThongTin));
        }

        if (!isEmpty(item.fileBangCap)) {
            files.addView(makeTag("DEG: " + item.fileBangCap));
        }

        if (!isEmpty(item.fileKhac)) {
            files.addView(makeTag("OTH: " + item.fileKhac));
        }

        if (files.getChildCount() > 0) {
            HorizontalScrollView fileScroll = new HorizontalScrollView(this);
            fileScroll.setHorizontalScrollBarEnabled(false);
            fileScroll.addView(files);
            card.addView(fileScroll);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(12), 0, 0);

        actions.addView(makeSmallButton("Mời PV", PRIMARY, Color.BLACK, v -> showInterviewDialog(item)));
        actions.addView(makeSmallButton("File", BLUE, Color.WHITE, v -> showFileDialog(item)));
        actions.addView(makeSmallButton("Tải hết", GREEN, Color.WHITE, v -> downloadOneCandidateFiles(item)));
        actions.addView(makeSmallButton("Xóa", RED, Color.WHITE, v -> showDeleteOneDialog(item)));

        HorizontalScrollView actionScroll = new HorizontalScrollView(this);
        actionScroll.setHorizontalScrollBarEnabled(false);
        actionScroll.addView(actions);

        card.addView(actionScroll);

        return card;
    }

    private void showFileDialog(CandidateItem item) {
        Dialog dialog = createStyledDialog("Tài liệu ứng viên");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView candidate = makeText(item.tenUngVien, 16, PRIMARY, true);
        candidate.setPadding(0, 0, 0, dp(10));
        content.addView(candidate);

        content.addView(makeFileDownloadRow("Hồ sơ", item.fileThongTin, item));
        content.addView(makeFileDownloadRow("Bằng cấp", item.fileBangCap, item));
        content.addView(makeFileDownloadRow("Tài liệu khác", item.fileKhac, item));

        LinearLayout actions = makeDialogActions();

        Button close = makeDialogButton("Đóng", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button downloadAll = makeDialogButton("Tải hết", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(close);
        actions.addView(downloadAll);

        content.addView(actions);
        currentDialogRoot.addView(content);

        close.setOnClickListener(v -> dialog.dismiss());

        downloadAll.setOnClickListener(v -> {
            dialog.dismiss();
            downloadOneCandidateFiles(item);
        });

        dialog.show();
    }

    private View makeFileDownloadRow(String label, String fileName, CandidateItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(8));

        TextView tv = makeText(
                label + ": " + (isEmpty(fileName) ? "(Không có)" : fileName),
                14,
                isEmpty(fileName) ? SUB : TEXT,
                false
        );

        row.addView(tv);

        if (!isEmpty(fileName)) {
            Button download = makeSmallButton("⬇ Tải " + label, ORANGE, Color.WHITE, v -> downloadSingleFile(item, fileName));
            row.addView(download);
        }

        return row;
    }

    private void downloadSingleFile(CandidateItem item, String fileName) {
        ActivityLogger.log(db, "DOWNLOAD_SINGLE_FILE", "HR", "Đã tải file: " + fileName + " của " + item.tenUngVien);
        if (isEmpty(fileName)) {
            Toast.makeText(this, "Không có file để tải.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                CandidateFiles.downloadOneFile(
                        CandidateActivity.this,
                        fileName,
                        item.tenUngVien
                );

                runOnUiThread(() -> Toast.makeText(
                        CandidateActivity.this,
                        "Đã tải file: " + fileName,
                        Toast.LENGTH_LONG
                ).show());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        CandidateActivity.this,
                        "Tải file thất bại: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        }).start();
    }

    private void downloadOneCandidateFiles(CandidateItem item) {
        new Thread(() -> {
            try {
                int count = CandidateFiles.downloadAllFilesOfCandidate(
                        CandidateActivity.this,
                        item
                );

                runOnUiThread(() -> {
                    if (count > 0) {
                        Toast.makeText(
                                CandidateActivity.this,
                                "Đã tải " + count + " file của " + item.tenUngVien + ".",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                CandidateActivity.this,
                                "Ứng viên này không có file để tải.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        CandidateActivity.this,
                        "Tải file thất bại: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        }).start();
    }

    private void downloadSelectedFiles() {
        ArrayList<CandidateItem> selected = new ArrayList<>();

        for (CandidateItem item : candidates) {
            if (selectedIds.contains(item.id)) {
                selected.add(item);
            }
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một ứng viên.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDownloadSelectedFiles.setEnabled(false);
        btnDownloadSelectedFiles.setText("Đang tải file...");

        new Thread(() -> {
            try {
                int count = CandidateFiles.downloadAllFilesOfCandidates(
                        CandidateActivity.this,
                        selected
                );

                runOnUiThread(() -> {
                    btnDownloadSelectedFiles.setEnabled(true);
                    btnDownloadSelectedFiles.setText("⬇ Tải file các ứng viên đã chọn");

                    Toast.makeText(
                            CandidateActivity.this,
                            "Đã tải " + count + " file của " + selected.size() + " ứng viên.",
                            Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnDownloadSelectedFiles.setEnabled(true);
                    btnDownloadSelectedFiles.setText("⬇ Tải file các ứng viên đã chọn");

                    Toast.makeText(
                            CandidateActivity.this,
                            "Tải file thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private void showInterviewDialog(CandidateItem item) {
        if (isEmpty(item.email)) {
            Toast.makeText(this, "Ứng viên này chưa có email trong database.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = createStyledDialog("Mời phỏng vấn");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView candidate = makeText(item.tenUngVien + " - " + item.email, 14, TEXT, true);
        candidate.setPadding(0, 0, 0, dp(8));
        content.addView(candidate);

        TextView submittedText = makeText(
                "Ngày nộp hồ sơ: " + formatDate(item.submittedAt),
                13,
                PRIMARY,
                true
        );
        submittedText.setPadding(0, 0, 0, dp(8));
        content.addView(submittedText);

        EditText edtDate = makeDarkInput("Chọn ngày phỏng vấn");
        setupDateInput(edtDate);

        EditText edtTime = makeDarkInput("Chọn giờ phỏng vấn");
        setupDateInput(edtTime);

        EditText edtNote = makeDarkInput("Ghi chú thêm");
        edtNote.setSingleLine(false);
        edtNote.setMinLines(3);
        edtNote.setGravity(Gravity.TOP);

        edtDate.setOnClickListener(v -> showDatePickerWithMinDate(edtDate, item.submittedAt));
        edtTime.setOnClickListener(v -> showTimePicker(edtTime));

        content.addView(makeDialogLabel("Ngày phỏng vấn"));
        content.addView(edtDate);

        content.addView(makeDialogLabel("Giờ phỏng vấn"));
        content.addView(edtTime);

        content.addView(makeDialogLabel("Ghi chú"));
        content.addView(edtNote);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button send = makeDialogButton("Gửi", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(send);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        send.setOnClickListener(v -> {
            String date = edtDate.getText().toString().trim();
            String time = edtTime.getText().toString().trim();
            String note = edtNote.getText().toString().trim();

            if (date.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ngày phỏng vấn.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (time.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn giờ phỏng vấn.", Toast.LENGTH_SHORT).show();
                return;
            }

            Date interviewDate = parseDate(date);

            if (interviewDate == null) {
                Toast.makeText(this, "Ngày phỏng vấn không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (item.submittedAt != null && startOfDay(interviewDate).before(startOfDay(item.submittedAt))) {
                Toast.makeText(
                        this,
                        "Ngày phỏng vấn không được nhỏ hơn ngày nộp hồ sơ.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            send.setEnabled(false);
            send.setText("Đang gửi...");

            new Thread(() -> {
                try {
                    EmailSender.sendInterviewEmail(
                            item.email,
                            item.tenUngVien,
                            date,
                            time,
                            note
                    );

                    runOnUiThread(() -> {
                        Toast.makeText(
                                CandidateActivity.this,
                                "Đã gửi email mời phỏng vấn.",
                                Toast.LENGTH_SHORT
                        ).show();

                        dialog.dismiss();
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        send.setEnabled(true);
                        send.setText("Gửi");

                        Toast.makeText(
                                CandidateActivity.this,
                                "Gửi email thất bại: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            }).start();
        });

        dialog.show();
        ActivityLogger.log(db, "SEND_INTERVIEW_EMAIL", "HR", "Đã gửi email mời phỏng vấn cho " + item.tenUngVien);
    }

    private void setupDateInput(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(true);
        editText.setInputType(InputType.TYPE_NULL);
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        String currentText = target.getText().toString().trim();

        if (!currentText.isEmpty()) {
            Date currentDate = parseDate(currentText);

            if (currentDate != null) {
                calendar.setTime(currentDate);
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String dateText = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    target.setText(dateText);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void showDatePickerWithMinDate(EditText target, Date minDate) {
        Calendar calendar = Calendar.getInstance();

        String currentText = target.getText().toString().trim();

        if (!currentText.isEmpty()) {
            Date currentDate = parseDate(currentText);

            if (currentDate != null) {
                calendar.setTime(currentDate);
            }
        } else if (minDate != null && calendar.getTime().before(startOfDay(minDate))) {
            calendar.setTime(startOfDay(minDate));
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String dateText = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    Date selectedDate = parseDate(dateText);

                    if (selectedDate != null && minDate != null && startOfDay(selectedDate).before(startOfDay(minDate))) {
                        Toast.makeText(
                                this,
                                "Ngày phỏng vấn không được nhỏ hơn ngày nộp hồ sơ.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    target.setText(dateText);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        if (minDate != null) {
            dialog.getDatePicker().setMinDate(startOfDay(minDate).getTime());
        }

        dialog.show();
    }

    private void showTimePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    String time = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            hourOfDay,
                            minute
                    );

                    target.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );

        dialog.show();
    }

    private void showDeleteOneDialog(CandidateItem item) {
        Dialog dialog = createStyledDialog("Xóa ứng viên");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText(
                "Bạn có chắc muốn xóa ứng viên " + item.tenUngVien + "?",
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
            deleteCandidate(item.id);
            selectedIds.remove(item.id);
            dialog.dismiss();
            loadCandidates();
        });

        dialog.show();
        ActivityLogger.log(db, "DELETE_CANDIDATE_ONE", "HR", "Đã xóa ứng viên " + item.tenUngVien);
    }

    private void showExportDeleteDialog() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một ứng viên.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = createStyledDialog("Xác nhận xóa");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView msg = makeText(
                "Bạn sắp tạo PDF tổng hợp và xóa vĩnh viễn "
                        + selectedIds.size()
                        + " ứng viên khỏi hệ thống. Hành động này không thể hoàn tác.",
                14,
                TEXT,
                false
        );
        msg.setPadding(0, 0, 0, dp(12));
        content.addView(msg);

        LinearLayout actions = makeDialogActions();

        Button cancel = makeDialogButton("Hủy", R.drawable.employee_button_dark_bg, Color.WHITE);
        Button confirm = makeDialogButton("Xác nhận", R.drawable.hr_logout_bg, Color.BLACK);

        actions.addView(cancel);
        actions.addView(confirm);

        content.addView(actions);
        currentDialogRoot.addView(content);

        cancel.setOnClickListener(v -> dialog.dismiss());

        confirm.setOnClickListener(v -> {
            exportSelectedToPdfAndDelete();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void exportSelectedToPdfAndDelete() {
        ArrayList<CandidateItem> selected = new ArrayList<>();

        for (CandidateItem item : candidates) {
            if (selectedIds.contains(item.id)) {
                selected.add(item);
            }
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "Chưa có ứng viên nào được chọn.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Candidate_" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date()) + ".pdf";

        try {
            PdfDocument pdf = new PdfDocument();

            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(18);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(12);

            int pageNumber = 1;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            int y = 42;

            canvas.drawText("Candidate summary", 40, y, titlePaint);
            y += 32;

            for (CandidateItem item : selected) {
                if (y > 760) {
                    pdf.finishPage(page);

                    pageNumber++;

                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();

                    y = 42;
                }

                canvas.drawText("Candidate #" + item.id, 40, y, titlePaint);
                y += 22;

                canvas.drawText("Full name: " + safe(item.tenUngVien), 40, y, textPaint);
                y += 18;

                canvas.drawText("Email: " + safe(item.email), 40, y, textPaint);
                y += 18;

                canvas.drawText("Submitted: " + formatDate(item.submittedAt), 40, y, textPaint);
                y += 18;

                canvas.drawText("File - Info: " + safe(item.fileThongTin), 40, y, textPaint);
                y += 18;

                canvas.drawText("File - Degree: " + safe(item.fileBangCap), 40, y, textPaint);
                y += 18;

                canvas.drawText("File - Other: " + safe(item.fileKhac), 40, y, textPaint);
                y += 30;
            }

            pdf.finishPage(page);

            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();

            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");

            Uri uri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download");
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
            }

            if (uri == null) {
                Toast.makeText(this, "Không tạo được file PDF.", Toast.LENGTH_SHORT).show();
                pdf.close();
                return;
            }

            OutputStream os = resolver.openOutputStream(uri);

            if (os == null) {
                Toast.makeText(this, "Không mở được nơi lưu PDF.", Toast.LENGTH_SHORT).show();
                pdf.close();
                return;
            }

            pdf.writeTo(os);
            os.close();
            pdf.close();

            db.beginTransaction();

            try {
                for (CandidateItem item : selected) {
                    db.delete(candidateTableName, "ID = ?", new String[]{String.valueOf(item.id)});
                }

                db.setTransactionSuccessful();

            } finally {
                db.endTransaction();
            }

            selectedIds.clear();

            Toast.makeText(this, "Đã xuất PDF vào Downloads và xóa ứng viên.", Toast.LENGTH_LONG).show();

            loadCandidates();
            ActivityLogger.log(db, "EXPORT_PDF", "HR", "Xuất PDF thành công cho " + selected.size() + " ứng viên");
            ActivityLogger.log(db, "DELETE_CANDIDATE_MULTI", "HR", "Đã xóa " + selected.size() + " ứng viên khỏi hệ thống");

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi export PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        ActivityLogger.log(db, "EXPORT_PDF", "HR", "Xuất PDF thành công cho " + selected.size() + " ứng viên");
    }

    private void deleteCandidate(int id) {
        int rows = db.delete(candidateTableName, "ID = ?", new String[]{String.valueOf(id)});

        if (rows > 0) {
            Toast.makeText(this, "Đã xóa ứng viên.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Không tìm thấy ứng viên.", Toast.LENGTH_SHORT).show();
        }
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
        edt.setInputType(InputType.TYPE_CLASS_TEXT);

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

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(104), dp(44));
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

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );

        lp.setMargins(0, 0, dp(7), 0);

        btn.setLayoutParams(lp);

        return btn;
    }

    private TextView makeAvatar(CandidateItem item) {
        TextView avatar = makeText(getInitial(item.tenUngVien), 18, Color.BLACK, true);

        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.employee_avatar_bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        lp.setMargins(dp(6), 0, 0, 0);

        avatar.setLayoutParams(lp);

        return avatar;
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
            return "UV";
        }

        String[] parts = fullName.trim().split("\\s+");
        String last = parts[parts.length - 1];

        if (last.length() >= 1) {
            return last.substring(0, 1).toUpperCase(Locale.getDefault());
        }

        return "UV";
    }

    private Date extractDateFromCandidate(CandidateItem item) {
        String mainFile = item.fileThongTin;

        if (isEmpty(mainFile)) {
            if (!isEmpty(item.fileBangCap)) {
                mainFile = item.fileBangCap;
            } else if (!isEmpty(item.fileKhac)) {
                mainFile = item.fileKhac;
            }
        }

        return extractDateFromFileName(mainFile);
    }

    private Date extractDateFromFileName(String fileName) {
        if (isEmpty(fileName)) {
            return null;
        }

        try {
            Matcher m = Pattern.compile("\\d{14}|\\d{12}|\\d{10}|\\d{8}").matcher(fileName);

            if (!m.find()) {
                return null;
            }

            String s = m.group();

            if (s.length() >= 14) {
                return new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(s.substring(0, 14));
            }

            if (s.length() >= 12) {
                return new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).parse(s.substring(0, 12));
            }

            if (s.length() >= 10) {
                return new SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).parse(s.substring(0, 10));
            }

            return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(s.substring(0, 8));

        } catch (Exception e) {
            return null;
        }
    }

    private Date parseDate(String s) {
        if (isEmpty(s)) {
            return null;
        }

        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s);
        } catch (Exception e) {
            Toast.makeText(this, "Ngày không hợp lệ: " + s, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private Date startOfDay(Date d) {
        try {
            String s = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(s + " 00:00:00");
        } catch (Exception e) {
            return d;
        }
    }

    private Date endOfDay(Date d) {
        try {
            String s = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(s + " 23:59:59");
        } catch (Exception e) {
            return d;
        }
    }

    private String formatDate(Date d) {
        if (d == null) {
            return "(Không rõ)";
        }

        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty() || s.equalsIgnoreCase("null");
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

    public static class CandidateItem {
        public int id;
        public String tenUngVien;
        public String email;
        public String fileThongTin;
        public String fileBangCap;
        public String fileKhac;
        public Date submittedAt;
    }
}