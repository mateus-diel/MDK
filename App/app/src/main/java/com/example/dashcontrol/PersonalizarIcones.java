package com.example.dashcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class PersonalizarIcones extends AppCompatActivity {
    static GridView gridView;
    GridAdapter adapter;
    static SharedPreferences prefs;
    static int pos;
    private static Context context;


    static AlertDialog show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personalizar_icones);
        gridView = findViewById(R.id.grid_view_personalizar_icone);
        prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        PersonalizarIcones.context = getApplicationContext();


        adapter = new GridAdapter(PersonalizarIcones.this, DashWeb.names,DashWeb.draw);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pos = position;
                AlertDialog.Builder icones = new AlertDialog.Builder(PersonalizarIcones.this);
                icones.setTitle("Selecione um icone");
                GridLayout grid = new GridLayout(PersonalizarIcones.this);
                grid.setColumnCount(5);
                Field[] drawablesFields = com.example.dashcontrol.R.drawable.class.getFields();
                ImageView img;

                for (Field field : drawablesFields) {
                    try {
                        Log.i("LOG_TAG", "com.your.project.R.drawable." + field.getName());
                        if(field.getName().contains("iconuserselect")) {
                            img = new ImageView(PersonalizarIcones.this);
                            img.setImageResource(field.getInt(null));
                            img.setColorFilter(ContextCompat.getColor(context, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
                            img.setTag(field.getName());
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 100);
                            layoutParams.setMargins(10, 10, 10, 10);
                            img.setLayoutParams(layoutParams);
                            img.setOnClickListener(PersonalizarIcones::onClicIconSelect);
                            grid.addView(img);
                            //img.requestLayout();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                icones.setView(grid);
                show = icones.show();
            }
        });
    }


    private static void onClicIconSelect(View view) {
        Log.d("Cliqueiiii", "eu sei");
        Log.d("tag da viewww","view tag ".concat(view.getTag().toString()));
        Field[] drawablesFields = com.example.dashcontrol.R.drawable.class.getFields();

        for (Field field : drawablesFields) {
            try {
                Log.i("LOG_TAG", "my drawww." + field.getName());
                if (field.getName().contains(view.getTag().toString())) {
                    Log.d("É esse aquii", field.getName());

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(prefs.getString("email","null").concat(DashWeb.names.get(pos).concat("/IconUser")), field.getName());
                    editor.apply();
                    Drawable unwrappedDrawable = AppCompatResources.getDrawable(context, field.getInt(null));
                    Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
                    DrawableCompat.setTint(wrappedDrawable, 0xFF01579B);
                    DashWeb.draw.set(pos,wrappedDrawable);
                    gridView.invalidateViews();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        show.dismiss();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        DashWeb.gridView.invalidateViews();
    }
}