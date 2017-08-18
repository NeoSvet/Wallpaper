package ru.neosvet.wallpaper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import ru.neosvet.wallpaper.adapters.GalleryAdapter;
import ru.neosvet.wallpaper.adapters.ListAdapter;
import ru.neosvet.wallpaper.adapters.PagesAdapter;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;
import ru.neosvet.wallpaper.ui.Tip;
import ru.neosvet.wallpaper.utils.GalleryLoader;
import ru.neosvet.wallpaper.utils.Lib;
import ru.neosvet.wallpaper.utils.LoaderMaster;
import ru.neosvet.wallpaper.utils.Settings;

public class MainActivity extends LoaderMaster
        implements NavigationView.OnNavigationItemSelectedListener, GalleryAdapter.OnItemClickListener, PagesAdapter.OnPageClickListener {
    private final String COUNT = "count";
    private int page = 1, count = 0;
    private String site, tag = null, catigory = null;// "/tags/helga+lovekaty/";
    private ListView lvSections;
    private List<String> lSections = new ArrayList<String>();
    private Tip tip;
    private RecyclerView rvGallery, rvPages;
    private ProgressBar progressBar;
    private ListAdapter adCategory;
    private GalleryAdapter adGallery;
    private PagesAdapter adPages;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        verifyStoragePermissions();

        initSettings();
        initUI();

        File d = Lib.getFile("");
        if (!d.exists()) d.mkdir();

        intSrv = new Intent(MainActivity.this, GalleryLoader.class);
        restoreActivityState(savedInstanceState);
    }

    private void initSettings() {
        settings = new Settings(MainActivity.this);
        site = settings.getSite();
    }

    private boolean verifyStoragePermissions() {
        //http://stackoverflow.com/questions/38989050/android-6-0-write-to-external-sd-card
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 1);
            return true;
        }
        return false;
    }

    private void initUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        rvGallery = (RecyclerView) findViewById(R.id.rvGallery);
        rvPages = (RecyclerView) findViewById(R.id.rvPages);
        lvSections = (ListView) findViewById(R.id.lvSections);
        tip = new Tip(MainActivity.this, findViewById(R.id.tvToast));

        lvSections.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                tag = lSections.get(i);
                catigory = adCategory.getItem(i);
                loadPage(1);
                lvSections.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void restoreActivityState(Bundle state) {
        super.restoreActivityState(state);
        if (state == null) {
            if (settings.getStartOpen() == Settings.START_MAIN) {
                MainActivity.this.setTitle(getResources().getString(R.string.main));
                loadPage(1);
            } else //Settings.START_FAVORITE
                changeRep(DBHelper.FAVORITE);
        } else {
            catigory = state.getString(Lib.CATEGORIES);
            page = state.getInt(Lib.PAGE);
            count = state.getInt(COUNT);
            tag = state.getString(Lib.TAG);
            changeRep(state.getString(Lib.NAME_REP));
            initSections();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (adGallery != null)
            outState.putString(Lib.NAME_REP, adGallery.getName());
        outState.putString(Lib.CATEGORIES, catigory);
        outState.putInt(Lib.PAGE, page);
        outState.putInt(COUNT, count);
        outState.putString(Lib.TAG, tag);
        super.onSaveInstanceState(outState);
    }

    private boolean unTag() {
        if (tag != null) {
            catigory = null;
            tag = null;
            loadPage(1);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (lvSections.getVisibility() == View.VISIBLE) {
                lvSections.setVisibility(View.GONE);
                return;
            }
            if (unTag()) return;
            try {
                File d = Lib.getFile("");
                for (File f : d.listFiles()) {
                    f.delete();
                }
                d.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_main:
                if (!unTag()) changeRep(DBHelper.LIST);
                break;
            case R.id.nav_sections:
                lvSections.setVisibility(View.VISIBLE);
                break;
            case R.id.nav_favorite:
                changeRep(DBHelper.FAVORITE);
                break;
            case R.id.nav_recent:
                changeRep(DBHelper.RECENT);
                break;
            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.nav_refresh:
                loadPage(page);
                break;
            case R.id.nav_reverse:
                adGallery.reverse();
                break;
            case R.id.nav_mix:
                adGallery.mix();
                break;
            case R.id.nav_import:
                cmdImport();
                break;
            case R.id.nav_export:
                if (adGallery.export())
                    tip.show();
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void cmdImport() {
        try {
            File f = new File(Lib.getFolder() + "/fav");
            if (!f.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(f));
            GalleryRepository fav = new GalleryRepository(MainActivity.this, DBHelper.FAVORITE);
            String s;
            while ((s = br.readLine()) != null) {
                fav.addUrl(getWithoutSite(s));
                fav.addMini(getWithoutSite(br.readLine()).replace("/mini", ""));
            }
            br.close();
            fav.save(false);
            f.delete();
            tip.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getWithoutSite(String s) {
        if (s.contains(site))
            return s.substring(site.length());
        return s;
    }

    private void changeRep(String name) {
        adGallery = new GalleryAdapter(MainActivity.this, name);
        if (name.equals(DBHelper.LIST)) {
            MainActivity.this.setTitle(getResources().getString(R.string.main));
        } else if (name.equals(DBHelper.FAVORITE)) {
            MainActivity.this.setTitle(getResources().getString(R.string.favorite));
        } else if (name.equals(DBHelper.RECENT)) {
            MainActivity.this.setTitle(getResources().getString(R.string.recent));
        }
        initGallery();
    }

    private boolean isTablet() {
        boolean xlarge = ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    @Override
    public void onConnect(IService srv) {
        super.onConnect(srv);
        progressBar.setVisibility(View.VISIBLE);
        srv.setAct(MainActivity.this);
    }

    public void onPost(boolean suc, int count) {
        Lib.log("onPost");
        finishLoader();
        progressBar.setVisibility(View.GONE);
        if (count == -1) {
            adGallery.update();
            return;
        }
        this.count = count;
        if (suc) {
            adGallery = new GalleryAdapter(MainActivity.this, DBHelper.LIST);
            initGallery();
            initSections();
        }
    }

    private void initSections() {
        try {
            File f = new File(getFilesDir() + Lib.CATEGORIES);
            if (!f.exists()) return;
            adCategory = new ListAdapter(MainActivity.this);
            BufferedReader br = new BufferedReader(new FileReader(f));
            String s;
            while ((s = br.readLine()) != null) {
                lSections.add(s);
                adCategory.add(br.readLine());
            }
            br.close();
            lvSections.setAdapter(adCategory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGallery() {
        if (isTablet()) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                GridLayoutManager layoutManager = new GridLayoutManager(MainActivity.this, 4);
                rvGallery.setLayoutManager(layoutManager);
            } else {
                GridLayoutManager layoutManager = new GridLayoutManager(MainActivity.this, 2);
                rvGallery.setLayoutManager(layoutManager);
            }
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false);
            rvGallery.setLayoutManager(layoutManager);
        } else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
            rvGallery.setLayoutManager(layoutManager);
        }

        rvGallery.setAdapter(adGallery);

        if (!adGallery.getName().equals(DBHelper.LIST)) {
            rvPages.setVisibility(View.GONE);
            return;
        }

        if (mode_srv != UNBOUND_SERVICE) {
            if (catigory != null)
                MainActivity.this.setTitle(catigory);
            else if (tag != null)
                MainActivity.this.setTitle(getResources().getString(R.string.tag)
                        + " " + tag.substring(6));
            else
                MainActivity.this.setTitle(getResources().getString(R.string.main));
        }
        if (count == 0) {
            rvPages.setVisibility(View.GONE);
            return;
        }
        rvPages.setVisibility(View.VISIBLE);
        adPages = new PagesAdapter(MainActivity.this, count, page);
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false);
        rvPages.setLayoutManager(layoutManager2);
        rvPages.setAdapter(adPages);
    }

    @Override
    public void onItemClick(String url) {
        Intent intent = new Intent(MainActivity.this, ImageActivity.class);
        intent.putExtra(Lib.SORT, adGallery.isDESC());
        intent.putExtra(DBHelper.LIST, adGallery.getName());
        intent.putExtra(DBHelper.URL, url);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            catigory = null;
            tag = "/tags/" + data.getStringExtra(Lib.TAG);
            loadPage(1);
        }
    }

    @Override
    public void onPageClick(int page) {
        loadPage(page);
    }

    private void loadPage(int new_page) {
        page = new_page;
        intSrv.putExtra(DBHelper.LIST, DBHelper.LIST);
        intSrv.putExtra(Lib.PAGE, page);
        intSrv.putExtra(Lib.TAG, (tag == null ? "" : tag.replace(" ", "+")));
        startLoader();
    }

    public void updateGallery() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adGallery.update();
            }
        });
    }

    public void loadMini(String url) {
        intSrv.putExtra(DBHelper.LIST, adGallery.getName());
        intSrv.putExtra(DBHelper.URL, url);
        if (mode_srv == NO_SERVICE) {
            startLoader();
        } else {
            startService(intSrv);
        }
    }
}
