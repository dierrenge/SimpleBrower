package cn.cheng.simpleBrower.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.util.AssetsReader;
import cn.cheng.simpleBrower.util.CommonUtils;

public class LikeActivity extends AppCompatActivity {

    private Button back;

    private Button like_change;

    private LinearLayout layout;

    private Handler handler;

    private RecyclerView recyclerView;

    private TextView like_t1;

    private TextView like_t2;

    private List<String> likeUrls = new ArrayList<>();

    private boolean isChange = false;

    private String flag = "收藏";

    private int firstTime = 500;

    @SuppressLint({"MissingInflatedId", "ResourceAsColor"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_like);

        // 返回
        back = findViewById(R.id.like_back);
        back.setOnClickListener(view -> {
            this.finish();
        });

        // 收藏列
        like_t1 = findViewById(R.id.like_t1);
        like_t1.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray4));
        like_t1.setOnClickListener(view -> {
            if ("历史".equals(flag)) {
                flag = "收藏";
                like_t1.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray4));
                like_t2.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray));
                getLikeUrls();
                change(isChange);
            }
        });

        // 历史列
        like_t2 = findViewById(R.id.like_t2);
        like_t2.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray));
        like_t2.setOnClickListener(view -> {
            if ("收藏".equals(flag)) {
                flag = "历史";
                like_t2.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray4));
                like_t1.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray));
                getLikeUrls();
                change(isChange);
            }
        });

        // 编辑
        like_change = findViewById(R.id.like_change);
        like_change.setOnClickListener(view -> {
            if (recyclerView != null) {
                isChange = !isChange;
                change(isChange);
            }
        });

        // 背景
        layout = findViewById(R.id.like_bg);

        initRecyclerView();

        // 初始化线程通信工具
        initHandler();

        // 读取收藏网址
        // getLikeUrls();
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.like_list);

        // 创建线性布局管理器 赋值给recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 给recyclerview设置适配器
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // 加载子项布局
                return new RecyclerView.ViewHolder(View.inflate(LikeActivity.this, R.layout.recyclerview_item, null)) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                LinearLayout item_l = holder.itemView.findViewById(R.id.item_l);
                // 设置TextView显示数据
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
                textView.setInputType(InputType.TYPE_NULL); // 屏蔽软键盘
                String likeUrl = likeUrls.get(position);
                textView.setText(likeUrl);
                textView.setOnClickListener(view -> {
                    click(likeUrl);
                });
                item_l.setOnClickListener(view -> {
                    click(likeUrl);
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
                    FeetDialog feetDialog = new FeetDialog(LikeActivity.this, "删除", "确定要删除该记录吗？", "删除", "取消");
                    feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                        @Override
                        public void close() {
                            feetDialog.dismiss();
                        }
                        @Override
                        public void ok() {
                            // 删除本项记录
                            deleteLikeUrl(likeUrl);
                            feetDialog.dismiss();
                        }
                    });
                    feetDialog.show();
                });
            }

            @Override
            public int getItemCount() {
                return likeUrls.size();
            }

            public void click(String likeUrl) {
                if (isChange) {
                    isChange = !isChange;
                    change(isChange);
                }
                // 跳转该网址
                Intent intent = new Intent(LikeActivity.this, BrowserActivity.class);
                intent.putExtra("webInfo", likeUrl);
                LikeActivity.this.startActivity(intent);
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
                    if (likeUrls.size() > 0) {
                        recyclerView.getAdapter().notifyDataSetChanged();
                        layout.setVisibility(View.GONE);
                    } else {
                        layout.setVisibility(View.VISIBLE);
                    }
                } else if (message.what == 3) {
                    if (recyclerView != null) {
                        recyclerView.getAdapter().notifyDataSetChanged();
                        recyclerView.getAdapter().notifyItemRangeChanged(0, likeUrls.size());
                        change(isChange);
                    }
                    // 删完了就显示背景
                    if (likeUrls.size() == 0) {
                        layout.setVisibility(View.VISIBLE);
                    }
                } else {
                    MyToast.getInstance(LikeActivity.this, message.obj + "").show();
                }
                return false;
            }
        });
    }

    private void getLikeUrls() {
        likeUrls.clear();
        if ("历史".equals(flag)) {
            List<String> urls = MyApplication.getUrls();
            for (int i = urls.size() -1; i >= 0; i--) {
                likeUrls.add(urls.get(i));
            }
            // 通知handler 数据获取完成 可以初始化recyclerview
            Message message = Message.obtain();
            message.what = 0;
            handler.sendMessage(message);
        } else {
            if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
                //启动线程开始执行 加载网址存档
                new Handler().postDelayed(() -> {
                    firstTime = 0;
                    try {
                        File file = CommonUtils.getFile("SimpleBrower/0_like", "like.txt", "");
                        if (file.exists()) {
                            try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
                                String line = null;
                                while ((line = reader.readLine()) != null) {
                                    likeUrls.add(line);
                                }
                            } catch (IOException e) {
                                e.getMessage();
                            }
                        }
                    } catch (Exception e) {
                        e.getMessage();
                    }
                    // 通知handler 数据获取完成 可以初始化recyclerview
                    Message message = Message.obtain();
                    message.what = 0;
                    handler.sendMessage(message);
                }, firstTime);
            }
        }
    }

    private void deleteLikeUrl(String url) {
        int position = likeUrls.indexOf(url);
        if ("历史".equals(flag)) {
            // 集合中删除该网址
            likeUrls.removeIf(s -> s.equals(url));
            MyApplication.clearUrls();
            for (String l : likeUrls) {
                MyApplication.setUrl(l);
            }
            // 通知handler 数据删除完成 可以刷新recyclerview
            Message message = Message.obtain();
            message.what = 3;
            message.obj = new String[]{url, position+""};
            handler.sendMessage(message);
        } else {
            if (Build.VERSION.SDK_INT >= 29 && url != null) { // android 12的sd卡读写
                //启动线程开始执行 删除网址存档
                new Thread(() -> {
                    try {
                        File file = CommonUtils.getFile("SimpleBrower/0_like", "like.txt", "");
                        if (file.exists()) {
                            // 集合中删除该网址
                            likeUrls.removeIf(s -> s.equals(url));
                            try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                                for (String s : likeUrls) {
                                    if (s != null) {
                                        bos.write((s + "\n").getBytes());
                                    }
                                }
                            } catch (IOException e) {
                                e.getMessage();
                            }
                        }
                    } catch (Exception e) {
                        e.getMessage();
                    }
                    // 通知handler 数据删除完成 可以刷新recyclerview
                    Message message = Message.obtain();
                    message.what = 3;
                    message.obj = new String[]{url, position+""};
                    handler.sendMessage(message);
                }).start();
            }
        }
    }

    private void change(boolean isChange) {
        new Handler().postDelayed(() -> {
            int num = recyclerView.getAdapter().getItemCount();
            for (int i = 0; i < num; i++) {
                // System.out.println("=========" + i);
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
        getLikeUrls();
        new Handler().post(() -> {
            change(isChange);
        });
        super.onResume();
    }
}