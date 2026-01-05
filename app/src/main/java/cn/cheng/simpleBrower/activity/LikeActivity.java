package cn.cheng.simpleBrower.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class LikeActivity extends AppCompatActivity {

    private Button back;

    private Button like_change;

    private LinearLayout layout;

    private Handler handler;

    private RecyclerView recyclerView;

    private RecyclerView.Adapter adapter;

    private TextView like_t1;

    private TextView like_t2;

    private LinearLayout like_head;

    private LinearLayout like_head2;

    private TextView like_text;

    private LinearLayout like_close;

    private LinearLayout menu_edit;

    private LinearLayout menu_clear;

    private List<String> likeUrls = new ArrayList<>();

    private List<String> clearUrls = new ArrayList<>();

    private boolean isChange = false;

    private String flag = "收藏";

    private int firstTime = 500;

    @SuppressLint({"MissingInflatedId", "ResourceAsColor"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_like);
        like_head = findViewById(R.id.like_head);
        back = findViewById(R.id.like_back);
        like_t1 = findViewById(R.id.like_t1);
        like_t2 = findViewById(R.id.like_t2);
        like_head2 = findViewById(R.id.like_head2);
        like_close = findViewById(R.id.like_close);
        like_text = findViewById(R.id.like_text);
        like_change = findViewById(R.id.like_change);
        layout = findViewById(R.id.like_bg);
        menu_edit = findViewById(R.id.menu_edit);
        menu_clear = findViewById(R.id.menu_clear);

        // 返回
        back.setOnClickListener(view -> {
            this.finish();
        });
        // 收藏列
        like_t1.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray4));
        like_t1.setOnClickListener(view -> {
            if ("历史".equals(flag)) {
                flag = "收藏";
                like_t1.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray4));
                like_t2.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray));
                getLikeUrls();
                clearUrls.clear();
                change(isChange);
            }
        });
        // 历史列
        like_t2.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray));
        like_t2.setOnClickListener(view -> {
            if ("收藏".equals(flag)) {
                flag = "历史";
                like_t2.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray4));
                like_t1.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray));
                getLikeUrls();
                clearUrls.clear();
                change(isChange);
            }
        });
        // 退出编辑
        like_close.setOnClickListener(view -> {
            if (isChange) {
                menu_edit.callOnClick();
            }
        });
        // 多选
        like_change.setOnClickListener(view -> {
            clearUrls.clear();
            if ("全选".equals(like_change.getText().toString())) {
                clearUrls.addAll(likeUrls);
            }
            change(isChange);
            likeChange();
        });
        // 编辑
        menu_edit.setOnClickListener(view -> {
            if (recyclerView != null) {
                if (isChange) {
                    like_head.setVisibility(View.VISIBLE);
                    like_head2.setVisibility(View.GONE);
                    menu_edit.setVisibility(View.VISIBLE);
                    menu_clear.setVisibility(View.GONE);
                } else {
                    like_head.setVisibility(View.GONE);
                    like_head2.setVisibility(View.VISIBLE);
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
            FeetDialog feetDialog = new FeetDialog(LikeActivity.this, "删除", "确定要删除该记录吗？", "删除", "取消");
            feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                @Override
                public void close() {
                    feetDialog.dismiss();
                }

                @Override
                public void ok(String txt) {
                    // 删除本项记录
                    deleteLikeUrl();
                    feetDialog.dismiss();
                }
            });
            feetDialog.show();
        });

        Intent intent = getIntent();
        String flagI = intent.getStringExtra("flag");
        if ("历史".equals(flagI)) {
            flag = "历史";
            like_t2.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray4));
            like_t1.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray));
        }

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
        adapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // 加载子项布局
                View itemView = LayoutInflater.from(LikeActivity.this)
                        .inflate(R.layout.recyclerview_item, parent, false); // 第三个参数必须是 false！
                return new RecyclerView.ViewHolder(itemView) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                LinearLayout item_l = holder.itemView.findViewById(R.id.item_l);
                // 设置TextView显示数据
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
                //textView.setInputType(InputType.TYPE_NULL); // 屏蔽软键盘
                String likeUrl = likeUrls.get(position);
                textView.setText(likeUrl);
                textView.setOnClickListener(view -> {
                    click(likeUrl);
                });
                item_l.setOnClickListener(view -> {
                    click(likeUrl);
                });
                // 解决：配置了android:textIsSelectable="true",同时也设置了点击事件，发现点第一次时候，点击事件没有生效
                /*textView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        view.requestFocus();
                        return false;
                    }
                });*/
                CheckBox item_select = holder.itemView.findViewById(R.id.item_select);
                item_select.setAnimation(null);
                String url = likeUrls.get(position);
                item_select.setOnClickListener(view -> {
                    if (!item_select.isChecked()) {
                        clearUrls.removeIf(item -> item != null && item.equals(url));
                    } else {
                        clearUrls.add(url);
                    }
                    likeChange();
                });
            }

            @Override
            public int getItemCount() {
                return likeUrls.size();
            }

            public void click(String likeUrl) {
                if (isChange) {
                    clearUrls.clear();
                    menu_edit.callOnClick();
                }
                // 跳转该网址
                // Intent intent = new Intent(LikeActivity.this, BrowserActivity.class);
                Intent intent = new Intent(LikeActivity.this, BrowserActivity2.class);
                intent.putExtra("webInfo", likeUrl);
                LikeActivity.this.startActivity(intent);
            }
        };
        recyclerView.setAdapter(adapter);

        // 滑动监听
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                change(isChange);
            }
        });

        // 配置触摸事件
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, // 拖拽方向
                0 // 左右不滑动
                // ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // 滑动方向
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // 处理拖拽换位
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(likeUrls, fromPos, toPos);
                moveLikeUrl();
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 处理滑动删除
                // int position = viewHolder.getAdapterPosition();
                // likeUrls.removeIf(item -> item != null && item.equals(likeUrls.get(position)));
                // adapter.notifyItemRemoved(position);
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
                    MyToast.getInstance(message.obj + "").show();
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

    private void deleteLikeUrl() {
        if ("历史".equals(flag)) {
            // 集合中删除该网址
            likeUrls.removeAll(clearUrls);
            MyApplication.clearUrls();
            for (String l : likeUrls) {
                MyApplication.setUrl(l);
            }
            // 通知handler 数据删除完成 可以刷新recyclerview
            Message message = Message.obtain();
            message.what = 3;
            handler.sendMessage(message);
        } else {
            if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
                //启动线程开始执行 删除网址存档
                new Thread(() -> {
                    try {
                        File file = CommonUtils.getFile("SimpleBrower/0_like", "like.txt", "");
                        if (file.exists()) {
                            // 集合中删除该网址
                            likeUrls.removeAll(clearUrls);
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
                    handler.sendMessage(message);
                }).start();
            }
        }
    }

    private void moveLikeUrl() {
        if ("历史".equals(flag)) {
            MyApplication.clearUrls();
            for (String l : likeUrls) {
                MyApplication.setUrl(l);
            }
        } else {
            if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
                //启动线程开始执行 删除网址存档
                new Thread(() -> {
                    try {
                        File file = CommonUtils.getFile("SimpleBrower/0_like", "like.txt", "");
                        if (file.exists()) {
                            // 集合中删除该网址
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
                }).start();
            }
        }
    }

    private void change(boolean isChange) {
        new Handler().postDelayed(() -> {
            int num = recyclerView.getAdapter().getItemCount();
            for (int i = 0; i < num; i++) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                if (viewHolder != null) {
                    CheckBox item_select = viewHolder.itemView.findViewById(R.id.item_select);
                    item_select.setChecked(clearUrls.contains(likeUrls.get(i)));
                    if (isChange) {
                        item_select.setVisibility(View.VISIBLE);
                    } else {
                        item_select.setVisibility(View.INVISIBLE);
                        item_select.setChecked(false);
                    }
                }
            }
            if (isChange) {
                like_change.setVisibility(View.VISIBLE);
            } else {
                like_change.setVisibility(View.INVISIBLE);
            }
            // likeChange();
        }, 50);
    }

    private void likeChange() {
        if (new HashSet<>(clearUrls).containsAll(likeUrls)) {
            like_change.setText("取消");
        } else {
            like_change.setText("全选");
        }
        if (clearUrls.isEmpty()) {
            like_text.setText("请选择");
            menu_clear.setAlpha(0.5f);
        } else  {
            menu_clear.setAlpha(1f);
            like_text.setText("已选择" + clearUrls.size() + "项");
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
        getLikeUrls();
        new Handler().post(() -> {
            change(isChange);
        });
        super.onResume();
    }
}