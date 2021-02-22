package com.example.dashcontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.PatternSyntaxException;

public class CadastrarUsuario extends AppCompatActivity {
    private EditText email, password, passwrodConfirm;
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
        String passConfirm = passwrodConfirm.getText().toString();
        if(mail.isEmpty()){
            email.setError("Digite seu email!");
            return;
        } else
        if(pass.isEmpty()){
            password.setError("Digite sua senha!");
            return;
        } else
        if(mail.isEmpty()){
            passwrodConfirm.setError("Confirme sua senha!");
            return;
        } else
        if(!isValidEmail(mail)){
            email.setError("Email inválido!");
            return;
        }
        progressDialog.setMessage("Por favor, aguarde...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth.createUserWithEmailAndPassword(mail,pass).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    myRef = database.getReference("chaves");
                    myRef.child(firebaseAuth.getCurrentUser().getUid().trim()).child("ativo").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(CadastrarUsuario.this, "Registrado com sucesso",Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(CadastrarUsuario.this, DashWeb.class);
                            startActivity(intent);
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CadastrarUsuario.this, "Não foi possível aprovisionar!",Toast.LENGTH_LONG).show();
                            firebaseAuth.getCurrentUser().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(CadastrarUsuario.this, "Não foi possível registrar, tente novamente!",Toast.LENGTH_LONG).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(CadastrarUsuario.this, "Não foi possível aprovisionar!",Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });

                }else{
                    Toast.makeText(CadastrarUsuario.this, "Erro ao Registrar!",Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            }
        });
    }
    private boolean isValidEmail(String str){
        return(!TextUtils.isEmpty(str) && Patterns.EMAIL_ADDRESS.matcher(str).matches());
    }
}