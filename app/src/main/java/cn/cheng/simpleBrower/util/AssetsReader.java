package cn.cheng.simpleBrower.util;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.WorkerThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 配置文件读取器
 *
 * 调用前 需要先使用init方法注册
 */
public class AssetsReader {

    private static String FILE = "";
    private static List<String> LIST = new ArrayList<>();
    private static HashMap<String, List<String>> map = new HashMap<>();

    public static void init(final Context context, String key) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    FILE = key;
                    if (LIST.isEmpty()) {
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
        InputStream stream = context.getAssets().open(FILE);
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (!FILE.contains(line)) {
                LIST.add(line);
            }
        }
        bufferedReader.close();
        inputStreamReader.close();
        stream.close();
        map.put(FILE, LIST);
    }

    public static List<String> getList(String key) {
        return map.get(key);
    }

    public static List<String> getList(Context context, String key) throws IOException {
        InputStream stream = context.getAssets().open(key);
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        List<String> list = new ArrayList<>();
        while ((line = bufferedReader.readLine()) != null) {
            if (!key.contains(line)) {
                list.add(line);
            }
        }
        bufferedReader.close();
        inputStreamReader.close();
        stream.close();
        return list;
    }

}
