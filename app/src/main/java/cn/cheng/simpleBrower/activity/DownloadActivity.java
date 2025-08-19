package cn.cheng.simpleBrower.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class DownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_download);
    }
}