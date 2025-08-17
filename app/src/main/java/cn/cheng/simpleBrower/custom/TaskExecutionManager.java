package cn.cheng.simpleBrower.custom;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.SparseArray;


/**
 * 相同 ID 的任务只由一个线程执行
 */
public class TaskExecutionManager {

    private static volatile TaskExecutionManager instance;

    private final SparseArray<Handler> mHandlerMap = new SparseArray<>();

    private TaskExecutionManager() {

    }

    public static TaskExecutionManager getInstance() {
        if (instance == null) {
            instance = new TaskExecutionManager();
        }
        return instance;
    }

    public void executeTask(int threadId, Runnable task) {
        if (mHandlerMap.indexOfKey(threadId) >= 0) {
            // 如果该 ID 的 Handler 已存在，说明任务已经在执行
            return;
        }

        HandlerThread handlerThread = new HandlerThread("Task-" + threadId);
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        mHandlerMap.put(threadId, handler);

        handler.post(() -> {
            task.run();
            // 执行完成后移除 Handler 并结束线程
            mHandlerMap.remove(threadId);
            handlerThread.quitSafely();
        });

    }
}
