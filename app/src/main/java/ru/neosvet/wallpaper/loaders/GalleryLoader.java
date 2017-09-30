package ru.neosvet.wallpaper.loaders;

import android.content.Context;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;

import ru.neosvet.wallpaper.database.DBHelper;
import ru.neosvet.wallpaper.database.GalleryRepository;
import ru.neosvet.wallpaper.utils.GalleryService;
import ru.neosvet.wallpaper.utils.Lib;

/**
 * Created by NeoSvet on 06.08.2017.
 */

public class GalleryLoader extends GalleryService.Loader {
    private Context context;
    private String site;

    public GalleryLoader(Context context, String site) {
        this.context = context;
        this.site = site;
    }

    @Override
    public boolean download(int page, String tag) {
        String line;
        File f = new File(context.getFilesDir() + Lib.CATEGORIES);
        try {
            //start:
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            client.setReadTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            String url;
            if (tag == null)
                url = site + "/page/" + page + "/";
            else if (tag.contains("/"))  //categories
                url = site + tag + "/page/" + page + "/";
            else //tag
                url = site + "/tags/" + tag.replace(" ", "+") + "/page/" + page + "/";
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
//            InputStream in = new BufferedInputStream(response.body().byteStream());
//            BufferedReader br = new BufferedReader(new InputStreamReader(in, Lib.ENCODING), 1000);
            BufferedReader br = new BufferedReader(response.body().charStream(), 1000);
            line = br.readLine();
            //categories:
            while (!line.contains("Sandbox"))
                line = br.readLine();
            line = br.readLine();
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            while (!line.contains("</ul>")) {
                if (line.contains("href")) {
                    line = line.substring(line.indexOf("href") + 6,
                            line.indexOf(">") - 2);
                    bw.write(line);
                    bw.newLine();
                    line = br.readLine();
                    bw.write(line.trim().replace("amp;", ""));
                    bw.newLine();
                    bw.flush();
                }
                line = br.readLine();
            }
            bw.close();
            if (page == 0) {
                br.close();
                status = GalleryService.FINISH;
                count = GalleryService.FINISH_CATEGORIES;
                return true;
            }
            //gallery:
            String url_image;
            GalleryRepository repository = new GalleryRepository(context, DBHelper.LIST);
            while (!line.contains("col-md-4"))
                line = br.readLine();
            while (!line.contains("<nav>") && !line.contains("<noindex>")) {
                line = br.readLine();
                if (line.contains("thumbnail")) {
                    line = br.readLine();
                    line = line.substring(line.indexOf("href") + 6);
                    url_image = line.substring(0, line.indexOf("\""));
                    repository.addUrl(url_image);
                    line = br.readLine();
                    if (line.contains("holder.js")) //for tag
                        line = br.readLine();
                    line = line.substring(line.indexOf("/", line.indexOf(".")), line.length() - 1);
                    repository.addMini(line);
                }
            }
            //count pages:
            if (line.contains("<nav>")) {
                line = br.readLine();
                while (!line.contains("next") && !line.contains("last")) {
                    line += br.readLine();
                }
                if (line.contains("last")) {
                    line = line.substring(0, line.indexOf("current") - 2);
                    line = line.substring(line.lastIndexOf(">") + 1);
                    line = line.substring(0, line.indexOf(" "));
                } else {
                    line = line.substring(0, line.indexOf("next"));
                    line = line.substring(line.lastIndexOf("\">") + 2);
                    line = line.substring(0, line.indexOf("<"));
                }
                count = Integer.parseInt(line);
            }
            //finish:
            br.close();
            status = GalleryService.SAVING;
            repository.save(true);
            status = GalleryService.FINISH;
//            if (urls.size() > 0 && !boolThread)
//                startThread();
        } catch (Exception e) {
            if (f.exists())
                f.delete();
            status = GalleryService.ERROR;
            count = GalleryService.FINISH_ERROR;
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String getMini(String url_image) {
        try {
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            client.setReadTimeout(Lib.TIMEOUT, TimeUnit.SECONDS);
            Request request = new Request.Builder().url(url_image).build();
            Response response = client.newCall(request).execute();
            String line = response.body().string();
            line = line.substring(line.indexOf("ges/") + 3);
            String s = line.substring(0, 8);
            line = line.substring(line.indexOf("_") + 1);
            line = line.substring(0, line.indexOf("\"") + 1);
            s = s + line;
            return site + "/mini" + s.substring(0, s.length() - 1);
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
