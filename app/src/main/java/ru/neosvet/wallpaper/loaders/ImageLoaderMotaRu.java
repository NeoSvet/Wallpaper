package ru.neosvet.wallpaper.loaders;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.util.concurrent.TimeUnit;

import ru.neosvet.wallpaper.utils.ImageService;
import ru.neosvet.wallpaper.utils.Lib;

/**
 * Created by NeoSvet on 06.08.2017.
 */

public class ImageLoaderMotaRu extends ImageService.Loader {
    private String site = null;

    public ImageLoaderMotaRu(String site) {
        this.site = site;
    }

    @Override
    public String[] load(String url, boolean onlyCarousel) {
        try {
            StringBuilder tags = new StringBuilder();
            if (!url.contains(":"))
                url = site + url;
            String link = url;
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            client.setReadTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            Request request = new Request.Builder().url(link).build();
            Response response = client.newCall(request).execute();
            BufferedReader br = new BufferedReader(response.body().charStream(), 1000);
            String line = br.readLine();
            if (!onlyCarousel) {
                while (!line.contains("tags-container"))
                    line = br.readLine();
                int i = line.indexOf("<a", line.indexOf("tag"));
                while (i < line.indexOf("modalAddTag")) {
                    i = line.indexOf("class", i);
                    tags.append(line.substring(line.indexOf(">", i) + 1,
                            line.indexOf("</", i)));
                    tags.append("@");
                    i = line.indexOf("<a", i + 10);
                }
                tags.delete(tags.length() - 1, tags.length());

                i = line.indexOf("download-wallpaper", i);
                String res = "1920x1080";
                if (!line.contains(res)) res = "1280x720";
                int p = line.indexOf("<a", i);
                while (i < line.indexOf(res)) {
                    p = i;
                    i = line.indexOf("<a", i + 10);
                }
                line = line.substring(p + 9, line.indexOf(">", p) - 1);
                request = new Request.Builder().url(site + line).build();
            }
            //load carousel:
            while (!line.contains("element list-dowln"))
                line = br.readLine();
            StringBuilder carousel = new StringBuilder();
            int i = line.indexOf("<li");
            String item;
            while (i > 0) {
                item = line.substring(line.indexOf("href", i) + 6);
                item = item.substring(0, item.indexOf("\""));
                carousel.append(item);
                if (!onlyCarousel) {
                    item = site + line.substring(line.indexOf("src", i) + 5);
                    item = item.substring(0, item.indexOf("\""));
                    carousel.append(item);
                }
                carousel.append("@");
                i = line.indexOf("<li", i + 10);
            }
            if (carousel.length() > 3)
                carousel.delete(carousel.length() - 1, carousel.length());
            br.close();

            if (!onlyCarousel) {
                response = client.newCall(request).execute();
                br = new BufferedReader(response.body().charStream(), 1000);
                while (!line.contains("full-img"))
                    line = br.readLine();
                br.close();
                line = line.substring(line.indexOf("src", line.indexOf("full-img")) + 5);
                link = site + line.substring(0, line.indexOf("\""));
            }

            return new String[]{link, tags.toString(), carousel.toString()};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}