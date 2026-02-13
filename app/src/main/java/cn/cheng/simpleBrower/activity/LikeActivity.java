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
import android.view.MotionEvent;
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
import cn.cheng.simpleBrower.custom.LongClickDialog;
import cn.cheng.simpleBrower.custom.LongTouchListener;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class LikeActivity extends AppCompatActivity {

    private Button back;

    private LinearLayout layout;

    private Handler handler;

    private RecyclerView recyclerView;

    private RecyclerView.Adapter adapter;

    private int mWindowTop;
    private int popupHeight;
    private int popupWidth;

    private ItemTouchHelper itemTouchHelper;

    private TextView like_t1;

    private TextView like_t2;

    private Button edit_select_all;

    private LinearLayout like_head;

    private LinearLayout edit_head;

    private TextView edit_txt;

    private LinearLayout edit_close;

    private LinearLayout menu_edit;

    private LinearLayout menu_clear;

    private List<String> clearUrls = new ArrayList<>();

    private List<String> likeUrls = new ArrayList<>();

    private boolean isChange = false; // 是否开启编辑模式

    private String flag = "收藏";

    @SuppressLint({"MissingInflatedId", "ResourceAsColor"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_like);
        back = findViewById(R.id.like_back);
        like_t1 = findViewById(R.id.like_t1);
        like_t2 = findViewById(R.id.like_t2);
        layout = findViewById(R.id.like_bg);
        like_head = findViewById(R.id.like_head);
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

        Intent intent = getIntent();
        String flagI = intent.getStringExtra("flag");
        if ("历史".equals(flagI)) {
            flag = "历史";
            new Handler().post(() -> {
                like_t2.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray4));
                like_t1.setTextColor(LikeActivity.this.getResources().getColor(R.color.gray));
            });
        }

        getLikeUrls();
    }

    private void initEvent() {
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
                clearUrls.addAll(likeUrls);
            }
            change();
            clearChange();
        });
        // 编辑
        menu_edit.setOnClickListener(view -> {
            if (!isChange && likeUrls.isEmpty()) return; // 没数据就不必编辑
            if (recyclerView != null) {
                clearUrls.clear();
                edit_select_all.setText("全选");
                edit_txt.setText("请选择");
                menu_clear.setAlpha(0.5f);
                if (isChange) {
                    like_head.setVisibility(View.VISIBLE);
                    edit_head.setVisibility(View.GONE);
                    menu_edit.setVisibility(View.VISIBLE);
                    menu_clear.setVisibility(View.GONE);
                    // 不处理滑动事件
                    itemTouchHelper.attachToRecyclerView(null);
                } else {
                    like_head.setVisibility(View.GONE);
                    edit_head.setVisibility(View.VISIBLE);
                    menu_edit.setVisibility(View.GONE);
                    menu_clear.setVisibility(View.VISIBLE);
                    // 处理滑动事件
                    itemTouchHelper.attachToRecyclerView(recyclerView);
                }
                isChange = !isChange;
                change();
            }
        });
        // 删除
        menu_clear.setOnClickListener(view -> {
            if (clearUrls.isEmpty()) return;
            FeetDialog feetDialog = new FeetDialog(LikeActivity.this, "删除", "确定要删除选中记录吗？", "删除", "取消");
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
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.like_list);
        recyclerView.post(() -> {
            int[] outLocation = new int[2];
            recyclerView.getLocationOnScreen(outLocation);
            mWindowTop = outLocation[1];
        });
        View view = LayoutInflater.from(this).inflate(R.layout.click_dialog, null, false);
        view.post(() -> {
            popupHeight = view.getMeasuredHeight();
            popupWidth = view.getMeasuredWidth();
        });

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

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                String likeUrl = likeUrls.get(position);
                LinearLayout item_layout = holder.itemView.findViewById(R.id.item_layout);
                item_layout.setBackgroundResource(R.color.white);
                LinearLayout item_drag = holder.itemView.findViewById(R.id.item_drag);
                LinearLayout item_l = holder.itemView.findViewById(R.id.item_l);
                LinearLayout item_select_bg = holder.itemView.findViewById(R.id.item_select_bg);
                CheckBox item_select = holder.itemView.findViewById(R.id.item_select);
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
                // textView.setInputType(InputType.TYPE_NULL); // 屏蔽软键盘
                // 设置TextView显示数据
                textView.setText(likeUrl);
                textView.setOnClickListener(view -> {
                    click(likeUrl, item_select);
                });
                textView.setOnLongClickListener(view -> true);
                textView.setOnTouchListener(new LongTouchListener() {
                    @Override
                    public void downEvent() {
                        item_layout.setBackgroundResource(R.color.colorLightGray1);
                    }
                    @Override
                    public void realUpEvent() {
                        item_layout.setBackgroundResource(R.color.white);
                    }
                    @Override
                    public void upEvent(float x, float y) {
                        if (isChange) return;
                        LongClickDialog dialog = new LongClickDialog(LikeActivity.this, x, y - mWindowTop, popupWidth, popupHeight);
                        dialog.setOnTouchListener(new LongClickDialog.TouchListener() {
                            @Override
                            public void close() {
                                dialog.dismiss();
                            }
                            @Override
                            public void open() {
                                dialog.dismiss();
                            }
                            @Override
                            public void copy() {
                                dialog.dismiss();
                            }
                            @Override
                            public void modify() {
                                dialog.dismiss();
                            }
                            @Override
                            public void delete() {
                                dialog.dismiss();
                            }
                            @Override
                            public void selectMore() {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                    }
                });
                item_l.setOnClickListener(view -> {
                    click(likeUrl, item_select);
                });
                // 解决：配置了android:textIsSelectable="true",同时也设置了点击事件，发现点第一次时候，点击事件没有生效
                /*textView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        view.requestFocus();
                        return false;
                    }
                });*/
                item_select.setChecked(clearUrls.contains(likeUrls.get(position)));
                if (isChange) {
                    item_select.setVisibility(View.VISIBLE);
                    item_drag.setVisibility(View.VISIBLE);
                    item_select_bg.setVisibility(View.GONE);
                } else {
                    item_select.setVisibility(View.GONE);
                    item_drag.setVisibility(View.GONE);
                    item_select_bg.setVisibility(View.VISIBLE);
                    item_select.setChecked(false);
                }
                item_select.setAnimation(null);
                item_select.setOnClickListener(view -> {
                    if (!item_select.isChecked()) {
                        clearUrls.removeIf(item -> item != null && item.equals(likeUrl));
                    } else {
                        clearUrls.add(likeUrl);
                    }
                    clearChange();
                });
            }

            @Override
            public int getItemCount() {
                return likeUrls.size();
            }

            public void click(String likeUrl, CheckBox item_select) {
                if (isChange) {
                    select(likeUrl, item_select);
                    return; // 编辑模式不可跳转
                }
                // 跳转该网址
                Intent intent = new Intent(LikeActivity.this, BrowserActivity2.class);
                intent.putExtra("webInfo", likeUrl);
                LikeActivity.this.startActivity(intent);
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

        // 配置触摸滑动事件
        itemTouchHelper = new ItemTouchHelper(getTouchCallback());
    }

    ItemTouchHelper.Callback getTouchCallback() {
        return new ItemTouchHelper.SimpleCallback(
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
    }

    void initHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                if (message.what == 0) {
                    if (likeUrls.size() > 0) {
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
                        recyclerView.getAdapter().notifyDataSetChanged();
                        // recyclerView.getAdapter().notifyItemRangeChanged(0, likeUrls.size());
                        // 删完了就显示背景
                        if (likeUrls.isEmpty()) {
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
                new Handler().post(() -> {
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
                });
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
            clearUrls.clear();
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
                            clearUrls.clear();
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

    private void change() {
        int num = recyclerView.getAdapter().getItemCount();
        if (num > 0) recyclerView.getAdapter().notifyItemRangeChanged(0, num);
        if (isChange) {
            edit_select_all.setVisibility(View.VISIBLE);
        } else {
            edit_select_all.setVisibility(View.INVISIBLE);
        }
        if (likeUrls.isEmpty()) {
            menu_edit.setAlpha(0.5f);
        } else  {
            menu_edit.setAlpha(1f);
        }
    }

    private void clearChange() {
        if (new HashSet<>(clearUrls).containsAll(likeUrls)) {
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
        super.onResume();
    }
}