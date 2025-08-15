package cn.cheng.simpleBrower.activity;

import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.listener.GSYVideoProgressListener;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;

import java.util.ArrayList;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.bean.SwitchVideoModel;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.custom.video.SampleVideo;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.util.AssetsReader;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class VideoActivity extends AppCompatActivity {

    SampleVideo mVideoPlayer;

    private OrientationUtils orientationUtils;

    private String videoUrl;

    // 记录播放进度
    private long position;

    // 记录播放时长
    private long durationAll = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.setActivity(this);
        try {
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
            String name = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
            // 获取影音列表
            List<String> formats = AssetsReader.getList("audioVideo.txt");
            List<SwitchVideoModel> videoList = new ArrayList<>();
            if (name.contains(".") && formats.contains(name.substring(name.lastIndexOf(".")))) {
                // 测试用2
                /*if (name.contains(".m3u8")) {
                    new Handler().post(() -> {
                        CommonUtils.setTsNumLog(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/SimpleBrower/m3u8/" + name.replace(".m3u8", "") + "/1.xyz2"));
                    });
                }*/
                List<String> videoUrls = new ArrayList<>();
                CommonUtils.fileWalk(videoUrl.substring(0, videoUrl.lastIndexOf("/") + 1), formats, videoUrls, 1);
                for (String url : videoUrls) {
                    SwitchVideoModel switchVideoModel = new SwitchVideoModel(url.substring(url.lastIndexOf("/") + 1), url);
                    videoList.add(switchVideoModel);
                }
            } else { // 未知格式文件
                // 测试用1
                /*if (name.contains(".xyz")) {
                    new Handler().post(() -> {
                        CommonUtils.setTsNumLog(new File(videoUrl));
                    });
                }*/
                SwitchVideoModel switchVideoModel = new SwitchVideoModel(videoUrl.substring(videoUrl.lastIndexOf("/") + 1), videoUrl);
                videoList.add(switchVideoModel);
            }

            // 初始化视频设置
            initVideoView(videoList, name);
        } catch (Throwable e) {
            MyToast.getInstance(this, "打开异常咯").show();
            e.printStackTrace();
            CommonUtils.saveLog("VideoActivity:" + e.getMessage());
            this.finish();
        }
    }

    private void initVideoView(List<SwitchVideoModel> videoList, String name) {

        //EXOPlayer内核，支持格式更多
        //PlayerFactory.setPlayManager(Exo2PlayerManager.class);
        //ijk内核，默认模式
        //PlayerFactory.setPlayManager(IjkPlayerManager.class);

        //exo缓存模式，支持m3u8，只支持exo
        //CacheFactory.setCacheManager(ExoPlayerCacheManager.class);
        //代理缓存模式，支持所有模式，不支持m3u8等，默认
        //CacheFactory.setCacheManager(ProxyCacheManager.class);

        //ijk关闭log
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);

        mVideoPlayer = findViewById(R.id.video_player);

        //播放视频
        boolean setUp = mVideoPlayer.setUp(videoList, name);

        //默认显示比例
        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);

        //根据视频尺寸，自动选择竖屏全屏或者横屏全屏
        mVideoPlayer.setAutoFullWithSize(true);

        //全屏动画
        mVideoPlayer.setShowFullAnimation(true);

        //是否可以滑动调整
        mVideoPlayer.setIsTouchWiget(true);

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
            public void onProgress(long progress, long secProgress, long currentPosition, long duration) {
                position = currentPosition;
                // 初始滑动快进比例设置
                if (durationAll == 0) {
                    // 根据播放时长 调整触摸滑动快进比例
                    // 大概估算 参数为1时小拖一下 进度1/15
                    // 快进比例 = duration/1000 * 1/15  / 目标小拖动时间
                    int m = 10; // 目标小拖动时间10秒
                    float pro = duration/1000F * 1/16F /m; // 快进比例
                    mVideoPlayer.setSeekRatio(pro);
                    durationAll = duration;
                    // System.out.println("=======================" + mVideoPlayer.getSeekRatio());
                    // System.out.println("=======================" + pro);
                }

                // 播放完成监听
                float last = 1F * currentPosition / duration;
                // System.out.println("==============" + last);
                // System.out.println("========progress======" + progress);
                if (last  >= 0.998 || progress == 100) {
                    mVideoPlayer.startNext();
                }
            }
        });

        //全屏按键
        mVideoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接横屏
                orientationUtils.resolveByClick();
                VideoActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE); // 根据传感器自动旋转到横屏模式
                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                mVideoPlayer.startWindowFullscreen(VideoActivity.this, true, true);

            }
        });

        // mVideoPlayer.setSpeed(10);

        //开始播放
        mVideoPlayer.startPlayLogic();

        // 恢复播放进度
        Long positionX = MyApplication.getVideoPosition().get(name);
        if (positionX != null && positionX > 0) {
            new Handler().postDelayed(() -> {
                mVideoPlayer.seekTo(positionX);
            }, 100);
        }
    }

    @Override
    public void onBackPressed() {
        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }
        if (GSYVideoManager.backFromWindowFull(this)) {
            return;
        }
        //释放所有
        mVideoPlayer.setVideoAllCallBack(null);
        GSYVideoManager.releaseAllVideos();
        super.onBackPressed();
    }

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
        MyApplication.setActivity(this);
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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoPlayer != null) {
            // 记录播放进度
            CharSequence text = mVideoPlayer.getCurrentPlayer().getTitleTextView().getText();
            MyApplication.setVideoPosition(text.toString(), position);
        }
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }

}