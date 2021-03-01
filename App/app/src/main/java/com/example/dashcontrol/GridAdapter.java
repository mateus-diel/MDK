package com.example.dashcontrol;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.List;

public class GridAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater layoutInflater;
    private ArrayList<String> number;
    private ArrayList <Drawable> numImg;

    public GridAdapter(Context c, ArrayList<String> number, ArrayList <Drawable> numImg){
        this.context = c;
        this.number = number;
        this.numImg = numImg;
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
            convertView = layoutInflater.inflate(R.layout.rowitem, null);
        }
        ImageView imageView = convertView.findViewById(R.id.image_view);
        TextView textView = convertView.findViewById(R.id.text_view);
        //imageView.setImageResource(numImg[position]);
        imageView.setImageDrawable(numImg.get(position));
        textView.setText(number.get(position));
        return convertView;
    }
}
