package cn.cheng.simpleBrower.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.M3u8DownLoader;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.AssetsReader;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.MIMEUtils;
import cn.cheng.simpleBrower.util.NotificationUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class DownloadActivity extends AppCompatActivity {

    private Button back;

    private LinearLayout layout;

    private LinearLayout download_list_head;

    private Button edit_select_all;

    private LinearLayout edit_head;

    private TextView edit_txt;

    private LinearLayout edit_close;

    private LinearLayout menu_edit;

    private LinearLayout menu_clear;

    private List<String> clearUrls = new ArrayList<>();

    private Map<String, String> clearMap = new HashMap<>(); // 删除的文件名

    private Handler handler;

    private Handler timer = new Handler();

    private static RecyclerView recyclerView;

    private List<String> fileUrls = new ArrayList<>();

    private volatile int notificationNum; // 消息数

    private boolean isChange = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_download);
        layout = findViewById(R.id.download_bg);
        back = findViewById(R.id.download_back);
        download_list_head = findViewById(R.id.download_list_head);
        edit_head = findViewById(R.id.edit_head);
        edit_close = findViewById(R.id.edit_close);
        edit_txt = findViewById(R.id.edit_txt);
        edit_select_all = findViewById(R.id.edit_select_all);
        menu_edit = findViewById(R.id.menu_edit);
        menu_clear = findViewById(R.id.menu_clear);

        initEvent();

        initRecyclerView();

        // 初始化线程通信工具
        initHandler();

        initFileUrls();
    }

    // 按钮事件
    private void initEvent() {
        // 返回
        back.setOnClickListener(view -> {
            this.finish();
        });
        // 退出编辑
        edit_close.setOnClickListener(view -> {
            if (isChange) {
                menu_edit.callOnClick();
            }
        });
        // 多选
        edit_select_all.setOnClickListener(view -> {
            clearUrls.clear();
            if ("全选".equals(edit_select_all.getText().toString())) {
                clearUrls.addAll(fileUrls);
            }
            change();
            clearChange();
        });
        // 编辑
        menu_edit.setOnClickListener(view -> {
            if (!isChange && fileUrls.isEmpty()) return; // 没数据就不必编辑
            if (recyclerView != null) {
                clearUrls.clear();
                edit_select_all.setText("全选");
                edit_txt.setText("请选择");
                menu_clear.setAlpha(0.5f);
                if (isChange) {
                    download_list_head.setVisibility(View.VISIBLE);
                    edit_head.setVisibility(View.GONE);
                    menu_edit.setVisibility(View.VISIBLE);
                    menu_clear.setVisibility(View.GONE);
                } else {
                    download_list_head.setVisibility(View.GONE);
                    edit_head.setVisibility(View.VISIBLE);
                    menu_edit.setVisibility(View.GONE);
                    menu_clear.setVisibility(View.VISIBLE);
                }
                isChange = !isChange;
                change();
            }
        });
        // 删除
        menu_clear.setOnClickListener(view -> {
            if (clearUrls.isEmpty()) return;
            FeetDialog feetDialog = new FeetDialog(DownloadActivity.this, "删除", "确定删除选中的下载记录吗？", "删除", "取消");
            feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                @Override
                public void close() {
                    feetDialog.dismiss();
                }

                @Override
                public void ok(String txt) {
                    // 删除本项记录
                    deleteFileRecord(txt);
                    feetDialog.dismiss();
                }
            });
            feetDialog.show();
        });
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.download_mg_list);

        // 创建线性布局管理器 赋值给recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 设置无动画（解决闪烁问题）
        recyclerView.setItemAnimator(null);

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
                    LinearLayout item_btn_l = holder.itemView.findViewById(R.id.item_btn_l);
                    LinearLayout item_select_l = holder.itemView.findViewById(R.id.item_select_l);
                    CheckBox item_select = holder.itemView.findViewById(R.id.item_select2);
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
                        if (isChange) {
                            select(fileRecordUrl, item_select);
                            return; // 编辑模式不可跳转
                        }
                        click(button, bean, processView);
                    });
                    item_l.setOnClickListener(view -> {
                        if (isChange) {
                            select(fileRecordUrl, item_select);
                            return; // 编辑模式不可跳转
                        }
                        click(button, bean, processView);
                    });
                    button.setOnClickListener(view -> {
                        try {
                            if ("完成".equals(button.getText().toString())) return;
                            button.setClickable(false);
                            String state = bean.getState();
                            state = state.equals("暂停") ? "继续" : "暂停";
                            bean.setState(state);
                            int notificationId = bean.getNotificationId();
                            if (fileRecordUrl.contains("/")) { // 从磁盘中读取记录
                                if (state.equals("暂停")) {
                                    NotificationBean info = MyApplication.getDownLoadInfo(notificationId);
                                    if (info == null) { // 内存中没有 说明没重复
                                        // CommonUtils.deleteLocalObject("downloadList", bean.getDate() + CommonUtils.zeroPadding(bean.getNotificationId()));
                                        Intent intent = new Intent(DownloadActivity.this, DownloadService.class);
                                        intent.putExtra("what", bean.getWhat());
                                        intent.putExtra("url", bean.getUrl());
                                        intent.putExtra("title", bean.getTitle());
                                        intent.putExtra("notificationId", notificationId);
                                        DownloadActivity.this.startService(intent);
                                        fileUrls.set(position, CommonUtils.getUrlName(fileRecordUrl));
                                        MyApplication.setDownLoadInfo(notificationId, bean);
                                    }
                                }
                            } else { // 从内存中读取
                                NotificationBean info = MyApplication.getDownLoadInfo(notificationId);
                                if (info != null) {
                                    String processStr = getProcess(info);
                                    info.setState(state);
                                    // bean.setState(state);
                                    // CommonUtils.writeObjectIntoLocal("downloadList", bean.getDate() + CommonUtils.zeroPadding(bean.getNotificationId()), bean);
                                    if (state.equals("暂停") && !processStr.contains("100")) {
                                        new M3u8DownLoader(notificationId).start();
                                    }
                                } else if (state.equals("暂停")) { // 内存中的记录被删除的情况
                                    // CommonUtils.deleteLocalObject("downloadList", bean.getDate() + CommonUtils.zeroPadding(bean.getNotificationId()));
                                    Intent intent = new Intent(DownloadActivity.this, DownloadService.class);
                                    intent.putExtra("what", bean.getWhat());
                                    intent.putExtra("url", bean.getUrl());
                                    intent.putExtra("title", bean.getTitle());
                                    intent.putExtra("notificationId", notificationId);
                                    DownloadActivity.this.startService(intent);
                                    String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                                    String url = dir + "/SimpleBrower/0_like/downloadList/" + fileRecordUrl + ".json";
                                    fileUrls.set(position, url);
                                    MyApplication.setDownLoadInfo(notificationId, bean);
                                }
                            }
                            button.setText(state);
                            button.setClickable(true);
                        } catch (Throwable e) {
                            CommonUtils.saveLog("=======处理按钮点击事件notification_clicked2=======" + e.getMessage());
                        }
                    });

                    // 刷新ui
                    clearMap.put(fileRecordUrl, CommonUtils.getUrlName(bean.getAbsolutePath()));
                    textView.setText(CommonUtils.getUrlName2(bean.getAbsolutePath()));
                    String processStr = getProcess(bean);
                    processView.setText(processStr);
                    if (processStr.contains("100")) {
                        if (!"完成".equals(button.getText().toString())) {
                            button.setText("完成");
                        }
                    } else {
                        if (!bean.getState().equals(button.getText().toString())) {
                            button.setText(bean.getState());
                        }
                    }

                    item_select.setChecked(clearUrls.contains(fileUrls.get(position)));
                    if (isChange) {
                        item_select_l.setVisibility(View.VISIBLE);
                        item_btn_l.setVisibility(View.GONE);
                    } else {
                        item_select_l.setVisibility(View.GONE);
                        item_btn_l.setVisibility(View.VISIBLE);
                        item_select.setChecked(false);
                    }
                    item_select.setAnimation(null);
                    item_select.setOnClickListener(view -> {
                        if (!item_select.isChecked()) {
                            clearUrls.removeIf(item -> item != null && item.equals(fileRecordUrl));
                        } else {
                            clearUrls.add(fileRecordUrl);
                        }
                        clearChange();
                    });
                } catch (Exception e) {
                    CommonUtils.saveLog("onBindViewHolder========" + e.getMessage());
                }
            }

            @Override
            public int getItemCount() {
                return fileUrls.size();
            }

            public void click(Button button, NotificationBean bean, TextView processView) {
                String btnTxt = button.getText().toString();
                if ("完成".equals(btnTxt) && bean != null) {
                    String absolutePath = bean.getAbsolutePath();
                    if (absolutePath == null) return;
                    if (!new File(absolutePath).exists()) {
                        FeetDialog feetDialog = new FeetDialog(DownloadActivity.this, "下载", "原文件已删除，是否重新下载？", "下载", "取消");
                        feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                            @Override
                            public void close() {
                                feetDialog.dismiss();
                            }

                            @Override
                            public void ok(String txt) {
                                bean.setState("继续");
                                bean.setBytesum(0);
                                bean.setTotalSize(0);
                                bean.setHlsFinishedCount(0);
                                bean.getHlsFinishedNumList().clear();
                                processView.setText("0.00%");
                                button.setText("重置");
                                button.callOnClick();
                                feetDialog.dismiss();
                            }
                        });
                        feetDialog.show();
                        return;
                    }
                    String format = CommonUtils.getUrlFormat(absolutePath);
                    List<String> formats = AssetsReader.getList("audioVideo.txt");
                    // 跳转该网址
                    Intent intent = null;
                    if (".txt".equalsIgnoreCase(format)) {
                        if (MyApplication.isTurnPageFlag()) {
                            MyToast.getInstance("朗读翻页中，请稍后").show();
                            return;
                        }
                        intent = new Intent(DownloadActivity.this, TxtActivity.class);
                        intent.putExtra("txtUrl", absolutePath);
                    } else if (formats.contains(format)) {
                        intent = new Intent(DownloadActivity.this, VideoActivity.class);
                        intent.putExtra("videoUrl", absolutePath);
                    } else {
                        try {
                            // 系统选择打开方式
                            intent = new Intent(Intent.ACTION_VIEW);
                            File file = new File(absolutePath);
                            Uri fileUri;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                // 使用 FileProvider 获取 URI
                                fileUri = FileProvider.getUriForFile(DownloadActivity.this, "cn.cheng.simpleBrower.fileprovider", file);
                            } else {
                                fileUri = Uri.fromFile(file);
                            }
                            intent.setDataAndType(fileUri, MIMEUtils.getMIMEType(absolutePath));
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 解决权限问题
                            intent = Intent.createChooser(intent, "选择要使用的应用"); // 弹出选择界面
                            if (intent.resolveActivity(getPackageManager()) == null) {
                                MyToast.getInstance("无应用可使用").show(); // 如果未找到处理程序，提供错误提示（可选）
                                return;
                            }
                        } catch (Exception e) {
                            CommonUtils.saveLog("系统选择打开方式" + e.getMessage());
                        }
                    }
                    if (intent != null) DownloadActivity.this.startActivity(intent);
                }
            }

            public void select(String likeUrl, CheckBox item_select) {
                if (item_select.isChecked()) {
                    item_select.setChecked(false);
                    clearUrls.removeIf(item -> item != null && item.equals(likeUrl));
                } else {
                    item_select.setChecked(true);
                    clearUrls.add(likeUrl);
                }
                clearChange();
            }
        };
        recyclerView.setAdapter(adapter);

        // 滑动监听
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                // change();
            }
        });
    }

    void initHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                if (message.what == 0) {
                    if (fileUrls.size() > 0) {
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.getAdapter().notifyDataSetChanged();
                        layout.setVisibility(View.GONE);
                        menu_edit.setAlpha(1f);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        layout.setVisibility(View.VISIBLE);
                        menu_edit.setAlpha(0.5f);
                    }
                } else if (message.what == 3) {
                    if (recyclerView != null) {
                        String url = (String) message.obj;
                        fileUrls.removeIf(s -> s.equals(url));
                        recyclerView.getAdapter().notifyDataSetChanged();
                        clearUrls.removeIf(s -> s.equals(url));
                        // recyclerView.getAdapter().notifyItemRangeChanged(0, fileUrls.size());
                        // 删完了就显示背景
                        if (fileUrls.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            layout.setVisibility(View.VISIBLE);
                            edit_close.callOnClick();
                        }
                        clearChange();
                    }
                } else {
                    MyToast.getInstance(message.obj + "").show();
                }
                return false;
            }
        });
    }

    private void initFileUrls() {
        notificationNum = MyApplication.getDownLoadInfoMap().size();
        fileUrls.clear();
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            new Handler().post(() -> {
                CommonUtils.downloadListFileWalk(dir + "/SimpleBrower/0_like/downloadList", fileUrls);
                Message message = handler.obtainMessage(0);
                handler.sendMessage(message);
            });
        }
    }

    private void deleteFileRecord(String txt) {
        if (Build.VERSION.SDK_INT >= 29 ) { // android 12的sd卡读写
            boolean isDeleteO = "delete".equals(txt);
            //启动线程开始执行 删除网址存档
            for (String url : clearUrls) {
                new Thread(() -> {
                    try {
                        if (isDeleteO) { // 删除原文件
                            NotificationBean bean = CommonUtils.readObjectFromLocal(NotificationBean.class, url);
                            if (bean != null && bean.getAbsolutePath() != null) {
                                String absolutePath = bean.getAbsolutePath();
                                if (!url.endsWith(".m3u8")) {
                                    CommonUtils.deleteFile(new File(absolutePath));
                                }
                            }
                        }
                        File file = new File(url);
                        boolean isDelete = CommonUtils.deleteFile(file);
                        // 通知handler 数据删除完成 可以刷新recyclerview
                        Message message = Message.obtain();
                        if (isDelete) {
                            message.what = 3;
                            message.obj = url;
                        } else {
                            message.what = 1;
                            message.obj = "删除失败（" + clearMap.get(url) + "）";
                        }
                        handler.sendMessage(message);
                    } catch (Exception e) {
                        e.getMessage();
                    }
                }).start();
            }
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (recyclerView == null) return;
                int num = recyclerView.getAdapter().getItemCount();
                if (num != fileUrls.size()) return;
                if (notificationNum != MyApplication.getDownLoadInfoMap().size()) {
                    initFileUrls();
                } else {
                    for (int i = 0; i < num; i++) {
                        String fileRecordUrl = fileUrls.get(i);
                        if (!fileRecordUrl.contains("/")) {
                            int notificationId = Integer.parseInt(fileRecordUrl.substring(8));
                            NotificationBean bean = MyApplication.getDownLoadInfo(notificationId);
                            if (bean == null) continue;
                            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                            if (holder != null) {
                                Button button = holder.itemView.findViewById(R.id.item_download_btn);
                                TextView processView  = holder.itemView.findViewById(R.id.downloadProcess);
                                String btnTxt = button.getText().toString();
                                String processTxt = processView.getText().toString();
                                String process = getProcess(bean);
                                if ("完成".equals(btnTxt) || (btnTxt.equals(bean.getState()) && process.equals(processTxt))) continue;
                                recyclerView.getAdapter().notifyItemChanged(i);
                            }
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

    // 获取下载进度
    private String getProcess(NotificationBean bean) {
        float process = 0;
        if (bean.getTsList() != null && bean.getTsList().size() > 0) {
            process = CommonUtils.getPercentage(bean.getHlsFinishedCount(), bean.getTsList().size());
        } else if (bean.getTotalSize() > 0) {
            process = CommonUtils.getPercentage(bean.getBytesum(), bean.getTotalSize());
        }
        return String.format("%.2f", process) + "%";
    }

    private void change() {
        int num = recyclerView.getAdapter().getItemCount();
        if (num > 0) recyclerView.getAdapter().notifyItemRangeChanged(0, num);
        if (isChange) {
            edit_select_all.setVisibility(View.VISIBLE);
        } else {
            edit_select_all.setVisibility(View.INVISIBLE);
        }
        if (fileUrls.isEmpty()) {
            menu_edit.setAlpha(0.5f);
        } else {
            menu_edit.setAlpha(1f);
        }
    }

    private void clearChange() {
        if (new HashSet<>(clearUrls).containsAll(fileUrls)) {
            edit_select_all.setText("取消");
        } else {
            edit_select_all.setText("全选");
        }
        if (clearUrls.isEmpty()) {
            edit_txt.setText("请选择");
            menu_clear.setAlpha(0.5f);
        } else  {
            menu_clear.setAlpha(1f);
            edit_txt.setText("已选择" + clearUrls.size() + "项");
        }
    }

    // 物理按键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { // 返回
            if (isChange) {
                menu_edit.callOnClick();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
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