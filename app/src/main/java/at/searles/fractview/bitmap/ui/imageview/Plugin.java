package at.searles.fractview.bitmap.ui.imageview;

import android.graphics.Canvas;
import android.view.MotionEvent;

import org.jetbrains.annotations.NotNull;

public abstract class Plugin {
    public abstract void onDraw(@NotNull Canvas canvas);

    public abstract boolean onTouchEvent(@NotNull MotionEvent event);
}
