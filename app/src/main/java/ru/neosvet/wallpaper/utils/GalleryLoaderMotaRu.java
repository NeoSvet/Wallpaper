package ru.neosvet.wallpaper.utils;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import ru.neosvet.wallpaper.MainActivity;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;

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
            URL url;
            if (tag.equals(""))
                url = new URL(site + "/wallpapers/top/page/" + page + "/order/date");
            else
                url = new URL(site + "/tags/view/page/" + page + "/title/" + tag);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(Lib.cPing);
            urlConnection.setReadTimeout(Lib.cPing);
            //urlConnection.setRequestProperty(cUserAgent, cClient);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in, Lib.ENCODING), 1000);
            line = br.readLine();
            //categories:
            int u, i = line.indexOf("root-menu__flex");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            while (i > 0) {
                i = line.indexOf("href", i) + 6;
                u = line.indexOf("\"", i);
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
                line = br.readLine();
            }
            bw.close();
            if (page == 0) {
                in.close();
                urlConnection.disconnect();
                status = FINISH;
                count = FINISH_CATEGORIES;
                return true;
            }
            //gallery:
            String url_image;
            repository = new GalleryRepository(GalleryLoaderMotaRu.this, DBHelper.LIST);
            while (!line.contains("element"))
                line = br.readLine();
            i = line.indexOf("\"element");
            while (i > 0) {
                i = line.indexOf("href", i) + 6;
                if (i == 5) {
                    while (!line.contains("<li>"))
                        line = br.readLine();
                    i = line.indexOf("href", i) + 6;
                }
                u = line.indexOf("\"", i);
                repository.addUrl(line.substring(i, u)); // url
                i = line.indexOf("src", u) + 5;
                u = line.indexOf("\"", i);
                repository.addMini(line.substring(i, u)); // mini
                i = line.indexOf("<li>", u);
            }
            //count pages:
            if (line.contains("pagination-sourse")) {
                line = line.substring(0,line.lastIndexOf("next"));
                line = line.substring(0,line.lastIndexOf("</a>"));
                line = line.substring(line.lastIndexOf(">"));
                count = Integer.parseInt(line);
            }
            //finish:
            in.close();
            urlConnection.disconnect();
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
            URL url = new URL(link);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(Lib.cPing);
            urlConnection.setReadTimeout(Lib.cPing);
            // urlConnection.setRequestProperty(cUserAgent, cClient);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in, Lib.ENCODING), 1000);
            String line = br.readLine();
            in.close();
            urlConnection.disconnect();
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
