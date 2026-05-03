package com.neith.subjectdemo.auth;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.Password;

public class ResetPasswordActivity extends AppCompatActivity {

    EditText edtNewPassword, edtReNewPassword;
    Button btnResetPassword;

    SQLiteDatabase db;
    String auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        db = DB.openDatabase(this);
        auth = getIntent().getStringExtra("AUTH");

        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtReNewPassword = findViewById(R.id.edtReNewPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnResetPassword.setBackgroundTintList(null);

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPass = edtNewPassword.getText().toString().trim();
        String reNewPass = edtReNewPassword.getText().toString().trim();

        if (newPass.isEmpty() || reNewPass.isEmpty()) {
            Toast.makeText(this, "Please enter full password information", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(reNewPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        String passHash = Password.sha256(newPass);

        try {
            db.execSQL(
                    "UPDATE USERS SET PASS = ? WHERE AUTH = ?",
                    new Object[]{passHash, auth}
            );

            Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, SignIn.class));
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Reset failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}