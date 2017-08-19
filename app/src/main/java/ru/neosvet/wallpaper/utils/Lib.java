package ru.neosvet.wallpaper.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by NeoSvet on 15.07.2017.
 */

public class Lib {
    public static final int TIMEOUT = 10;
    public static final String FOLDER = "/wallpaper", MOTARU = "mota.ru", SORT = "sort", CATEGORIES = "cat",
            PAGE = "page", TAG = "tag", MODE = "mode", NAME_REP = "name_rep";

    public static void log(String msg) {
        Log.d("wallpaper", msg);
    }

    public static String getFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath();
    }

    public static File getFile(String url) {
        if (url.contains("/")) {
            url = url.substring(url.lastIndexOf("/"));
            if (url.contains("-"))
                url = url.substring(0, url.indexOf("-"));
        }
        return new File(getFolder() + FOLDER + url + ".jpg");
    }
}
