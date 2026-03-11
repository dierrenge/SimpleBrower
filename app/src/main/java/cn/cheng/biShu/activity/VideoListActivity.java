package cn.cheng.biShu.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import cn.cheng.biShu.R;
import cn.cheng.biShu.custom.FeetDialog;
import cn.cheng.biShu.custom.LongClickDialog;
import cn.cheng.biShu.custom.LongTouchListener;
import cn.cheng.biShu.custom.MyToast;
import cn.cheng.biShu.util.AssetsReader;
import cn.cheng.biShu.util.CommonUtils;
import cn.cheng.biShu.util.PhoneSysPath;
import cn.cheng.biShu.util.SysWindowUi;

public class VideoListActivity extends AppCompatActivity {

    private LinearLayout back;

    private LinearLayout layout;

    private LinearLayout video_file;

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

    private int mWindowTop; // recyclerView距离屏幕顶部的高度

    private List<String> videoUrls = new ArrayList<>();

    private boolean isChange = false;

    private static final int REQUEST_CODE_PICK_FILE = 7;

    private ValueCallback<Boolean> stopLongTouchCallback;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_video_list);
        back = findViewById(R.id.list_back);
        layout = findViewById(R.id.list_bg);
        video_file = findViewById(R.id.video_file);
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
    }

    // 按钮事件
    private void initEvent() {
        // 返回
        back.setOnClickListener(view -> {
            this.finish();
        });
        // 文件管理
        video_file.setOnClickListener(view -> {
            try {
                // 1. 创建Intent
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                // String[] packageName = CommonUtils.getPackageName(this);
                // intent.setComponent(new ComponentName(packageName[0], packageName[1])); // 只能单选 已弃用
                // 2. 关键：启用多选模式
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                // 3. 可选：限制文件类型
                intent.setType("*/*");  // 允许所有文件类型
                String[] mimeTypes = {"audio/*", "video/*"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                // 4. 添加临时文件读取权限
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // 5. 启动选择器
                startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
            } catch (Exception e1) {
                CommonUtils.saveLog("download_file.setOnClickListener：" + e1.getMessage());
            }
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
            change();
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
                change();
            }
        });
        // 删除
        menu_clear.setOnClickListener(view -> {
            if (clearUrls.isEmpty()) return;
            String text = "确定要删除选中文件吗？";
            if (clearUrls.size() == 1) {
                String[] s = clearUrls.get(0).split("/");
                if (s.length > 0) {
                    text = "确定要删除文件“" + s[s.length - 1] + "”吗？";
                }
            }
            FeetDialog feetDialog = new FeetDialog(VideoListActivity.this, "删除", text, "删除", "取消");
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
        recyclerView.post(() -> {
            int[] outLocation = new int[2];
            recyclerView.getLocationOnScreen(outLocation);
            mWindowTop = outLocation[1];
        });

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

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                LinearLayout item_layout = holder.itemView.findViewById(R.id.item_layout);
                item_layout.setBackgroundResource(R.color.white);
                LinearLayout item_select_bg = holder.itemView.findViewById(R.id.item_select_bg);
                CheckBox item_select = holder.itemView.findViewById(R.id.item_select);
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
                // textView.setInputType(InputType.TYPE_NULL); // 屏蔽软键盘
                String videoUrl = videoUrls.get(position);
                String[] s = videoUrl.split("/");
                if (s.length > 0) {
                    String name = s[s.length - 1];
                    textView.setText(name);
                    textView.setOnLongClickListener(view -> true);
                    textView.setOnTouchListener(new LongTouchListener() {
                        @Override
                        public void downEvent(ValueCallback<Boolean> callback) {
                            stopLongTouchCallback = callback;
                            item_layout.setBackgroundResource(R.color.colorLightGray1);
                        }
                        @Override
                        public void realUpEvent() {
                            item_layout.setBackgroundResource(R.color.white);
                        }
                        @Override
                        public void clickEvent() {
                            click(videoUrl, item_select);
                        }
                        @Override
                        public void lClickEvent() {
                            if (isChange) select(videoUrl, item_select);
                        }
                        @Override
                        public void longEvent(float x, float y) {
                            if (isChange) return;
                            setScrollEnabled(false);
                            LongClickDialog dialog = new LongClickDialog(VideoListActivity.this, x, y - mWindowTop);
                            dialog.setOnTouchListener(new LongClickDialog.TouchListener() {
                                @Override
                                public void close() {
                                    dialog.dismiss();
                                    setScrollEnabled(true);
                                }
                                @Override
                                public void open() {
                                    dialog.dismiss();
                                    click(videoUrl, item_select);
                                    setScrollEnabled(true);
                                }
                                @Override
                                public void copy() {
                                    dialog.dismiss();
                                    CommonUtils.copy(VideoListActivity.this, name);
                                    setScrollEnabled(true);
                                }
                                @Override
                                public void modify() {
                                    dialog.dismiss();
                                    FeetDialog feetDialog = new FeetDialog(VideoListActivity.this, "重命名", name, "确定", "取消");
                                    feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                                        @Override
                                        public void close() {
                                            feetDialog.dismiss();
                                        }
                                        @Override
                                        public void ok(String txt) {
                                            String fileUrl = videoUrl.substring(0, videoUrl.lastIndexOf("/") + 1) + txt;
                                            if (!txt.startsWith(".") && !videoUrl.equals(fileUrl)) {
                                                new Thread(() -> {
                                                    boolean re = new File(videoUrl).renameTo(new File(fileUrl));
                                                    Message message = Message.obtain();
                                                    if (!re) {
                                                        message.what = 1;
                                                        message.obj = "更名失败" ;
                                                    } else {
                                                        message.what = 2;
                                                        message.obj = new String[] {txt, videoUrl, fileUrl, position+""};
                                                    }
                                                    handler.sendMessage(message);
                                                }).start();
                                            }
                                            feetDialog.dismiss();
                                        }
                                    });
                                    feetDialog.show();
                                    setScrollEnabled(true);
                                }
                                @Override
                                public void delete() {
                                    dialog.dismiss();
                                    clearUrls.add(videoUrl);
                                    menu_clear.callOnClick();
                                    setScrollEnabled(true);
                                }
                                @Override
                                public void selectMore() {
                                    dialog.dismiss();
                                    menu_edit.callOnClick();
                                    setScrollEnabled(true);
                                }
                            });
                            dialog.show();
                        }
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
                item_select.setChecked(clearUrls.contains(videoUrls.get(position)));
                if (isChange) {
                    item_select.setVisibility(View.VISIBLE);
                    item_select_bg.setVisibility(View.GONE);
                } else {
                    item_select.setVisibility(View.GONE);
                    item_select_bg.setVisibility(View.VISIBLE);
                    item_select.setChecked(false);
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

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (stopLongTouchCallback != null) {
                    stopLongTouchCallback.onReceiveValue(true);
                    stopLongTouchCallback = null;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    // 获取第一个可见项的位置
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    // 获取最后一个可见项的位置
                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                    // 遍历可见项，获取具体的元素
                    for (int i = firstVisibleItemPosition; i <= lastVisibleItemPosition; i++) {
                        View view = layoutManager.findViewByPosition(i);
                        if (view != null) {
                            // 对可见元素进行操作
                            LinearLayout item_layout = view.findViewById(R.id.item_layout);
                            item_layout.setBackgroundResource(R.color.white);
                        }
                    }
                }
                return false;
            }
        });

        // 滑动监听
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                // change();
            }
        });
    }

    void setScrollEnabled(boolean start) {
        if (recyclerView == null) return;
        if (start) {
            recyclerView.getLayoutManager().setItemPrefetchEnabled(true);
            recyclerView.setLayoutFrozen(false);
        } else {
            recyclerView.getLayoutManager().setItemPrefetchEnabled(false);
            recyclerView.setLayoutFrozen(true);
        }
    }

    void initHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                if (message.what == 0) {
                    if (videoUrls.size() > 0) {
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.getAdapter().notifyDataSetChanged();
                        layout.setVisibility(View.GONE);
                        menu_edit.setAlpha(1f);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        layout.setVisibility(View.VISIBLE);
                        menu_edit.setAlpha(0.5f);
                    }
                } else if (message.what == 2) {
                    if (recyclerView != null) {
                        String[] arr = (String[]) message.obj;
                        String txt = arr[0];
                        String videoUrl = arr[1];
                        String fileUrl = arr[2];
                        int position = Integer.parseInt(arr[3]);
                        videoUrls = videoUrls.stream().map(item -> videoUrl.equals(item) ? fileUrl : item).collect(Collectors.toList());
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
                        if (holder != null) {
                            TextView textView = holder.itemView.findViewById(R.id.item_txt);
                            textView.setText(txt);
                            recyclerView.getAdapter().notifyItemChanged(position);
                        }
                    }
                } else if (message.what == 3) {
                    if (recyclerView != null) {
                        String url = (String) message.obj;
                        videoUrls.removeIf(s -> s.equals(url));
                        clearUrls.removeIf(s -> s.equals(url));
                        recyclerView.getAdapter().notifyDataSetChanged();
                        // recyclerView.getAdapter().notifyItemRangeChanged(0, videoUrls.size());
                        // 删完了就显示背景
                        if (videoUrls.isEmpty()) {
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

    private void initVideoUrls() {
        // 影音文件格式
        videoUrls.clear();
        List<String> formats = AssetsReader.getList("audioVideo.txt");
        CommonUtils.fileWalk(PhoneSysPath.getDownloadDir(), formats, videoUrls, 2);
        Message message = handler.obtainMessage(0);
        handler.sendMessage(message);
    }

    private void deleteVideoUrl() {
        //启动线程开始执行 删除网址存档
        boolean hasM3u8 = clearUrls.stream().anyMatch(url -> url.endsWith(".m3u8"));
        if (hasM3u8) {
            Message message = Message.obtain();
            message.what = 1;
            message.obj = "后台删除中，请勿关闭应用";
            handler.sendMessage(message);
        }
        // 先删除单个文件的
        for (String url : clearUrls) {
            if (!url.endsWith(".m3u8")) {
                delete(url);
            }
        }
        // 后删除多个文件的
        for (String url : clearUrls) {
            if (url.endsWith(".m3u8")) {
                delete(url);
            }
        }
    }
    private void delete(String url) {
        new Thread(() -> {
            try {
                boolean isDelete = true;
                if (!url.endsWith(".m3u8")) {
                    File file = new File(url);
                    isDelete = CommonUtils.deleteFile(file);
                } else {
                    // 删除该m3u8对应的所有ts文件
                    File file = new File(url);
                    String dir = CommonUtils.getHlsDirBy(file);
                    if (StringUtils.isNotEmpty(dir)) {
                        isDelete = CommonUtils.batchDeleteFile(new File(dir));
                    }
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
            } catch (Exception e) {
                e.getMessage();
            }
        }).start();
    }

    private void change() {
        int num = recyclerView.getAdapter().getItemCount();
        if (num > 0) recyclerView.getAdapter().notifyItemRangeChanged(0, num);
        if (isChange) {
            edit_select_all.setVisibility(View.VISIBLE);
        } else {
            edit_select_all.setVisibility(View.INVISIBLE);
        }
        if (videoUrls.isEmpty()) {
            menu_edit.setAlpha(0.5f);
        } else {
            menu_edit.setAlpha(1f);
        }
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK && data != null) {
            List<Uri> fileUris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    fileUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                fileUris.add(data.getData());
            }
            new Thread(() -> {
                for (Uri uri : fileUris) {
                    String fileName = CommonUtils.getFileName(this, uri);
                    File file = CommonUtils.getFile("BiShu", fileName, "");
                    CommonUtils.getCopyFile(this, uri, file);
                }
                initVideoUrls();
            }).start();
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
        super.onResume();
        // 读取影音文件地址
        new Handler().post(this::initVideoUrls);
    }
}