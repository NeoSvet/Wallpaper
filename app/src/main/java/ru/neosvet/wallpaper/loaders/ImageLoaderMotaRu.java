package ru.neosvet.wallpaper.loaders;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.util.concurrent.TimeUnit;

import ru.neosvet.wallpaper.ImageActivity;
import ru.neosvet.wallpaper.R;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.utils.Lib;
import ru.neosvet.wallpaper.utils.LoaderMaster;
import ru.neosvet.wallpaper.utils.Settings;

/**
 * Created by NeoSvet on 06.08.2017.
 */

public class ImageLoaderMotaRu extends IntentService implements LoaderMaster.IService {
    private final IBinder binder = new LoaderMaster.MyBinder(ImageLoaderMotaRu.this);
    private String site = null, link;
    private StringBuilder tags, carousel;
    private ImageActivity act;

    public void setAct(LoaderMaster act) {
        this.act = (ImageActivity) act;
    }

    public ImageLoaderMotaRu() {
        super("ImageLoader");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (site == null) {
            Settings settings = new Settings(ImageLoaderMotaRu.this);
            site = settings.getSite();
        }
        if (intent.hasExtra(DBHelper.CARAROUSEL)) {
            downloadWithCarousel(intent.getStringExtra(DBHelper.CARAROUSEL),
                    intent.getStringExtra(DBHelper.URL));
        } else {
            download(intent.getStringExtra(DBHelper.URL));
        }
        stopSelf();
    }

    private void download(String url) {
        final boolean suc = load(url, false);
        if (act != null) {
            act.finishLoader();
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] t = new String[]{};
                    String[] c = new String[]{};
                    if (suc) {
                        tags.insert(0, act.getResources().getString(R.string.tags) + ":@");
                        t = tags.toString().split("@");
                        c = carousel.toString().split("@");
                    }
                    act.onPost(suc, link, t, c);
                }
            });
        }
    }

    private void downloadWithCarousel(String car_url, String url) {
        final boolean suc = load(car_url, true);
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] c = new String[]{};
                    if (suc)
                        c = carousel.toString().split("@");
                    act.onPost(c);
                }
            });
            download(url);
        }
    }

    private boolean load(String url, boolean onlyCarousel) {
        try {
            link = url;
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            client.setReadTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            Request request = new Request.Builder().url(site + link).build();
            Response response = client.newCall(request).execute();
            BufferedReader br = new BufferedReader(response.body().charStream(), 1000);
            String line = br.readLine();
            if (!onlyCarousel) {
                while (!line.contains("tags-container"))
                    line = br.readLine();
                int i = line.indexOf("<a", line.indexOf("tag"));
                tags = new StringBuilder();
                while (i < line.indexOf("modalAddTag")) {
                    i = line.indexOf("class", i);
                    tags.append(line.substring(line.indexOf(">", i) + 1,
                            line.indexOf("</", i)));
                    tags.append("@");
                    i = line.indexOf("<a", i + 10);
                }
                tags.delete(tags.length() - 1, tags.length());

                i = line.indexOf("download-wallpaper", i);
                String res = "1920x1080";
                if (!line.contains(res)) res = "1280x720";
                int p = line.indexOf("<a", i);
                while (i < line.indexOf(res)) {
                    p = i;
                    i = line.indexOf("<a", i + 10);
                }
                line = line.substring(p + 9, line.indexOf(">", p) - 1);
                request = new Request.Builder().url(site + line).build();
            }
            //load carousel:
            while (!line.contains("element list-dowln"))
                line = br.readLine();
            carousel = new StringBuilder();
            int i = line.indexOf("<li");
            String item;
            while (i > 0) {
                item = line.substring(line.indexOf("href", i) + 6);
                item = item.substring(0, item.indexOf("\""));
                carousel.append(item);
                if (!onlyCarousel) {
                    item = site + line.substring(line.indexOf("src", i) + 5);
                    item = item.substring(0, item.indexOf("\""));
                    carousel.append(item);
                }
                carousel.append("@");
                i = line.indexOf("<li", i + 10);
            }
            if (carousel.length() > 3)
                carousel.delete(carousel.length() - 1, carousel.length());
            br.close();

            if (onlyCarousel)
                return true;

            response = client.newCall(request).execute();
            br = new BufferedReader(response.body().charStream(), 1000);
            while (!line.contains("full-img"))
                line = br.readLine();
            br.close();
            line = line.substring(line.indexOf("src", line.indexOf("full-img")) + 5);
            link = site + line.substring(0, line.indexOf("\""));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}