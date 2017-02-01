package at.searles.fractview.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import at.searles.fractview.PaletteActivity;
import at.searles.fractview.editors.EditableDialogFragment;
import at.searles.math.Commons;
import at.searles.math.color.Colors;

import static at.searles.fractview.ui.NewPaletteView.SelectionType.BoundX;

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
public class NewPaletteView extends View implements MultiScrollView.InternalView {

    // some arguments
    int margin = 40; // space at borders
    int padding = 60; // space between items
    int iconSize = 160; // size of icons.

    Paint framePaint = null;
    Paint fillPaint = null;

    Paint boundPaint = null;
    Paint fillBoundPaint = null;
    Paint selectedBoundPaint = null;

    // done with constants.
    
    /**
     * For scrolling, shift in coordinates.
     */
    int left = 0, top = 0;

    /**
     * the following variable indicates if something is dragged currently in here.
     */
    private boolean dragging;

    /**
     * When dragging there are three points of importance.
     * lastPt is the last point of an event, currentPt is
     * the current one.
     */
    private float lastPtX, lastPtY;
    private float currentPtX, currentPtY;

    enum SelectionType {
        Column, Row, TopLeft, Color, BoundX, BoundY, BoundBoth
    }

    private class Selection {
        SelectionType type;

        //float rx; // relative x coordinate
        //float ry; // relative y coordinate

        int x; // coordinates in terms of columns/rows
        int y; // [not used if BoundBoth]

        public String toString() {
            return type + ": " + x + ", " + y;
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
    float index(float f) {
        // returns -1 if it is the header.
        return (f - margin) / (iconSize + padding) - 1;
    }

    /**
     * Inverse of index function.
     * @param index
     * @return
     */
    float pix(float index) {
        return margin + (index + 1) * (iconSize + padding);
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
     * What is selected when something is selected at x0/y0?
     * x0 and y0 are not relative.
     * @param x0
     * @param y0
     * @return
     */
    private Selection at(float x0, float y0) {
        // make relative, and so on...
        float rx = index(x0 + left), ry = index(y0 + top);
        float rx0 = index(x0), ry0 = index(y0);
        int ix = (int) Math.floor(rx), iy = (int) Math.floor(ry);
        int ix0 = (int) Math.floor(rx0), iy0 = (int) Math.floor(ry0);

        // if rx?-ix?/ry?-iy? is larger than padding ratio it is on the border.
        float paddingRatio = ((float) iconSize) / (float) (padding + iconSize);

        // where is the selection?
        boolean onBoundsX = ix == model().width() - 1 && (rx - ix) >= paddingRatio;
        boolean onBoundsY = iy == model().height() - 1 && (ry - iy) >= paddingRatio;

        if(onBoundsX || onBoundsY) {
            Selection sel = new Selection();
            //sel.rx = rx; sel.ry = ry;

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
        boolean onSquareX = ix < model().width() && (rx - ix) < paddingRatio;
        boolean onSquareY = iy < model().height() && (ry - iy) < paddingRatio;

        // to check whether it is on a header, we use the original coordinates.
        boolean isHeaderX = ix0 == -1 && (rx0 - ix0) < paddingRatio;
        boolean isHeaderY = iy0 == -1 && (ry0 - iy0) < paddingRatio;

        // header first.
        if(isHeaderX) {
            if(isHeaderY || onSquareY) {
                Selection sel = new Selection();

                // information which square was selected.
                sel.x = ix; sel.y = iy;

                if(isHeaderY) {
                    Log.d("NPV", "top left");
                    sel.type = SelectionType.TopLeft;
                } else {
                    Log.d("NPV", "row " + iy);
                    sel.type = SelectionType.Row;
                }

                return sel;
            }
        } else if(isHeaderY && onSquareX) {
            Selection sel = new Selection();

            sel.x = ix; sel.y = iy; // which one?

            Log.d("NPV", "col " + ix);
            sel.type = SelectionType.Column;

            sel.x = ix; // which column?
            sel.y = iy; // info for rotation

            return sel;
        } else if(onSquareX && onSquareY) {
            Selection sel = new Selection();

            sel.x = ix; sel.y = iy; // which one?

            Log.d("NPV", "square " + ix + ", " + iy);
            sel.type = SelectionType.Color;

            return sel;
        }

        return null; // nothing.
    }

    public NewPaletteView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public NewPaletteView(Context context) {
        super(context);
        init();
    }

    private void init() {
        framePaint = new Paint();
        framePaint.setStrokeWidth(iconSize / 20.f);
        framePaint.setStyle(Paint.Style.STROKE);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);

        boundPaint = new Paint();
        boundPaint.setStrokeWidth(padding / 4f);
        boundPaint.setColor(Color.GRAY);
        boundPaint.setStyle(Paint.Style.STROKE);

        fillBoundPaint = new Paint();
        fillBoundPaint.setColor(Color.LTGRAY);
        fillBoundPaint.setStyle(Paint.Style.FILL);

        selectedBoundPaint = new Paint();
        selectedBoundPaint.setStrokeWidth(padding / 2f);
        selectedBoundPaint.setColor(Color.BLUE);
        selectedBoundPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public View view() {
        return this;
    }

    @Override
    public boolean singleTap(MotionEvent evt) {
        Selection s = at(evt.getX(), evt.getY());

        if(s == null) return false;

        Log.d("NPV", "single tab at " + evt + " = " + s.type);

        switch(s.type) {
            case Color: {
                // color dialog
                EditableDialogFragment ft = EditableDialogFragment.newInstance(
                        s.x + s.y * model().width(),
                        "Select Color", false,
                        EditableDialogFragment.Type.Color).setInitVal(model().get(s.x, s.y));

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
                    items = new String[]{"Duplicate", "Delete"};
                } else {
                    items = new String[]{"Duplicate"};
                }

                builder.setItems(items,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {// duplicate
                                if(s.type == SelectionType.Column) {
                                    model().duplicateColumn(s.x);
                                } else {
                                    model().duplicateRow(s.y);
                                }

                                invalidate();
                            }
                            break;
                            case 1: {// insert after
                                if(s.type == SelectionType.Column) {
                                    model().removeColumn(s.x);
                                } else {
                                    model().removeRow(s.y);
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
            } return true;
        }

        return false;
    }

    @Override
    public boolean doubleTap(MotionEvent evt) {
        Log.d("NPV", "double tab at " + evt);
        return startSelect(evt);
    }

    @Override
    public boolean longPress(MotionEvent evt) {
        Log.d("NPV", "long press at " + evt);
        return startSelect(evt);
    }
    
    Selection sel = null; 

    private boolean startSelect(MotionEvent evt) {
        // what is selected?
        if((sel = at(evt.getX(), evt.getY())) != null) {
            // The selection object may be modified in
            // update-selection!
            
            dragging = true;
            
            // these two are for dragging.
            lastPtX = currentPtX = evt.getX() + left;
            lastPtY = currentPtY = evt.getY() + top;

            Log.d("NPV", "selected is " + sel);
            invalidate();
            return true;
        } else {
            return false;
        }
    }

    private NewPaletteViewModel model() {
        return ((PaletteActivity) getContext()).model();
    }

    private void updateSelection() {
        // If this was a bound selection type, where would width and height be?
        int w = (int) index(currentPtX + padding + iconSize / 2);
        int h = (int) index(currentPtY + padding + iconSize / 2);

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
        int sx = (int) Math.floor(index(lastPtX)), sy = (int) Math.floor(index(lastPtY));
        int tx = (int) Math.floor(index(currentPtX)), ty = (int) Math.floor(index(currentPtY));

        Log.d("NPV", "last = " + lastPtX + ", " + lastPtY);
        Log.d("NPV", "sx, sy, tx, ty = " + sx + ", " + sy + ", " + tx + ", " + ty);

        sx = Commons.clamp(sx, 0, model().width() - 1);
        tx = Commons.clamp(tx, 0, model().width() - 1);
        sy = Commons.clamp(sy, 0, model().height() - 1);
        ty = Commons.clamp(ty, 0, model().height() - 1);

        // get distance travelled
        int dx = tx - sx;
        int dy = ty - sy;

        if(dx == 0 && dy == 0) return; // nothing to do.

        if(sel.type == SelectionType.TopLeft) {
            // rotate all
            model().rotateAll(dx, dy);
        } else if(sel.type == SelectionType.Column) {
            Log.d("NPV", "Column " + dx + " | " + sx + "->" + tx);

            // first rotate old one
            while(dy > 0) {
                model().rotateDown(sx);
                dy--;
            }

            while(dy < 0) {
                model().rotateUp(sx);
                dy++;
            }

            // rotate (try both directions)
            for(int x = sx; x < tx; ++x) {
                model().moveRight(x);
            }

            for(int x = sx; x > tx; --x) {
                model().moveLeft(x);
            }
        } else if(sel.type == SelectionType.Row) {
            Log.d("NPV", "Row " + dx + " | " + sy + " -> " + ty);
            while(dx > 0) {
                model().rotateRight(sy);
                dx--;
            }

            while(dx < 0) {
                model().rotateLeft(sy);
                dx++;
            }

            // rotate (try both directions)
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
    public boolean moveTo(MotionEvent evt) {
        //Log.d("NPV", "dragging something to " + evt);

        if(dragging) {
            currentPtX = evt.getX() + left;
            currentPtY = evt.getY() + top;

            updateSelection();

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean tapUp(MotionEvent event) {
        Log.d("NPV", "tap is up" + event);
        if(dragging) {
            // confirm drag event
            if(sel.type == SelectionType.BoundBoth
                    || sel.type == BoundX
                    || sel.type == SelectionType.BoundY) {
                // changing the dimension in moveTo is not a good idea
                // because it will mess with relative points.
                ((PaletteActivity) getContext()).dimensionChanged();
            }

            dragging = false;
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
        //Log.d("NPV", "setLeftTop " + left + " - " + top);
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


    @Override
    public void onDraw(Canvas canvas) {
        Log.d("NPV", "onDraw");
        // Look:
        // + Frame around header with transparent gray background.
        // + Header should be a white empty frame.
        // + Once selected, its line style changes.
        // + If a row/column is selected, a frame is drawn around it
        //   that indicates where the original 0 is.


        if(dragging) {
            // feedback on the action:
            // first if the bounds are dragged

            Log.d("NPV", "some selection is here...");

            if (sel.type == SelectionType.BoundX
                    || sel.type == SelectionType.BoundY
                    || sel.type == SelectionType.BoundBoth) {
                float lt = margin + iconSize + padding / 2f; // left top

                float bx = Math.max(currentPtX, lt + iconSize + padding);
                float by = Math.max(currentPtY, lt + iconSize + padding);

                if (sel.type == SelectionType.BoundY) {
                    // if I only drag y, the x is fixed
                    bx = pix(model().width()) - padding / 2f;
                } else if (sel.type == SelectionType.BoundX) {
                    by = pix(model().height()) - padding / 2f;
                }

                // fill area
                canvas.drawRoundRect(
                        lt - left, lt - top,
                        pix(model().width()) - left - padding / 2f,
                        pix(model().height()) - top - padding / 2f,
                        padding, padding, fillPaint);

                // draw bounds (relative to current movement)
                canvas.drawRoundRect(
                        lt - left, lt - top,
                        bx - left, by - top,
                        padding, padding, selectedBoundPaint);
            } else {
                // we draw a selection.
                //
                // For a proper one we need to know the coordinates one
                // square below/right of the initial selection.
                // this was stored in sel.x/sel.y.
                //
                // thus, get the current tile
                int ix = Commons.clamp((int) Math.floor(index(currentPtX)), 0, model().width() - 1);
                int iy = Commons.clamp((int) Math.floor(index(currentPtY)), 0, model().height() - 1);

                // and calculate where the first tile is
                int square1X = ix - sel.x - 1;
                int square1Y = iy - sel.y - 1;

                // First the corners of
                // the whole area
                float x0 = margin + iconSize + padding / 2f;
                //noinspection SuspiciousNameCombination
                float y0 = x0;
                float x1 = x0 + (iconSize + padding) * model().width();
                float y1 = y0 + (iconSize + padding) * model().height();

                // mx/my is in the middle of the top left corner of the
                // former square 1.
                float mx = pix(square1X) - padding / 2f;
                float my = pix(square1Y) - padding / 2f;

                if(sel.type == SelectionType.Column) {
                    //  change x0/x1. mx wont be used.
                    x0 = pix(ix) - padding / 2;
                    x1 = x0 + iconSize + padding;

                    // two rectangles on top of each other
                    canvas.drawRoundRect(
                            x0 - left, y0 - top,
                            x1 - left, my - top,
                            padding, padding, selectedBoundPaint);

                    canvas.drawRoundRect(
                            x0 - left, my - top,
                            x1 - left, y1 - top,
                            padding, padding, selectedBoundPaint);
                } else if(sel.type == SelectionType.Row) {
                    y0 = pix(iy) - padding / 2;
                    y1 = y0 + iconSize + padding;

                    // two rectangles next to each other
                    canvas.drawRoundRect(
                            x0 - left, y0 - top,
                            mx - left, y1 - top,
                            padding, padding, selectedBoundPaint);

                    canvas.drawRoundRect(
                            mx - left, y0 - top,
                            x1 - left, y1 - top,
                            padding, padding, selectedBoundPaint);
                } else {
                    // four rectangles
                    canvas.drawRoundRect(
                            x0 - left, y0 - top,
                            mx - left, my - top,
                            padding, padding, selectedBoundPaint);

                    canvas.drawRoundRect(
                            mx - left, y0 - top,
                            x1 - left, my - top,
                            padding, padding, selectedBoundPaint);

                    canvas.drawRoundRect(
                            x0 - left, my - top,
                            mx - left, y1 - top,
                            padding, padding, selectedBoundPaint);

                    canvas.drawRoundRect(
                            mx - left, my - top,
                            x1 - left, y1 - top,
                            padding, padding, selectedBoundPaint);
                }
            }
        } else {
            // draw a normal frame
            canvas.drawRoundRect(
                    margin + iconSize + padding / 2f - left,
                    margin + iconSize + padding / 2f - top,
                    pix(model().width()) - left - padding / 2f,
                    pix(model().height()) - top - padding / 2f,
                    padding, padding, fillPaint);
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
    }
}