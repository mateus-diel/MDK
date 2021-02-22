package com.example.dashcontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Login extends AppCompatActivity {
    Button btnLoginLocal, novoUsuario, loginBtn;
    EditText login, senha;
    TextView esqueceuSenha;
    ProgressDialog progressDialog;
    FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginBtn = findViewById(R.id.btnLogin);
        btnLoginLocal = findViewById(R.id.btnLoginLocal);

        login = findViewById(R.id.loginUsuario);
        senha = findViewById(R.id.senhaUsuario);
        novoUsuario = findViewById(R.id.btnCadastrar);
        progressDialog = new ProgressDialog(this);
        firebaseAuth = FirebaseAuth.getInstance();
        esqueceuSenha = findViewById(R.id.esqueciSenha);

        esqueceuSenha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText resetMail = new EditText(v.getContext());
                resetMail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                AlertDialog.Builder passResetDialog = new AlertDialog.Builder(v.getContext());
                passResetDialog.setTitle("Redefinir Senha?");
                passResetDialog.setMessage("Inisira seu email para receber o link de redefinição.");
                passResetDialog.setView(resetMail);
                passResetDialog.setPositiveButton("Redefinir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String mail = resetMail.getText().toString();
                        if(isValidEmail(mail)){
                            firebaseAuth.sendPasswordResetEmail(mail).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(Login.this, "Link enviado para o seu email!",Toast.LENGTH_LONG).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(Login.this, "Não foi possível enviar o link!",Toast.LENGTH_LONG).show();
                                }
                            });
                        }else{
                            Toast.makeText(getApplicationContext(), "Email inválido!",Toast.LENGTH_LONG).show();

                        }
                    }
                });
                passResetDialog.setNegativeButton("Voltar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                passResetDialog.create().show();

            }

        });

        novoUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, CadastrarUsuario.class);
                startActivity(intent);
            }
        });


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
                /*Log.d("login", login.getText().toString());
                Log.d("senha", senha.getText().toString());
                if(login.getText().toString().equals("admin") && senha.getText().toString().equals("1234")){
                    Log.d("intennnt", "nova intent");
                    Intent intent = new Intent(getApplicationContext(), DashLocal.class);
                    startActivity(intent);
                }*/
                Login();

            }
        });


    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    private void Login(){
        String mail = login.getText().toString();
        String pass = senha.getText().toString();
        if(mail.isEmpty()){
            login.setError("Digite seu email!");
            return;
        }else if(pass.isEmpty()){
            senha.setError("Digite sua senha!");
            return;
        }

        if(!isValidEmail(mail)){
            login.setError("Email inválido!");
            return;
        }
        progressDialog.setMessage("Por favor, aguarde...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth.signInWithEmailAndPassword(mail,pass).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    if(firebaseAuth.getCurrentUser().isEmailVerified()){
                        Toast.makeText(Login.this, "Bem vindo!",Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Login.this, DashWeb.class);
                        startActivity(intent);
                        Log.d("Usuário logado", firebaseAuth.getCurrentUser().getEmail());
                        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("email", firebaseAuth.getCurrentUser().getEmail());
                        editor.putString("chave", firebaseAuth.getCurrentUser().getUid());
                        editor.apply();
                    }else{
                        firebaseAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(Login.this, "Email não verificado! Um link foi enviado para a verificação!",Toast.LENGTH_LONG).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(Login.this, "Email não verificado! Não foi possível enviar um email de verificação, contate-nos!",Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }else{
                    Toast.makeText(Login.this, "Erro ao entrar!",Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            }
        });
    }
    private boolean isValidEmail(String str){
        return(!TextUtils.isEmpty(str) && Patterns.EMAIL_ADDRESS.matcher(str).matches());
    }
}