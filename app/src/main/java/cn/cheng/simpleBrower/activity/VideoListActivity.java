package cn.cheng.simpleBrower.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.util.AssetsReader;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class VideoListActivity extends AppCompatActivity {

    private Button back;

    private LinearLayout layout;

    private LinearLayout video_list_head;

    private Button edit_select_all;

    private LinearLayout edit_head;

    private TextView edit_txt;

    private LinearLayout edit_close;

    private LinearLayout menu_edit;

    private LinearLayout menu_clear;

    private List<String> clearUrls = new ArrayList<>();

    private Handler handler;

    private RecyclerView recyclerView;

    private List<String> videoUrls = new ArrayList<>();

    private boolean isChange = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_video_list);
        back = findViewById(R.id.list_back);
        layout = findViewById(R.id.list_bg);
        video_list_head = findViewById(R.id.video_list_head);
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

        // 读取影音文件地址
        // initVideoUrls();
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
                clearUrls.addAll(videoUrls);
            }
            change(isChange);
            clearChange();
        });
        // 编辑
        menu_edit.setOnClickListener(view -> {
            if (!isChange && videoUrls.isEmpty()) return; // 没数据就不必编辑
            if (recyclerView != null) {
                clearUrls.clear();
                edit_select_all.setText("全选");
                edit_txt.setText("请选择");
                menu_clear.setAlpha(0.5f);
                if (isChange) {
                    video_list_head.setVisibility(View.VISIBLE);
                    edit_head.setVisibility(View.GONE);
                    menu_edit.setVisibility(View.VISIBLE);
                    menu_clear.setVisibility(View.GONE);
                } else {
                    video_list_head.setVisibility(View.GONE);
                    edit_head.setVisibility(View.VISIBLE);
                    menu_edit.setVisibility(View.GONE);
                    menu_clear.setVisibility(View.VISIBLE);
                }
                isChange = !isChange;
                change(isChange);
            }
        });
        // 删除
        menu_clear.setOnClickListener(view -> {
            if (clearUrls.isEmpty()) return;
            FeetDialog feetDialog = new FeetDialog(VideoListActivity.this, "删除", "确定要删除选中文件吗？", "删除", "取消");
            feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                @Override
                public void close() {
                    feetDialog.dismiss();
                }

                @Override
                public void ok(String txt) {
                    // 删除本项记录
                    deleteVideoUrl();
                    feetDialog.dismiss();
                }
            });
            feetDialog.show();
        });
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.list_list);

        // 创建线性布局管理器 赋值给recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 给recyclerview设置适配器
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // 加载子项布局
                View itemView = LayoutInflater.from(VideoListActivity.this)
                        .inflate(R.layout.recyclerview_item, parent, false); // 第三个参数必须是 false！
                return new RecyclerView.ViewHolder(itemView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                LinearLayout item_l = holder.itemView.findViewById(R.id.item_l);
                CheckBox item_select = holder.itemView.findViewById(R.id.item_select);
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
                // textView.setInputType(InputType.TYPE_NULL); // 屏蔽软键盘
                String videoUrl = videoUrls.get(position);
                String[] s = videoUrl.split("/");
                if (s.length > 0) {
                    textView.setText(s[s.length - 1]);
                    textView.setOnClickListener(view -> {
                        click(videoUrl, item_select);
                    });
                    item_l.setOnClickListener(view -> {
                        click(videoUrl, item_select);
                    });
                    // 解决：配置了android:textIsSelectable="true",同时也设置了点击事件，发现点第一次时候，点击事件没有生效
                    /*textView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            view.requestFocus();
                            return false;
                        }
                    });*/
                }
                item_select.setAnimation(null);
                item_select.setOnClickListener(view -> {
                    if (!item_select.isChecked()) {
                        clearUrls.removeIf(item -> item != null && item.equals(videoUrl));
                    } else {
                        clearUrls.add(videoUrl);
                    }
                    clearChange();
                });
            }

            @Override
            public int getItemCount() {
                return videoUrls.size();
            }

            public void click(String videoUrl, CheckBox item_select) {
                if (isChange) {
                    select(videoUrl, item_select);
                    return; // 编辑模式不可跳转
                }
                // 跳转该网址
                Intent intent = new Intent(VideoListActivity.this, VideoActivity.class);
                intent.putExtra("videoUrl", videoUrl);
                VideoListActivity.this.startActivity(intent);
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
        });

        // 滑动监听
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                change(isChange);
            }
        });
    }

    void initHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                if (message.what == 0) {
                    if (videoUrls.size() > 0) {
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.getAdapter().notifyDataSetChanged();
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        layout.setVisibility(View.VISIBLE);
                    }
                } else if (message.what == 3) {
                    if (recyclerView != null) {
                        String url = (String) message.obj;
                        videoUrls.removeIf(s -> s.equals(url));
                        recyclerView.getAdapter().notifyDataSetChanged();
                        recyclerView.getAdapter().notifyItemRangeChanged(0, videoUrls.size());
                        // 删完了就显示背景
                        if (videoUrls.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            layout.setVisibility(View.VISIBLE);
                            edit_close.callOnClick();
                        } else {
                            change(isChange);
                        }
                        clearUrls.removeIf(s -> s.equals(url));
                        clearChange();
                    }
                } else {
                    MyToast.getInstance(message.obj + "").show();
                }
                return false;
            }
        });
    }

    private void initVideoUrls() {
        videoUrls.clear();
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            // 影音文件格式
            List<String> formats = AssetsReader.getList("audioVideo.txt");
            new Handler().postDelayed(() -> {
                CommonUtils.fileWalk(dir, formats, videoUrls, 2);
                Message message = handler.obtainMessage(0);
                handler.sendMessage(message);
            }, 0);
        }
    }

    private void deleteVideoUrl() {
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            //启动线程开始执行 删除网址存档
            new Thread(() -> {
                try {
                    boolean hasM3u8 = clearUrls.stream().anyMatch(url -> url.endsWith(".m3u8") && url.contains("SimpleBrower"));
                    if (hasM3u8) {
                        Message message = Message.obtain();
                        message.what = 1;
                        message.obj = "删除中，请稍后";
                        handler.sendMessage(message);
                    }
                    // 先删除单个文件的
                    for (String url : clearUrls) {
                        if (!url.endsWith(".m3u8") || !url.contains("SimpleBrower")) {
                            delete(url);
                        }
                    }
                    // 后删除多个文件的
                    for (String url : clearUrls) {
                        if (url.endsWith(".m3u8") && url.contains("SimpleBrower")) {
                            delete(url);
                        }
                    }
                } catch (Exception e) {
                    e.getMessage();
                }
            }).start();
        }
    }
    private void delete(String url) {
        boolean isDelete;
        if (!url.endsWith(".m3u8") || !url.contains("SimpleBrower")) {
            File file = new File(url);
            isDelete = CommonUtils.deleteFile(file);
        } else {
            // 删除该m3u8对应的所有ts文件
            File file = new File(url);
            String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            File dirFile = new File(dir + "/SimpleBrower/m3u8/" + url.substring(url.lastIndexOf("/") + 1).replace(".m3u8", ""));
            isDelete = CommonUtils.batchDeleteFile(dirFile);
            if (isDelete) {
                isDelete = CommonUtils.deleteFile(file);
            }
        }
        // 通知handler 数据删除完成 可以刷新recyclerview
        Message message = Message.obtain();
        if (isDelete) {
            message.what = 3;
            message.obj = url;
        } else {
            message.what = 1;
            message.obj = "删除失败（" + CommonUtils.getUrlName(url) + "）" ;
        }
        handler.sendMessage(message);
    }

    private void change(boolean isChange) {
        new Handler().postDelayed(() -> {
            int num = recyclerView.getAdapter().getItemCount();
            for (int i = 0; i < num; i++) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                if (viewHolder != null) {
                    CheckBox item_select = viewHolder.itemView.findViewById(R.id.item_select);
                    item_select.setChecked(clearUrls.contains(videoUrls.get(i)));
                    if (isChange) {
                        item_select.setVisibility(View.VISIBLE);
                    } else {
                        item_select.setVisibility(View.INVISIBLE);
                        item_select.setChecked(false);
                    }
                }
            }
            if (isChange) {
                edit_select_all.setVisibility(View.VISIBLE);
            } else {
                edit_select_all.setVisibility(View.INVISIBLE);
            }
            if (videoUrls.isEmpty()) {
                menu_edit.setAlpha(0.5f);
            } else  {
                menu_edit.setAlpha(1f);
            }
            // clearChange();
        }, 50);
    }

    private void clearChange() {
        if (new HashSet<>(clearUrls).containsAll(videoUrls)) {
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
        initVideoUrls();
        new Handler().post(() -> {
            change(isChange);
        });
        super.onResume();
    }
}