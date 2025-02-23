package cn.cheng.simpleBrower.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.PositionBean;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class TxtCatalogActivity extends AppCompatActivity {

    private String txtUrl = "";
    private ArrayList<String> lines;
    private PositionBean positionBean;
    private ArrayList<HashMap<String, String>> chapters = new ArrayList<>();
    int index = 0;

    private TextView c_title;
    private Button back;

    private Handler handler;
    private RecyclerView recyclerView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_txt_catalog);

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


        // 获取章节
        chapters = CommonUtils.getTitles(lines, positionBean.getStartLine());
        if (chapters.size() > 0) {
            index = Integer.parseInt(chapters.get(0).get("index"));
            System.out.println(positionBean.getStartLine());
            System.out.println(index);
            // MyToast.getInstance(this, "+" + index).show();

            initHandler();

            new Handler().postDelayed(() -> {
                initRecyclerView();
            }, 400);
        }
    }

    void initHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                if (message.what == 0) {
                    if (chapters.size() > 0) {
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }
                } else if (message.what == 3) {
                    if (recyclerView != null) {
                        recyclerView.getAdapter().notifyDataSetChanged();
                        recyclerView.getAdapter().notifyItemRangeChanged(0, chapters.size());
                    }
                } else {
                    MyToast.getInstance(TxtCatalogActivity.this, message.obj + "").show();
                }
                return false;
            }
        });
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.catalog_list);

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
                // 设置TextView显示数据
                TextView textView = holder.itemView.findViewById(R.id.item_txt);
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
            }

            @Override
            public int getItemCount() {
                return chapters.size();
            }

            public void click(HashMap<String, String> map) {
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
               change();
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

    private void change() {
        new Handler().postDelayed(() -> {
            int num = recyclerView.getAdapter().getItemCount();
            for (int i = 0; i < num; i++) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                if (viewHolder != null) {
                    Button button = viewHolder.itemView.findViewById(R.id.item_true);
                    if (i == index) {
                        button.setVisibility(View.VISIBLE);
                    } else {
                        button.setVisibility(View.GONE);
                    }
                }
            }
        }, 50);
    }
}