package com.example.dashcontrol;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CadastrarUsuario extends AppCompatActivity {
    private EditText email, password, passwrodConfirm, nome;
    private Button bntCadastrar;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase database;
    private DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_usuario);
        // Write a message to the database
        database = FirebaseDatabase.getInstance();


        firebaseAuth = FirebaseAuth.getInstance();
        email=findViewById(R.id.email);
        password = findViewById(R.id.newUserPassword);
        passwrodConfirm = findViewById(R.id.newUserPasswordConfirm);
        bntCadastrar = findViewById(R.id.newUserButtonCad);
        nome = findViewById(R.id.NomeUsuario);
        progressDialog = new ProgressDialog(this);

        bntCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

    }

    private void register() {
        String mail = email.getText().toString();
        String pass = password.getText().toString();
        String name = nome.getText().toString();
        String passConfirm = passwrodConfirm.getText().toString();

        if(name.isEmpty()){
            nome.setError("Digite seu nome!");
            return;
        }else
        if(mail.isEmpty()){
            email.setError("Digite seu email!");
            return;
        } else
        if(pass.isEmpty()){
            password.setError("Digite sua senha!");
            return;
        } else
        if(passConfirm.isEmpty()){
            passwrodConfirm.setError("Confirme sua senha!");
            return;
        } else if (!isValidEmail(mail)) {
            email.setError("Email inválido!");
            return;
        }
        progressDialog.setMessage("Por favor, aguarde...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth.createUserWithEmailAndPassword(mail,pass).addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                    myRef = database.getReference("chaves");
                    myRef.child(firebaseAuth.getCurrentUser().getUid().trim()).child("ativo").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(name).build();
                            firebaseAuth.getCurrentUser().updateProfile(profileChangeRequest).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    AlertDialog.Builder dial = new AlertDialog.Builder(CadastrarUsuario.this);
                                    dial.setTitle("Aviso");
                                    dial.setCancelable(false);
                                    dial.setMessage("Cadastrado com sucesso!");
                                    dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(CadastrarUsuario.this, Login.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                    dial.create().show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    firebaseAuth.getCurrentUser().delete();AlertDialog.Builder dial = new AlertDialog.Builder(CadastrarUsuario.this);
                                    dial.setTitle("Aviso");
                                    dial.setCancelable(false);
                                    dial.setMessage("Não foi possível registrar seu nome, tente novamente!");
                                    dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                    dial.create().show();}
                            });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            firebaseAuth.getCurrentUser().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    AlertDialog.Builder dial = new AlertDialog.Builder(CadastrarUsuario.this);
                                    dial.setTitle("Aviso");
                                    dial.setCancelable(false);
                                    dial.setMessage("Não foi possível gerar sua inscrição, tente novamente mais tarde!");
                                    dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                    dial.create().show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    AlertDialog.Builder dial = new AlertDialog.Builder(CadastrarUsuario.this);
                                    dial.setTitle("Aviso");
                                    dial.setCancelable(false);
                                    dial.setMessage("Cadastro realizado com sucesso. Entre em contato para ativação do seu cadastro!");
                                    dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(CadastrarUsuario.this, Login.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                    dial.create().show();
                                }
                            });
                        }
                    });
                progressDialog.dismiss();
            }
        });
    }
    private boolean isValidEmail(String str){
        return(!TextUtils.isEmpty(str) && Patterns.EMAIL_ADDRESS.matcher(str).matches());
    }
}