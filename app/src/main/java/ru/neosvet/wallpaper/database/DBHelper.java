package ru.neosvet.wallpaper.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by NeoSvet on 14.07.2017.
 */

public class DBHelper extends SQLiteOpenHelper {
    public static final String LIST = "list", CARAROUSEL = "car", FAVORITE = "fav", RECENT = "rec",
            URL = "url", MINI = "mini", TIME = "time";
    private String name;

    public DBHelper(Context context, String name) {
        super(context, "db", null, 1);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String[] m = new String[]{LIST, CARAROUSEL, FAVORITE, RECENT};
        for (int i = 0; i < m.length; i++)
            db.execSQL(getRequest(m[i]));
    }

    private String getRequest(String name) {
        return "create table " + name + " ("
                + URL + " text primary key,"
                + MINI + " text,"
                + TIME + " integer);";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}