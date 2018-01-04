package ru.neosvet.wallpaper.utils;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

import java.util.LinkedList;
import java.util.List;

import ru.neosvet.wallpaper.MainActivity;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;
import ru.neosvet.wallpaper.loaders.GalleryLoader;
import ru.neosvet.wallpaper.loaders.GalleryLoaderMotaRu;

/**
 * Created by NeoSvet on 30.09.2017.
 */

public class GalleryService extends IntentService implements LoaderMaster.IService {
    public static abstract class Loader {
        public byte status = WORK;
        public int count = 0;

        abstract public boolean download(int page, String tag);

        abstract public String getMini(String url_image);

        abstract public String getSite();

        public int getCount() {
            return count;
        }
    }


    public static final byte WORK = 0, SAVING = 1, FINISH = 2, ERROR = 3;
    public static final int FINISH_ERROR = -1, FINISH_MINI = -2, FINISH_CATEGORIES = -3;
    private byte status = WORK;
    private final IBinder binder = new LoaderMaster.MyBinder(GalleryService.this);
    private Loader loader;
    private GalleryRepository repository;
    private List<String> urls = new LinkedList<String>();
    private boolean boolThread = false;
    private MainActivity act;
    private String site = null;

    public void setAct(LoaderMaster act) {
        this.act = (MainActivity) act;
    }

    public GalleryService() {
        super("GalleryService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (site == null) {
            Settings settings = new Settings(GalleryService.this);
            site = settings.getSite();
        }
        if (intent.hasExtra(DBHelper.URL)) { // load mini
            status = WORK;
            urls.add(intent.getStringExtra(DBHelper.URL));
            if (repository == null)
                repository = new GalleryRepository(GalleryService.this, intent.getStringExtra(DBHelper.LIST));
            if (!boolThread)
                startThread();
            status = FINISH;
            return;
        }

        if (site.contains(Lib.MOTARU))
            loader = new GalleryLoaderMotaRu(GalleryService.this, site);
        else
            loader = new GalleryLoader(GalleryService.this, site);
        final boolean suc = loader.download(intent.getIntExtra(Lib.PAGE, 1), intent.getStringExtra(Lib.TAG));
        waitForAct();
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    act.onPost(suc, loader.getCount());
                    act.finishLoader();
                }
            });
        }
        if (!boolThread)
            stopSelf();
    }

    private void waitForAct() {
        try {
            while (act == null) {
                Lib.log("wait");
                Thread.sleep(100);
            }
        } catch (Exception e) {
        }
    }

    private String getMini(String url) {
        if (loader != null) {
            if (!url.contains(":") || url.contains(loader.getSite()))
                return loader.getMini(url);
        }
        String site;
        if (!url.contains(":"))
            site = this.site;
        else
            site = url.substring(0, url.indexOf("/", 10));
        if (site.contains(Lib.MOTARU))
            loader = new GalleryLoaderMotaRu(GalleryService.this, site);
        else
            loader = new GalleryLoader(GalleryService.this, site);
        return loader.getMini(url);
    }

    private void startThread() {
        boolThread = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mini;
                    while (urls.size() > 0 && status != ERROR) {
                        if (urls.get(0) == null) return;
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
                    waitForAct();
                    if (act != null) {
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                act.onPost(true, FINISH_MINI);
                                act.finishLoader();
                                stopSelf();
                            }
                        });
                    }
                }
            }
        }).start();
    }
}
