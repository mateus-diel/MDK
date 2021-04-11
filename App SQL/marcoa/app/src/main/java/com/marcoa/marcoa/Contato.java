package com.marcoa.marcoa;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class Contato extends AppCompatActivity {

    Button enviar;
    EditText corpo;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contato);
        enviar = findViewById(R.id.btnEnviarEmail);
        corpo = findViewById(R.id.textContent);
        prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String subject = "MarcoA Piso Térmico Aquecido";
                String body = corpo.getText().toString();
                final String destinatario = "suporte@gigabyte.inf.br";
                body= body+"\n\n---------------\nID de requisição:";
                body = body +prefs.getString("chave","10314");
                Log.d("body", body.toString());
                Log.d("pref", prefs.getString("chave","10314"));

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("*/*");
                intent.setData(Uri.parse("mailto:"+destinatario)); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, destinatario);
                intent.putExtra(Intent.EXTRA_TEXT, body);
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }

        });
    }
}