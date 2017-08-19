package ru.neosvet.wallpaper.loaders;

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
import ru.neosvet.wallpaper.utils.Lib;
import ru.neosvet.wallpaper.utils.LoaderMaster;
import ru.neosvet.wallpaper.utils.Settings;

/**
 * Created by NeoSvet on 06.08.2017.
 */

public class GalleryLoader extends IntentService implements LoaderMaster.IService {
    private final byte WORK = 0, SAVING = 1, FINISH = 2, ERROR = 3;
    public static final int FINISH_ERROR = -1, FINISH_MINI = -2, FINISH_CATEGORIES = -3;
    private byte status = WORK;
    private final IBinder binder = new LoaderMaster.MyBinder(GalleryLoader.this);
    private int count = 0;
    private GalleryRepository repository;
    private List<String> urls = new LinkedList<String>();
    private boolean boolThread = false;
    private MainActivity act;
    private String site = null;

    public void setAct(LoaderMaster act) {
        this.act = (MainActivity) act;
    }

    public GalleryLoader() {
        super("GalleryLoader");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (site == null) {
            Settings settings = new Settings(GalleryLoader.this);
            site = settings.getSite();
        }
        if (intent.hasExtra(DBHelper.URL)) { // load mini
            status = WORK;
            urls.add(intent.getStringExtra(DBHelper.URL));
            if (repository == null)
                repository = new GalleryRepository(GalleryLoader.this, intent.getStringExtra(DBHelper.LIST));
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
            URL url = new URL(site + tag + "/page/" + page + "/");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(Lib.cPing);
            urlConnection.setReadTimeout(Lib.cPing);
            //urlConnection.setRequestProperty(cUserAgent, cClient);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in, Lib.ENCODING), 1000);
            line = br.readLine();
            //categories:
            while (!line.contains("Sandbox"))
                line = br.readLine();
            line = br.readLine();
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            while (!line.contains("</ul>")) {
                if (line.contains("href")) {
                    line = line.substring(line.indexOf("href") + 6,
                            line.indexOf(">") - 2);
                    bw.write(line);
                    bw.newLine();
                    line = br.readLine();
                    bw.write(line.trim().replace("amp;", ""));
                    bw.newLine();
                    bw.flush();
                }
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
            repository = new GalleryRepository(GalleryLoader.this, DBHelper.LIST);
            while (!line.contains("col-md-4"))
                line = br.readLine();
            while (!line.contains("<nav>") && !line.contains("<noindex>")) {
                line = br.readLine();
                if (line.contains("thumbnail")) {
                    line = br.readLine();
                    line = line.substring(line.indexOf("href") + 6);
                    url_image = line.substring(0, line.indexOf("\""));
                    repository.addUrl(url_image);
                    line = br.readLine();
                    if (line.contains("holder.js")) //for tag
                        line = br.readLine();
                    line = line.substring(line.indexOf(site) + site.length() + 5, line.length() - 1);
                    repository.addMini(line);
                }
            }
            //count pages:
            if (line.contains("<nav>")) {
                line = br.readLine();
                while (!line.contains("next") && !line.contains("last")) {
                    line += br.readLine();
                }
                if (line.contains("last")) {
                    line = line.substring(0, line.indexOf("current") - 2);
                    line = line.substring(line.lastIndexOf(">") + 1);
                    line = line.substring(0, line.indexOf(" "));
                } else {
                    line = line.substring(0, line.indexOf("next"));
                    line = line.substring(line.lastIndexOf("\">") + 2);
                    line = line.substring(0, line.indexOf("<"));
                }
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
