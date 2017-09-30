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

public class ImageLoader extends ImageService.Loader {
    private String site = null;

    public ImageLoader(String site) {
        this.site = site;
    }

    @Override
    public String[] load(String url, boolean onlyCarousel) {
        try {
            StringBuilder tags = new StringBuilder();
            if (url.contains(":")) {
                site = url.substring(0, url.indexOf("/", 10));
            } else
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
                while (!line.contains("/large"))
                    line = br.readLine();
                //img = line.substring(line.indexOf("src") + 5);
                //img = img.substring(0, img.indexOf("\"")).replace("large", "mini");
                line = br.readLine();
                while (!line.contains("href"))
                    line = br.readLine();
                line = site + line.substring(line.indexOf("href") + 6);
                if (line.contains("1920x1080"))
                    line = line.substring(0, line.indexOf("\"") - 3) + "1920_1080";
                else
                    line = line.substring(0, line.indexOf("\"") - 3) + "1280_800";
                request = new Request.Builder().url(line).build(); // screen_w + "x" + screen_h

                while (!line.contains("Wallpaper tags"))
                    line = br.readLine();
                line = br.readLine();
                int i = line.indexOf("<a");

                while (i > 0) {
                    tags.append(line.substring(line.indexOf(">", i) + 1,
                            line.indexOf("</", i)));
                    tags.append("@");
                    i = line.indexOf("<a", i + 10);
                }
                tags.delete(tags.length() - 1, tags.length());
            }
            //load carousel:
            while (!line.contains("scrollbar"))
                line = br.readLine();
            StringBuilder carousel = new StringBuilder();
            if (!line.contains("iframe")) {
                line = br.readLine();
                while (!line.contains("</ul>")) {
                    if (onlyCarousel) {
                        line = line.substring(line.indexOf("href") + 6);
                        line = line.substring(0, line.indexOf("\""));
                    } else {
                        line = line.substring(line.indexOf("href") + 6,
                                line.length() - 1).replace("\"><img src=\"", "");
                    }
                    carousel.append(line);
                    carousel.append("@");
                    line = br.readLine();
                }
                carousel.delete(carousel.length() - 1, carousel.length());
            }
            br.close();

            if (!onlyCarousel) {
                response = client.newCall(request).execute();
                br = new BufferedReader(response.body().charStream(), 1000);
                while (!line.contains("photo\""))
                    line = br.readLine();
                line = br.readLine();
                line = line.substring(line.indexOf("src") + 5);
                link = line.substring(0, line.indexOf("\""));
                br.close();
            }

            return new String[]{link, tags.toString(), carousel.toString()};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}