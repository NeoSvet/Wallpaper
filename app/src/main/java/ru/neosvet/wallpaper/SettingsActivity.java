package ru.neosvet.wallpaper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import ru.neosvet.wallpaper.utils.Settings;

public class SettingsActivity extends AppCompatActivity {
    private EditText edSite;
    private Spinner spViewObject;
    private SeekBar sbSlideShow;
    private TextView tvSlideShow;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = new Settings(SettingsActivity.this);
        initSite();
        initViewObject();
        initSlideshowTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        settings.setSite(edSite.getText().toString());
        settings.setView(spViewObject.getSelectedItemPosition());
        settings.setSlideshowTime(sbSlideShow.getProgress() + 1);
        settings.save();
    }

    private void initSite() {
        edSite = (EditText) findViewById(R.id.etSite);
        edSite.setText(settings.getSite());
    }

    private void initViewObject() {
        spViewObject = (Spinner) findViewById(R.id.spViewObject);
        spViewObject.setPopupBackgroundResource(R.drawable.cell_none);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.list_item, new String[]{"Zooming View", "Simple View"});
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
}
