package ru.neosvet.wallpaper.loaders;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ru.neosvet.wallpaper.MainActivity;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;
import ru.neosvet.wallpaper.utils.Lib;
import ru.neosvet.wallpaper.utils.LoaderMaster;
import ru.neosvet.wallpaper.utils.Settings;

/**
 * Created by NeoSvet on 19.08.2017.
 */

public class GalleryLoaderMotaRu extends IntentService implements LoaderMaster.IService {
    private final byte WORK = 0, SAVING = 1, FINISH = 2, ERROR = 3;
    public static final int FINISH_ERROR = -1, FINISH_MINI = -2, FINISH_CATEGORIES = -3;
    private byte status = WORK;
    private final IBinder binder = new LoaderMaster.MyBinder(GalleryLoaderMotaRu.this);
    private int count = 0;
    private GalleryRepository repository;
    private List<String> urls = new LinkedList<String>();
    private boolean boolThread = false;
    private MainActivity act;
    private String site = null;

    public void setAct(LoaderMaster act) {
        this.act = (MainActivity) act;
    }

    public GalleryLoaderMotaRu() {
        super("GalleryLoader");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (site == null) {
            Settings settings = new Settings(GalleryLoaderMotaRu.this);
            site = settings.getSite();
        }
        if (intent.hasExtra(DBHelper.URL)) { // load mini
            status = WORK;
            urls.add(intent.getStringExtra(DBHelper.URL));
            if (repository == null)
                repository = new GalleryRepository(GalleryLoaderMotaRu.this, intent.getStringExtra(DBHelper.LIST));
            if (!boolThread)
                startThread();
            status = FINISH;
            return;
        }

        final boolean suc = download(intent.getIntExtra(Lib.PAGE, 1), intent.getStringExtra(Lib.TAG));
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    act.onPost(suc, count);
                }
            });
        }
        if (!boolThread)
            stopSelf();
    }

    private boolean download(int page, String tag) {
        String line;
        File f = new File(getFilesDir() + Lib.CATEGORIES);
        try {
            //start:
            String url;
            if (tag == null)
                url = site + "/wallpapers/top/page/" + page + "/order/date";
            else {
                try {
                    tag = URLEncoder.encode(tag, "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                url = site + "/tags/view/page/" + page + "/title/" + tag;
            }
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            client.setReadTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
//            InputStream in = new BufferedInputStream(response.body().byteStream());
//            BufferedReader br = new BufferedReader(new InputStreamReader(in, Lib.ENCODING), 1000);
            BufferedReader br = new BufferedReader(response.body().charStream(), 1000);
            line = br.readLine();
            //categories:
            int u, i = line.indexOf("root-menu__flex");
            i = line.indexOf("href", i) + 6;
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            while (i > 5) {
                u = line.indexOf("\"", i);
                //https://ero.mota.ru/categories/view/name/erotica
                bw.write(line.substring(i, u)); // url
                bw.newLine();
                if (line.indexOf("src", i) > 0) {
                    i = line.indexOf("src", u) + 5;
                    u = line.indexOf("\"", i);
                    bw.write(line.substring(i, u)); // icon
                    bw.newLine();
                }
                i = line.indexOf("span", u) + 5;
                u = line.indexOf("<", i);
                bw.write(line.substring(i, u)); // name
                bw.newLine();
                bw.flush();
                i = line.indexOf("href", u) + 6;
            }
            bw.close();
            if (page == 0) {
                br.close();
                status = FINISH;
                count = FINISH_CATEGORIES;
                return true;
            }
            //gallery:
            repository = new GalleryRepository(GalleryLoaderMotaRu.this, DBHelper.LIST);
            while (!line.contains("element"))
                line = br.readLine();
            i = line.indexOf("\"element");
            i = line.indexOf("href", i) + 6;
            int end = line.lastIndexOf("deskription");
            while (i < end) {
                u = line.indexOf("\"", i);
                repository.addUrl(line.substring(i, u)); // url
                i = line.indexOf("src", u) + 5;
                u = line.indexOf("\"", i);
                repository.addMini(line.substring(i, u)); // mini
                i = line.indexOf("<li>", u);
                i = line.indexOf("href", i) + 6;
                if (i == 5) { //skip ad block
                    br.readLine();
                    line = br.readLine();
                    if (line.contains("Yandex")) {
                        while (!line.contains("<li>"))
                            line = br.readLine();
                        end = line.lastIndexOf("deskription");
                        i = line.indexOf("href", i) + 6;
                    }
                }
            }
            //count pages:
            if (line.contains("pagination-sourse")) {
                line = line.substring(0, line.lastIndexOf("next"));
                line = line.substring(0, line.lastIndexOf("</a>"));
                line = line.substring(line.lastIndexOf(">") + 1);
                count = Integer.parseInt(line);
            }
            //finish:
            br.close();
            status = SAVING;
            repository.save(true);
            status = FINISH;
            if (urls.size() > 0 && !boolThread)
                startThread();
        } catch (Exception e) {
            if (f.exists())
                f.delete();
            status = ERROR;
            count = FINISH_ERROR;
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void startThread() {
        boolThread = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mini;
                    while (urls.size() > 0 && status != ERROR) {
                        mini = getMini(urls.get(0));
                        while (status == SAVING) {
                            Thread.sleep(200);
                        }
                        if (status == FINISH) {
                            repository.updateMini(urls.get(0), mini);
                            if (act != null)
                                act.updateGallery();
                        } else // WORK
                            repository.addMini(urls.get(0), mini);
                        urls.remove(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                boolThread = false;
                if (status == FINISH) {
                    if (act != null) {
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                act.onPost(true, FINISH_MINI);
                                stopSelf();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private String getMini(String url_image) {
        try {
            final String link = site + url_image;
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            client.setReadTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            Request request = new Request.Builder().url(link).build();
            Response response = client.newCall(request).execute();
            String line = response.body().string();
            line = line.substring(line.indexOf("ges/") + 3);
            String s = line.substring(0, 8);
            line = line.substring(line.indexOf("_") + 1);
            line = line.substring(0, line.indexOf("\"") + 1);
            s = s + line;
            return s.substring(0, s.length() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
