package ru.neosvet.wallpaper.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;

import ru.neosvet.wallpaper.R;

/**
 * Created by NeoSvet on 01.10.2017.
 */

public class LoaderMini implements Target {
    private final String FOLDER = "/mini";
    private ImageView imageView;
    private File file;
    private boolean finish = false;

    public LoaderMini(Context context, String url, ImageView object) {
        imageView = object;
        file = new File(context.getFilesDir() + FOLDER);
        if (!file.exists()) file.mkdir();
        file = new File(context.getFilesDir() + FOLDER + url.substring(url.lastIndexOf("/")));
        if (!file.exists()) {
            Picasso.with(context)
                    .load(url)
                    .into(this);
        } else {
            imageView.setImageDrawable(Drawable.createFromPath(file.toString()));
        }
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.close();
            imageView.setImageDrawable(Drawable.createFromPath(file.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            if (file.exists())
                file.delete();
        }
        finish = true;
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        imageView.setImageResource(R.drawable.no_image);
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        imageView.setImageResource(R.drawable.load_image);
    }

    public boolean isFinish() {
        return finish;
    }
}
