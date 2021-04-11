package com.marcoa.marcoa;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class PersonalizarIcones extends AppCompatActivity {
    static GridView gridViewP;
    static SharedPreferences prefs;
    static int pos;
    private static Context context;


    static AlertDialog show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personalizar_icones);
        gridViewP = findViewById(R.id.grid_view_personalizar_icone);
        prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        PersonalizarIcones.context = getApplicationContext();
        gridViewP.setAdapter(DashWeb.adapter);
        String nameForIcons;
        if(prefs.getBoolean("residencial",true)){
            nameForIcons = "iconuserselect";
        }else{
            nameForIcons = "pigiconuser";
        }
        gridViewP.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pos = position;
                AlertDialog.Builder icones = new AlertDialog.Builder(PersonalizarIcones.this);
                icones.setTitle("Selecione um icone");
                GridLayout grid = new GridLayout(PersonalizarIcones.this);
                grid.setColumnCount(5);
                GridLayout.LayoutParams param =new GridLayout.LayoutParams();
                param.height = GridLayout.LayoutParams.WRAP_CONTENT;
                param.width = GridLayout.LayoutParams.WRAP_CONTENT;
                param.rightMargin = 5;
                param.topMargin = 5;
                param.setGravity(Gravity.CENTER);
                grid.setLayoutParams(param);
                Field[] drawablesFields = R.drawable.class.getFields();
                ImageView img;

                for (Field field : drawablesFields) {
                    try {
                        if(field.getName().contains(nameForIcons)) {
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
        gridViewP.invalidate();
    }


    private static void onClicIconSelect(View view) {
        Log.d("Cliqueiiii", "eu sei");
        Log.d("tag da viewww","view tag ".concat(view.getTag().toString()));
        Field[] drawablesFields = R.drawable.class.getFields();

        for (Field field : drawablesFields) {
            try {
                if (field.getName().contains(view.getTag().toString())) {

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(prefs.getString("email","null").concat(DashWeb.names.get(pos).toLowerCase().concat("/IconUser")), field.getName());
                    Log.d("salvei para",prefs.getString("email","null").concat(DashWeb.names.get(pos).toLowerCase().concat("/IconUser")));
                    Log.d("o que",field.getName());
                    editor.apply();
                    Drawable unwrappedDrawable = AppCompatResources.getDrawable(context, field.getInt(null));
                    Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
                    DrawableCompat.setTint(wrappedDrawable, 0xFFF58634);
                    DashWeb.draw.set(pos,wrappedDrawable);
                    gridViewP.invalidateViews();
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
        //DashWeb.gridView.invalidateViews();
        Intent a = new Intent(this, DashWeb.class);
        startActivity(a);
        finish();
    }
}