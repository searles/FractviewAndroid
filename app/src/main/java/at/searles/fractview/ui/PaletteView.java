package at.searles.fractview.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;

import at.searles.fractview.PaletteActivity;
import at.searles.fractview.editors.EditableDialogFragment;
import at.searles.math.Commons;
import at.searles.math.color.Colors;

import static at.searles.fractview.ui.PaletteView.SelectionType.BoundX;
import static at.searles.fractview.ui.PaletteView.SelectionType.BoundY;

/**
 + Design: SelectAt should return an object instead setting members.
 + Add context menus
 - If color is selected, colorpicker
 - If column/row, delete/duplicate
 - If left top, transpose
 + Prettify
 - Nice dragging with the following coordinates
 - min(relX1, pix(index)) - (iconSize + padding) / 2
 - max(relX1, pix(index)) + (iconSize + padding) / 2
 + Save model on rotate
 + Better random
 - Pick 3 random colors and use the one with the biggest
 difference to its neighbor:
 - See https://en.wikipedia.org/wiki/Color_difference#CIEDE2000
 */
public class PaletteView extends View implements MultiScrollView.InternalView {

    // some arguments
    int margin; // space at borders
    int padding; // space between items
    int iconSize; // size of icons.

    Paint framePaint;
    Paint fillPaint;

    // done with constants.
    
    /**
     * For scrolling, shift in coordinates.
     */
    int left = 0, top = 0;

    /**
     * When dragging there are three points of importance.
     * lastPt is the last point of an event, currentPt is
     * the current one.
     */
    private float lastPtX, lastPtY;
    private float currentPtX, currentPtY;

    private Paint outsideSelectionPaint;
    private Paint insideSelectionPaint;
    private Paint borderPaint;

    enum SelectionType {
        Column, Row, TopLeft, Color, BoundX, BoundY, BoundBoth
    }

    private class Selection {
        SelectionType type;

        //float rx; // relative x coordinate
        //float ry; // relative y coordinate

        int initX; // coordinates in terms of columns/rows
        int initY; // needed for drawing rotation.

        public String toString() {
            return type + ": " + initX + ", " + initY;
        }
    }

    // Geometry stuff

    /**
     * Converts relative coordinate to an (col or row) index (there is a reason
     * why things are squares).
     * Geometry is as follows: margin - icon - padding - icon ... - padding - icon - margin
     * icon and the adjacent padding are combined.
     * @param f the relative coordinate (ie left/top is considered already)
     * @return The value index (starting with -1 which is the header)
     */
    int index(float f) {
        // returns -1 if it is the header.
        return (int) Math.floor((f - margin + padding / 2f) / (iconSize + padding) - 1);
    }

    /**
     * Inverse of index function. It returns the upper/left edge of
     * the area in which the icon is. To get the center of the icon,
     * add (iconsize+padding)/2.
     * @param index
     * @return
     */
    float pix(float index) {
        return margin - padding / 2f + (index + 1) * (iconSize + padding);
    }

    public int getIntendedWidth() {
        // two extra icons.
        if(model() == null) return 0;
        return 2 * margin + (model().width() + 1) * (padding + iconSize) + iconSize;
    }

    public int getIntendedHeight() {
        if(model() == null) return 0;
        return 2 * margin + (model().height() + 1) * (padding + iconSize) + iconSize;
    }

    /**
     * Returns true if s (being relative) is between the tile with
     * index index and index+1.
     * @param s
     * @return
     */
    boolean isOnBorderAt(float s, int index) {
        return index(s + padding / 2) == index + 1 && index(s - padding / 2) == index;
    }

    /**
     * What is selected when something is selected at x0/y0?
     * x0 and y0 are not relative.
     * @param x0
     * @param y0
     * @return
     */
    private Selection at(float x0, float y0) {
        // where is the selection?
        boolean onBoundsX = isOnBorderAt(x0 + left, model().width() - 1);
        boolean onBoundsY = isOnBorderAt(y0 + top, model().height() - 1);

        if(onBoundsX || onBoundsY) {
            Selection sel = new Selection();

            // no context information needed because currentPt?
            // determines where the selection is.

            if(onBoundsX) {
                if(onBoundsY) {
                    sel.type = SelectionType.BoundBoth;
                } else {
                    sel.type = BoundX;
                }
            } else {
                sel.type = SelectionType.BoundY;
            }

            // it is on the bounds.
            return sel;
        }

        // is it on a square or a header square?

        // to check whether it is on a header, we use the original coordinates.
        boolean isHeaderX = index(x0) == -1; // header: absolute coordinates are in first row.
        boolean isHeaderY = index(y0) == -1;

        int ix = index(x0 + left), iy = index(y0 + top);

        boolean onSquareX = 0 <= ix && ix < model().width();
        boolean onSquareY = 0 <= iy && iy < model().height();

        SelectionType type = null;

        // header first.
        if(isHeaderX) {
            if(isHeaderY || onSquareY) {
                if(isHeaderY) {
                    type = SelectionType.TopLeft;
                } else {
                    type = SelectionType.Row;
                }
            }
        } else if(isHeaderY && onSquareX) {
            type = SelectionType.Column;
        } else if(onSquareX && onSquareY) {
            type = SelectionType.Color;
        } else {
            // nothing.
            return null;
        }

        Selection sel = new Selection();
        sel.type = type;
        sel.initX = ix;
        sel.initY = iy;

        return sel;
    }

    public PaletteView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public PaletteView(Context context) {
        super(context);
        init();
    }

    private void init() {
        /*
        LDPI - 36 x 36
        MDPI - 48 x 48
        HDPI - 72 x 72
        XHDPI - 96 x 96
        XXHDPI - 144 x 144
        XXXHDPI - 192 x 192.
         */
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        /*switch(metrics.densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                iconSize = 36; break;
            case DisplayMetrics.DENSITY_MEDIUM:
                iconSize = 48; break;
            case DisplayMetrics.DENSITY_HIGH:
                iconSize = 72; break;
            case DisplayMetrics.DENSITY_XHIGH:
                iconSize = 96; break;
            case DisplayMetrics.DENSITY_XXHIGH:
                iconSize = 144; break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                iconSize = 192; break;
            default:
                Log.e("NPV", "Unknown display metrics: " + metrics.densityDpi);
                iconSize = 48; // compromise
        }*/

        Log.d("NPV", metrics.density + " dpi");

        iconSize = (int) (metrics.density * 48);

        padding = iconSize / 3;
        margin = padding / 2;

        framePaint = new Paint();
        framePaint.setStrokeWidth(padding / 12f);
        framePaint.setColor(Color.GRAY);
        framePaint.setStyle(Paint.Style.STROKE);

        fillPaint = new Paint();
        fillPaint.setColor(Color.LTGRAY); // color will be updated
        fillPaint.setStyle(Paint.Style.FILL);

        outsideSelectionPaint = new Paint();
        outsideSelectionPaint.setColor(Color.LTGRAY);
        outsideSelectionPaint.setStyle(Paint.Style.FILL);

        insideSelectionPaint = new Paint();
        insideSelectionPaint.setColor(Color.WHITE);
        insideSelectionPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.DKGRAY);
        borderPaint.setStrokeWidth(padding / 12f);
        borderPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public View view() {
        return this;
    }

    @Override
    public boolean singleTap(MotionEvent evt) {
        Selection s = at(evt.getX(), evt.getY());

        if(s == null) return false;

        switch(s.type) {
            case Color: {
                // color dialog
                EditableDialogFragment ft = EditableDialogFragment.newInstance(
                        s.initX + s.initY * model().width(),
                        "Select Color", false,
                        EditableDialogFragment.Type.Color).setInitVal(model().get(s.initX, s.initY));

                ft.show(((Activity) getContext()).getFragmentManager(), "dialog");
                // it will be set in the activity...
            } return true;
            case Row:
            case Column: {
                // delete/duplicate
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                String[] items;

                if((s.type == SelectionType.Column && model().width() > 1)
                        || (s.type == SelectionType.Row && model().height() > 1)) {
                    items = new String[]{"Duplicate", "Randomize", "Delete"};
                } else {
                    items = new String[]{"Duplicate", "Randomize"};
                }

                builder.setItems(items,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {// duplicate
                                if(s.type == SelectionType.Column) {
                                    model().duplicateColumn(s.initX);
                                } else {
                                    model().duplicateRow(s.initY);
                                }

                                invalidate();
                            }
                            break;
                            case 1: {// randomize
                                if(s.type == SelectionType.Column) {
                                    for(int y = 0; y < model().height(); ++y) {
                                        model().set(s.initX, y, model().randomColor());
                                    }
                                } else {
                                    for(int x = 0; x < model().width(); ++x) {
                                        model().set(x, s.initY, model().randomColor());
                                    }
                                }

                                invalidate();
                            }
                            break;
                            case 2: {// delete
                                if(s.type == SelectionType.Column) {
                                    model().removeColumn(s.initX);
                                } else {
                                    model().removeRow(s.initY);
                                }

                                invalidate();
                            }
                            break;
                            default:
                                throw new IllegalArgumentException("no such selection: " + which);
                        }
                    }
                });
                builder.setCancelable(true);

                builder.show();
            } return true;
            case TopLeft: {
                // context menu to transpose
                // delete/duplicate
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                String[] items;

                if(model().width() > 1 || model().height() > 1) {
                    items = new String[]{"Transpose", "Randomize", "Clear"};
                } else {
                    items = new String[]{"Transpose", "Randomize"};
                }

                builder.setItems(items,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: {// transpose
                                        int w = model().width();
                                        int h = model().height();

                                        ArrayList<Integer> l = new ArrayList<>(model().width() * model().height());

                                        for(int y = 0; y < h; ++y) {
                                            for(int x = 0; x < w; ++x) {
                                                l.add(model().get(x, y));
                                            }
                                        }

                                        Iterator<Integer> it = l.iterator();

                                        model().setHeight(w);
                                        model().setWidth(h);

                                        for(int x = 0; x < h; ++x) {
                                            for(int y = 0; y < w; ++y) {
                                                model().set(x, y, it.next());
                                            }
                                        }

                                        invalidate();
                                    }
                                    break;
                                    case 1: {// randomize
                                        for(int y = 0; y < model().height(); ++y) {
                                            for(int x = 0; x < model().width(); ++x) {
                                                model().set(x, y, model().randomColor());
                                            }
                                        }

                                        invalidate();
                                    }
                                    break;
                                    case 2: {// delete
                                        while(model().width() > 1) {
                                            model().removeColumn(model().width() - 1);
                                        }

                                        while(model().height() > 1) {
                                            model().removeRow(model().height() - 1);
                                        }

                                        invalidate();
                                    }
                                    break;
                                    default:
                                        throw new IllegalArgumentException("no such selection: " + which);
                                }
                            }
                        });
                builder.setCancelable(true);

                builder.show();
            } return true;
        }

        return false;
    }

    @Override
    public boolean doubleTap(MotionEvent evt) {
        return false; // confusion with tab...
    }

    @Override
    public boolean longPress(MotionEvent evt) {
        return startSelect(evt);
    }
    
    Selection sel = null; 

    private boolean startSelect(MotionEvent evt) {
        // what is selected?
        if((sel = at(evt.getX(), evt.getY())) != null) {
            // The selection object may be modified in
            // update-selection!
            
            // these two are for dragging.
            lastPtX = currentPtX = evt.getX() + left;
            lastPtY = currentPtY = evt.getY() + top;

            invalidate();
            return true;
        } else {
            sel = null;
            return false;
        }
    }

    private PaletteViewModel model() {
        return ((PaletteActivity) getContext()).model();
    }


    @Override
    public boolean moveTo(MotionEvent evt) {
        //Log.d("NPV", "dragging something to " + evt);

        if(sel != null) {
            updateSelection(evt.getX() + left, evt.getY() + top);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean tapUp(MotionEvent event) {
        Log.d("NPV", "up");
        if(sel != null) {
            Log.d("NPV", "up and dragging");
            // confirm drag event
            if(sel.type == SelectionType.BoundBoth
                    || sel.type == BoundX
                    || sel.type == SelectionType.BoundY) {
                // changing the dimension in moveTo is not a good idea
                // because it will mess with relative points.
                ((PaletteActivity) getContext()).dimensionChanged();
            } else if(sel.type == SelectionType.Color) {
                Log.d("NPV", "up and dragging and color");
                int x = index(currentPtX);
                int y = index(currentPtY);

                Log.d("NPV", "up and dragging and color " + x + ", " + y);

                // if in range
                if(x >= 0 && y >= 0 && x < model().width() && y < model().height()) {
                    model().set(x, y, model().get(sel.initX, sel.initY));
                }
            }

            sel = null;
            invalidate();
            return true;
        } else {
            return false;
        }
    }

    /**
     * This one is for updating the scrolling.
     * @param left
     * @param top
     */
    public void setLeftTop(int left, int top) {
        // FIXME bad name!
        this.left = left;
        this.top = top;
        invalidate();
    }

    enum ButtonType { Color, Header, Outside };

    private void drawColor(Canvas canvas, int x0, int y0, int color, ButtonType type) {
        if ((x0 < -iconSize || x0 >= getWidth()) || (y0 < -iconSize || y0 >= getHeight())) return;

        if(type == ButtonType.Outside) {
            // draw empty gray
            fillPaint.setColor(Color.LTGRAY);
            framePaint.setColor(Color.LTGRAY);
        } else if(type == ButtonType.Header) {
            // in this case, it is one of the headers.
            fillPaint.setColor(Color.BLACK);
            framePaint.setColor(Color.WHITE);
        } else {
            fillPaint.setColor(color);

            int color2;

            // make the frame lighter or darker.
            if(Colors.brightness(color) > 0.7) {
                // it is bright, thus make it darker
                color2 = (color >> 1) & 0xff7f7f7f;
            } else {
                // just do the same for the inverted color
                color2 = ~((~color >> 1) & 0xff7f7f7f);
            }

            framePaint.setColor(color2);
        }

        canvas.drawRoundRect(x0, y0,
                x0 + iconSize, y0 + iconSize,
                iconSize / 4f, iconSize / 4f, fillPaint);

        canvas.drawRoundRect(x0, y0,
                x0 + iconSize, y0 + iconSize,
                iconSize / 4f, iconSize / 4f, framePaint);
    }


    private void updateSelection(float x0, float y0) {
        currentPtX = x0; currentPtY = y0;

        if(sel.type == SelectionType.Color) {
            invalidate();
            return;
        }
        // If this was a bound selection type, where would width and height be?
        int w = index(currentPtX + padding + iconSize / 2);
        int h = index(currentPtY + padding + iconSize / 2);

        // for the selection of bounds, the start
        // value of the selection is of no importance.
        if(sel.type == BoundX) {
            if(h >= model().height()) {
                // we now also allow dragging in y direction.
                sel.type = SelectionType.BoundBoth;
            } else {
                // update palette
                model().setWidth(w);
                invalidate();
                return;
            }
        } else if(sel.type == SelectionType.BoundY) {
            if(w >= model().width()) {
                // we now also allow dragging in y direction.
                sel.type = SelectionType.BoundBoth;
            } else {
                // update palette
                model().setHeight(h);
                invalidate();
                return;
            }
        }

        if(sel.type == SelectionType.BoundBoth) {
            model().setWidth(w).setHeight(h);
            invalidate();
            return;
        }

        // okay, so bounds are fine. The (possibly new) borders
        // can be determined by checking the size of the model.

        // now, if a column or a row or all is selected, the
        // row/column is always determined by currentPt{X,Y}.

        // where are we now?
        int sx = index(lastPtX), sy = index(lastPtY);
        int tx = index(currentPtX), ty = index(currentPtY);

        // get distance travelled
        int dx = tx - sx;
        int dy = ty - sy;

        if(dx == 0 && dy == 0) return; // nothing to do.

        // clamp sx/tx/sy/ty
        sx = Commons.clamp(sx, 0, model().width() - 1);
        sy = Commons.clamp(sy, 0, model().height() - 1);
        tx = Commons.clamp(tx, 0, model().width() - 1);
        ty = Commons.clamp(ty, 0, model().height() - 1);

        if(sel.type == SelectionType.TopLeft) {
            // rotate all
            model().rotateAll(dx, dy);
        } else if(sel.type == SelectionType.Column) {
            // first rotate old one
            while(dy > 0) {
                model().rotateDown(sx);
                dy--;
            }

            while(dy < 0) {
                model().rotateUp(sx);
                dy++;
            }

            // in drawing, this will require a modulo operation.

            // now, move to new position.
            for(int x = sx; x < tx; ++x) {
                model().moveRight(x);
            }

            for(int x = sx; x > tx; --x) {
                model().moveLeft(x);
            }
        } else if(sel.type == SelectionType.Row) {
            while(dx > 0) {
                model().rotateRight(sy);
                dx--;
            }

            while(dx < 0) {
                model().rotateLeft(sy);
                dx++;
            }

            // now, move to new position.

            for(int y = sy; y < ty; ++y) {
                model().moveDown(y);
            }

            for(int y = sy; y > ty; --y) {
                model().moveUp(y);
            }
        }

        // update selection points
        lastPtX = currentPtX;
        lastPtY = currentPtY;

        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Look:
        // + Frame around header with transparent gray background.
        // + Header should be a white empty frame.
        // + Once selected, its line style changes.
        // + If a row/column is selected, a frame is drawn around it
        //   that indicates where the original 0 is.


        if(sel != null) {
            // feedback on the action:
            // first if the bounds are dragged

            if (sel.type == SelectionType.BoundX
                    || sel.type == SelectionType.BoundY
                    || sel.type == SelectionType.BoundBoth) {
                float lt = margin + iconSize + padding / 2f; // left top

                // right bottom (should at least contain one square)
                float bx = Math.max(currentPtX, lt + iconSize + padding);
                float by = Math.max(currentPtY, lt + iconSize + padding);

                if (sel.type == SelectionType.BoundY) {
                    // if I only drag y, the x is fixed
                    bx = pix(model().width());
                } else if (sel.type == SelectionType.BoundX) {
                    // and same thing the other way round.
                    by = pix(model().height());
                }

                // drawing is as follows: two rectangles separated by padding/2
                // fill area depending on what is selected
                float addX = sel.type != BoundY ? padding / 4f : -padding / 4f;
                float addY = sel.type != BoundX ? padding / 4f : -padding / 4f;

                // Two filled
                canvas.drawRoundRect(
                        lt - left, lt - top,
                        pix(model().width()) - left + addX,
                        pix(model().height()) - top + addY,
                        padding, padding, outsideSelectionPaint);

                canvas.drawRoundRect(
                        lt - left, lt - top,
                        bx - left - padding / 4f, by - top - padding / 4f,
                        padding, padding, insideSelectionPaint);

                canvas.drawRoundRect(
                        lt - left, lt - top,
                        pix(model().width()) - left + addX,
                        pix(model().height()) - top + addY,
                        padding, padding, borderPaint);

                canvas.drawRoundRect(
                        lt - left, lt - top,
                        bx - left - padding / 4f, by - top - padding / 4f,
                        padding, padding, borderPaint);

            } else if (sel.type == SelectionType.Color) {
                // in case of a color, we gray out everything
                canvas.drawRoundRect(
                        margin - padding / 2f, margin - padding / 2f,
                        pix(model().width()) - left, pix(model().height()) - top,
                        padding, padding, outsideSelectionPaint);

                canvas.drawRoundRect(
                        margin - padding / 2f, margin - padding / 2f,
                        pix(model().width()) - left, pix(model().height()) - top,
                        padding, padding, borderPaint);

                int ix = index(currentPtX);
                int iy = index(currentPtY);

                if(ix >= 0 && iy >= 0 && ix < model().width() && iy < model().height()) {
                    float x0 = pix(ix) + padding / 4f - left;
                    float y0 = pix(iy) + padding / 4f - top;

                    canvas.drawRoundRect(
                            x0, y0, x0 + iconSize + padding / 2f, y0 + iconSize + padding / 2f,
                            padding, padding, insideSelectionPaint);

                    canvas.drawRoundRect(
                            x0, y0, x0 + iconSize + padding / 2f, y0 + iconSize + padding / 2f,
                            padding, padding, borderPaint);
                }
            } else {
                // we draw a selection.
                //
                // For a proper one we need to know the coordinates one
                // square below/right of the initial selection.
                // this was stored in sel.x/sel.y.
                //
                // thus, get the current tile
                int ix = index(currentPtX);
                int iy = index(currentPtY);

                // and calculate where the first tile is. This is
                // needed for rotations.
                int square1X = Commons.mod(ix - sel.initX, model().width());
                int square1Y = Commons.mod(iy - sel.initY, model().height());

                // now, draw selection.
                // we can clamp the tiles now.
                ix = Commons.clamp(ix, 0, model().width() - 1);
                iy = Commons.clamp(iy, 0, model().height() - 1);

                // First the corners of the whole area
                float x0 = margin + iconSize + padding / 2f;
                //noinspection SuspiciousNameCombination
                float y0 = x0;
                float x1 = x0 + (iconSize + padding) * model().width();
                float y1 = y0 + (iconSize + padding) * model().height();

                // mx/my is in the middle of the top left corner of the
                // former square 1.
                float mx = pix(square1X);
                float my = pix(square1Y);

                if (sel.type == SelectionType.Column) {
                    //  change x0/x1. mx wont be used.
                    x0 = pix(ix);
                    x1 = x0 + iconSize + padding;

                    // one big rectangle around the whole column
                    canvas.drawRoundRect(
                            x0 - left - padding / 4f, margin - padding / 2f - top - padding / 4f,
                            x1 - left + padding / 4f, y1 - top + padding / 4f,
                            padding, padding, outsideSelectionPaint);

                    canvas.drawRoundRect(
                            x0 - left - padding / 4f, margin - padding / 2f - top - padding / 4f,
                            x1 - left + padding / 4f, y1 - top + padding / 4f,
                            padding, padding, borderPaint);

                    // two rectangles on top of each other
                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, y0 - top + padding / 4f,
                            x1 - left - padding / 4f, my - top - padding / 4f,
                            padding, padding, insideSelectionPaint);

                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, my - top + padding / 4f,
                            x1 - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, insideSelectionPaint);

                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, y0 - top + padding / 4f,
                            x1 - left - padding / 4f, my - top - padding / 4f,
                            padding, padding, borderPaint);

                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, my - top + padding / 4f,
                            x1 - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, borderPaint);
                } else if (sel.type == SelectionType.Row) {
                    //  change y0/y1. my wont be used.
                    y0 = pix(iy);
                    y1 = y0 + iconSize + padding;

                    // one big rectangle around the whole column
                    canvas.drawRoundRect(
                            margin - padding / 2f - left - padding / 4f, y0 - top - padding / 4f,
                            x1 - left + padding / 4f, y1 - top + padding / 4f,
                            padding, padding, outsideSelectionPaint);

                    // two rectangles on top of each other
                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, y0 - top + padding / 4f,
                            mx - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, insideSelectionPaint);

                    canvas.drawRoundRect(
                            mx - left + padding / 4f, y0 - top + padding / 4f,
                            x1 - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, insideSelectionPaint);


                    canvas.drawRoundRect(
                            margin - padding / 2f - left - padding / 4f, y0 - top - padding / 4f,
                            x1 - left + padding / 4f, y1 - top + padding / 4f,
                            padding, padding, borderPaint);

                    // two rectangles on top of each other
                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, y0 - top + padding / 4f,
                            mx - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, borderPaint);

                    canvas.drawRoundRect(
                            mx - left + padding / 4f, y0 - top + padding / 4f,
                            x1 - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, borderPaint);
                } else {
                    // 5 rectangles: one around it
                    canvas.drawRoundRect(
                            margin - padding / 2f, margin - padding / 2f,
                            x1 - left + padding / 4f, y1 - top + padding / 4f,
                            padding, padding, outsideSelectionPaint);

                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, y0 - top + padding / 4f,
                            mx - left - padding / 4f, my - top - padding / 4f,
                            padding, padding, insideSelectionPaint);

                    canvas.drawRoundRect(
                            mx - left + padding / 4f, y0 - top + padding / 4f,
                            x1 - left - padding / 4f, my - top - padding / 4f,
                            padding, padding, insideSelectionPaint);

                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, my - top + padding / 4f,
                            mx - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, insideSelectionPaint);

                    canvas.drawRoundRect(
                            mx - left + padding / 4f, my - top + padding / 4f,
                            x1 - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, insideSelectionPaint);

                    canvas.drawRoundRect(
                            margin - padding / 2f, margin - padding / 2f,
                            x1 - left + padding / 4f, y1 - top + padding / 4f,
                            padding, padding, borderPaint);

                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, y0 - top + padding / 4f,
                            mx - left - padding / 4f, my - top - padding / 4f,
                            padding, padding, borderPaint);

                    canvas.drawRoundRect(
                            mx - left + padding / 4f, y0 - top + padding / 4f,
                            x1 - left - padding / 4f, my - top - padding / 4f,
                            padding, padding, borderPaint);

                    canvas.drawRoundRect(
                            x0 - left + padding / 4f, my - top + padding / 4f,
                            mx - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, borderPaint);

                    canvas.drawRoundRect(
                            mx - left + padding / 4f, my - top + padding / 4f,
                            x1 - left - padding / 4f, y1 - top - padding / 4f,
                            padding, padding, borderPaint);
                }
            }
        } else { // sel is null
            // draw two normal frames to show where dragging can happen.
            canvas.drawRoundRect(
                    margin + iconSize + padding / 2f - left,
                    margin + iconSize + padding / 2f - top,
                    pix(model().width()) - left - padding / 4f,
                    pix(model().height()) - top - padding / 4f,
                    padding, padding, borderPaint);

            canvas.drawRoundRect(
                    margin + iconSize + padding / 2f - left,
                    margin + iconSize + padding / 2f - top,
                    pix(model().width()) - left + padding / 4f,
                    pix(model().height()) - top + padding / 4f,
                    padding, padding, borderPaint);
        }


        //canvas.drawRoundRect(l, t, r, b, padding, padding, boundPaint);

        // Next, draw colors (skip dragged ones)
        for(int y = model().height(); y >= -1; --y) {
            int y0 = y * (padding + iconSize) + margin - top + iconSize + padding;

            for(int x = model().width(); x >= -1; --x) {
                int x0 = x * (padding + iconSize) + margin - left + iconSize + padding;

                ButtonType type = ButtonType.Color;
                if(x == -1 || y == -1) type = ButtonType.Header;
                if(x == model().width() || y == model().height()) {
                    if(type == ButtonType.Header) continue; // do not draw.
                    else type = ButtonType.Outside;
                }

                drawColor(canvas,
                        x == -1 ? margin : x0,
                        y == -1 ? margin : y0,
                        type == ButtonType.Color ? model().get(x, y) : 0,
                        type);
            }
        }

        if(sel != null && sel.type == SelectionType.Color) {
            // draw the dragged color
            drawColor(canvas,
                    (int) (currentPtX - iconSize / 2f - padding / 2f - left),
                    (int) (currentPtY - iconSize / 2f - padding / 2f - top),
                    model().get(sel.initX, sel.initY),
                    ButtonType.Color);
        }
    }
}