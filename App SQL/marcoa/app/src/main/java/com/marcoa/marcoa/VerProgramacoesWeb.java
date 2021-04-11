package com.marcoa.marcoa;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerProgramacoesWeb extends AppCompatActivity {
    List<String>   grupos;
    List <String> filhos;
    Map<String, List<String>> colecao;
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    private FirebaseDatabase database;
    SharedPreferences prefs;
    Intent intent;
    TextView nomeDisp;
    String dispositivo;
    AlertDialog.Builder dialog;
    AlertDialog show;
    ProgressDialog progressDialog;
    static int lastExpadedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_programacoes_web);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_bar, null);
        actionBar.setCustomView(view);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Por favor, aguarde...");

        progressDialog.setCanceledOnTouchOutside(false);
         dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Aviso");
        dialog.setCancelable(false);
        dialog.setMessage("Não existem programações definidas! Deseja criar uma agora?");
        dialog.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(VerProgramacoesWeb.this, NovaProgramacao.class);
                startActivity(intent);
                finish();
            }
        }).setNegativeButton("Não", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onBackPressed();
                finish();
            }
        });
        progressDialog.show();



        intent= getIntent();
        nomeDisp = findViewById(R.id.nomeDispositivoProgramacao);
        grupos = new ArrayList<>();

        colecao = new HashMap<String, List<String>>();


        prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
        dispositivo = intent.getStringExtra("deviceName").toLowerCase();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("deviceNameForAdapter", dispositivo.toLowerCase());
        editor.apply();
        database = FirebaseDatabase.getInstance();
        nomeDisp.setText(dispositivo.toUpperCase());

        updateList();


        //criarGrupos();
        //criarColecoes();






    }

    private void updateList() {
        database.getReference("cliente").child(prefs.getString("chave","null")).child(dispositivo).child("R").child("programacoes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();
                show = dialog.show();
                if(snapshot.exists()) {
                    show.dismiss();
                    Log.d("snapshot", snapshot.getValue().toString());
                    for(DataSnapshot data : snapshot.getChildren()){
                        Log.d("Keeey", data.getKey());
                        Log.d("sxdfsdfsdfsdf", data.toString());
                        grupos.add(diaSemana(data.getKey()));
                        List<String> hr =  new ArrayList<>();
                        for(DataSnapshot d : data.getChildren()){

                            //hr.add(d.child("tempPROG").getValue().toString().concat(" ºC de ").concat(d.child("liga").getValue().toString()).concat(" até ").concat(d.child("desliga").getValue().toString()).concat("*/*").concat(d.getKey()));
                            hr.add(d.child("liga").getValue().toString().concat("*").concat(d.child("desliga").getValue().toString()).concat("%").concat(d.child("tempPROG").getValue().toString()).concat("*/*").concat(d.getKey()));
                            Log.d("childrens key", d.getKey());
                            Log.d("childrens  values", d.toString());
                            Log.d("Liga", d.child("liga").getValue().toString());
                            Log.d("desliga", d.child("desliga").getValue().toString());
                            Log.d("tempPROG", d.child("tempPROG").getValue().toString());
                        }
                        Log.d("antes do sort", Integer.toString(hr.size()));
                        Collections.sort(hr);
                        Log.d("depois do sort", Integer.toString(hr.size()));
                        List<String> bp =  new ArrayList<>(hr);


                        hr.clear();
                        Log.d("size do bp", Integer.toString(bp.size()));
                        Log.d("valor bp", bp.get(0));
                        for(String z : bp){
                            Log.d("string zzz", z);
                            hr.add(z.substring(z.indexOf("%")+1,z.indexOf("*/*")).concat(" ºC de ").concat(z.substring(0,z.indexOf("*"))).concat(" até ").concat(z.substring(z.indexOf("*")+1,z.indexOf("%"))).concat(z.substring(z.indexOf("*/*"),z.length())));
                        }
                        Log.d("dia da semana", diaSemana(data.getKey()));
                        Log.d("HRRRRR",hr.toString());
                        colecao.put(diaSemana(data.getKey()),hr);
                    }
                }
                expandableListView=findViewById(R.id.listViewProgramacoesDispositivos);
                expandableListAdapter= new MyExpandableListAdapter(VerProgramacoesWeb.this,  grupos, colecao);
                expandableListView.setAdapter(expandableListAdapter);
                expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                    @Override
                    public void onGroupExpand(int groupPosition) {
                        if(lastExpadedPosition  != -1  && groupPosition !=  lastExpadedPosition){
                            expandableListView.collapseGroup(lastExpadedPosition);
                        }
                        lastExpadedPosition =  groupPosition;
                    }
                });
                expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                    @Override
                    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                        String selected=expandableListAdapter.getChild(groupPosition,childPosition).toString();
                        Toast.makeText(VerProgramacoesWeb.this,selected,Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String diaSemana(String num){
        if(num.contains("1a")){
            return "Segunda";
        }else if(num.contains("2a")){
            return "Terça";
        }else if(num.contains("3a")){
            return "Quarta";
        }else if(num.contains("4a")){
            return "Quinta";
        }else if(num.contains("5a")){
            return "Sexta";
        }else if(num.contains("6a")){
            return "Sábado";
        }else if(num.contains("0a")){
            return "Domingo";
        }
    return "";
    }

    /*private void criarColecoes() {
        String [] m1g1= {"item 1 g1", "item2g1  ","item 5 g1"};
        String [] m1g2= {"item 1 g2", "item2g2  ","item 5 g2"};
        String [] m1g3= {"item 1 g3", "item2g3  ","item 5 g3"};
        String [] m1g4= {"item 1 g4", "item2g4 ","item 5 g4"};
        String [] m1g5= {"item 1 g5", "item2g5 ","item 5 g5"};
        for(String  g : grupos){
            if(g.equals("grupo 1")){
                loadItems(m1g1);
            }else if(g.equals("grupo 2")){
                loadItems(m1g2);
            } else if(g.equals("grupo 3")){
                loadItems(m1g3);
            }else if(g.equals("grupo 4")){
                loadItems(m1g4);
            }else if(g.equals("grupo 5")){
                loadItems(m1g5);
            }
            colecao.put(g,filhos);
        }
    }

    private void loadItems(String[] itens) {
        filhos  = new ArrayList<>();
        for(String i :   itens){
            filhos.add(i);
        }
    }

    private void criarGrupos() {
        grupos.add("grupo 1");
        grupos.add("grupo 2");
        grupos.add("grupo 3");
        grupos.add("grupo 4");
        grupos.add("grupo 5");
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        if(expandableListView == null){
            Log.d("A epandable é nulaa"," ave marinaha");
        }else{
            updateList();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}