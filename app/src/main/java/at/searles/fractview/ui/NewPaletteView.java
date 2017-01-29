package at.searles.fractview.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import at.searles.math.color.Colors;

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

    int margin = 40; // space at borders
    int padding = 60; // space between items
    int iconSize = 160; // size of icons.

    // For testing something really large
    NewPaletteViewModel viewModel;

    Paint framePaint = null;
    Paint fillPaint = null;

    Paint boundPaint = null;
    Paint fillBoundPaint = null;
    Paint selectedBoundPaint = null;

    int left = 0, top = 0;

    /**
     * the following variable indicates if something is dragged currently in here.
     */
    private boolean dragging;

    private float relX0, relY0,
            relX1, relY1; // current drag in relative coordinates

    // four integers for some selection modes
    private int selX0, selY0,
            selX1, selY1;

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

    public int getIntendedWidth() {
        // two extra icons.
        if(viewModel == null) return 0;
        return 2 * margin + (viewModel.width() + 1) * (padding + iconSize) + iconSize;
    }

    public int getIntendedHeight() {
        if(viewModel == null) return 0;
        return 2 * margin + (viewModel.height() + 1) * (padding + iconSize) + iconSize;
    }

    @Override
    public View view() {
        return this;
    }

    @Override
    public boolean singleTap(MotionEvent evt) {
        Log.d("NPV", "single tab at " + evt);

        // If color,

        // FIXME don't set dragging to true if a color has been selected.

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

    private boolean startSelect(MotionEvent evt) {
        // what is selected?
        if(selectAt(evt.getX(), evt.getY()) != Selected.None) {
            Log.d("NPV", "selected is " + selectedMode);

            dragging = true;

            relX0 = relX1 = evt.getX() + left;
            relY0 = relY1 = evt.getY() + top;

            invalidate();

            return true;
        } else {
            return false;
        }
    }

    // Now comes the dragging...
    enum Selected { None, Color, Column, Row, All, Width, Height, Dim };

    Selected selectedMode = Selected.None;
    int selectedColor; // If a color was selected we need to store it.

    /**
     * Finds something at these coordinates. They are NOT relative!
     * @param x0 screen coordinates, eg from a MotionEvent
     * @param y0
     */
    private Selected selectAt(float x0, float y0) {
        boolean isRowHeader = x0 >= margin && x0 <= iconSize + margin;
        boolean isColHeader = y0 >= margin && y0 <= iconSize + margin;

        // make relative
        x0 = (x0 + left - margin) / (iconSize + padding) - 1;
        y0 = (y0 + top - margin) / (iconSize + padding) - 1;

        int x = (int) Math.floor(x0);
        int y = (int) Math.floor(y0);

        // store the stored indices
        selX0 = selX1 = x;
        selY0 = selY1 = y;

        boolean onSquareX = x < viewModel.width()
                && (x0 - Math.floor(x0)) * (iconSize + padding) <= iconSize;

        boolean onSquareY = y < viewModel.height()
                && (y0 - Math.floor(y0)) * (iconSize + padding) <= iconSize;

        boolean onBoundsX = x == viewModel.width() - 1 && !onSquareX;

        boolean onBoundsY = y == viewModel.height() - 1 && !onSquareY;

        isColHeader &= onSquareX;
        isRowHeader &= onSquareY;

        if(isColHeader) {
            if(isRowHeader) {
                // it is the left upper corner
                Log.d("NPV", "left up");
                return selectedMode = Selected.All;
            } else {
                Log.d("NPV", "column " + x);
                return selectedMode = Selected.Column;
            }
        } else if(isRowHeader) {
            Log.d("NPV", "row " + y);
            return selectedMode = Selected.Row;
        } else if(onSquareX && onSquareY) {
            Log.d("NPV", "color " + x + ", " + y);
            return selectedMode = Selected.Color;
        } else if(onBoundsX) {
            if(onBoundsY) {
                Log.d("NPV", "dim");
                return selectedMode = Selected.Dim;
            } else {
                Log.d("NPV", "width");
                return selectedMode = Selected.Width;
            }
        } else if(onBoundsY) {
            Log.d("NPV", "height");
            return selectedMode = Selected.Height;
        } else {
            Log.d("NPV", "nothing");
            return selectedMode = Selected.None;
        }
    }

    private DimensionListener l;

    @Override
    public void setDimensionListener(DimensionListener l) {
        this.l = l;
    }

    private void updateSelection() {
        // Update selection after relX1/Y1 was set.
        // This is necessary when I change the bounds because
        // the view will resize
        int w = Math.round((relX1 - margin - iconSize / 2f) / (iconSize + padding));
        int h = Math.round((relY1 - margin - iconSize / 2f) / (iconSize + padding));

        if(selectedMode == Selected.Dim) {
            // these procedures do nothing if the size didn't change.
            viewModel.setWidth(w).setHeight(h);
        } else if(selectedMode == Selected.Width) {
            viewModel.setWidth(w);
        } else if(selectedMode == Selected.Height) {
            viewModel.setHeight(h);
        } else if(selectedMode != Selected.Color && selectedMode != Selected.None) {
            // translate selection coordinates
            float r0 = index(relY0),
                    c0 = index(relX0),
                    r1 = index(relY1),
                    c1 = index(relX1);

            int dr = (int) (r1 - r0);
            int dc = (int) (c1 - c0);

            if(dr != 0 || dc != 0) {
                // something changed...
                selX1 = Math.max(0, Math.min(viewModel.width() - 1, (int) c1));
                selY1 = Math.max(0, Math.min(viewModel.height() - 1, (int) r1));

                relX0 = relX1;
                relY0 = relY1;

                if (selectedMode == Selected.All) {
                    // shift colors
                    for(int x = 0; x < viewModel.width(); ++x) {
                        // rotate all columns
                        for(int i = 0; i < dr; ++i) {
                            viewModel.rotateDown(x);
                        }

                        for(int i = 0; i > dr; --i) {
                            viewModel.rotateUp(x);
                        }
                    }

                    for(int y = 0; y < viewModel.height(); ++y) {
                        for(int i = 0; i < dc; ++i) {
                            viewModel.rotateRight(y);
                        }

                        for(int i = 0; i > dc; --i) {
                            viewModel.rotateLeft(y);
                        }
                    }
                } else if (selectedMode == Selected.Row) {
                    Log.d("NPV", "Row " + dc + " | " + r0 + " -> " + r1);
                    while(dc > 0) {
                        viewModel.rotateRight((int) r0);
                        dc--;
                    }

                    while(dc < 0) {
                        viewModel.rotateLeft((int) r0);
                        dc++;
                    }

                    // rotate (try both directions)
                    for(int r = (int) r0; r < (int) r1; ++r) {
                        viewModel.moveDown(r);
                    }

                    for(int r = (int) r0; r > (int) r1; --r) {
                        viewModel.moveUp(r);
                    }
                } else if (selectedMode == Selected.Column) {
                    Log.d("NPV", "Column " + dr + " | " + c0 + " -> " + c1);

                    while(dr > 0) {
                        viewModel.rotateDown((int) c0);
                        dr--;
                    }

                    while(dr < 0) {
                        viewModel.rotateUp((int) c0);
                        dr++;
                    }

                    // rotate (try both directions)
                    for(int c = (int) c0; c < (int) c1; ++c) {
                        viewModel.moveRight(c);
                    }

                    for(int c = (int) c0; c > (int) c1; --c) {
                        viewModel.moveLeft(c);
                    }
                }
            }

            invalidate();
        }
    }

    @Override
    public boolean moveTo(MotionEvent evt) {
        //Log.d("NPV", "dragging something to " + evt);

        if(dragging) {
            relX1 = evt.getX() + left;
            relY1 = evt.getY() + top;
            updateSelection();
            invalidate();
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
            if(selectedMode == Selected.Dim || selectedMode == Selected.Width || selectedMode == Selected.Height) {
                // changing the dimension in moveTo is not a good idea
                // because it will mess with relative points.
                l.onDimensionChanged(this);
            }

            dragging = false;
            invalidate();
            return true;
        } else {
            return false;
        }
    }

    public void setLeftTop(int left, int top) {
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

    /**
     * Converts relative coordinate to an (col or row) index (there is a reason
     * why things are squares.
     * @param f the relative coordinate
     * @return The value index + (padding / 2 + iconSize + padding / 2) / (iconSize + padding)
     */
    float index(float f) {
        // returns -1 if it is the header.
        return (f - margin + padding / 2) / (iconSize + padding) - 1;
    }

    /**
     * Reverse of index function.
     * @param index
     * @return
     */
    float pix(float index) {
        return margin + (index + 1) * (iconSize + padding) - padding / 2;
    }

    /**
     * Same as modulo but always positive!
     * @param
     * @param
     * @return
     */
    /*static int mod(int a, int b) {
        a = a % b;
        if(a < 0) a = a + b;
        return a;
    }

    void offset(int x, int y, int[] pt) {
        // x == -1 means that it is header.
        if(x != -1 && dragging) {
            // from - to where it is selected.
            float x0 = index(relX0);
            float y0 = index(relY0);
            float x1 = index(relX1);
            float y1 = index(relY1);

            if(selectedMode == Selected.Row || selectedMode == Selected.All) {
                // we have to add an offset to x in this case.

                int offset = ((int) (x1 - x0)) % viewModel.width();
                if(offset < 0) offset += viewModel.width();

                return offset;
            } else if(selectedMode == Selected.Column) {
                // selectedX is the index of the selected column
                int insert = (int) (relX1 - margin + padding / 2) / (iconSize + padding);

                // clamp
                insert = Math.max(Math.min(insert, viewModel.width() - 1), 0);

                if(x < insert && x < selectedX || x > insert && x > selectedX) {
                    return x;
                } else if(x == insert) {
                    return selectedX;
                } else if(selectedX <= x && x < insert) {
                    return x + 1;
                } else if(insert < x && x <= selectedX) {
                    return x - 1;
                } else {
                    throw new AssertionError("there should be no case missing");
                }
            } else if(selectedMode == Selected.Color) {

            }
        } else {
            return x;
        }
        // returns by how many icons the selction positions are moved.
        // always returns a positive value.
        if(!dragging || (selectedMode != Selected.Row && selectedMode != Selected.All)) {
            // if no appropriate selection, it is 0.
            return x;
        } else if(x == -1) {
            return -1;
        } else {
            int offset = x + (int) (relX0 - relX1) / (iconSize + padding);

            if (offset < 0) {
                return (viewModel.width() - (-offset % viewModel.width())) % viewModel.width();
            } else {
                return offset;
            }
        }
    }

    int offsetY(int y) {
        // returns by how many icons the selction positions are moved.
        // always returns a positive value.
        if(!dragging || (selectedMode != Selected.Column && selectedMode != Selected.All)) {
            // if no appropriate selection, it is 0.
            return y;
        } else if(y == -1) {
            return -1;
        } else {
            int offset = y + (int) (relY0 - relY1) / (iconSize + padding);

            if (offset < 0) {
                return (viewModel.height() - (-offset % viewModel.height())) % viewModel.height();
            } else {
                return offset;
            }
        }
    }*/

    @Override
    public void onDraw(Canvas canvas) {
        // First, draw a frame around the elements.
        int l = margin + iconSize + padding / 2 - left,
                t = margin + iconSize + padding / 2 - top;
        int r = l + viewModel.width() * (iconSize + padding),
                b = t + viewModel.height() * (iconSize + padding);

        if(dragging) {
            // a line for dragging was selected
            switch(selectedMode) {
                case Dim: {
                    canvas.drawLine(l + padding / 2, b, r - padding / 2, b, selectedBoundPaint);
                    canvas.drawLine(r, t + padding / 2, r, b - padding / 2, selectedBoundPaint);
                }
                break;
                case Width: {
                    canvas.drawLine(r, t + padding / 2, r, b - padding / 2, selectedBoundPaint);
                }
                break;
                case Height: {
                    canvas.drawLine(l + padding / 2, b, r - padding / 2, b, selectedBoundPaint);
                }
                break;
                case All: {

                }
                break;
                case Row: {
                    Log.d("NPV", "selected row is " + selY1);
                    // reverse of index function
                    float y0 = pix(selY1) - top;
                    float y1 = y0 + iconSize + padding;

                    // bounds of row can move
                    float x0 = margin - left;
                    float x1 = margin + (iconSize + padding) * (viewModel.width() + 1) - padding / 2;

                    canvas.drawRoundRect(x0, y0, x1, y1, padding, padding, selectedBoundPaint);
                }
                break;
                case Column: {
                    // reverse of index function
                    float x0 = pix(selX1) - left;
                    float x1 = x0 + iconSize + padding;

                    float y0 = margin - top;
                    float y1 = margin + (iconSize + padding) * (viewModel.height() + 1) - padding / 2;

                    float mid = pix(selX1) - pix(selX1);

                    canvas.drawRoundRect(x0, y0, x1, mid, padding, padding, selectedBoundPaint);
                    canvas.drawRoundRect(x0, mid, x1, y1, padding, padding, selectedBoundPaint);
                }
                break;
                case Color: {
                    // not used yet.
                }
                break;
            }
        } else {
            canvas.drawRoundRect(l, t, r, b, padding, padding, boundPaint);
        }

        //canvas.drawRoundRect(l, t, r, b, padding, padding, boundPaint);

        // Next, draw colors (skip dragged ones)
        for(int y = viewModel.height(); y >= -1; --y) {
            int y0 = y * (padding + iconSize) + margin - top + iconSize + padding;

            for(int x = viewModel.width(); x >= -1; --x) {
                int x0 = x * (padding + iconSize) + margin - left + iconSize + padding;

                ButtonType type = ButtonType.Color;
                if(x == -1 || y == -1) type = ButtonType.Header;
                if(x == viewModel.width() || y == viewModel.height()) {
                    if(type == ButtonType.Header) continue; // do not draw.
                    else type = ButtonType.Outside;
                }

                drawColor(canvas,
                        x == -1 ? margin : x0,
                        y == -1 ? margin : y0,
                        type == ButtonType.Color ? viewModel.get(x, y) : 0,
                        type);
            }
        }

        // Draw a color if it is dragged.
        if(dragging && selectedMode == Selected.Color) {
            // draw the dragged color
            drawColor(canvas,
                    (int) (relX1 - left + padding / 2f + iconSize / 2f),
                    (int) (relY1 - top  + padding / 2f + iconSize / 2f),
                    selectedColor, ButtonType.Color);
        }
    }

    public void setModel(NewPaletteViewModel model) {
        this.viewModel = model;
        invalidate();
    }

}