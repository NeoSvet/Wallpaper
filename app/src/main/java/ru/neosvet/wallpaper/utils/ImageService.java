package ru.neosvet.wallpaper.utils;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

import ru.neosvet.wallpaper.ImageActivity;
import ru.neosvet.wallpaper.R;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.loaders.ImageLoader;
import ru.neosvet.wallpaper.loaders.ImageLoaderMotaRu;

/**
 * Created by NeoSvet on 30.09.2017.
 */

public class ImageService extends IntentService implements LoaderMaster.IService {
    public static abstract class Loader {

        abstract public String[] load(String url, boolean onlyCarousel);

    }

    private final int LINK = 0, TAGS = 1, CAROUSEL = 2;
    private final IBinder binder = new LoaderMaster.MyBinder(ImageService.this);
    private Loader loader;
    private ImageActivity act;

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

    private void download(String url) {
        final String[] result = loader.load(url, false);
        if (act != null) {
            act.finishLoader();
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] t = new String[]{};
                    String[] c = new String[]{};
                    if (result != null) {
                        t = (act.getResources().getString(R.string.tags)
                                + ":@" + result[TAGS]).split("@");
                        c = result[CAROUSEL].split("@");
                        act.onPost(true, result[LINK], t, c);
                    } else
                        act.onPost(false, null, t, c);
                }
            });
        }
    }

    private void downloadWithCarousel(String car_url, String url) {
        final String[] result = loader.load(car_url, true);
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