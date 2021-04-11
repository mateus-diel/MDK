package com.marcoa.marcoa;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

public class CadastrarUsuario extends AppCompatActivity {
    private EditText email, password, passwrodConfirm, nome, cpfcnpj, celularCli, enderecoCli;
    private Button bntCadastrar;
    private ProgressDialog progressDialog;
    private DatabaseReference myRef;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_usuario);
        // Write a message to the database
        email=findViewById(R.id.email);
        cpfcnpj = findViewById(R.id.cpf_cnpj);
        enderecoCli = findViewById(R.id.enderecoCliente);
        cpfcnpj.addTextChangedListener(Mask.insert(cpfcnpj));

        celularCli = findViewById(R.id.celularCliente);
        celularCli.addTextChangedListener(MaskCel.insert(MaskCel.CELULAR_MASK,celularCli));

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
        String cpf_cnpj = cpfcnpj.getText().toString();
        String celular = celularCli.getText().toString();
        String endereco = enderecoCli.getText().toString();
        String passConfirm = passwrodConfirm.getText().toString();

        if (name.isEmpty()) {
            nome.setError("Digite seu nome!");
            return;
        } else if (cpf_cnpj.isEmpty()) {
            cpfcnpj.setError("Digite seu CPF ou CNPJ!");
            return;
        } else if (celular.isEmpty()) {
            celularCli.setError("Digite seu celular!");
            return;
        }else if (endereco.isEmpty()) {
            enderecoCli.setError("Digite seu endereço!");
            return;
        } else if (!isValidEmail(mail)) {
            email.setError("Email inválido!");
            return;
        }

        else if (pass.length()<8) {
            password.setError("Senha deve ter mais de 8 caracteres!");
            return;
        } else if (passConfirm.isEmpty()) {
            passwrodConfirm.setError("Confirme sua senha!");
            return;
        }else if(!pass.equals(passConfirm)){
            passwrodConfirm.setError("Senhas não conferem!");
            return;
        }
        progressDialog.setMessage("Por favor, aguarde...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);


        requestQueue = Volley.newRequestQueue(getApplicationContext());
        JSONObject params = new JSONObject();
        try {
            params.put("nome", name);
            params.put("cpf_cnpj", cpf_cnpj);
            params.put("email", mail);
            params.put("telefone", celular);
            params.put("endereco", endereco);
            params.put("senha", passConfirm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getResources().getString(R.string.server).concat("api/cliente/add"), params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("sucessso", response.toString());
                try {
                    JSONObject json = new JSONObject(response.toString());
                    Log.d("json array", json.toString());

                    if (json.has("code")) {
                        if (json.getInt("code") == 901) {
                            AlertDialog.Builder dial = new AlertDialog.Builder(CadastrarUsuario.this);
                            dial.setTitle("Aviso");
                            dial.setCancelable(false);
                            dial.setMessage("Houve uma falha ao realizar o cadastro!");
                            dial.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dial.create().show();
                        } else if (json.getInt("code") == 200) {
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


    }
    private boolean isValidEmail(String str){
        return(!TextUtils.isEmpty(str) && Patterns.EMAIL_ADDRESS.matcher(str).matches());
    }
}
