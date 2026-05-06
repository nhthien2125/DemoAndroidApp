package com.neith.subjectdemo.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.auth.SignIn;
import com.neith.subjectdemo.helper.SessionManager;

public class AdminActivity extends AppCompatActivity {

    TextView txtWelcome, txtInfo;
    Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        txtWelcome = findViewById(R.id.txtWelcome);
        txtInfo = findViewById(R.id.txtInfo);
        btnLogout = findViewById(R.id.btnLogout);

        String username = getIntent().getStringExtra("USERNAME");
        String auth = getIntent().getStringExtra("AUTH");

        txtInfo.setText("User: " + username + "\nAUTH: " + auth);

        btnLogout.setOnClickListener(v -> {
            SessionManager.logout(this);

            Intent intent = new Intent(this, SignIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}