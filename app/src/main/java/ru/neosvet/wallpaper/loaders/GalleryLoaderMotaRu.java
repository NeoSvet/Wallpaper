package ru.neosvet.wallpaper.loaders;

import android.content.Context;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;
import ru.neosvet.wallpaper.utils.GalleryService;
import ru.neosvet.wallpaper.utils.Lib;

import static ru.neosvet.wallpaper.utils.GalleryService.*;

/**
 * Created by NeoSvet on 19.08.2017.
 */

public class GalleryLoaderMotaRu extends GalleryService.Loader {
    private Context context;
    private String site;

    public GalleryLoaderMotaRu(Context context, String site) {
        this.context = context;
        this.site = site;
    }

    @Override
    public boolean download(int page, String tag) {
        String line;
        File f = new File(context.getFilesDir() + Lib.CATEGORIES);
        try {
            //start:
            String url;
            if (tag == null)
                url = site + "/wallpapers/top/page/" + page + "/order/date";
            else {
                if (tag.contains("/")) { //categories
                    url = site + "/categories/view/page/" + page + tag.substring(tag.indexOf("/name"));
                } else { //tag
                    try {
                        tag = URLEncoder.encode(tag, "UTF-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    url = site + "/tags/view/page/" + page + "/title/" + tag;
                }
            }
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            client.setReadTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
//            InputStream in = new BufferedInputStream(response.body().byteStream());
//            BufferedReader br = new BufferedReader(new InputStreamReader(in, Lib.ENCODING), 1000);
            BufferedReader br = new BufferedReader(response.body().charStream(), 1000);
            line = "";
            while(!line.contains("root-menu__flex"))
                line = br.readLine();
            //categories:
            int u, i = line.indexOf("root-menu__flex");
            i = line.indexOf("href", i) + 6;
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            while (line.indexOf("src", i) > 0) {
                u = line.indexOf("\"", i);
                if (i == line.indexOf("http", i))
                    break;
                bw.write(line.substring(i, u)); // url
                bw.newLine();
                i = line.indexOf("src", u) + 5;
                u = line.indexOf("\"", i);
                bw.write(line.substring(i, u)); // icon
                bw.newLine();
                i = line.indexOf("span", u) + 5;
                u = line.indexOf("<", i);
                bw.write(line.substring(i, u)); // name
                bw.newLine();
                bw.flush();
                i = line.indexOf("href", u) + 6;
            }
            bw.close();
            if (page == 0) {
                br.close();
                status = FINISH;
                count = FINISH_CATEGORIES;
                return true;
            }
            //gallery:
            GalleryRepository repository = new GalleryRepository(context, DBHelper.LIST);
            while (!line.contains("parent-element"))
                line = br.readLine();
            i = line.indexOf("parent-element");
            i = line.indexOf("element", i + 10);
            i = line.indexOf("href", i) + 6;
            int end = line.lastIndexOf("deskription");
            while (i < end && i > 5) {
                u = line.indexOf("\"", i);
                repository.addUrl(line.substring(i, u)); // url
                i = line.indexOf("src", u) + 5;
                u = line.indexOf("\"", i);
                repository.addMini(line.substring(i, u)); // mini
                i = line.indexOf("<li>", u);
                if (i == -1) break;
                i = line.indexOf("href", i) + 6;
                if (i == 5) { //skip ad block
                    br.readLine();
                    line = br.readLine();
                    if (line.contains("Yandex")) {
                        while (!line.contains("<li>"))
                            line = br.readLine();
                    } else
                        line = br.readLine();
                    end = line.lastIndexOf("deskription");
                    i = line.indexOf("href", i) + 6;
                }
            }
            //count pages:
            if (line.contains("pagination-sourse")) {
                line = line.substring(0, line.lastIndexOf("next"));
                line = line.substring(0, line.lastIndexOf("</a>"));
                line = line.substring(line.lastIndexOf(">") + 1);
                count = Integer.parseInt(line);
            }
            //finish:
            br.close();
            status = SAVING;
            repository.save(true);
            status = FINISH;
//            if (urls.size() > 0 && !boolThread)
//                startThread();
        } catch (Exception e) {
            if (f.exists())
                f.delete();
            status = ERROR;
            count = FINISH_ERROR;
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String getMini(String url_image) {
        try {
            final String link = site + "/wallpapers/get/id" +
                    url_image.substring(url_image.lastIndexOf("/")) + "/resolution/320x240";
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            client.setReadTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            Request request = new Request.Builder().url(link).build();
            Response response = client.newCall(request).execute();
            BufferedReader br = new BufferedReader(response.body().charStream(), 1000);
            String line = br.readLine();
            while (!line.contains("full-img"))
                line = br.readLine();
            br.close();
            line = line.substring(line.indexOf("src", line.indexOf("full-img")) + 5);
            return site + line.substring(0, line.indexOf("\""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getSite() {
        return site;
    }
}
