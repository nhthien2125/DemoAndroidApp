package com.neith.subjectdemo.auth;

import android.app.Dialog;
import android.content.ContentValues;
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
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.EmailSender;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class CandidateSend extends AppCompatActivity {

    SQLiteDatabase db;

    EditText edtCandidateName, edtCandidateEmail;
    TextView txtInfoFileName, txtDegreeFileName, txtOtherFileName;
    Button btnChooseInfo, btnChooseDegree, btnChooseOther, btnSubmitCandidate, btnBackSignIn;

    Uri infoUri, degreeUri, otherUri;
    String infoOriginalName = "", degreeOriginalName = "", otherOriginalName = "";

    int choosingType = 0;

    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int RED = Color.parseColor("#EF4444");

    ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate_send);

        db = DB.openDatabase(this);
        ensureCandidateEmailColumn();

        edtCandidateName = findViewById(R.id.edtCandidateName);
        edtCandidateEmail = findViewById(R.id.edtCandidateEmail);

        txtInfoFileName = findViewById(R.id.txtInfoFileName);
        txtDegreeFileName = findViewById(R.id.txtDegreeFileName);
        txtOtherFileName = findViewById(R.id.txtOtherFileName);

        btnChooseInfo = findViewById(R.id.btnChooseInfo);
        btnChooseDegree = findViewById(R.id.btnChooseDegree);
        btnChooseOther = findViewById(R.id.btnChooseOther);
        btnSubmitCandidate = findViewById(R.id.btnSubmitCandidate);
        btnBackSignIn = findViewById(R.id.btnBackSignIn);

        btnChooseInfo.setBackgroundTintList(null);
        btnChooseDegree.setBackgroundTintList(null);
        btnChooseOther.setBackgroundTintList(null);
        btnSubmitCandidate.setBackgroundTintList(null);
        btnBackSignIn.setBackgroundTintList(null);

        edtCandidateName.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        edtCandidateName.setSingleLine(true);

        edtCandidateEmail.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        edtCandidateEmail.setSingleLine(true);

        setupFilePicker();

        btnChooseInfo.setOnClickListener(v -> openFilePicker(1));
        btnChooseDegree.setOnClickListener(v -> openFilePicker(2));
        btnChooseOther.setOnClickListener(v -> openFilePicker(3));

        btnSubmitCandidate.setOnClickListener(v -> submitCandidate());

        btnBackSignIn.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }

                    Uri uri = result.getData().getData();

                    if (uri == null) {
                        return;
                    }

                    String fileName = getFileName(uri);

                    if (choosingType == 1) {
                        if (!isValidExt(fileName, new String[]{".pdf", ".doc", ".docx", ".jpg", ".jpeg", ".png"})) {
                            showToast("File thông tin cá nhân không hợp lệ. Chỉ nhận PDF, DOC, DOCX, JPG, JPEG, PNG.");
                            return;
                        }

                        infoUri = uri;
                        infoOriginalName = fileName;
                        txtInfoFileName.setText(fileName);
                        txtInfoFileName.setTextColor(TEXT);

                    } else if (choosingType == 2) {
                        if (!isValidExt(fileName, new String[]{".pdf", ".jpg", ".jpeg", ".png"})) {
                            showToast("File bằng cấp không hợp lệ. Chỉ nhận PDF, JPG, JPEG, PNG.");
                            return;
                        }

                        degreeUri = uri;
                        degreeOriginalName = fileName;
                        txtDegreeFileName.setText(fileName);
                        txtDegreeFileName.setTextColor(TEXT);

                    } else if (choosingType == 3) {
                        if (!isValidExt(fileName, new String[]{".pdf", ".doc", ".docx", ".zip"})) {
                            showToast("File khác không hợp lệ. Chỉ nhận PDF, DOC, DOCX, ZIP.");
                            return;
                        }

                        otherUri = uri;
                        otherOriginalName = fileName;
                        txtOtherFileName.setText(fileName);
                        txtOtherFileName.setTextColor(TEXT);
                    }
                }
        );
    }

    private void openFilePicker(int type) {
        choosingType = type;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        if (type == 1) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "image/jpeg",
                    "image/png"
            });
        } else if (type == 2) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/pdf",
                    "image/jpeg",
                    "image/png"
            });
        } else {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/zip"
            });
        }

        filePickerLauncher.launch(intent);
    }

    private void submitCandidate() {
        String name = edtCandidateName.getText().toString().trim();
        String email = edtCandidateEmail.getText().toString().trim();

        if (name.isEmpty()) {
            showToast("Vui lòng nhập họ tên.");
            edtCandidateName.requestFocus();
            return;
        }

        if (name.length() > 100) {
            showToast("Họ tên không được vượt quá 100 ký tự.");
            edtCandidateName.requestFocus();
            return;
        }

        if (!isValidVietnameseName(name)) {
            showToast("Họ tên chỉ được chứa chữ cái tiếng Việt và khoảng trắng.");
            edtCandidateName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            showToast("Vui lòng nhập email.");
            edtCandidateEmail.requestFocus();
            return;
        }

        if (email.length() > 150) {
            showToast("Email không được vượt quá 150 ký tự.");
            edtCandidateEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            showToast("Email không hợp lệ. Ví dụ đúng: you@example.com");
            edtCandidateEmail.requestFocus();
            return;
        }

        if (infoUri == null) {
            showToast("Vui lòng chọn file thông tin cá nhân.");
            return;
        }

        if (degreeUri == null) {
            showToast("Vui lòng chọn file bằng cấp.");
            return;
        }

        btnSubmitCandidate.setEnabled(false);
        btnSubmitCandidate.setText("Đang nộp hồ sơ...");

        new Thread(() -> {
            File infoFile;
            File degreeFile;
            File otherFile = null;

            try {
                Date now = new Date();
                String folderName = buildCandidateFolderName(name, now);
                File candidateFolder = new File(getExternalFilesDir("CandidateFiles"), folderName);

                if (!candidateFolder.exists()) {
                    boolean created = candidateFolder.mkdirs();

                    if (!created) {
                        throw new Exception("Không tạo được thư mục lưu hồ sơ.");
                    }
                }

                infoFile = copyCandidateFile(infoUri, candidateFolder, name, "ThongTin", now, infoOriginalName);
                degreeFile = copyCandidateFile(degreeUri, candidateFolder, name, "BangCap", now, degreeOriginalName);

                if (otherUri != null) {
                    otherFile = copyCandidateFile(otherUri, candidateFolder, name, "Khac", now, otherOriginalName);
                }

                long insertedId = insertCandidate(
                        name,
                        email,
                        infoFile.getName(),
                        degreeFile.getName(),
                        otherFile == null ? null : otherFile.getName()
                );

                if (insertedId == -1) {
                    throw new Exception("Không lưu được hồ sơ vào database.");
                }

                try {
                    EmailSender.sendApplicationReceivedEmail(
                            email,
                            name,
                            infoFile.getName(),
                            degreeFile.getName(),
                            otherFile == null ? "" : otherFile.getName(),
                            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(now),
                            infoFile,
                            degreeFile,
                            otherFile
                    );
                } catch (Exception mailEx) {
                    runOnUiThread(() -> Toast.makeText(
                            CandidateSend.this,
                            "Đã lưu hồ sơ nhưng gửi email xác nhận thất bại.",
                            Toast.LENGTH_LONG
                    ).show());
                }

                runOnUiThread(() -> {
                    btnSubmitCandidate.setEnabled(true);
                    btnSubmitCandidate.setText("Submit your file");

                    showSuccessDialog();

                    edtCandidateName.setText("");
                    edtCandidateEmail.setText("");

                    infoUri = null;
                    degreeUri = null;
                    otherUri = null;

                    infoOriginalName = "";
                    degreeOriginalName = "";
                    otherOriginalName = "";

                    txtInfoFileName.setText("PDF, DOC, DOCX, JPG, PNG...");
                    txtDegreeFileName.setText("PDF, JPG, JPEG, PNG...");
                    txtOtherFileName.setText("PDF, DOC, DOCX, ZIP...");

                    txtInfoFileName.setTextColor(SUB);
                    txtDegreeFileName.setTextColor(SUB);
                    txtOtherFileName.setTextColor(SUB);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSubmitCandidate.setEnabled(true);
                    btnSubmitCandidate.setText("Submit your file");

                    Toast.makeText(
                            CandidateSend.this,
                            "Nộp hồ sơ thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private void ensureCandidateEmailColumn() {
        try {
            db.execSQL("ALTER TABLE HOSOVIECLAM ADD COLUMN EMAIL TEXT");
        } catch (Exception ignored) {
        }
    }

    private long insertCandidate(
            String name,
            String email,
            String fileInfo,
            String fileDegree,
            String fileOther
    ) {
        ContentValues values = new ContentValues();

        values.put("TENUNGVIEN", name);
        values.put("EMAIL", email);
        values.put("FILETHONGTIN", fileInfo);
        values.put("FILEBANGCAP", fileDegree);

        if (fileOther == null || fileOther.trim().isEmpty()) {
            values.putNull("FILEKHAC");
        } else {
            values.put("FILEKHAC", fileOther);
        }

        return db.insert("HOSOVIECLAM", null, values);
    }

    private File copyCandidateFile(
            Uri uri,
            File folder,
            String candidateName,
            String type,
            Date now,
            String originalName
    ) throws Exception {
        String ext = getExtension(originalName);
        String fileName = buildNormalizedFileName(candidateName, type, now, ext);
        File outFile = new File(folder, fileName);

        InputStream inputStream = getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            throw new Exception("Không đọc được file: " + originalName);
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

        return outFile;
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

        return result == null ? "file" : result;
    }

    private boolean isValidExt(String fileName, String[] exts) {
        String ext = getExtension(fileName).toLowerCase(Locale.getDefault());

        for (String item : exts) {
            if (ext.equals(item.toLowerCase(Locale.getDefault()))) {
                return true;
            }
        }

        return false;
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

    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }

        String value = email.trim();

        if (value.isEmpty()) {
            return false;
        }

        if (value.contains(" ")) {
            return false;
        }

        if (value.length() > 150) {
            return false;
        }

        return Patterns.EMAIL_ADDRESS.matcher(value).matches();
    }

    private String buildCandidateFolderName(String candidateName, Date now) {
        String time = new SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).format(now);
        String name = removeDiacritics(candidateName)
                .replaceAll("\\s+", "")
                .replaceAll("[^a-zA-Z0-9]", "");

        if (name.isEmpty()) {
            name = "Candidate";
        }

        return time + "_" + name;
    }

    private String buildNormalizedFileName(String candidateName, String type, Date now, String ext) {
        String name = removeDiacritics(candidateName)
                .toLowerCase(Locale.getDefault())
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9]", "");

        if (name.isEmpty()) {
            name = "candidate";
        }

        String time = new SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).format(now);

        if (ext == null || ext.trim().isEmpty()) {
            ext = ".pdf";
        }

        return name + "_" + type + "_" + time + ext.toLowerCase(Locale.getDefault());
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

    private void showSuccessDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(20));
        root.setBackgroundResource(R.drawable.employee_dialog_bg);

        TextView title = makeText("Nộp hồ sơ thành công", 20, PRIMARY, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView msg = makeText(
                "Hồ sơ của bạn đã được gửi. TBT Center sẽ liên hệ lại sau khi xem xét.",
                14,
                TEXT,
                false
        );
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, dp(12), 0, dp(14));
        root.addView(msg);

        Button ok = new Button(this);
        ok.setText("OK");
        ok.setAllCaps(false);
        ok.setTextColor(Color.BLACK);
        ok.setTypeface(null, Typeface.BOLD);
        ok.setBackgroundTintList(ColorStateList.valueOf(PRIMARY));

        root.addView(ok, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
        ));

        dialog.setContentView(root);

        Window w = dialog.getWindow();

        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();

            if (window != null) {
                window.setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.90f),
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        ok.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}