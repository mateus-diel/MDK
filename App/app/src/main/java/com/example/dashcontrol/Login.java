package com.example.dashcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Login extends AppCompatActivity {
    Button loginBtn;
    Button btnLoginLocal;
    EditText login;
    EditText senha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginBtn = findViewById(R.id.btnLogin);
        btnLoginLocal = findViewById(R.id.btnLoginLocal);

        login = findViewById(R.id.loginUsuario);
        senha = findViewById(R.id.senhaUsuario);

        btnLoginLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DashLocal.class);
                startActivity(intent);
            }
        });


        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("login", login.getText().toString());
                Log.d("senha", senha.getText().toString());
                if(login.getText().toString().equals("admin") && senha.getText().toString().equals("1234")){
                    Log.d("intennnt", "nova intent");
                    Intent intent = new Intent(getApplicationContext(), DashLocal.class);
                    startActivity(intent);
                }

            }
        });


    }
}