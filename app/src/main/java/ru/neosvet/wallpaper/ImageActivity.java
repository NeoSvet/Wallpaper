package ru.neosvet.wallpaper;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.neosvet.wallpaper.adapters.UniAdapter;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;
import ru.neosvet.wallpaper.ui.CustomImageView;
import ru.neosvet.wallpaper.ui.HistoryItem;
import ru.neosvet.wallpaper.ui.Tip;
import ru.neosvet.wallpaper.utils.ImageService;
import ru.neosvet.wallpaper.utils.Lib;
import ru.neosvet.wallpaper.utils.LoaderMaster;
import ru.neosvet.wallpaper.utils.Settings;

public class ImageActivity extends LoaderMaster implements UniAdapter.OnItemClickListener {
    private final String MENU = "menu", URL_CAR = "url_car", HISTORY = "/his";
    private CustomImageView imageView;
    private GalleryRepository repository, fav, recent;
    private UniAdapter adMenu, adCarousel;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private RecyclerView rvMenu, rvCarousel;
    private TextView tvToast;
    private Tip tip;
    private String url, url_car;
    private File file;
    private Animation anPrev, anNext;
    private String[] tags;
    private boolean menu = true, move = false, load = false, anim = false;
    private float dpi, def_scale, x;
    private Timer tSlideShow;
    private List<HistoryItem> history = new LinkedList<HistoryItem>();
    private boolean boolSlideShow;
    private Settings settings;
    private int slideshow_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Lib.log("onCreate: " + hashCode());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.image_activity);

        settings = new Settings(ImageActivity.this);
        initUI();
        initGesturesImage();
        dpi = getResources().getDisplayMetrics().density;
        intSrv = new Intent(ImageActivity.this, ImageService.class);
        fav = new GalleryRepository(ImageActivity.this, DBHelper.FAVORITE);
        recent = new GalleryRepository(ImageActivity.this, DBHelper.RECENT);
        restoreActivityState(savedInstanceState);
    }

    @Override
    protected void restoreActivityState(Bundle state) {
        super.restoreActivityState(state);
        if (state == null) {
            imageView.setImage(R.drawable.load_image);
            repository = new GalleryRepository(ImageActivity.this, getIntent().getStringExtra(DBHelper.LIST));
            loadImage(getIntent().getStringExtra(DBHelper.URL), null);
        } else {
            repository = new GalleryRepository(ImageActivity.this, state.getString(Lib.NAME_REP));
            url_car = state.getString(URL_CAR);
            url = state.getString(DBHelper.URL);
            if (url == null) return;
            loadHistory();
            tags = state.getStringArray(Lib.TAG);
            if (state.getBoolean(MENU))
                defaultMenu();
            else {
                menu = false;
                adMenu = new UniAdapter(ImageActivity.this, tags);
                rvMenu.setAdapter(adMenu);
            }
            adCarousel = new UniAdapter(ImageActivity.this, state.getStringArray(DBHelper.CARAROUSEL));
            rvCarousel.setAdapter(adCarousel);
            openImage(url);
        }
    }

    private void loadHistory() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File f = new File(getFilesDir() + HISTORY);
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String s;
                    while ((s = br.readLine()) != null) {
                        history.add(new HistoryItem(s, br.readLine()));
                    }
                    br.close();
                    f.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        try {
            File f = new File(getFilesDir() + HISTORY);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            for (int i = 0; i < history.size(); i++) {
                bw.write(history.get(i).getUrl());
                bw.newLine();
                bw.write(history.get(i).getList());
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        outState.putString(DBHelper.URL, url);
        outState.putString(URL_CAR, url_car);
        outState.putBoolean(MENU, menu);
        outState.putStringArray(Lib.TAG, tags);
        outState.putString(Lib.NAME_REP, repository.getName());
        if (adCarousel != null)
            outState.putStringArray(DBHelper.CARAROUSEL, adCarousel.getData());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (boolSlideShow)
            stopSlideShow();
    }

    private void loadImage(String url, @Nullable String carousel) {
        progressBar.setVisibility(View.VISIBLE);
        load = true;
        if (!url.contains(":"))
            url = settings.getSite() + url;
        intSrv.putExtra(DBHelper.URL, url);
        if (carousel == null)
            intSrv.removeExtra(DBHelper.CARAROUSEL);
        else
            intSrv.putExtra(DBHelper.CARAROUSEL, carousel);
        startLoader();
    }

    private boolean openImage(@Nullable String url) {
        def_scale = 0f;
        if (url != null) {
            this.url = url;
            file = Lib.getFile(url);
        }
        if (file.exists()) {
            if (imageView.setImage(file.toString())) {
                addRecent(url);
                return true;
            }
        }
        if (!load)
            imageView.setImage(R.drawable.no_image);
        return false;
    }

    private void addRecent(final String url) {
        if (repository.getName().equals(DBHelper.RECENT))
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                recent.addRecent(url);
            }
        }).start();
    }

    private void initUI() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setVisibility(View.GONE);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        ImageActivity.this.setTitle("");
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tvToast = (TextView) findViewById(R.id.tvToast);
        tip = new Tip(ImageActivity.this, tvToast);
        imageView = (CustomImageView) findViewById(R.id.imageView);
        imageView.selectSimpleView(settings.getView() == Settings.SIMPLE_VIEW);
        rvMenu = (RecyclerView) findViewById(R.id.rvMenu);
        rvCarousel = (RecyclerView) findViewById(R.id.rvCarousel);
        initAnimation();
        rvMenu.setLayoutManager(new LinearLayoutManager(ImageActivity.this, LinearLayoutManager.HORIZONTAL, false));
        rvCarousel.setLayoutManager(new LinearLayoutManager(ImageActivity.this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void initAnimation() {
        anPrev = AnimationUtils.loadAnimation(ImageActivity.this, R.anim.move_right);
        anPrev.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!changeImage(false))
                    showToast(getResources().getString(R.string.list_finish));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anNext = AnimationUtils.loadAnimation(ImageActivity.this, R.anim.move_left);
        anNext.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!changeImage(true))
                    showToast(getResources().getString(R.string.list_finish));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void initGesturesImage() {
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (boolSlideShow) {
                    stopSlideShow();
                    return imageView.isSimpleView();
                }
                if (def_scale == 0f)
                    def_scale = imageView.getScale();
                if (motionEvent.getPointerCount() > 1 || load || imageView.getScale() != def_scale) {
                    return imageView.isSimpleView();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    x = motionEvent.getX();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    float mx = motionEvent.getX() - x;
                    if (move) {
                        x = motionEvent.getX();
                        if (!anim) {
                            anim = true;
                            if (mx > 0f)
                                imageView.startAnimation(anPrev);
                            else
                                imageView.startAnimation(anNext);
                        }
                    } else if (mx < -100f * dpi || mx > 100f * dpi) {
                        move = true;
                    }
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                        motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (!move) {
                        if (toolbar.getVisibility() == View.GONE) {
                            toolbar.setVisibility(View.VISIBLE);
                            rvCarousel.setVisibility(View.VISIBLE);
                        } else {
                            toolbar.setVisibility(View.GONE);
                            rvCarousel.setVisibility(View.GONE);
                        }
                    }
                    move = false;
                    anim = false;
                }
                return imageView.isSimpleView();
            }
        });
    }

    private boolean changeImage(boolean next) {
        String url_prev = null, u;
        if (repository.getCount() == 0)
            repository.load(getIntent().getBooleanExtra(Lib.SORT, false));
        for (int i = 0; i < repository.getCount(); i++) {
            u = repository.getUrl(i);
            if (url.contains(u)) {
                if (next) {
                    i++;
                    if (i < repository.getCount()) {
//                        addHistory();
                        loadImage(repository.getUrl(i), null);
                        return true;
                    }
                    break;
                } else if (url_prev != null) {
//                    addHistory();
                    loadImage(url_prev, null);
                    return true;
                } else break;
            }
            url_prev = u;
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        final int prev_item = history.size() - 1;
        if (prev_item > -1) {
            url = history.get(prev_item).getUrl();
            if (history.get(prev_item).isCarousel()) {
                loadImage(url, history.get(prev_item).getList());
            } else {
                repository = new GalleryRepository(ImageActivity.this, history.get(prev_item).getList());
                repository.load(getIntent().getBooleanExtra(Lib.SORT, false));
                loadImage(url, null);
            }
            history.remove(prev_item);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onConnect(IService srv) {
        Lib.log("onConnect");
        progressBar.setVisibility(View.VISIBLE);
        load = true;
        srv.setAct(ImageActivity.this);
        super.onConnect(srv);
    }

    public void onPost(String[] carousel) {
        repository = new GalleryRepository(ImageActivity.this, DBHelper.CARAROUSEL);
        for (int i = 0; i < carousel.length; i++) {
            repository.addUrl(carousel[i]);
        }
        repository.save(true);
    }

    public void onPost(boolean suc, @Nullable String url, @Nullable String link, String[] tags, final String[] carousel) {
        Lib.log("onPost: " + hashCode() + ", "+url);
        this.tags = tags;
        if (suc) {
            if (openImage(url)) {
                if (!boolSlideShow)
                    progressBar.setVisibility(View.GONE);
                load = false;
            }
            defaultMenu();
            adCarousel = new UniAdapter(ImageActivity.this, carousel);
            rvCarousel.setAdapter(adCarousel);
        } else {
            imageView.setImage(R.drawable.no_image);
        }
    }

    private void defaultMenu() {
        menu = true;
        String fav_item;
        if (fav.contains(url))
            fav_item = getResources().getString(R.string.del_fav);
        else
            fav_item = getResources().getString(R.string.add_fav);
        adMenu = new UniAdapter(ImageActivity.this, new String[]{
                getResources().getString(R.string.slideshow),
                getResources().getString(R.string.tags),
                fav_item
        });
        rvMenu.setAdapter(adMenu);
    }

    @Override
    public void onItemClick(String item) {
        if (item.contains("/")) { // item Carousel
            url_car = url;
            addHistory();
            loadImage(item, null);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    repository = new GalleryRepository(ImageActivity.this, DBHelper.CARAROUSEL);
                    for (int i = 0; i < adCarousel.getItemCount(); i++) {
                        repository.addUrl(adCarousel.getUrl(i));
                    }
                    repository.save(true);
                }
            }).start();
        } else if (menu) {
            if (item.equals(getResources().getString(R.string.slideshow))) {
                startSlideshow();
            } else if (item.equals(getResources().getString(R.string.tags))) {
                menu = false;
                adMenu = new UniAdapter(ImageActivity.this, tags);
                rvMenu.setAdapter(adMenu);
            } else if (item.equals(getResources().getString(R.string.add_fav))) {
                fav.addItem(url);
                defaultMenu();
            } else if (item.equals(getResources().getString(R.string.del_fav))) {
                fav.deleteItem(url);
                defaultMenu();
            }
        } else if (item.contains(":")) { //return to menu
            defaultMenu();
        } else { // open tag
            Intent result = new Intent();
            result.putExtra(Lib.TAG, item);
            setResult(0, result);
            super.onBackPressed();
        }
    }

    private void startSlideshow() {
        toolbar.setVisibility(View.GONE);
        rvCarousel.setVisibility(View.GONE);
        slideshow_time = settings.getSlideshowTime();
        TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    while (ImageActivity.this.mode_srv != NO_SERVICE)
                        Thread.sleep(500);
                } catch (Exception e) {
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        slideshow_time--;
                        if (slideshow_time == 0) {
                            if (!changeImage(true))
                                stopSlideShow();
                            else {
                                progressBar.setProgress(0);
                                slideshow_time = settings.getSlideshowTime();
                            }
                        } else
                            progressBar.incrementProgressBy(1);
                    }
                });
            }
        };
        tSlideShow = new Timer();
        tSlideShow.schedule(tTask, 1000, 1000);
        boolSlideShow = true;
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
        progressBar.setMax(slideshow_time);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void stopSlideShow() {
        boolSlideShow = false;
        tSlideShow.cancel();
        tSlideShow.purge();
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        showToast(getResources().getString(R.string.slideshow_stop));
    }

    private void addHistory() {
        if (repository.getName().equals(DBHelper.CARAROUSEL)) {
            history.add(new HistoryItem(url, url_car));
        } else {
            history.add(new HistoryItem(url, repository.getName()));
        }
    }

    private void showToast(String msg) {
        tvToast.setText(msg);
        tip.show();
    }
}