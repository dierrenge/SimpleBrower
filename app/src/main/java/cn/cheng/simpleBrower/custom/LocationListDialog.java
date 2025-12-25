package cn.cheng.simpleBrower.custom;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.LocationBean;
import cn.cheng.simpleBrower.util.CommonUtils;

/**
 * Created by YanGeCheng on 2025/12/24.
 * 定位记录底部弹框
 */
public class LocationListDialog extends Dialog {

    private Context context;

    private CallListener callListener;

    private static RecyclerView recyclerView;
    private LinearLayout txt_bg3;
    private List<LocationBean> locationList;


    public LocationListDialog(@NonNull Context context, List<LocationBean> locationList) {
        super(context, R.style.dialog);
        this.context = context;
        this.locationList = locationList;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定view
        setContentView(R.layout.location_list_dialog);
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
        recyclerView = findViewById(R.id.location_list);
        txt_bg3 = findViewById(R.id.txt_bg3);

        if (locationList.size() == 0) {
            recyclerView.setVisibility(View.GONE);
            txt_bg3.setVisibility(View.VISIBLE);
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
                        .inflate(R.layout.location_recyclerview_item, parent, false); // 第三个参数必须是 false！
                return new RecyclerView.ViewHolder(itemView) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
                // 设置TextView显示数据
                NoCutPasteTextView textView = holder.itemView.findViewById(R.id.item_address);
                Button button = holder.itemView.findViewById(R.id.item_location);
                LocationBean bean = locationList.get(position);
                if (bean == null) return;
                textView.setText(bean.getAddress());
                button.setOnClickListener(view -> {
                    try {
                        callListener.click(bean);
                        dismiss();
                    } catch (Throwable e) {
                        CommonUtils.saveLog("=====底部对话框====点击定位=====" + e.getMessage());
                    }
                });
            }

            @Override
            public int getItemCount() {
                return locationList.size();
            }
        };
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                // ItemTouchHelper.UP | ItemTouchHelper.DOWN, // 拖拽方向
                0, // 不允许拖拽
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // 滑动方向
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // 不允许拖拽，所以返回false
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                try {
                    // 处理滑动删除
                    int position = viewHolder.getAdapterPosition();
                    LocationBean locationBean = locationList.get(position);
                    if (locationBean == null) return;
                    locationList.removeIf(item -> item.getLatitude() == locationBean.getLatitude()
                            && item.getLongitude() == locationBean.getLongitude());
                    new Handler().post(() -> {
                        CommonUtils.deleteLocalObject("locationList",
                                ""+locationBean.getLongitude()+locationBean.getLatitude());
                    });
                    adapter.notifyItemRemoved(position);
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
        LocationListDialog.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
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
        void click(LocationBean bean);
    }

}
