package ru.neosvet.wallpaper.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import ru.neosvet.wallpaper.R;
import ru.neosvet.wallpaper.utils.Settings;

/**
 * Created by NeoSvet on 13.08.2017.
 */

public class ListAdapter extends BaseAdapter {
    private List<String> data = new ArrayList<String>();
    private LayoutInflater inflater;
    private String site = null;

    public ListAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void add(String item) {
        data.add(item);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public String getItem(int i) {
        String item = null;
        if (i < data.size()) {
            item = data.get(i);
            if (item.contains("@"))
                item = item.substring(item.indexOf("@") + 1);
        }
        return item;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, null);
        }
        TextView tv = (TextView) convertView.findViewById(R.id.text);
        String item = data.get(position);
        if (item.contains("@")) {
            ImageView iv = (ImageView) convertView.findViewById(R.id.icon);
            iv.setVisibility(View.VISIBLE);
            int i = item.indexOf("@");
            if (site == null) {
                Settings settings = new Settings(parent.getContext());
                site = settings.getSite();
            }
            Picasso.with(parent.getContext())
                    .load(site + item.substring(0, i))
                    .placeholder(R.drawable.load_image)
                    .error(R.drawable.no_image)
                    .into(iv);
            item = item.substring(i + 1);
        }
        tv.setText(item);
        return convertView;
    }
}
