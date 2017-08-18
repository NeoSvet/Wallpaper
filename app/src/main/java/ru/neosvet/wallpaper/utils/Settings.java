package ru.neosvet.wallpaper.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by NeoSvet on 18.08.2017.
 */

public class Settings {
    private final String VIEW = "view", SITE = "site", SLIDESHOW = "slideshow";
    public static final int ZOOMING_VIEW = 0, SIMPLE_VIEW = 1;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public Settings(Context context) {
        pref = context.getSharedPreferences(this.getClass().getSimpleName(), context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void save() {
        editor.apply();
    }

    public int getView() {
        return pref.getInt(VIEW, ZOOMING_VIEW);
    }

    public void setView(int mode) {
        editor.putInt(VIEW, mode);
    }

    public String getSite() {
        return pref.getString(SITE, "http://mota.ru");
    }

    public void setSite(String site) {
        editor.putString(SITE, site);
    }

    public int getSlideshowTime() {
        return pref.getInt(SLIDESHOW, 4);
    }

    public void setSlideshowTime(int time) {
        editor.putInt(SLIDESHOW, time);
    }
}
