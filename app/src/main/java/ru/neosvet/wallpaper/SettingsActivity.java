package ru.neosvet.wallpaper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;
import ru.neosvet.wallpaper.ui.Tip;
import ru.neosvet.wallpaper.utils.Settings;

public class SettingsActivity extends AppCompatActivity {
    private final String SITES = "sites";
    private AutoCompleteTextView etSite;
    private Spinner spViewObject, spStartOpen;
    private SeekBar sbSlideShow;
    private TextView tvSlideShow;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        settings = new Settings(SettingsActivity.this);
        initSite();
        initViewObject();
        initSlideshowTimer();
        initStartOpen();
        initClearRecent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        String site = etSite.getText().toString();
        settings.setSite(site);
        settings.setView(spViewObject.getSelectedItemPosition());
        settings.setSlideshowTime(sbSlideShow.getProgress() + 1);
        settings.setStartOpen(spStartOpen.getSelectedItemPosition());
        settings.save();

        for (int i = 0; i < etSite.getAdapter().getCount(); i++) {
            if(site.equals(etSite.getAdapter().getItem(i)))
                return;
        }

        File f = new File(getFilesDir() + File.separator + SITES);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
            bw.write(site);
            bw.newLine();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSite() {
        final File f = new File(getFilesDir() + File.separator + SITES);
        List<String> liSite = new ArrayList<String>();
        try {
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String s;
                while ((s = br.readLine()) != null) {
                    liSite.add(s);
                }
                br.close();
            } else {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write(settings.getSite());
                bw.newLine();
                bw.close();
                liSite.add(settings.getSite());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        etSite = (AutoCompleteTextView) findViewById(R.id.etSite);
        etSite.setText(settings.getSite());
        ArrayAdapter<String> adSite = new ArrayAdapter<String>(SettingsActivity.this, R.layout.popmenu_item, liSite);
        etSite.setThreshold(1);
        etSite.setAdapter(adSite);
        etSite.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                    etSite.showDropDown();
                return false;
            }
        });
    }

    private void initViewObject() {
        spViewObject = (Spinner) findViewById(R.id.spViewObject);
        spViewObject.setPopupBackgroundResource(R.drawable.cell_none);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.popmenu_item, new String[]{"Zooming View", "Simple View"});
        spViewObject.setAdapter(adapter);
        spViewObject.setSelection(settings.getView());
    }

    private void initSlideshowTimer() {
        tvSlideShow = (TextView) findViewById(R.id.tvSlideShow);
        sbSlideShow = (SeekBar) findViewById(R.id.sbSlideShow);
        sbSlideShow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvSlideShow.setText(String.valueOf(i + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbSlideShow.setProgress(settings.getSlideshowTime() - 1);
    }

    private void initStartOpen() {
        spStartOpen = (Spinner) findViewById(R.id.spStartOpen);
        spStartOpen.setPopupBackgroundResource(R.drawable.cell_none);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.popmenu_item, new String[]{
                getResources().getString(R.string.main), getResources().getString(R.string.favorite)});
        spStartOpen.setAdapter(adapter);
        spStartOpen.setSelection(settings.getStartOpen());
    }

    private void initClearRecent() {
        findViewById(R.id.bClearRecent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GalleryRepository recent = new GalleryRepository(SettingsActivity.this, DBHelper.RECENT);
                recent.save(true);
                Tip tip = new Tip(SettingsActivity.this, findViewById(R.id.tvToast));
                tip.show();
            }
        });
    }
}
