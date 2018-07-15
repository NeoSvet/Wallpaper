package ru.neosvet.wallpaper.utils;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.IBinder;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;

import ru.neosvet.wallpaper.ImageActivity;
import ru.neosvet.wallpaper.R;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.loaders.ImageLoader;
import ru.neosvet.wallpaper.loaders.ImageLoaderMotaRu;

/**
 * Created by NeoSvet on 30.09.2017.
 */

public class ImageService extends IntentService implements Target, LoaderMaster.IService {
    public static abstract class Loader {

        abstract public String[] load(String url, boolean onlyCarousel);

    }

    private final int URL = 0, LINK = 1, TAGS = 2, CAROUSEL = 3;
    private final IBinder binder = new LoaderMaster.MyBinder(ImageService.this);
    private Loader loader;
    private ImageActivity act;
    private String[] result;

    public void setAct(LoaderMaster act) {
        this.act = (ImageActivity) act;
    }

    public ImageService() {
        super("ImageService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String url = intent.getStringExtra(DBHelper.URL);
        String site;
        if (!url.contains(":")) {
            Settings settings = new Settings(ImageService.this);
            site = settings.getSite();
        } else
            site = url.substring(0, url.indexOf("/", 10));

        if (site.contains(Lib.MOTARU))
            loader = new ImageLoaderMotaRu(site);
        else
            loader = new ImageLoader(site);

        if (intent.hasExtra(DBHelper.CARAROUSEL)) {
            downloadWithCarousel(intent.getStringExtra(DBHelper.CARAROUSEL), url);
        } else {
            download(url);
        }
        stopSelf();
    }

    private void waitForAct() {
        try {
            while (act == null) {
                Lib.LOG("wait");
                Thread.sleep(100);
            }
        } catch (Exception e) {
        }
    }

    private void download(String url) {
        Lib.LOG("start download: " + url);
        result = loader.load(url, false);
        waitForAct();
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Lib.LOG("act: " + act.hashCode());
                    if (result != null) {
                        File file = Lib.getFile(result[URL]);
                        if (!file.exists()) {
                            downloadImage();
                            return;
                        }
                        openImage();
                    } else
                        act.onPost(false, null, null, new String[]{}, new String[]{});
                    act.finishLoader();
                }
            });
        }
    }

    private void openImage() {
        String[] t, c;
        t = (act.getResources().getString(R.string.tags)
                + ":@" + result[TAGS]).split("@");
        c = result[CAROUSEL].split("@");
        act.onPost(true, result[URL], result[LINK], t, c);
    }

    private void downloadImage() {
        Picasso.with(ImageService.this)
                .load(result[LINK]).memoryPolicy(MemoryPolicy.NO_CACHE)
                .placeholder(R.drawable.load_image)
                .error(R.drawable.no_image)
                .into(ImageService.this);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        try {
            FileOutputStream out = new FileOutputStream(Lib.getFile(result[URL]));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.close();
            openImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (!boolSlideShow)
//            progressBar.setVisibility(View.GONE);
//        load = false;
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        act.onPost(false, null, null, new String[]{}, new String[]{});
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
    }

    private void downloadWithCarousel(String car_url, String url) {
        final String[] result = loader.load(car_url, true);
        waitForAct();
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] c = new String[]{};
                    if (result != null)
                        c = result[CAROUSEL].split("@");
                    act.onPost(c);
                }
            });
            download(url);
        }
    }

}