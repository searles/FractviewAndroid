package at.searles.fractview.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.Timer;
import java.util.TimerTask;

import at.searles.fractview.R;

/**
 * Thanks to http://stackoverflow.com/questions/12074950/android-horizontalscrollview-inside-scrollview
 */

public class MultiScrollView extends LinearLayout {


    public interface InternalView {
        void setLeftTop(int left, int top);
        int getIntendedWidth();
        int getIntendedHeight();
        View view();

        boolean singleTap(MotionEvent evt);
        boolean doubleTap(MotionEvent evt);
        boolean longPress(MotionEvent evt);
        boolean moveTo(MotionEvent evt);

        boolean tapUp(MotionEvent event);
    }

    private HorizontalScrollView hscroll;
    private ScrollView vscroll;

    // spacings so that the scrolls will be of correct size
    private View hspace, vspace;

    private GestureDetector gestureDetector;

    private InternalView content;

    public MultiScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public MultiScrollView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.multiscroll_layout, this);

        // two hidden scrollviews in the background
        hscroll = (HorizontalScrollView) findViewById(R.id.hscroll);
        vscroll = (ScrollView) findViewById(R.id.vscroll);

        // add some space in them
        hspace = findViewById(R.id.hspace);
        vspace = findViewById(R.id.vspace);

        // TODO if a generalization is needed, here this should be modified.
        content = (InternalView) findViewById(R.id.paletteView);

        updateSize();

        // on rotation the scrollbars store their values which is
        // very useful here...
        content.setLeftTop(hscroll.getScrollX(), vscroll.getScrollY());

        // add listener to scrollevents
        hscroll.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                updateViewCoordinates();
            }
        });

        vscroll.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                updateViewCoordinates();
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // forward this one to the internal view
                return content.singleTap(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // forward this one to the internal view
                return content.doubleTap(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // Long press starts a dragging event
                content.longPress(e);
            }
        });
    }

    private void updateViewCoordinates() {
        content.setLeftTop(hscroll.getScrollX(), vscroll.getScrollY());
    }

    public void updateSize() {
        // force it
        int width = content.getIntendedWidth();
        hspace.setMinimumWidth(content.getIntendedWidth());

        ViewGroup.LayoutParams l = hspace.getLayoutParams();
        l.width = width; // height is 1.
        hspace.setLayoutParams(l);

        // same for vspace
        int height = content.getIntendedHeight();
        vspace.setMinimumHeight(height);

        l = vspace.getLayoutParams();
        l.height = height; // height is 1.
        vspace.setLayoutParams(l);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Log.d("MSV", hscroll.getScrollX() + "x" + vscroll.getScrollY());

        content.view().draw(canvas);

        hscroll.draw(canvas);
        vscroll.draw(canvas);
    }

    int tapMargin = 64;
    float initPxPerMove = 24;
    float maxPxPerMove = 240;
    final int dragScrollFrequency = 40; // 25 times per hour
    float accelerationFactor = 1.05f;

    float pxPerMove;
    float dx, dy;

    Timer scrollDragTimer = null;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_MOVE) {
            if(content.moveTo(event)) {
                // check whether we should adjust the scrolling
                // and also in which direction
                dx = event.getX() - getWidth() / 2f;
                dy = event.getY() - getHeight() / 2f;

                if(Math.abs(dx) > getWidth() / 2f - tapMargin ||
                        Math.abs(dy) > getHeight() / 2f - tapMargin) {
                    // yes, we should update it
                    float d = (float) Math.sqrt(dx * dx + dy * dy);

                    dx = dx / d;
                    dy = dy / d;

                    if(scrollDragTimer == null) {
                        // there is no timer yet, create one.
                        pxPerMove = initPxPerMove;
                        scrollDragTimer = new Timer();
                        scrollDragTimer.scheduleAtFixedRate(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        if(pxPerMove < maxPxPerMove) {
                                            pxPerMove *= accelerationFactor;
                                        }

                                        hscroll.setScrollX(hscroll.getScrollX() + (int) (dx * pxPerMove));
                                        vscroll.setScrollY(vscroll.getScrollY() + (int) (dy * pxPerMove));
                                    }
                                }, 0, dragScrollFrequency
                        );
                    }
                } else if(scrollDragTimer != null) {
                    // no scrolling anymore. cancel timer.
                    scrollDragTimer.cancel();
                    scrollDragTimer = null;
                }

                return true;
            }
        } else if(event.getAction() == MotionEvent.ACTION_UP) {
            if(scrollDragTimer != null) {
                scrollDragTimer.cancel();
                scrollDragTimer = null;
            }
            if(content.tapUp(event)) return true;
        }

        if(!gestureDetector.onTouchEvent(event)) {
            boolean ret = hscroll.dispatchTouchEvent(event);
            ret |= vscroll.dispatchTouchEvent(event);

            return ret;
        } else {
            return true;
        }
    }
}
