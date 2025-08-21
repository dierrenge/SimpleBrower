package cn.cheng.simpleBrower.custom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.DownloadBean;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.CommonUtils;

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


    public DownloadListDialog(@NonNull Context context) {
        super(context, R.style.dialog);
        this.context = context;
        downloadList = MyApplication.getDownloadList();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定view
        setContentView(R.layout.download_list_dialog);
        // 设置返回键可以关闭弹框
        setCancelable(true);
        // 设置触摸弹框以外区域可以关闭弹框
        setCanceledOnTouchOutside(true);

        // view窗口显示设置
        Window window = this.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.DialogAnimation); // 应用动画样式
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = 0.3F;
        window.setAttributes(params);

        initRecyclerView();
    }

    private void initRecyclerView() {
        // 获取recyclerview视图实例
        recyclerView = findViewById(R.id.download_list);
        txt_bg2 = findViewById(R.id.txt_bg2);

        if (downloadList.size() == 0) {
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
                        .inflate(R.layout.download_recyclerview_item, parent, false); // 第三个参数必须是 false！
                return new RecyclerView.ViewHolder(itemView) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                // 设置TextView显示数据
                EditText editText = holder.itemView.findViewById(R.id.item_downloadFilename);
                TextView textView = holder.itemView.findViewById(R.id.item_downloadFileType);
                // textView.setInputType(InputType.TYPE_NULL); // 屏蔽软键盘
                Button button = holder.itemView.findViewById(R.id.item_download);

                DownloadBean bean = downloadList.get(position);
                if (bean == null) return;
                editText.setText(bean.getTitle());
                textView.setText(bean.getFileType());
                button.setOnClickListener(view -> {
                    try {
                        // 会用到的权限
                        if (!CommonUtils.hasStoragePermissions(context)) {
                            CommonUtils.requestStoragePermissions((Activity) context);
                            return;
                        }
                        if (!CommonUtils.requestNotificationPermissions((Activity) context)) return; // 通知
                        Intent intent = new Intent(MyApplication.getContext(), DownloadService.class);
                        intent.putExtra("what", bean.getWhat());
                        intent.putExtra("url", bean.getUrl());
                        String title0 = editText.getText().toString();
                        intent.putExtra("title", title0.length() > 30 ? title0.substring(0, 24) + "···" + title0.substring(title0.length() - 6) : title0);
                        MyApplication.getContext().startService(intent);
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
                    if (callListener != null && downloadList.size() == 0) {
                        callListener.deleteAll();
                    }
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

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        DownloadListDialog.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
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
    }

}
