package ru.neosvet.wallpaper.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import ru.neosvet.wallpaper.MainActivity;
import ru.neosvet.wallpaper.R;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;
import ru.neosvet.wallpaper.utils.Lib;
import ru.neosvet.wallpaper.utils.Settings;

/**
 * Created by NeoSvet on 14.07.2017.
 */

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(String url);
    }

    private MainActivity act;
    private OnItemClickListener mListener;
    private GalleryRepository repository;
    private boolean DESC = false;
    private String site = null;

    public GalleryAdapter(MainActivity activity, String name) {
        act = activity;
        mListener = activity;
        repository = new GalleryRepository(act, name);
        if (!name.equals(DBHelper.LIST))
            DESC = !DESC;
        repository.load(DESC);
    }

    public String getName() {
        return repository.getName();
    }

    public boolean isDESC() {
        return DESC;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(act).inflate(R.layout.image_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GalleryAdapter.ViewHolder holder, final int position) {
        if (repository.getMini(position) == null) {
            holder.image.setImageResource(R.drawable.load_image);
            act.loadMini(repository.getUrl(position));
        } else {
            if (site == null) {
                Settings settings = new Settings(act);
                site = settings.getSite();
            }
            Picasso.with(act)
                    .load(site + repository.getMini(position))
                    .placeholder(R.drawable.load_image)
                    .error(R.drawable.no_image)
                    .into(holder.image);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onItemClick(repository.getUrl(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return repository.getCount();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);
        }
    }

    public void update() {
        repository.load(DESC);
        GalleryAdapter.this.notifyDataSetChanged();
    }

    public void reverse() {
        DESC = !DESC;
        repository.load(DESC);
        GalleryAdapter.this.notifyDataSetChanged();
    }

    public void mix() {
        DESC = true;
        repository.mix();
        GalleryAdapter.this.notifyDataSetChanged();
    }

    public boolean export() {
        try {
            int n, e, s;
            if (DESC) {
                n = repository.getCount() - 1;
                e = -1;
                s = -1;
            } else {
                n = 0;
                e = repository.getCount();
                s = 1;
            }
            File f = new File(Lib.getFolder() + "/fav");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            for (int i = n; i != e; i += s) {
                bw.write(repository.getUrl(i));
                bw.newLine();
                bw.write(repository.getMini(i));
                bw.newLine();
                bw.flush();
            }
            bw.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
