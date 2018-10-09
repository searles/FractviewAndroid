package at.searles.fractview.bitmap.ui.imageview;

import android.graphics.Canvas;
import android.view.MotionEvent;

import org.jetbrains.annotations.NotNull;

import at.searles.fractview.bitmap.ui.ScalableImageView;

public abstract class Plugin {
    protected final ScalableImageView parent;

    protected Plugin(ScalableImageView parent) {
        this.parent = parent;
    }

    public abstract void onDraw(@NotNull Canvas canvas);

    public abstract boolean onTouchEvent(@NotNull MotionEvent event);

}
