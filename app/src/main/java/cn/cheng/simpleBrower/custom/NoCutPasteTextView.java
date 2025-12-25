package cn.cheng.simpleBrower.custom;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * 自定义TextView，允许复制但不允许剪切和粘贴
 */
public class NoCutPasteTextView extends AppCompatTextView {
    public NoCutPasteTextView(@NonNull Context context) {
        super(context);
    }

    public NoCutPasteTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoCutPasteTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    public boolean onTextContextMenuItem(int id) {
        // 允许复制操作 (id=android.R.id.copy)
        if (id == android.R.id.copy) {
            return super.onTextContextMenuItem(id);
        }
        // 禁止剪切 (id=android.R.id.cut) 和粘贴 (id=android.R.id.paste)
        if (id == android.R.id.cut || id == android.R.id.paste) {
            return true; // 拦截操作但不执行
        }
        return super.onTextContextMenuItem(id);
    }
}
