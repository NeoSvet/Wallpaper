package ru.neosvet.wallpaper.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.neosvet.wallpaper.ImageActivity;
import ru.neosvet.wallpaper.R;
import ru.neosvet.wallpaper.utils.LoaderMini;

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
    private Animation animation, animationInvert;
    private int lastPosition = -1;
    private List<LoaderMini> loaders;

    public UniAdapter(ImageActivity activity, String[] data) {
        context = activity;
        mListener = activity;
        this.data = data;
        if (isImage()) {
            loaders = new ArrayList<LoaderMini>();
            animation = AnimationUtils.loadAnimation(context, R.anim.add_item_h);
            animationInvert = AnimationUtils.loadAnimation(context, R.anim.add_item_hi);
        }
    }

    public String[] getData() {
        return data;
    }

    public void clear() {
        data = null;
    }

    private boolean isImage() {
        if (data == null) return false;
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
            loaders.add(new LoaderMini(context,
                    data[position].substring(data[position].lastIndexOf("http")),
                    (ImageView) holder.item));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onItemClick(getUrl(position));
                }
            });
            setAnimation(holder.itemView, position);
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
        return data[index].substring(0, data[index].lastIndexOf("http"));
    }

    @Override
    public int getItemCount() {
        return (data == null ? 0 : data.length);
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

    private void setAnimation(View viewToAnimate, int position) {
        viewToAnimate.clearAnimation();
        if (position > lastPosition)
            viewToAnimate.startAnimation(animation);
        else
            viewToAnimate.startAnimation(animationInvert);
        lastPosition = position;
    }
}