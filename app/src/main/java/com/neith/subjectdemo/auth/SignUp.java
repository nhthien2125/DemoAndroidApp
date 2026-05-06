package com.neith.subjectdemo.auth;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.EmailSender;
import com.neith.subjectdemo.helper.OTP;
import com.neith.subjectdemo.helper.Password;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUp extends AppCompatActivity {

    EditText edtUsername, edtPassword, edtRePassword, edtEmail, edtCode;
    Button btnSignUp;
    TextView txtGoSignIn;
    ImageView imgTogglePassword, imgToggleRePassword;

    SQLiteDatabase db;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    boolean isPasswordVisible = false;
    boolean isRePasswordVisible = false;

    final String AUTH_DEFAULT = "EM";
    final String NAME_AUTH_DEFAULT = "Nhân viên";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        db = DB.openDatabase(this);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtRePassword = findViewById(R.id.edtRePassword);
        edtEmail = findViewById(R.id.edtEmail);
        edtCode = findViewById(R.id.edtCode);

        btnSignUp = findViewById(R.id.btnSignUp);
        txtGoSignIn = findViewById(R.id.txtGoSignIn);

        imgTogglePassword = findViewById(R.id.imgTogglePassword);
        imgToggleRePassword = findViewById(R.id.imgToggleRePassword);

        btnSignUp.setBackgroundTintList(null);

        btnSignUp.setOnClickListener(v -> prepareRegister());

        txtGoSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, SignIn.class));
            finish();
        });

        imgTogglePassword.setOnClickListener(v -> togglePassword());
        imgToggleRePassword.setOnClickListener(v -> toggleRePassword());
    }

    private void togglePassword() {
        if (isPasswordVisible) {
            edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            imgTogglePassword.setImageResource(R.drawable.ic_eye_off);
            isPasswordVisible = false;
        } else {
            edtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            imgTogglePassword.setImageResource(R.drawable.ic_eye);
            isPasswordVisible = true;
        }

        edtPassword.setSelection(edtPassword.getText().length());
    }

    private void toggleRePassword() {
        if (isRePasswordVisible) {
            edtRePassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            imgToggleRePassword.setImageResource(R.drawable.ic_eye_off);
            isRePasswordVisible = false;
        } else {
            edtRePassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            imgToggleRePassword.setImageResource(R.drawable.ic_eye);
            isRePasswordVisible = true;
        }

        edtRePassword.setSelection(edtRePassword.getText().length());
    }

    private void prepareRegister() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String rePassword = edtRePassword.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String code = edtCode.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || rePassword.isEmpty()
                || email.isEmpty() || code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(rePassword)) {
            Toast.makeText(this, "Password nhập lại không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isUsernameExists(username)) {
            Toast.makeText(this, "Username đã tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEmailExists(email)) {
            Toast.makeText(this, "Gmail này đã được đăng ký", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCodeExistsInConfirmAuth(code)) {
            Toast.makeText(this, "CODE này đã được đăng ký", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEmployeeCodeExists(code)) {
            Toast.makeText(this, "Mã nhân viên không tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }

        String otp = OTP.generateOTP8();
        String passHash = Password.sha256(password);

        btnSignUp.setEnabled(false);
        Toast.makeText(this, "Đang gửi OTP đến Gmail...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                EmailSender.sendOTP(email, otp);

                runOnUiThread(() -> {
                    btnSignUp.setEnabled(true);

                    Intent intent = new Intent(this, VerifyOtpActivity.class);
                    intent.putExtra("USERNAME", username);
                    intent.putExtra("PASS_HASH", passHash);
                    intent.putExtra("EMAIL", email);
                    intent.putExtra("CODE", code);
                    intent.putExtra("OTP", otp);
                    intent.putExtra("AUTH", AUTH_DEFAULT);
                    intent.putExtra("NAME_AUTH", NAME_AUTH_DEFAULT);

                    startActivity(intent);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSignUp.setEnabled(true);
                    Toast.makeText(this, "Gửi OTP lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean isUsernameExists(String username) {
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM USERS WHERE USERNAME = ?",
                new String[]{username}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private boolean isEmailExists(String email) {
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM THONGTINLIENHE WHERE X = ?",
                new String[]{email}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private boolean isCodeExistsInConfirmAuth(String code) {
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM CONFIRMAUTH WHERE CODE = ?",
                new String[]{code}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private boolean isEmployeeCodeExists(String code) {
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM NHANVIEN WHERE MANV = ?",
                new String[]{code}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }
}