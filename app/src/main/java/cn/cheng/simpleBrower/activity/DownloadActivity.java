package cn.cheng.simpleBrower.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class DownloadActivity extends AppCompatActivity {

    private Button back;

    private Button change;

    private LinearLayout layout;

    private static RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_download);

        // 返回
        back = findViewById(R.id.download_back);
        back.setOnClickListener(view -> {
            this.finish();
        });

        // 编辑
        change = findViewById(R.id.download_change);
        change.setOnClickListener(view -> {
            if (recyclerView != null) {

            }
        });

        // 背景
        layout = findViewById(R.id.download_bg);

        initRecyclerView();
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.download_mg_list);

        // 创建线性布局管理器 赋值给recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

    }
}