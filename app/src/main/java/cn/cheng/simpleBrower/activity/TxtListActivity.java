package cn.cheng.simpleBrower.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.PhoneSysPath;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class TxtListActivity extends AppCompatActivity {

    private Button back;

    private LinearLayout layout;

    private LinearLayout txt_file;

    private LinearLayout txt_list_head;

    private Button edit_select_all;

    private LinearLayout edit_head;

    private TextView edit_txt;

    private LinearLayout edit_close;

    private LinearLayout menu_edit;

    private LinearLayout menu_clear;

    private List<String> clearUrls = new ArrayList<>();

    private Handler handler;

    private static RecyclerView recyclerView;

    private List<String> txtUrls = new ArrayList<>();

    private boolean isChange = false;

    private static final int REQUEST_CODE_PICK_FILE = 7;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_txt_list);
        back = findViewById(R.id.txt_back);
        layout = findViewById(R.id.txt_bg);
        txt_file = findViewById(R.id.txt_file);
        txt_list_head = findViewById(R.id.txt_list_head);
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
        txt_file.setOnClickListener(view -> {
            try {
                // 1. 创建Intent
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                // String[] packageName = CommonUtils.getPackageName(this);
                // intent.setComponent(new ComponentName(packageName[0], packageName[1])); // 只能单选 已弃用
                // 2. 关键：启用多选模式
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                // 3. 可选：限制文件类型
                intent.setType("text/*");
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
                clearUrls.addAll(txtUrls);
            }
            change();
            clearChange();
        });
        // 编辑
        menu_edit.setOnClickListener(view -> {
            if (!isChange && txtUrls.isEmpty()) return; // 没数据就不必编辑
            if (recyclerView != null) {
                clearUrls.clear();
                edit_select_all.setText("全选");
                edit_txt.setText("请选择");
                menu_clear.setAlpha(0.5f);
                if (isChange) {
                    txt_list_head.setVisibility(View.VISIBLE);
                    edit_head.setVisibility(View.GONE);
                    menu_edit.setVisibility(View.VISIBLE);
                    menu_clear.setVisibility(View.GONE);
                } else {
                    txt_list_head.setVisibility(View.GONE);
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
            FeetDialog feetDialog = new FeetDialog(TxtListActivity.this, "删除", "确定要删除选中文件吗？", "删除", "取消");
            feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                @Override
                public void close() {
                    feetDialog.dismiss();
                }

                @Override
                public void ok(String txt) {
                    // 删除本项记录
                    deleteTxtUrl();
                    feetDialog.dismiss();
                }
            });
            feetDialog.show();
        });
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.txt_list);

        // 创建线性布局管理器 赋值给recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 给recyclerview设置适配器
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // 加载子项布局
                View itemView = LayoutInflater.from(TxtListActivity.this)
                        .inflate(R.layout.recyclerview_item, parent, false); // 第三个参数必须是 false！
                return new RecyclerView.ViewHolder(itemView) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                LinearLayout item_l = holder.itemView.findViewById(R.id.item_l);
                LinearLayout item_select_bg = holder.itemView.findViewById(R.id.item_select_bg);
                CheckBox item_select = holder.itemView.findViewById(R.id.item_select);
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
                // textView.setInputType(InputType.TYPE_NULL); // 屏蔽软键盘
                String txtUrl = txtUrls.get(position);
                String[] s = txtUrl.split("/");
                if (s.length > 0) {
                    textView.setText(s[s.length - 1]);
                    textView.setOnClickListener(view -> {
                        click(txtUrl, item_select);
                    });
                    item_l.setOnClickListener(view -> {
                        click(txtUrl, item_select);
                    });
                    // 解决：配置了android:textIsSelectable="true",同时也设置了点击事件，发现点第一次时候，点击事件没有生效
                    /*textView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            view.requestFocus();
                            return false;
                        }
                    });*/
                    item_select.setChecked(clearUrls.contains(txtUrls.get(position)));
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
                            clearUrls.removeIf(item -> item != null && item.equals(txtUrl));
                        } else {
                            clearUrls.add(txtUrl);
                        }
                        clearChange();
                    });
                }
            }

            @Override
            public int getItemCount() {
                return txtUrls.size();
            }

            public void click(String txtUrl, CheckBox item_select) {
                try {
                    if (isChange) {
                        select(txtUrl, item_select);
                        return; // 编辑模式不可跳转
                    }
                    // 跳转该网址
                    if (MyApplication.isTurnPageFlag()) {
                        MyToast.getInstance("朗读翻页中，请稍后").show();
                        return;
                    }
                    Intent intent = new Intent(TxtListActivity.this, TxtActivity.class);
                    intent.putExtra("txtUrl", txtUrl);
                    TxtListActivity.this.startActivity(intent);
                } catch (Exception e) {
                    CommonUtils.saveLog("TxtListActivity-click:" + e.getMessage());
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
        });

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
                    if (txtUrls.size() > 0) {
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
                        txtUrls.removeIf(s -> s.equals(url));
                        recyclerView.getAdapter().notifyDataSetChanged();
                        // recyclerView.getAdapter().notifyItemRangeChanged(0, txtUrls.size());
                        // 删完了就显示背景
                        if (txtUrls.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            layout.setVisibility(View.VISIBLE);
                            edit_close.callOnClick();
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

    private void initTxtUrls() {
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            // 文本文件格式
            txtUrls.clear();
            List<String> formats = new ArrayList<>();
            formats.add(".txt");
            CommonUtils.fileWalk(PhoneSysPath.getDownloadDir(), formats, txtUrls, 2);
            Message message = handler.obtainMessage(0);
            handler.sendMessage(message);
        }
    }

    private void deleteTxtUrl() {
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            //启动线程开始执行 删除网址存档
            for (String url : clearUrls) {
                delete(url);
            }
        }
    }

    private void delete(String url) {
        new Thread(() -> {
            try {
                boolean isDelete;
                File file = new File(url);
                isDelete = CommonUtils.deleteFile(file);
                // 通知handler 数据删除完成 可以刷新recyclerview
                Message message = Message.obtain();
                if (isDelete) {
                    message.what = 3;
                    message.obj = url;
                } else {
                    message.what = 1;
                    message.obj = "删除失败（" + CommonUtils.getUrlName(url) + "）";
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
        if (txtUrls.isEmpty()) {
            menu_edit.setAlpha(0.5f);
        } else {
            menu_edit.setAlpha(1f);
        }
    }

    private void clearChange() {
        if (new HashSet<>(clearUrls).containsAll(txtUrls)) {
            edit_select_all.setText("取消");
        } else {
            edit_select_all.setText("全选");
        }
        if (clearUrls.isEmpty()) {
            edit_txt.setText("请选择");
            menu_clear.setAlpha(0.5f);
        } else {
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
                    File file = CommonUtils.getFile("SimpleBrower", fileName, "");
                    CommonUtils.getCopyFile(this, uri, file);
                }
                initTxtUrls();
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
        // 读取文本文件地址
        new Handler().post(this::initTxtUrls);
    }
}