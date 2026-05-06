package com.neith.subjectdemo.auth;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;

public class VerifyOtpActivity extends AppCompatActivity {

    EditText otp1, otp2, otp3, otp4, otp5, otp6, otp7, otp8;
    Button btnVerifyOtp, btnBack;
    TextView txtTitle, txtOtpInfo;

    SQLiteDatabase db;
    EditText[] boxes;
    boolean isFilling = false;

    String mode;
    String username, passHash, email, code, otp, auth, nameAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        db = DB.openDatabase(this);

        mode = getIntent().getStringExtra("MODE");
        if (mode == null) mode = "SIGNUP";

        username = getIntent().getStringExtra("USERNAME");
        passHash = getIntent().getStringExtra("PASS_HASH");
        email = getIntent().getStringExtra("EMAIL");
        code = getIntent().getStringExtra("CODE");
        otp = getIntent().getStringExtra("OTP");
        auth = getIntent().getStringExtra("AUTH");
        nameAuth = getIntent().getStringExtra("NAME_AUTH");

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        otp7 = findViewById(R.id.otp7);
        otp8 = findViewById(R.id.otp8);

        boxes = new EditText[]{otp1, otp2, otp3, otp4, otp5, otp6, otp7, otp8};

        txtTitle = findViewById(R.id.txtTitle);
        txtOtpInfo = findViewById(R.id.txtOtpInfo);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnBack = findViewById(R.id.btnBack);

        btnVerifyOtp.setBackgroundTintList(null);
        btnBack.setBackgroundTintList(null);

        txtTitle.setText(mode.equals("FORGOT") ? "Reset Password" : "Verify Gmail");
        txtOtpInfo.setText("The verification code has been sent to:\n" + email);

        setupOtpBoxes();

        btnVerifyOtp.setOnClickListener(v -> verifyOtp());

        btnBack.setOnClickListener(v -> {
            if (mode.equals("FORGOT")) {
                startActivity(new Intent(this, ForgotPassword.class));
            } else {
                startActivity(new Intent(this, SignUp.class));
            }
            finish();
        });
    }

    private void setupOtpBoxes() {
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;

            boxes[index].setSelectAllOnFocus(true);

            boxes[index].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DEL
                        && boxes[index].getText().toString().isEmpty()
                        && index > 0) {
                    boxes[index - 1].requestFocus();
                    return true;
                }
                return false;
            });

            boxes[index].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isFilling) return;

                    String value = s.toString().trim().toUpperCase();

                    if (value.length() > 1) {
                        fillOtpFrom(index, value);
                        return;
                    }

                    if (value.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                }

                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void fillOtpFrom(int startIndex, String text) {
        isFilling = true;

        String clean = text.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        int charIndex = 0;

        for (int i = startIndex; i < boxes.length && charIndex < clean.length(); i++) {
            boxes[i].setText(String.valueOf(clean.charAt(charIndex)));
            boxes[i].setSelection(1);
            charIndex++;
        }

        int focusIndex = Math.min(startIndex + charIndex, boxes.length - 1);
        boxes[focusIndex].requestFocus();

        isFilling = false;
    }

    private String getOtpInput() {
        StringBuilder result = new StringBuilder();

        for (EditText box : boxes) {
            result.append(box.getText().toString().trim().toUpperCase());
        }

        return result.toString();
    }

    private void verifyOtp() {
        String otpInput = getOtpInput();

        if (otpInput.length() < 8) {
            Toast.makeText(this, "Please enter the full 8-character OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!otpInput.equals(otp)) {
            Toast.makeText(this, "Incorrect OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mode.equals("FORGOT")) {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("AUTH", auth);
            startActivity(intent);
            finish();
        } else {
            registerAccount();
        }
    }

    private void registerAccount() {
        try {
            db.beginTransaction();

            db.execSQL(
                    "INSERT INTO USERS(USERNAME, PASS, AUTH) VALUES (?, ?, ?)",
                    new Object[]{username, passHash, auth}
            );

            db.execSQL(
                    "INSERT INTO CONFIRMAUTH(AUTH, NAMEAUTH, CODE) VALUES (?, ?, ?)",
                    new Object[]{auth, nameAuth, code}
            );

            db.execSQL(
                    "UPDATE THONGTINLIENHE SET X = ? WHERE MANV = ?",
                    new Object[]{email, code}
            );

            db.setTransactionSuccessful();

            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, SignIn.class));
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_LONG).show();

        } finally {
            db.endTransaction();
        }
    }
}