package com.marcoa.marcoa;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class GridAdapterLocal extends BaseAdapter {
    private Context context;
    private LayoutInflater layoutInflater;
    private ArrayList<String> number;
    private ArrayList <Drawable> numImg;

    public GridAdapterLocal(Context c, ArrayList<String> nome, ArrayList <Drawable> Img){
        context = c;
        number = nome;
        numImg = Img;
    }
    @Override
    public int getCount() {
        return number.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(layoutInflater == null){
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if(convertView ==  null){
            convertView = layoutInflater.inflate(R.layout.rowitemlocal, null);
        }
        ImageView imageView = convertView.findViewById(R.id.image_view_local);
        TextView textView = convertView.findViewById(R.id.text_view_local);
        Button button = convertView.findViewById(R.id.btnGridLocal);
        button.setTag(number.get(position).substring(number.get(position).indexOf("*/*")+3));
        imageView.setImageDrawable(numImg.get(0));
        imageView.setBackgroundColor(ContextCompat.getColor(context, R.color.cinzaBackground));
        textView.setText(number.get(position).substring(0,(number.get(position).indexOf("*/*"))).toUpperCase());
        return convertView;
    }
}
