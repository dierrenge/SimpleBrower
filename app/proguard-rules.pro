# GSYVideoPlayer视频播放框架
-keep class com.shuyu.gsyvideoplayer.video.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.video.**
-keep class com.shuyu.gsyvideoplayer.video.base.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.video.base.**
-keep class com.shuyu.gsyvideoplayer.utils.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.utils.**
-keep class com.shuyu.gsyvideoplayer.player.** {*;}
-dontwarn com.shuyu.gsyvideoplayer.player.**
-keep class tv.danmaku.ijk.** { *; }
-dontwarn tv.danmaku.ijk.**
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, java.lang.Boolean);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 高德地图
# 3D 地图 V5.0.0之后：
-keep class com.amap.api.mapcore.** {*;}
-keep class com.amap.api.maps.**{*;}
-keep class com.amap.api.offlineservice.** {*;}
-keep class com.amap.api.trace.**{*;}
-keep class com.autonavi.**{*;}   # 高德内部渲染类
# 定位
-keep class com.amap.api.location.**{*;}
-keep class com.amap.api.fence.**{*;}
-keep class com.amap.apis.utils.core.api.**{*;}
# 搜索
-keep class com.amap.api.services.**{*;}
# 高危遗漏补充（官方强制要求，否则白屏/崩溃）
-keep class com.amap.api.col.** {*;}      # 地图so加载核心
-keep class com.loc.** {*;}              # 定位so加载核心
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**

#################### 其他依赖（补充您原规则缺失） ####################
# 📦 Apache Commons Lang3
-keep class org.apache.commons.lang3.** { *; }
-dontwarn org.apache.commons.lang3.**

# Gson（ 实体类需您手动补充）
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class cn.cheng.simpleBrower.bean.** { *; }

# Bouncy Castle (本地 jar)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper