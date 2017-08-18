package ru.neosvet.wallpaper.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import ru.neosvet.wallpaper.ImageActivity;
import ru.neosvet.wallpaper.R;

/**
 * Created by NeoSvet on 15.07.2017.
 */

public class UniAdapter extends RecyclerView.Adapter<UniAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(String item);
    }

    private Context context;
    private UniAdapter.OnItemClickListener mListener;
    private String[] data;

    public UniAdapter(ImageActivity activity, String[] data) {
        context = activity;
        mListener = activity;
        this.data = data;
    }

    public String[] getData() {
        return data;
    }

    private boolean isImage() {
        return data[0].contains("jpg");
    }

    @Override
    public UniAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (isImage())
            view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false);
        else
            view = LayoutInflater.from(context).inflate(R.layout.text_item, parent, false);
        return new UniAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UniAdapter.ViewHolder holder, final int position) {
        if (isImage()) {
            Picasso.with(context)
                    .load(data[position].substring(data[position].indexOf("http")))
                    .placeholder(R.drawable.load_image)
                    .error(R.drawable.no_image)
                    .into((ImageView) holder.item);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onItemClick(getUrl(position));
                }
            });
        } else {
            ((TextView) holder.item).setText(data[position]);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onItemClick(data[position]);
                }
            });
        }
    }

    public String getUrl(int index) {
        return data[index].substring(0, data[index].indexOf("http"));
    }

    @Override
    public int getItemCount() {
        return data.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View item;

        ViewHolder(View itemView) {
            super(itemView);
            if (isImage())
                item = itemView.findViewById(R.id.image);
            else
                item = itemView.findViewById(R.id.text);
        }
    }
}
