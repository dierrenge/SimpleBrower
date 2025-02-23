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
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class TxtListActivity extends AppCompatActivity {

    private Button back;

    private Button txt_change;

    private LinearLayout layout;

    private Handler handler;

    private static RecyclerView recyclerView;

    private List<String> txtUrls = new ArrayList<>();

    private boolean isChange = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_txt_list);

        // 返回
        back = findViewById(R.id.txt_back);
        back.setOnClickListener(view -> {
            this.finish();
        });

        // 编辑
        txt_change = findViewById(R.id.txt_change);
        txt_change.setOnClickListener(view -> {
            if (recyclerView != null) {
                isChange = !isChange;
                change(isChange);
            }
        });

        // 背景
        layout = findViewById(R.id.txt_bg);

        initRecyclerView();

        // 初始化线程通信工具
        initHandler();

        // 读取文本文件地址
        // initTxtUrls();
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
                return new RecyclerView.ViewHolder(itemView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                LinearLayout item_l = holder.itemView.findViewById(R.id.item_l);
                // 设置TextView显示数据
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
                textView.setInputType(InputType.TYPE_NULL); // 屏蔽软键盘
                String txtUrl = txtUrls.get(position);
                String[] s = txtUrl.split("/");
                if (s.length > 0) {
                    textView.setText(s[s.length - 1]);
                    textView.setOnClickListener(view -> {
                        click(txtUrl);
                    });
                    item_l.setOnClickListener(view -> {
                        click(txtUrl);
                    });
                    // 解决：配置了android:textIsSelectable="true",同时也设置了点击事件，发现点第一次时候，点击事件没有生效
                    textView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            view.requestFocus();
                            return false;
                        }
                    });
                    Button button = holder.itemView.findViewById(R.id.item_del);
                    button.setOnClickListener(view -> {
                        FeetDialog feetDialog = new FeetDialog(TxtListActivity.this, "删除", "确定要删除该文件吗？", "删除", "取消");
                        feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                            @Override
                            public void close() {
                                feetDialog.dismiss();
                            }
                            @Override
                            public void ok() {
                                // 删除本项记录
                                deleteTxtUrl(txtUrl, position);
                                feetDialog.dismiss();
                            }
                        });
                        feetDialog.show();
                    });
                }
            }

            @Override
            public int getItemCount() {
                return txtUrls.size();
            }

            public void click(String txtUrl) {
                try {
                    if (isChange) {
                        isChange = !isChange;
                        change(isChange);
                    }
                    // 跳转该网址
                    Intent intent = new Intent(TxtListActivity.this, TxtActivity.class);
                    intent.putExtra("txtUrl", txtUrl);
                    TxtListActivity.this.startActivity(intent);
                } catch (Exception e) {
                    CommonUtils.saveLog("TxtListActivity-click:" + e.getMessage());
                }
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
                    if (txtUrls.size() > 0) {
                        recyclerView.getAdapter().notifyDataSetChanged();
                    } else {
                        layout.setVisibility(View.VISIBLE);
                    }
                } else if (message.what == 3) {
                    if (recyclerView != null) {
                        String[] arr = (String[]) message.obj;
                        // 集合中删除该网址
                        txtUrls.removeIf(s -> s.equals(arr[0]));
                        recyclerView.getAdapter().notifyDataSetChanged();
                        recyclerView.getAdapter().notifyItemRangeChanged(0, txtUrls.size());
                        change(isChange);
                    }
                    // 删完了就显示背景
                    if (txtUrls.size() == 0) {
                        layout.setVisibility(View.VISIBLE);
                    }
                } else {
                    MyToast.getInstance(TxtListActivity.this, message.obj + "").show();
                }
                return false;
            }
        });
    }

    private void initTxtUrls() {
        txtUrls.clear();
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            // 文本文件格式
            new Handler().postDelayed(() -> {
                List<String> formats = new ArrayList<>();
                formats.add(".txt");
                CommonUtils.fileWalk(dir, formats, txtUrls, 2);
                Message message = handler.obtainMessage(0);
                handler.sendMessage(message);
            }, 0);
        }
    }

    private void deleteTxtUrl(String url, int position) {
        if (Build.VERSION.SDK_INT >= 29 && url != null) { // android 12的sd卡读写
            //启动线程开始执行 删除网址存档
            new Thread(() -> {
                boolean isDelete = false;
                try {
                    File file = new File(url);
                    if (file.exists()) {
                        // file.delete();
                        // TxtActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file)));
                        isDelete = CommonUtils.deleteFile(file);
                    }
                } catch (Exception e) {
                    e.getMessage();
                }
                // 通知handler 数据删除完成 可以刷新recyclerview
                Message message = Message.obtain();
                if (isDelete) {
                    CommonUtils.deleteObjectIntoLocal(url); // 删除阅读记录
                    message.what = 3;
                    message.obj = new String[]{url, position+""};
                } else {
                    message.what = 1;
                    message.obj = "删除失败";
                }
                handler.sendMessage(message);
            }).start();
        }
    }

    private void change(boolean isChange) {
        new Handler().postDelayed(() -> {
            int num = recyclerView.getAdapter().getItemCount();
            // System.out.println(num + "*****************************************************************");
            for (int i = 0; i < num; i++) {
                // System.out.println("=====++++++++++++++++++++++++++++++++++++++++++++++++++====" + i);
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                if (viewHolder != null) {
                    Button button = viewHolder.itemView.findViewById(R.id.item_del);
                    if (isChange) {
                        button.setVisibility(View.VISIBLE);
                    } else {
                        button.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }, 50);
    }

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
        initTxtUrls();
        new Handler().post(() -> {
            change(isChange);
        });
        super.onResume();
    }
}