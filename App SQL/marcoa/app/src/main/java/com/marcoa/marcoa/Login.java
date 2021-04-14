package com.marcoa.marcoa;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

public class Login extends AppCompatActivity {
    Button btnLoginLocal, novoUsuario, loginBtn;
    EditText login, senha;
    TextView esqueceuSenha, manterCon;
    ProgressDialog progressDialog;
    FirebaseAuth firebaseAuth;
    CheckBox  check;
    private RequestQueue requestQueue;
    SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        loginBtn = findViewById(R.id.btnLogin);
        btnLoginLocal = findViewById(R.id.btnLoginLocal);

        manterCon = findViewById(R.id.materConectado);

        login = findViewById(R.id.loginUsuario);
        senha = findViewById(R.id.senhaUsuario);
        novoUsuario = findViewById(R.id.btnCadastrar);
        progressDialog = new ProgressDialog(this);
        firebaseAuth = FirebaseAuth.getInstance();
        esqueceuSenha = findViewById(R.id.esqueciSenha);
        check = findViewById(R.id.checkBoxConectado);

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
                Intent intent = new Intent(Login.this, DashLocal.class);
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

        manterCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(check.isChecked()){
                    check.setChecked(false);
                }else{
                    check.setChecked(true);
                }
            }
        });
        if(prefs.getBoolean("autoLogin",false)){
            check.setChecked(true);
            login.setText(prefs.getString("email","null"));
            senha.setText(prefs.getString("senha","null"));
            Login();

        }

        if(!prefs.getString("email","null").equals("null")||!prefs.getString("senha","null").equals("null")){
            login.setText(prefs.getString("email","null"));
            senha.setText(prefs.getString("senha","null"));
        }


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









        requestQueue = Volley.newRequestQueue(getApplicationContext());
        JSONObject params = new JSONObject();
        try {
            params.put("email", mail);
            params.put("senha", pass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.server).concat("api/login"), params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("sucessso", response.toString());
                try {
                    AlertDialog.Builder dial = new AlertDialog.Builder(Login.this);
                    dial.setTitle("Aviso");
                    dial.setCancelable(false);
                    JSONObject json = new JSONObject(response.toString());
                    Log.d("json array", json.toString());

                    if (json.has("code")) {
                        if (json.getInt("code") == 900) {
                            dial.setMessage("Houve uma falha ao entrar, tente novamente mais tarde!\nCódigo: ".concat(Integer.toString(json.getInt("code"))));
                            dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dial.create().show();
                        } else if (json.getInt("code") == 200) {
                            if(json.getInt("ativo")==1){
                                Toast.makeText(Login.this, "Bem vindo!",Toast.LENGTH_LONG).show();
                                Log.d("Usuário logado", json.getString("usuario"));
                                Log.d("UUID logado", json.getString("uuid"));
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("email", json.getString("usuario"));
                                editor.putString("senha", pass);
                                editor.putString("chave", json.getString("uuid"));
                                editor.putString("chave_cliente", json.getString("uuid_cliente"));
                                if(check.isChecked()){
                                    editor.putBoolean("autoLogin", true);
                                }else{
                                    editor.putBoolean("autoLogin", false);
                                }
                                editor.apply();
                                Intent intent = new Intent(Login.this, DashWeb.class);
                                startActivity(intent);
                            }else{
                                dial.setMessage("Seu sistema não está ativo! Para maiores informações entre em contato.");
                                dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                dial.create().show();
                            }
                        } else if (json.getInt("code") == 902) {
                            dial.setMessage("Usuário e/ou senha incorretos. Tente novamente.");
                            dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dial.create().show();
                        }

                    }

                    //loadingDialog.dimissDialog();


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                progressDialog.dismiss();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("erroooo", error.getMessage());
                progressDialog.dismiss();

            }
        });

        requestQueue.add(jsonObjectRequest);












/*


        firebaseAuth.signInWithEmailAndPassword(mail,pass).addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                    if(firebaseAuth.getCurrentUser().isEmailVerified()){
                        Toast.makeText(Login.this, "Bem vindo!",Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Login.this, DashWeb.class);
                        startActivity(intent);
                        Log.d("Usuário logado", firebaseAuth.getCurrentUser().getEmail());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("email", firebaseAuth.getCurrentUser().getEmail());
                        editor.putString("senha", pass);
                        editor.putString("chave", firebaseAuth.getCurrentUser().getUid());
                        if(check.isChecked()){
                            editor.putBoolean("autoLogin", true);
                        }else{
                            editor.putBoolean("autoLogin", false);
                        }
                        editor.apply();
                }else{
                        firebaseAuth.getCurrentUser().sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                AlertDialog.Builder dial = new AlertDialog.Builder(Login.this);
                                dial.setTitle("Aviso");
                                dial.setCancelable(true);
                                dial.setMessage("Seu email ainda não foi verificado, verifique sua caixa de entrada e confirme seu endereço email!");
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
                                AlertDialog.Builder dial = new AlertDialog.Builder(Login.this);
                                dial.setTitle("Aviso");
                                dial.setCancelable(true);
                                dial.setMessage("Houve muitas tentativas de acesso, aguarde alguns instantes e tente novamente!");
                                dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                dial.create().show();Log.d("on failure send email,", e.getMessage());
                            }
                        });
                    }

                progressDialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Erro failue", e.getMessage());
                Log.d("Erro localized", e.getLocalizedMessage());
                progressDialog.dismiss();
                if(e.getMessage().contains("The password is invalid or the user does not have a password")){
                    AlertDialog.Builder dial = new AlertDialog.Builder(Login.this);
                    dial.setTitle("Aviso");
                    dial.setCancelable(true);
                    dial.setMessage("Usuário e/ou senha inválidos!");
                    dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dial.create().show();
                }else if(e.getMessage().contains("We have blocked all requests from this device due to unusual activity")) {
                    AlertDialog.Builder dial = new AlertDialog.Builder(Login.this);
                    dial.setTitle("Aviso");
                    dial.setCancelable(true);
                    dial.setMessage("Sua conta foi bloqueada temporariamente. Redefina sua senha ou tente novamente dentro de alguns minutos!");
                    dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dial.create().show();
                }
            }
        });*/
    }
    private boolean isValidEmail(String str){
        return(!TextUtils.isEmpty(str) && Patterns.EMAIL_ADDRESS.matcher(str).matches());
    }
}