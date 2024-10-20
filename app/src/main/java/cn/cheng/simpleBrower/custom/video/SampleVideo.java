package cn.cheng.simpleBrower.custom.video;

import static android.text.util.Linkify.ALL;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.cache.CacheFactory;
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.SwitchVideoModel;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.util.CommonUtils;
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by shuyu on 2016/12/7.
 * 注意
 * 这个播放器的demo配置切换到全屏播放器
 * 这只是单纯的作为全屏播放显示，如果需要做大小屏幕切换，请记得在这里耶设置上视频全屏的需要的自定义配置
 *
 * Modify by yangecheng on 2024/10/08.
 */

public class SampleVideo extends StandardGSYVideoPlayer {

    private TextView mNowTime;

    private TextView mSwitchSize;

    private TextView mChangeRotate;

    private TextView mChangeTransform;

    private List<SwitchVideoModel> mUrlList = new ArrayList<>();

    //记住切换数据源类型
    private int mType = 0;

    private int mTransformSize = 0;

    //数据源
    private int mSourcePosition = 0;

    private String mTypeText = "选集";

    /**
     * 1.5.0开始加入，如果需要不同布局区分功能，需要重载
     */
    public SampleVideo(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public SampleVideo(Context context) {
        super(context);
    }

    public SampleVideo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        initView();
    }

    private void initView() {
        mNowTime = (TextView) findViewById(R.id.nowTime);
        mSwitchSize = (TextView) findViewById(R.id.switchSize);
        mChangeRotate = (TextView) findViewById(R.id.change_rotate);
        mChangeTransform = (TextView) findViewById(R.id.change_transform);

        // 时间
        mNowTime.setText(CommonUtils.SysTime());
        mNowTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mHadPlay) {
                    return;
                }

            }
        });

        //切换视频
        mSwitchSize.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSwitchDialog();
            }
        });

        //旋转播放角度
        mChangeRotate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mHadPlay) {
                    return;
                }
                if ((mTextureView.getRotation() - mRotate) == 270) {
                    mTextureView.setRotation(mRotate);
                    mTextureView.requestLayout();
                } else {
                    mTextureView.setRotation(mTextureView.getRotation() + 90);
                    mTextureView.requestLayout();
                }
            }
        });

        //镜像旋转
        mChangeTransform.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mHadPlay) {
                    return;
                }
                if (mTransformSize == 0) {
                    mTransformSize = 1;
                } else if (mTransformSize == 1) {
                    mTransformSize = 2;
                } else if (mTransformSize == 2) {
                    mTransformSize = 0;
                }
                resolveTransform();
            }
        });

    }

    /**
     * 需要在尺寸发生变化的时候重新处理
     */
    @Override
    public void onSurfaceSizeChanged(Surface surface, int width, int height) {
        super.onSurfaceSizeChanged(surface, width, height);
        resolveTransform();
    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        super.onSurfaceAvailable(surface);
        resolveRotateUI();
        resolveTransform();
    }

    /**
     * 处理镜像旋转
     * 注意，暂停时
     */
    protected void resolveTransform() {
        switch (mTransformSize) {
            case 1: {
                Matrix transform = new Matrix();
                transform.setScale(-1, 1, mTextureView.getWidth() / 2, 0);
                mTextureView.setTransform(transform);
                mChangeTransform.setText("左右镜像");
                mTextureView.invalidate();
            }
            break;
            case 2: {
                Matrix transform = new Matrix();
                transform.setScale(1, -1, 0, mTextureView.getHeight() / 2);
                mTextureView.setTransform(transform);
                mChangeTransform.setText("上下镜像");
                mTextureView.invalidate();
            }
            break;
            case 0: {
                Matrix transform = new Matrix();
                transform.setScale(1, 1, mTextureView.getWidth() / 2, 0);
                mTextureView.setTransform(transform);
                mChangeTransform.setText("旋转镜像");
                mTextureView.invalidate();
            }
            break;
        }
    }


    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param title         title
     * @return
     */
    public boolean setUp(List<SwitchVideoModel> url, String title) {
        mUrlList = url;
        // 根据视频名称 指定播放列表中对应的视频
        for (int i = 0; i < mUrlList.size(); i++) {
            if (title.equals(mUrlList.get(i).getName())) {
                mSourcePosition = i;
                break;
            }
        }

        String thisUrl = url.get(mSourcePosition).getUrl();
        return setUp0(thisUrl,  (File) null, title);
    }

    public boolean setUp0(String url, File cachePath, String title) {
        boolean cacheWithPlay;
        if (url.toLowerCase().endsWith(".mp4")) {
            //ijk内核，默认模式
            PlayerFactory.setPlayManager(IjkPlayerManager.class);
            //代理缓存模式，支持所有模式，不支持m3u8等，默认
            CacheFactory.setCacheManager(ProxyCacheManager.class);
            cacheWithPlay = true;
        } else {
            //EXOPlayer内核，支持格式更多
            PlayerFactory.setPlayManager(Exo2PlayerManager.class);
            //exo缓存模式，支持m3u8，只支持exo
            CacheFactory.setCacheManager(ExoPlayerCacheManager.class);
            cacheWithPlay = false;
            // m3u8格式播放 尝试设置
            if (url.toLowerCase().endsWith(".m3u8")) {
                VideoOptionModel videoOptionModel =
                        new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "crypto,file,http,https,tcp,tls,udp");
                VideoOptionModel videoOptionModel2 =
                        new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "allowed_extensions", ALL);
                List<VideoOptionModel> list = new ArrayList<>();
                list.add(videoOptionModel);
                list.add(videoOptionModel2);
                GSYVideoManager.instance().setOptionModelList(list);
            }
        }
        return setUp(url, cacheWithPlay, cachePath, title);
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param cachePath     缓存路径，如果是M3U8或者HLS，请设置为false
     * @param title         title
     * @return
     */
    public boolean setUp(List<SwitchVideoModel> url, boolean cacheWithPlay, File cachePath, String title) {
        mUrlList = url;
        return setUp(url.get(mSourcePosition).getUrl(), cacheWithPlay, cachePath, title);
    }

    @Override
    public int getLayoutId() {
        return R.layout.sample_video;
    }


    /**
     * 全屏时将对应处理参数逻辑赋给全屏播放器
     *
     * @param context
     * @param actionBar
     * @param statusBar
     * @return
     */
    @Override
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        SampleVideo sampleVideo = (SampleVideo) super.startWindowFullscreen(context, actionBar, statusBar);
        sampleVideo.mSourcePosition = mSourcePosition;
        sampleVideo.mType = mType;
        sampleVideo.mTransformSize = mTransformSize;
        sampleVideo.mUrlList = mUrlList;
        sampleVideo.mTypeText = mTypeText;
        //sampleVideo.resolveTransform();
        sampleVideo.resolveTypeUI();
        //sampleVideo.resolveRotateUI();
        //这个播放器的demo配置切换到全屏播放器
        //这只是单纯的作为全屏播放显示，如果需要做大小屏幕切换，请记得在这里耶设置上视频全屏的需要的自定义配置
        //比如已旋转角度之类的等等
        //可参考super中的实现
        return sampleVideo;
    }

    /**
     * 推出全屏时将对应处理参数逻辑返回给非播放器
     *
     * @param oldF
     * @param vp
     * @param gsyVideoPlayer
     */
    @Override
    protected void resolveNormalVideoShow(View oldF, ViewGroup vp, GSYVideoPlayer gsyVideoPlayer) {
        super.resolveNormalVideoShow(oldF, vp, gsyVideoPlayer);
        if (gsyVideoPlayer != null) {
            SampleVideo sampleVideo = (SampleVideo) gsyVideoPlayer;
            mSourcePosition = sampleVideo.mSourcePosition;
            mType = sampleVideo.mType;
            mTransformSize = sampleVideo.mTransformSize;
            mTypeText = sampleVideo.mTypeText;
            setUp(mUrlList, mCache, mCachePath, mTitle);
            resolveTypeUI();
        }
    }

    /**
     * 旋转逻辑
     */
    private void resolveRotateUI() {
        if (!mHadPlay) {
            return;
        }
        mTextureView.setRotation(mRotate);
        mTextureView.requestLayout();
    }

    /**
     * 显示比例
     * 注意，GSYVideoType.setShowType是全局静态生效，除非重启APP。
     */
    private void resolveTypeUI() {
        if (!mHadPlay) {
            return;
        }
        if (mType == 0) {
            // 默认比例
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);
        }
        changeTextureViewShowType();
        if (mTextureView != null)
            mTextureView.requestLayout();
        // mSwitchSize.setText(mTypeText);
    }

    /**
     * 弹出选集
     */
    private void showSwitchDialog() {
        if (!mHadPlay) {
            return;
        }
        SwitchVideoTypeDialog switchVideoTypeDialog = new SwitchVideoTypeDialog(getContext());
        String name = mUrlList.get(mSourcePosition).getName();
        switchVideoTypeDialog.initList(mUrlList, name, new SwitchVideoTypeDialog.OnListItemClickListener() {
            @Override
            public void onItemClick(int position) {
                final String name = mUrlList.get(position).getName();
                if (mSourcePosition != position) {
                    start(position, name);
                } else {
                    MyToast.getInstance(MyApplication.getActivity(), "当前正在播放").show();
                    // Toast.makeText(getContext(), "已经是 " + name, Toast.LENGTH_LONG).show();
                }
            }
        });
        switchVideoTypeDialog.show();
    }

    private void start(int position, String name) {
        if ((mCurrentState == GSYVideoPlayer.CURRENT_STATE_PLAYING
                || mCurrentState == GSYVideoPlayer.CURRENT_STATE_PAUSE)) {
            final String url = mUrlList.get(position).getUrl();
            onVideoPause();
            final long currentPosition = mCurrentPosition;
            getGSYVideoManager().releaseMediaPlayer();
            cancelProgressTimer();
            hideAllWidget();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setUp0(url, mCachePath, name);
                    // setSeekOnStart(currentPosition);
                    startPlayLogic();
                    cancelProgressTimer();
                    hideAllWidget();
                }
            }, 500);
            mTypeText = name;
            // mSwitchSize.setText(name);
            mSourcePosition = position;
        }
    }

    // 播放下一集
    public void startNext() {
        if (!mHadPlay) {
            return;
        }
        if (mUrlList.size() > mSourcePosition + 1) {
            mSourcePosition++;
            final String name = mUrlList.get(mSourcePosition).getName();
            start(mSourcePosition, name);
        }
    }

    /**
     * 点击触摸显示和隐藏逻辑
     * @param e
     */
    @Override
    protected void onClickUiToggle(MotionEvent e) {
        mNowTime.setText(CommonUtils.SysTime());
        super.onClickUiToggle(e);
    }
}
