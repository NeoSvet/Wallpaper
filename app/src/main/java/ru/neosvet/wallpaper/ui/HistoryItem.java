package ru.neosvet.wallpaper.ui;

/**
 * Created by NeoSvet on 11.08.2017.
 */

public class HistoryItem {
    private String url, list;

    public HistoryItem(String url, String list) {
        this.url = url;
        this.list = list;
    }

    public String getUrl() {
        return url;
    }

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public boolean isCarousel() {
        return list.contains("/");
    }
}
