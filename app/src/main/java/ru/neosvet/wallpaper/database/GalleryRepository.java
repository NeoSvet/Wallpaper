package ru.neosvet.wallpaper.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ru.neosvet.wallpaper.ui.GalleryItem;

/**
 * Created by NeoSvet on 14.07.2017.
 */

public class GalleryRepository {
    private DBHelper helper;
    private List<GalleryItem> data = new ArrayList<GalleryItem>();
    private int index = 0;

    public GalleryRepository(Context context, String name) {
        helper = new DBHelper(context, name);
    }

    public String getName() {
        return helper.getName();
    }

    public void load(boolean DESC) {
        data.clear();
        SQLiteDatabase dataBase = helper.getReadableDatabase();
        Cursor cursor;
        if (DESC)
            cursor = dataBase.query(helper.getName(), null, null, null, null, null,
                    DBHelper.TIME + " DESC");
        else
            cursor = dataBase.query(helper.getName(), null, null, null, null, null,
                    DBHelper.TIME);
        if (cursor.moveToFirst()) {
            int iUrl = cursor.getColumnIndex(DBHelper.URL);
            int iMini = cursor.getColumnIndex(DBHelper.MINI);
            do {
                data.add(new GalleryItem(cursor.getString(iUrl),
                        cursor.getString(iMini)));
//                Lib.log("mini: "+cursor.getString(iMini));
            } while (cursor.moveToNext());
        }
        dataBase.close();
    }

    public int getCount() {
        return data.size();
    }

    public String getUrl(int index) {
        return data.get(index).getUrl();
    }

    public String getMini(int index) {
        return data.get(index).getMini();
    }

    public void addUrl(String url) {
        data.add(new GalleryItem(url));
    }

    public void addMini(String mini) {
        data.get(data.size() - 1).setMini(mini);
    }

    public void addMini(String url, String mini) {
        for (int i = index; i < data.size(); i++) {
            if (data.get(i).getUrl().equals(url)) {
                data.get(i).setMini(mini);
                index = i;
                return;
            }
        }
    }

    public void updateMini(String url, String mini) {
        SQLiteDatabase dataBase = helper.getWritableDatabase();
        ContentValues field = new ContentValues();
        field.put(DBHelper.MINI, mini);
        dataBase.update(helper.getName(), field, DBHelper.URL + "= ?", new String[]{url});
        dataBase.close();
    }

    public void save(boolean CLEAR) {
        SQLiteDatabase dataBase = helper.getWritableDatabase();
        if (CLEAR)
            dataBase.delete(helper.getName(), null, null); // clear table
        ContentValues field;
        for (int i = 0; i < getCount(); i++) {
            field = new ContentValues();
            field.put(DBHelper.URL, data.get(i).getUrl());
            if (data.get(i).containsMini())
                field.put(DBHelper.MINI, data.get(i).getMini());
            field.put(DBHelper.TIME, i);
            dataBase.insert(helper.getName(), null, field);
        }
        dataBase.close();
    }

    public void addItem(String url) {//, String mini
        SQLiteDatabase dataBase = helper.getWritableDatabase();
        ContentValues field = new ContentValues();
        field.put(DBHelper.URL, url);
//        field.put(DBHelper.MINI, mini);
        field.put(DBHelper.TIME, System.currentTimeMillis());
        dataBase.insert(helper.getName(), null, field);
        dataBase.close();
    }

    public void addRecent(String url) {
        SQLiteDatabase dataBase = helper.getWritableDatabase();
        ContentValues field = new ContentValues();
        field.put(DBHelper.TIME, System.currentTimeMillis());
        int result = dataBase.update(helper.getName(), field, DBHelper.URL + "= ?", new String[]{url});
        if (result == 0) { // no update
            field.put(DBHelper.URL, url);
            dataBase.insert(helper.getName(), null, field);
        }
        dataBase.close();
    }

    public void deleteItem(String url) {
        SQLiteDatabase dataBase = helper.getWritableDatabase();
        dataBase.delete(helper.getName(), DBHelper.URL + "= ?", new String[]{url});
        dataBase.close();
    }

    public boolean contains(String url) {
        SQLiteDatabase dataBase = helper.getReadableDatabase();
        Cursor cursor = dataBase.query(helper.getName(), null,
                DBHelper.URL + "= ?", new String[]{url},
                null, null, null);
        boolean result = cursor.moveToFirst();
        dataBase.close();
        return result;
    }

    public void mix() {
        Random r = new Random();
        int n;
        GalleryItem item;
        for (int i = 0; i < data.size(); i++) {
            n = r.nextInt(data.size());
            item = data.get(i);
            data.remove(i);
            data.add(n, item);
        }
        save(true);
    }

    public void clear() {
        data.clear();
    }
}
