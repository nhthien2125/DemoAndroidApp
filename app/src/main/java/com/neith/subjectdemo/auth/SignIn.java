package com.neith.subjectdemo.auth;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.Password;
import com.neith.subjectdemo.helper.SessionManager;

public class SignIn extends AppCompatActivity {

    EditText edtUsername, edtPassword;
    Button btnSignIn, btnGoSignUp;
    TextView txtForgotPassword;
    CheckBox chkRemember;
    ImageView imgTogglePassword;
    SQLiteDatabase db;

    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SessionManager.checkLoginAndRedirect(this)) {
            return;
        }

        setContentView(R.layout.activity_sign_in);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnGoSignUp = findViewById(R.id.btnGoSignUp);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        chkRemember = findViewById(R.id.chkRemember);
        imgTogglePassword = findViewById(R.id.imgTogglePassword);

        db = DB.openDatabase(this);

        btnSignIn.setBackgroundTintList(null);
        btnGoSignUp.setBackgroundTintList(null);

        btnSignIn.setOnClickListener(v -> login());

        btnGoSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUp.class));
        });

        txtForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPassword.class));
        });

        imgTogglePassword.setOnClickListener(v -> togglePassword());
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

    private void login() {
        String input = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (input.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập username/email và password", Toast.LENGTH_SHORT).show();
            return;
        }

        String passHash = Password.sha256(password);

        Cursor cursor;

        if (input.contains("@")) {
            cursor = db.rawQuery(
                    "SELECT U.PASS, U.AUTH, T.X " +
                            "FROM THONGTINLIENHE T " +
                            "JOIN CONFIRMAUTH C ON T.MANV = C.CODE " +
                            "JOIN USERS U ON C.AUTH = U.AUTH " +
                            "WHERE T.X = ?",
                    new String[]{input}
            );
        } else {
            cursor = db.rawQuery(
                    "SELECT PASS, AUTH FROM USERS WHERE USERNAME = ?",
                    new String[]{input}
            );
        }

        if (cursor.moveToFirst()) {
            String dbPass = cursor.getString(0);
            String auth = cursor.getString(1);

            String email;

            if (input.contains("@")) {
                email = cursor.getString(2);
            } else {
                Cursor c2 = db.rawQuery(
                        "SELECT X FROM THONGTINLIENHE T " +
                                "JOIN CONFIRMAUTH C ON T.MANV = C.CODE " +
                                "JOIN USERS U ON C.AUTH = U.AUTH " +
                                "WHERE U.USERNAME = ?",
                        new String[]{input}
                );

                email = "";
                if (c2.moveToFirst()) {
                    email = c2.getString(0);
                }
                c2.close();
            }

            Log.d("LOGIN_EMAIL", "Email: " + email);

            if (!passHash.equals(dbPass)) {
                Toast.makeText(this, "Sai password", Toast.LENGTH_SHORT).show();
                cursor.close();
                return;
            }

            if (chkRemember.isChecked()) {
                SessionManager.saveLogin(this, input, auth);
            }

            cursor.close();

            boolean ok = SessionManager.goToArea(this, input, auth);

            if (!ok) {
                Toast.makeText(this, "Không xác định quyền: " + auth, Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
            cursor.close();
        }
    }
}