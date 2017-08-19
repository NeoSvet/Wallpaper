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

public class ImageLoader extends IntentService implements LoaderMaster.IService {
    private final IBinder binder = new LoaderMaster.MyBinder(ImageLoader.this);
    private String site = null, link, img;
    private StringBuilder tags, carousel;
    private ImageActivity act;

    public void setAct(LoaderMaster act) {
        this.act = (ImageActivity) act;
    }

    public ImageLoader() {
        super("ImageLoader");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (site == null) {
            Settings settings = new Settings(ImageLoader.this);
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
                while (!line.contains("/large"))
                    line = br.readLine();
                img = line.substring(line.indexOf("src") + 5);
                img = img.substring(0, img.indexOf("\"")).replace("large", "mini");
                line = br.readLine();
                while (!line.contains("href"))
                    line = br.readLine();
                line = site + line.substring(line.indexOf("href") + 6);
                if (line.contains("1920x1080"))
                    line = line.substring(0, line.indexOf("\"") - 3) + "1920_1080";
                else
                    line = line.substring(0, line.indexOf("\"") - 3) + "1280_800";
                request = new Request.Builder().url(line).build(); // screen_w + "x" + screen_h

                while (!line.contains("Wallpaper tags"))
                    line = br.readLine();
                line = br.readLine();
                int i = line.indexOf("<a");
                tags = new StringBuilder();
                while (i > 0) {
                    tags.append(line.substring(line.indexOf(">", i) + 1,
                            line.indexOf("</", i)));
                    tags.append("@");
                    i = line.indexOf("<a", i + 10);
                }
                tags.delete(tags.length() - 1, tags.length());
            }
            //load carousel:
            while (!line.contains("scrollbar"))
                line = br.readLine();
            carousel = new StringBuilder();
            if (!line.contains("iframe")) {
                line = br.readLine();
                while (!line.contains("</ul>")) {
                    if (onlyCarousel) {
                        line = line.substring(line.indexOf("href") + 6);
                        line = line.substring(0, line.indexOf("\""));
                    } else {
                        line = line.substring(line.indexOf("href") + 6,
                                line.length() - 1).replace("\"><img src=\"", "");
                    }
                    carousel.append(line);
                    carousel.append("@");
                    line = br.readLine();
                }
                carousel.delete(carousel.length() - 1, carousel.length());
            }
            br.close();

            if (onlyCarousel)
                return true;

            response = client.newCall(request).execute();
            br = new BufferedReader(response.body().charStream(), 1000);
            while (!line.contains("photo\""))
                line = br.readLine();
            line = br.readLine();
            line = line.substring(line.indexOf("src") + 5);
            link = line.substring(0, line.indexOf("\""));
            br.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}