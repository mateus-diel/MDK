package com.example.dashcontrol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Map;

public class MyExpandableListAdapter extends BaseExpandableListAdapter {
    private Context  context;
    private Map<String, List<String>>  mobileCollection;
    private List<String> groupList;
    private FirebaseDatabase database;
    private DatabaseReference ref;
    private SharedPreferences prefs;

    public MyExpandableListAdapter(Context ctx, List<String> g, Map<String, List<String>> c) {
    this.context =  ctx;
    this.mobileCollection = c;
    this.groupList=  g;
    }

    @Override
    public int getGroupCount() {
        return mobileCollection.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mobileCollection.get(groupList.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mobileCollection.get(groupList.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return  groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String name = groupList.get(groupPosition).toString();
        if(convertView ==null){
            LayoutInflater inflater =  (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.group_item,null);
        }
        TextView item =  convertView.findViewById(R.id.txtHoraList);
        item.setTextColor(Color.BLACK);
        item.setTypeface(null, Typeface.BOLD);
        item.setText(name);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String str = mobileCollection.get(groupList.get(groupPosition)).get(childPosition).toString();
        if(convertView ==null){
            LayoutInflater inflater =  (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.horario_item,null);
        }

        Button edit  = convertView.findViewById(R.id.btnEditHora);
        Button button= convertView.findViewById(R.id.deleteHora);
        Log.d("tag do button",str.substring(str.lastIndexOf("*/*")+3));
        button.setTag(str.substring(str.lastIndexOf("*/*")+3));
        edit.setTag(str.substring(str.lastIndexOf("*/*")+3));
        str= str.substring(0,str.indexOf("*/*"));

        TextView item= convertView.findViewById(R.id.txtHoraListItem);
        item.setTextColor(Color.BLACK);
        item.setText(str);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder =new AlertDialog.Builder(context);
                builder.setMessage("Quer apager este horário?");
                builder.setCancelable(true);
                builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        database = FirebaseDatabase.getInstance();
                        prefs = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE);
                        Log.d("chave na adapt", prefs.getString("chave","null"));
                        String dsp = prefs.getString("deviceNameForAdapter","null").toLowerCase();

                        database.getReference().child("cliente").child(prefs.getString("chave","null")).child(dsp).child("R").child("programacoes").child(getNumericSemana(groupList.get(groupPosition).toString())).child(((Button)v).getTag().toString()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("groupPosition", String.valueOf(groupPosition));
                                Log.d("childPosition", String.valueOf(childPosition));
                                List<String> filho = mobileCollection.get(groupList.get(groupPosition));
                                filho.remove(childPosition);
                                Log.d("filho size", String.valueOf(filho.size()));
                                if(filho.size()==0){
                                    mobileCollection.remove(groupList.get(groupPosition));
                                    groupList.remove(groupPosition);
                                }
                                notifyDataSetChanged();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
                        dialog.dismiss();
                    }
                }).setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("numeric semana", groupList.get(groupPosition).toString());
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE);
                Log.d("chave na adapt", prefs.getString("chave","null"));
                Intent i = new  Intent(context, NovaProgramacao.class);
                i.putExtra("numSemana",getNumericSemana(groupList.get(groupPosition).toString()));
                i.putExtra("chaveHora",((Button)v).getTag().toString());
                i.putExtra("op",1);
                context.startActivity(i);

            }
        });
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    private String getNumericSemana(String str){
        if(str.contains("Segunda")){
            return "1a";
        } else if(str.contains("Terça")){
            return "2a";
        }else if(str.contains("Quarta")){
            return "3a";
        }else if(str.contains("Quinta")){
            return "4a";
        }else if(str.contains("Sexta")){
            return "5a";
        }else if(str.contains("Sábado")){
            return "6a";
        }else if(str.contains("Domingo")){
            return "0a";
        }
            return  "10a";
    }
}