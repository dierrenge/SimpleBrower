package cn.cheng.biShu.custom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import cn.cheng.biShu.MyApplication;
import cn.cheng.biShu.R;
import cn.cheng.biShu.bean.DownloadBean;
import cn.cheng.biShu.service.DownloadService;
import cn.cheng.biShu.util.CommonUtils;

/**
 * Created by YanGeCheng on 2023/4/2.
 * 底部对话框（弹框）
 */
public class DownloadListDialog extends Dialog {

    private Context context;

    private CallListener callListener;

    private static RecyclerView recyclerView;
    private LinearLayout txt_bg2;
    private List<DownloadBean> downloadList;
    private ActivityResultLauncher<Intent> resultLauncher; // 授权回调


    public DownloadListDialog(@NonNull Context context, ActivityResultLauncher<Intent> resultLauncher) {
        super(context, R.style.dialog);
        this.context = context;
        this.resultLauncher = resultLauncher;
        downloadList = MyApplication.getDownloadList();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定view
        setContentView(R.layout.dialog_download_list);
        // 设置返回键可以关闭弹框
        setCancelable(true);
        // 设置触摸弹框以外区域可以关闭弹框
        setCanceledOnTouchOutside(true);

        // view窗口显示设置
        Window window = this.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.DialogAnimation); // 应用动画样式
        // window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // params.dimAmount = 0.2F;
        window.setAttributes(params);

        initRecyclerView();
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.download_list);
        txt_bg2 = findViewById(R.id.txt_bg2);

        if (downloadList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            txt_bg2.setVisibility(View.VISIBLE);
        }

        // 创建线性布局管理器 赋值给recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        // 给recyclerview设置适配器
        RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // 加载子项布局
                View itemView = LayoutInflater.from(context)
                        .inflate(R.layout.recyclerview_dialog_download_item, parent, false); // 第三个参数必须是 false！
                return new RecyclerView.ViewHolder(itemView) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                // 设置TextView显示数据
                LinearLayout parent_l = holder.itemView.findViewById(R.id.item_download_parent_l);
                EditText editText = holder.itemView.findViewById(R.id.item_downloadFilename);
                TextView textView = holder.itemView.findViewById(R.id.item_downloadFileType);
                LinearLayout item_ll = holder.itemView.findViewById(R.id.item_download_ll);
                Button button = holder.itemView.findViewById(R.id.item_download);
                // 动态设置最大宽度
                parent_l.post(() -> {
                    int editTextMaxWidth = parent_l.getWidth() - item_ll.getWidth() - textView.getWidth() - CommonUtils.dpToPx(context, 28);
                    editText.setMaxWidth(editTextMaxWidth);
                });

                DownloadBean bean = downloadList.get(position);
                if (bean == null) return;
                editText.setText(bean.getTitle());
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        bean.setTitle(editText.getText().toString());
                    }
                });
                textView.setText(bean.getFileType());
                button.setOnClickListener(view -> {
                    if (CommonUtils.checkFilename(bean.getTitle())) return;
                    try {
                        // 会用到的权限
                        if (!CommonUtils.hasStoragePermissions(context)) {
                            callListener.setCallback(b -> {
                                if (CommonUtils.hasStoragePermissions(context)) {
                                    if (CommonUtils.hasNotificationPermissions(context)) {
                                        startDownloadService(bean);
                                    } else {
                                        CommonUtils.requestNotificationPermissions((Activity) context, "开启“通知”接收下载进度信息", resultLauncher);
                                    }
                                } else if (CommonUtils.hasStoragePermissions(context) && CommonUtils.hasNotificationPermissions(context)) {
                                    startDownloadService(bean);
                                }
                            });
                            CommonUtils.requestStoragePermissions((Activity) context, resultLauncher);
                        } else {
                            if (CommonUtils.hasNotificationPermissions(context)) {
                                startDownloadService(bean);
                            } else {
                                callListener.setCallback(b -> {
                                    if (CommonUtils.hasNotificationPermissions(context)) {
                                        startDownloadService(bean);
                                    }
                                });
                                CommonUtils.requestNotificationPermissions((Activity) context, "开启“通知”接收下载进度信息", resultLauncher);
                            }
                        }
                    } catch (Throwable e) {
                        CommonUtils.saveLog("=====底部对话框====点击下载=====" + e.getMessage());
                    }
                });
            }

            @Override
            public int getItemCount() {
                return downloadList.size();
            }
        };
        recyclerView.setAdapter(adapter);

        // 滑动监听
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {

            }
        });

        // 配置触摸事件
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, // 拖拽方向
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // 滑动方向
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                try {
                    // 处理拖拽换位
                    int fromPos = viewHolder.getAdapterPosition();
                    int toPos = target.getAdapterPosition();
                    Collections.swap(downloadList, fromPos, toPos);
                    adapter.notifyItemMoved(fromPos, toPos);
                } catch (Throwable e) {
                    CommonUtils.saveLog("===底部对话框====处理拖拽换位=======" + e.getMessage());
                }
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                try {
                    // 处理滑动删除
                    int position = viewHolder.getAdapterPosition();
                    DownloadBean downloadBean = downloadList.get(position);
                    if (downloadBean == null) return;
                    MyApplication.deleteDownloadList(downloadBean.getUrl());
                    adapter.notifyItemRemoved(position);
                    updateMonitorUI();
                } catch (Throwable e) {
                    CommonUtils.saveLog("==底部对话框===处理滑动删除=========" + e.getMessage());
                }
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

    private void startDownloadService(DownloadBean bean) {
        Intent intent = new Intent(MyApplication.getContext(), DownloadService.class);
        intent.putExtra("what", bean.getWhat());
        intent.putExtra("url", bean.getUrl());
        intent.putExtra("title", bean.getTitle());
        intent.putExtra("fileType", bean.getFileType());
        MyApplication.getContext().startService(intent);
    }

    @Override
    public void show() {
        if (callListener != null) callListener.setBackground(true);
        updateMonitorUI();
        super.show();
    }

    @Override
    public void dismiss() {
        updateMonitorUI();
        // DownloadListDialog.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        if (callListener != null) callListener.setBackground(false);
        super.dismiss();
    }

    // Dialog设置外部点击事件
    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
    }

    public void setOnCallListener(CallListener touchListener) {
        this.callListener = touchListener;
    }

    public interface CallListener {
        void deleteAll();
        // 授权回调函数
        void setCallback(ValueCallback<Boolean> callback);
        // 设置阴影
        void setBackground(boolean flag);
    }

    // 更新下载检测按钮图标
    private void updateMonitorUI() {
        if (callListener != null && downloadList != null && downloadList.isEmpty()) {
            callListener.deleteAll();
        }
    }

}
