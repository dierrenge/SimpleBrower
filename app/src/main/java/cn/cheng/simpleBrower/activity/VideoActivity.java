package cn.cheng.simpleBrower.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;

import com.shuyu.gsyvideoplayer.GSYVideoADManager;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.cache.CacheFactory;
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.listener.GSYVideoProgressListener;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class VideoActivity extends AppCompatActivity {

    StandardGSYVideoPlayer mVideoPlayer;

    private OrientationUtils orientationUtils;

    private String videoUrl;

    // 记录播放进度
    private long position;

    // 记录播放时长
    private int durationAll = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏状态栏和导航栏
        /*View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }*/
        SysWindowUi.hideStatusNavigationBar(this, true);

        setContentView(R.layout.activity_video);

        Intent intent = this.getIntent();

        String action = intent.getAction();
        Uri uri = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            // 设置此activity可用于打开 视频文件
            videoUrl = CommonUtils.correctUrl(uri.getPath());
        } else {
            // 获取上个页面传递的信息
            videoUrl = intent.getStringExtra("videoUrl");
        }
        CommonUtils.saveLog("打开方式-视频文件：" + videoUrl);

        if (videoUrl == null || !videoUrl.contains("/")) {
            return;
        }
        String[] s = videoUrl.split("/");
        String name = s[s.length - 1];

        // 初始化视频设置
        initVideoView(videoUrl, name);
    }

    private void initVideoView(String videoUrl, String name) {

        //EXOPlayer内核，支持格式更多
        //PlayerFactory.setPlayManager(Exo2PlayerManager.class);
        //ijk内核，默认模式
        //PlayerFactory.setPlayManager(IjkPlayerManager.class);


        //exo缓存模式，支持m3u8，只支持exo
        // CacheFactory.setCacheManager(ExoPlayerCacheManager.class);
        //代理缓存模式，支持所有模式，不支持m3u8等，默认
        //CacheFactory.setCacheManager(ProxyCacheManager.class);

        //ijk关闭log
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);

        mVideoPlayer = findViewById(R.id.video_player);

        //播放横屏视频
        boolean setUp = mVideoPlayer.setUp(videoUrl, true, name);

        //默认显示比例
        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);

        //根据视频尺寸，自动选择竖屏全屏或者横屏全屏
        mVideoPlayer.setAutoFullWithSize(true);

        //全屏动画
        mVideoPlayer.setShowFullAnimation(true);

        //循环播放 关闭
        mVideoPlayer.setLooping(false);

        //设置封面
        // if (setUp) {
        //     Glide.with(this).load(productItem.getCapture()).into((ImageView) mVideoPlayer.getThumbImageView());
        // }

        //隐藏自带的标题 返回键
        // mVideoPlayer.getTitleTextView().setVisibility(View.GONE);
        // mVideoPlayer.getBackButton().setVisibility(View.GONE);
        // 返回键
        mVideoPlayer.getBackButton().setOnClickListener(view -> {
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // 当前为横屏，返回到竖屏
                if (orientationUtils != null) {
                    orientationUtils.backToProtVideo();
                }
            } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // 当前为竖屏，返回到上个页面
                this.finish();
            }
        });

        // 全屏相关设置
        orientationUtils = new OrientationUtils(this, mVideoPlayer);
        mVideoPlayer.setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                //开始播放了才能旋转和全屏
                orientationUtils.setEnable(true);
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                if (orientationUtils != null) {
                    orientationUtils.backToProtVideo();
                }
            }
        });
        mVideoPlayer.setLockClickListener(new LockClickListener() {
            @Override
            public void onClick(View view, boolean lock) {
                if (orientationUtils != null) {
                    // 配合下方的onConfigurationChanged
                    orientationUtils.setEnable(!lock);
                }
            }
        });

        // 获取进度
        mVideoPlayer.setGSYVideoProgressListener(new GSYVideoProgressListener() {
            @Override
            public void onProgress(int progress, int secProgress, int currentPosition, int duration) {
                position = currentPosition;
                if (durationAll == 0) {
                    // 根据播放时长 调整触摸滑动快进比例
                    // 大概估算 参数为1时小拖一下 进度1/15
                    // 快进比例 = duration/1000 * 1/15  / 目标小拖动时间
                    int m = 10; // 目标小拖动时间10秒
                    float pro = duration/1000F * 1/15F /m; // 快进比例
                    mVideoPlayer.setSeekRatio(pro);
                    durationAll = duration;
                    /*System.out.println("=======================" + mVideoPlayer.getSeekRatio());
                    System.out.println("=======================" + pro);
                    System.out.println("=======================" + duration);*/
                }
            }
        });

        //全屏按键
        mVideoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接横屏
                orientationUtils.resolveByClick();
                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                mVideoPlayer.startWindowFullscreen(VideoActivity.this, true, true);


            }
        });

        // mVideoPlayer.setSpeed(10);

        //开始播放
        mVideoPlayer.startPlayLogic();

    }

    @Override
    public void onBackPressed() {
        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }
        if (GSYVideoManager.backFromWindowFull(this)) {
            return;
        }
        super.onBackPressed();
    }

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
        // SysWindowUi.hideStatusNavigationBar(this, true);
        super.onResume();
        if (mVideoPlayer != null) {
            mVideoPlayer.onVideoResume();
        }
    }

    //再一次从后台 到前台时
    @Override
    protected void onRestart() {
        // SysWindowUi.hideStatusNavigationBar(this, true);
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoPlayer != null) {
            mVideoPlayer.onVideoPause();
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // 当前为横屏
            } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // 当前为竖屏
            }
            // 定位历史播放进度
            new Handler().postDelayed(() -> {
                if (position > 2000) {
                    mVideoPlayer.seekTo(position);
                }
            }, 500);
        }
    }

    /*@Override
    public void onConfigurationChanged(Configuration newConfig) {
        System.out.println("貌似该方法没被调用");
        super.onConfigurationChanged(newConfig);
        // 如果旋转了就全屏
        if (isPlay) {
            mVideoPlayer.onConfigurationChanged(this, newConfig, orientationUtils, true, true);
        }
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 当前为横屏
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 当前为竖屏
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GSYVideoADManager.releaseAllVideos();
    }

}