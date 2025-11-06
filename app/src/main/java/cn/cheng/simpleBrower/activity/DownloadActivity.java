package cn.cheng.simpleBrower.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.DownloadBean;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.custom.M3u8DownLoader;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.AssetsReader;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class DownloadActivity extends AppCompatActivity {

    private Button back;

    private Button change;

    private LinearLayout layout;

    private Handler handler;

    private Handler timer = new Handler();

    private static RecyclerView recyclerView;

    private List<String> fileUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_download);

        // 返回
        back = findViewById(R.id.download_back);
        back.setOnClickListener(view -> {
            this.finish();
        });

        // 编辑
        change = findViewById(R.id.download_change);
        change.setOnClickListener(view -> {
            if (recyclerView != null) {

            }
        });

        // 背景
        layout = findViewById(R.id.download_bg);

        initRecyclerView();

        // 初始化线程通信工具
        initHandler();
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.download_mg_list);

        // 创建线性布局管理器 赋值给recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 给recyclerview设置适配器
        RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // 加载子项布局
                View itemView = LayoutInflater.from(DownloadActivity.this)
                        .inflate(R.layout.recyclerview_download_item, parent, false); // 第三个参数必须是 false！
                return new RecyclerView.ViewHolder(itemView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                try {
                    LinearLayout item_l = holder.itemView.findViewById(R.id.item_download_l);
                    TextView textView = holder.itemView.findViewById(R.id.item_download_txt);
                    TextView processView  = holder.itemView.findViewById(R.id.downloadProcess);
                    Button button = holder.itemView.findViewById(R.id.item_download_btn);
                    String fileRecordUrl = fileUrls.get(position);
                    NotificationBean bean;
                    if (fileRecordUrl.contains("/")) {
                        bean = CommonUtils.readObjectFromLocal(NotificationBean.class, fileRecordUrl);
                        if (bean == null) return;
                        bean.setState("继续");
                    } else {
                        int notificationId = Integer.parseInt(fileRecordUrl.substring(8));
                        bean = MyApplication.getDownLoadInfo(notificationId);
                        if (bean == null) return;
                    }
                    textView.setOnClickListener(view -> {
                        // click();
                    });
                    item_l.setOnClickListener(view -> {
                        // click();
                    });
                    button.setOnClickListener(view -> {
                        if ("完成".equals(button.getText().toString())) return;
                        try {
                            String state = bean.getState();
                            state = state.equals("暂停") ? "继续" : "暂停";
                            int notificationId = bean.getNotificationId();
                            if (fileRecordUrl.contains("/")) {
                                NotificationBean info = MyApplication.getDownLoadInfo(notificationId);
                                if (info != null) return; // 说明重复了
                                // CommonUtils.deleteLocalObject("downloadList", bean.getDate() + bean.getNotificationId());
                                Intent intent = new Intent(DownloadActivity.this, DownloadService.class);
                                intent.putExtra("what", bean.getWhat());
                                intent.putExtra("url", bean.getUrl());
                                intent.putExtra("title", bean.getTitle());
                                intent.putExtra("notificationId", notificationId);
                                DownloadActivity.this.startService(intent);
                                fileUrls.set(position, CommonUtils.getUrlName(fileRecordUrl));
                            } else {
                                NotificationBean info = MyApplication.getDownLoadInfo(notificationId);
                                if (info != null) info.setState(state);
                                CommonUtils.updateRemoteViews(notificationId, null, state, null);
                                // bean.setState(state);
                                // CommonUtils.writeObjectIntoLocal("downloadList", bean.getDate() + bean.getNotificationId(), bean);
                                if (state.equals("暂停")) {
                                    new M3u8DownLoader(notificationId).start();
                                }
                            }
                            button.setText(state);
                        } catch (Throwable e) {
                            CommonUtils.saveLog("=======处理按钮点击事件notification_clicked2=======" + e.getMessage());
                        }
                    });

                    // 刷新ui
                    textView.setText(CommonUtils.getUrlName2(bean.getAbsolutePath()));
                    float process = 0;
                    if (bean.getTsList() != null && bean.getTsList().size() > 0) {
                        process = CommonUtils.getPercentage(bean.getHlsFinishedCount(), bean.getTsList().size());
                    } else if (bean.getTotalSize() > 0) {
                        process = CommonUtils.getPercentage(bean.getBytesum(), bean.getTotalSize());
                    }
                    String processStr = String.format("%.2f", process) + "%";
                    processView.setText(processStr);
                    if (process == 100) {
                        button.setText("完成");
                    } else {
                        button.setText(bean.getState());
                    }
                } catch (Exception e) {
                    CommonUtils.saveLog("onBindViewHolder========" + e.getMessage());
                }
            }

            @Override
            public int getItemCount() {
                return fileUrls.size();
            }

            public void click(String fileUrl) {
                // 跳转该网址
                Intent intent = new Intent(DownloadActivity.this, TxtActivity.class);
                intent.putExtra("FileUrl", fileUrl);
                DownloadActivity.this.startActivity(intent);
            }
        };
        recyclerView.setAdapter(adapter);

        // 滑动监听
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
 
            }
        });

        // 配置触摸事件
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, // 拖拽方向
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // 滑动方向
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                try {
                    // 处理滑动删除
                    int position = viewHolder.getAdapterPosition();
                    String url = fileUrls.get(position);
                    deleteFileRecord(url, position);
                } catch (Throwable e) {
                    CommonUtils.saveLog("==底部对话框===处理滑动删除=========" + e.getMessage());
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    void initHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                if (message.what == 0) {
                    if (fileUrls.size() > 0) {
                        recyclerView.getAdapter().notifyDataSetChanged();
                    } else {
                        layout.setVisibility(View.VISIBLE);
                    }
                } else if (message.what == 3) {
                    if (recyclerView != null) {
                        String[] arr = (String[]) message.obj;
                        fileUrls.removeIf(s -> s.equals(arr[0]));
                        recyclerView.getAdapter().notifyDataSetChanged();
                        recyclerView.getAdapter().notifyItemRangeChanged(0, fileUrls.size());
                    }
                    // 删完了就显示背景
                    if (fileUrls.size() == 0) {
                        layout.setVisibility(View.VISIBLE);
                    }
                } else {
                    MyToast.getInstance(message.obj + "").show();
                }
                return false;
            }
        });
    }

    private void initFileUrls() {
        fileUrls.clear();
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            new Handler().postDelayed(() -> {
                CommonUtils.downloadListFileWalk(dir + "/SimpleBrower/0_like/downloadList", fileUrls);
                Message message = handler.obtainMessage(0);
                handler.sendMessage(message);
            }, 0);
        }
    }

    private void deleteFileRecord(String url, int position) {
        if (Build.VERSION.SDK_INT >= 29 && url != null) { // android 12的sd卡读写
            //启动线程开始执行 删除网址存档
            new Thread(() -> {
                boolean isDelete = false;
                try {
                    File file = new File(url);
                    isDelete = CommonUtils.deleteFile(file);
                    // 通知handler 数据删除完成 可以刷新recyclerview
                    Message message = Message.obtain();
                    if (isDelete) {
                        message.what = 3;
                        message.obj = new String[]{url, position+""};
                    } else {
                        message.what = 1;
                        message.obj = "删除失败";
                    }
                    handler.sendMessage(message);
                } catch (Exception e) {
                    e.getMessage();
                }
            }).start();
        }
    }

    private void flushUi() {
        
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (recyclerView == null) return;
                int num = recyclerView.getAdapter().getItemCount();
                if (num != fileUrls.size()) return;
                for (int i = 0; i < num; i++) {
                    String fileRecordUrl = fileUrls.get(i);
                    if (!fileRecordUrl.contains("/")) {
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                        if (holder != null) {
                            TextView processView  = holder.itemView.findViewById(R.id.downloadProcess);
                            Button button = holder.itemView.findViewById(R.id.item_download_btn);
                            String btnTxt = button.getText().toString();
                            String procTxt = processView.getText().toString();
                            if ("完成".equals(btnTxt)) continue;
                            int notificationId = Integer.parseInt(fileRecordUrl.substring(8));
                            NotificationBean bean = MyApplication.getDownLoadInfo(notificationId);
                            if (bean == null) continue;
                            float process = 0;
                            if (bean.getTsList() != null && bean.getTsList().size() > 0) {
                                process = CommonUtils.getPercentage(bean.getHlsFinishedCount(), bean.getTsList().size());
                            } else if (bean.getTotalSize() > 0) {
                                process = CommonUtils.getPercentage(bean.getBytesum(), bean.getTotalSize());
                            }
                            String processStr = String.format("%.2f", process) + "%";
                            if ("继续".equals(bean.getState()) && "继续".equals(btnTxt) && processStr.equals(procTxt)) continue;
                            recyclerView.getAdapter().notifyItemChanged(i);
                        }
                    }
                }
                // recyclerView.getAdapter().notifyDataSetChanged();
                timer.postDelayed(this, 1000);
            } catch (Exception e) {
                CommonUtils.saveLog("runnable=====" + e.getMessage());
            }
        }
    };

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
        initFileUrls();
        timer.postDelayed(runnable, 1000);
        super.onResume();
    }

    @Override
    protected void onPause() {
        timer.removeCallbacks(runnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        timer.removeCallbacks(runnable);
        super.onDestroy();
    }
}