package ru.neosvet.wallpaper.ui;

/**
 * Created by NeoSvet on 07.08.2017.
 */

public class GalleryItem {
    private String url, mini = null;

    public GalleryItem(String url) {
        this.url = url;
    }

    public GalleryItem(String url, String mini) {
        this.url = url;
        this.mini = mini;
    }

    public String getUrl() {
        return url;
    }

    public String getMini() {
        if (mini == null) return "";
        return mini;
    }

    public void setMini(String mini) {
        this.mini = mini;
    }

    public boolean containsMini() {
        return mini != null;
    }
}
