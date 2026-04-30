package cn.cheng.biShu.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.cheng.biShu.MyApplication;
import cn.cheng.biShu.R;
import cn.cheng.biShu.bean.PositionBean;
import cn.cheng.biShu.custom.MyToast;
import cn.cheng.biShu.service.ReadService;
import cn.cheng.biShu.util.CommonUtils;
import cn.cheng.biShu.util.SysWindowUi;

public class TxtCatalogActivity extends AppCompatActivity {

    private String txtUrl = "";
    private ArrayList<String> lines;
    private PositionBean positionBean;
    private ArrayList<HashMap<String, String>> chapters = new ArrayList<>();
    int index = 0;

    private TextView c_title;
    private LinearLayout back;
    private RecyclerView recyclerView;

    @SuppressLint({"MissingInflatedId", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_txt_catalog);

        // 注册广播接收器
        IntentFilter filter = new IntentFilter("CLOSE_TC_ACTIVITY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        // 获取上一界面传过来的数据
        Intent intent = getIntent();
        txtUrl = intent.getStringExtra("txtUrl");
        String title = txtUrl.substring(txtUrl.lastIndexOf("/") + 1);
        Bundle extras = intent.getExtras();
        positionBean = (PositionBean) extras.get("positionBean");

        // 设置书名
        c_title = findViewById(R.id.c_title);
        c_title.setText(title);

        // 返回
        back = findViewById(R.id.c_back);
        back.setOnClickListener(view -> {
            this.finish();
        });

        // 历史进度
        Map<String, ArrayList<String>> novelLinesMap = MyApplication.getNovelLinesMap();
        lines = novelLinesMap.get(txtUrl);

        Handler handler = new Handler(message -> {
            if (message.what == 0) {
                // 获取章节
                if (!chapters.isEmpty()) {
                    index = Integer.parseInt(chapters.get(0).get("index"));
                    // System.out.println(positionBean.getStartLine());
                    // System.out.println(index);
                    // MyToast.getInstance("+" + index).show();
                    initRecyclerView();
                } else {
                    // 反馈解析结果
                    ((TextView) findViewById(R.id.txt_catalog_load_text)).setText("未发现章节目录");
                }
            }
            return false;
        });

        // 异步解析章节名
        new Thread(() -> {
            chapters = CommonUtils.getTitles(lines, positionBean.getStartLine());
            Message message = handler.obtainMessage(0);
            handler.sendMessage(message);
        }).start();

    }

    private void initRecyclerView() {
        // 关闭加载背景
        findViewById(R.id.txt_catalog_load).setVisibility(View.GONE);

        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.catalog_list);
        recyclerView.setVisibility(View.VISIBLE);

        // 创建线性布局管理器 赋值给recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 给recyclerview设置适配器
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // 加载子项布局
                View itemView = LayoutInflater.from(TxtCatalogActivity.this)
                        .inflate(R.layout.recyclerview_item, parent, false); // 第三个参数必须是 false！
                return new RecyclerView.ViewHolder(itemView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                LinearLayout item_l = holder.itemView.findViewById(R.id.item_l);
                Button button = holder.itemView.findViewById(R.id.item_true);
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setTextSize(18);
                textView.setTextIsSelectable(false);
                HashMap<String, String> map = chapters.get(position);
                textView.setText(map.get("title"));
                textView.setOnClickListener(view -> {
                    click(map);
                });
                item_l.setOnClickListener(view -> {
                    click(map);
                });
                if (position == index) {
                    button.setVisibility(View.VISIBLE);
                } else {
                    button.setVisibility(View.GONE);
                }
            }

            @Override
            public int getItemCount() {
                return chapters.size();
            }

            public void click(HashMap<String, String> map) {
                if (ReadService.textToSpeech != null && ReadService.textToSpeech.isSpeaking() && MyApplication.turnThePage) {
                    MyToast.getInstance("加载中请稍后").show();
                    return;
                }
                TxtActivity.txtActivity.finish();
                // 跳转该网址
                Intent intent = new Intent(TxtCatalogActivity.this, TxtActivity.class);
                intent.putExtra("txtUrl", txtUrl);
                intent.putExtra("catalog", "catalog");
                int endLine = Integer.parseInt(map.get("line"));
                positionBean.setEndLine(endLine - 1);
                positionBean.setStartNum(endLine - 1 > 0 ? lines.get(endLine - 1).length() : 0);
                Bundle bundle = new Bundle();
                bundle.putSerializable("positionBean", (Serializable) positionBean);
                intent.putExtras(bundle);
                TxtCatalogActivity.this.startActivity(intent);
                TxtCatalogActivity.this.finish();
            }
        });

        // 滑动监听
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {

            }
        });
        // 第一个方法scrollToPosition(position)是定位到指定item，是瞬间显示在页面上，用户可见的范围。位置不固定。
        // 第二个方法与第一个不同的是平滑到你指定的item，而scrollToPosition是瞬间定位到指定位置
        recyclerView.scrollToPosition(index);
        new Handler().postDelayed(() -> {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(index);
            if (viewHolder != null) {
                Button button = viewHolder.itemView.findViewById(R.id.item_true);
                button.setVisibility(View.VISIBLE);
            }
        }, 100);
    }

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        unregisterReceiver(receiver);
    }

    // 关闭当前界面，回到TxtActivity
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("CLOSE_TC_ACTIVITY".equals(intent.getAction())) {
                if (ReadService.textToSpeech != null && ReadService.textToSpeech.isSpeaking() && MyApplication.turnThePage) {
                    MyToast.getInstance("加载中请稍后").show();
                    return;
                }
                TxtCatalogActivity.this.finish();
                Intent i = new Intent(context, TxtActivity.class);
                i.putExtra("txtUrl", MyApplication.getTxtUrl());
                TxtCatalogActivity.this.startActivity(i);
            }
        }
    };
}