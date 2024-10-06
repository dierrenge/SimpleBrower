package cn.cheng.simpleBrower.util;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.webkit.WebResourceResponse;

import androidx.annotation.WorkerThread;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 配置文件读取器
 *
 * 调用前 需要先使用init方法注册
 */
public class AssetsReader {

    private static final String LIKE_FILE = "like.txt";
    private static final List<String> LIKE_HOSTS = new ArrayList<>();

    public static void init(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (LIKE_HOSTS.size() == 0) {
                        loadFromAssets(context);
                    }
                } catch (IOException e) {
                    // noop
                }
                return null;
            }
        }.execute();
    }

    @WorkerThread
    private static void loadFromAssets(Context context) throws IOException {
        InputStream stream = context.getAssets().open(LIKE_FILE);
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (!LIKE_FILE.contains(line)) {
                LIKE_HOSTS.add(line);
            }
        }
        bufferedReader.close();
        inputStreamReader.close();
        stream.close();
    }

    public static List<String> getLikeList() {
        return LIKE_HOSTS;
    }

}
