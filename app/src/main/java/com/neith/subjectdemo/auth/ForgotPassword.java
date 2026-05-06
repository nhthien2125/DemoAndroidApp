package com.neith.subjectdemo.auth;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.EmailSender;
import com.neith.subjectdemo.helper.OTP;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForgotPassword extends AppCompatActivity {

    EditText edtInput;
    Button btnSendOtp;
    TextView txtBackSignIn;

    SQLiteDatabase db;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    String foundUsername = "";
    String foundAuth = "";
    String foundEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = DB.openDatabase(this);

        edtInput = findViewById(R.id.edtInput);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        txtBackSignIn = findViewById(R.id.txtBackSignIn);

        btnSendOtp.setBackgroundTintList(null);

        btnSendOtp.setOnClickListener(v -> checkAndSendOtp());

        txtBackSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, SignIn.class));
            finish();
        });
    }

    private void checkAndSendOtp() {
        String input = edtInput.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter username or Gmail", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean found;

        if (input.contains("@")) {
            found = findByEmail(input);
        } else {
            found = findByUsername(input);
        }

        if (!found || foundEmail.isEmpty()) {
            Toast.makeText(this, "Account not found or Gmail is not linked", Toast.LENGTH_SHORT).show();
            return;
        }

        String otp = OTP.generateOTP8();

        btnSendOtp.setEnabled(false);
        Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                EmailSender.sendOTP(foundEmail, otp);

                runOnUiThread(() -> {
                    btnSendOtp.setEnabled(true);

                    Intent intent = new Intent(this, VerifyOtpActivity.class);
                    intent.putExtra("MODE", "FORGOT");
                    intent.putExtra("USERNAME", foundUsername);
                    intent.putExtra("AUTH", foundAuth);
                    intent.putExtra("EMAIL", foundEmail);
                    intent.putExtra("OTP", otp);

                    startActivity(intent);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSendOtp.setEnabled(true);
                    Toast.makeText(this, "Send OTP failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean findByEmail(String email) {
        Cursor cursor = db.rawQuery(
                "SELECT U.USERNAME, U.AUTH, T.X " +
                        "FROM THONGTINLIENHE T " +
                        "JOIN CONFIRMAUTH C ON T.MANV = C.CODE " +
                        "JOIN USERS U ON C.AUTH = U.AUTH " +
                        "WHERE T.X = ?",
                new String[]{email}
        );

        boolean found = false;

        if (cursor.moveToFirst()) {
            foundUsername = cursor.getString(0);
            foundAuth = cursor.getString(1);
            foundEmail = cursor.getString(2);
            found = true;
        }

        cursor.close();
        return found;
    }

    private boolean findByUsername(String username) {
        Cursor cursor = db.rawQuery(
                "SELECT U.USERNAME, U.AUTH, T.X " +
                        "FROM USERS U " +
                        "JOIN CONFIRMAUTH C ON U.AUTH = C.AUTH " +
                        "JOIN THONGTINLIENHE T ON C.CODE = T.MANV " +
                        "WHERE U.USERNAME = ?",
                new String[]{username}
        );

        boolean found = false;

        if (cursor.moveToFirst()) {
            foundUsername = cursor.getString(0);
            foundAuth = cursor.getString(1);
            foundEmail = cursor.getString(2);
            found = true;
        }

        cursor.close();
        return found;
    }
}