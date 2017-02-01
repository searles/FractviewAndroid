package at.searles.fractview.ui;

import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import at.searles.math.color.Palette;

/**
 * Model for NewPaletteView
 */
public class NewPaletteViewModel {
    private int w;
    private int h;

    private ArrayList<ArrayList<Integer>> table;

    public NewPaletteViewModel(Palette p) {
        this.w = p.width();
        this.h = p.height();

        table = new ArrayList<>();

        for(int y = 0; y < this.h; ++y) {
            ArrayList<Integer> row = new ArrayList<>();

            for(int x = 0; x < this.w; ++x) {
                row.add(p.argb(x, y));
            }

            table.add(row);
        }
    }

    public int get(int x, int y) {
        return table.get(y).get(x);
    }

    public void set(int x, int y, int color) {
        table.get(y).set(x, color);
    }

    public Palette createPalette() {
        int argbs[][] = new int[h][w];

        for (int y = 0; y < h; ++y) {
            ArrayList<Integer> row = table.get(y);
            for (int x = 0; x < w; ++x) {
                argbs[y][x] = row.get(x);
            }
        }

        Log.d("PV", this.toString() + " returns " + Arrays.toString(argbs));

        return new Palette(argbs);
    }

    /*public void removeColumn(int columnIndex) {
        for (ArrayList<Integer> row : table) {
            row.remove(columnIndex);
        }
        --w;
        if (view != null) view.invalidate();
    }

    public void removeRow(int rowIndex) {
        table.remove(rowIndex);
        --h;
        if (view != null) view.invalidate();
    }

    public void duplicateRow(int rowIndex) {
        ArrayList<Integer> row = new ArrayList<Integer>();

        table.add(rowIndex, row);
        h++;
        if (view != null) view.invalidate();
    }

    public void duplicateColumn(int columnIndex) {
        Random rnd = new Random();

        float[] hsv = new float[3];

        for (ArrayList<Integer> row : table) {
            hsv[0] = rnd.nextFloat() * 360;
            hsv[1] = rnd.nextFloat();
            hsv[2] = rnd.nextFloat();
            row.add(columnIndex, Color.HSVToColor(hsv));
        }
        w++;
        if (view != null) view.invalidate();
    }

    public void moveRow(int startIndex, int dstIndex) {
        ArrayList<Integer> tmp = table.get(rowIndex);
        table.set(rowIndex, table.get(dstIndex));
        table.set(dstIndex, tmp);
        if (view != null) view.invalidate();
    }

    public void moveColumn(int startIndex, int dstIndex) {
        for (ArrayList<Integer> row : table) {
            int tmp = row.get(columnIndex);
            row.set(columnIndex, row.get(dstIndex));
            row.set(dstIndex, tmp);
        }
        if (view != null) view.invalidate();
    }

    public void moveColor(int x0, int y0, int x1, int y1) {

    }*/

    /**
     * A stream of infinitely many random colors
     */
    private Iterator<Integer> randomColorStream = new Iterator<Integer>() {
        Random rnd = new Random();

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Integer next() {
            int r = (int) (255 * Math.sqrt(rnd.nextDouble()));
            int g = (int) (255 * Math.sqrt(rnd.nextDouble()));
            int b = (int) (255 * Math.sqrt(rnd.nextDouble()));

            return Color.rgb(r, g, b);
        }
    };

    /**
     * Set Width adds columns. Missing ones are filled up with random
     * colors
     * @param width
     * @return
     */
    public NewPaletteViewModel setWidth(int width) {
        // fill up all rows if necessary
        int d = width - table.get(0).size();

        if(d > 0) {
            for(ArrayList<Integer> row : table) {
                row.ensureCapacity(width);
                for(int i = 0; i < d; ++i) {
                    row.add(randomColorStream.next());
                }
            }
        }

        // must keep at least one.
        this.w = Math.max(1, width);

        return this;
    }

    public NewPaletteViewModel setHeight(int height) {
        int d = height - table.size();

        if(d > 0) {
            // fill up with random colors
            for(int k = 0; k < d; ++k) {
                int size = table.get(0).size(); // because the size might be bigger than w
                ArrayList<Integer> newRow = new ArrayList<>();

                for (int i = 0; i < size; ++i) {
                    newRow.add(randomColorStream.next());
                }

                table.add(newRow);
            }
        }

        // must keep at least 1.
        this.h = Math.max(1, height);

        return this;
    }

    public int width() {
        return w;
    }

    public int height() {
        return h;
    }

    public void rotateAll(int dx, int dy) {
        // shift colors
        for(int x = 0; x < width(); ++x) {
            // rotate all columns
            for(int i = 0; i < dy; ++i) {
                rotateDown(x);
            }

            // in both directions.
            for(int i = 0; i > dy; --i) {
                rotateUp(x);
            }
        }

        for(int y = 0; y < height(); ++y) {
            for(int i = 0; i < dx; ++i) {
                rotateRight(y);
            }

            for(int i = 0; i > dx; --i) {
                rotateLeft(y);
            }
        }
    }

    public void rotateUp(int x) {
        int i = get(x, 0);

        for(int y = 1; y < height(); ++y) {
            set(x, y - 1, get(x, y));
        }

        set(x, height() - 1, i);
    }

    public void rotateDown(int x) {
        int i = get(x, height() - 1);

        for(int y = height() - 2; y >= 0; --y) {
            set(x, y + 1, get(x, y));
        }

        set(x, 0, i);
    }

    public void rotateLeft(int y) {
        int i = get(0, y);

        for(int x = 1; x < width(); ++x) {
            set(x - 1, y, get(x, y));
        }

        set(width() - 1, y, i);
    }

    public void rotateRight(int y) {
        int i = get(width() - 1, y);

        for(int x = width() - 2; x >= 0; --x) {
            set(x + 1, y, get(x, y));
        }

        set(0, y, i);
    }

    public void moveDown(int y) {
        if(height() == 1 || y >= height() - 1 || y < 0) return;

        for(int x = 0; x < width(); ++x) {
            int i = get(x, y);
            set(x, y, get(x, y + 1));
            set(x, y + 1, i);
        }
    }

    public void moveUp(int y) {
        moveDown(y - 1);
    }

    public void moveRight(int x) {
        if(width() == 1 || x >= width() - 1 || x < 0) return;

        for(int y = 0; y < height(); ++y) {
            int i = get(x, y);
            set(x, y, get(x + 1, y));
            set(x + 1, y, i);
        }
    }

    public void moveLeft(int colIndex) {
        moveRight(colIndex - 1);
    }

    public void duplicateColumn(int x) {
        for(ArrayList<Integer> row : table) {
            row.add(x, row.get(x));
        }
        this.w++;
    }

    public void duplicateRow(int y) {
        ArrayList<Integer> newRow = new ArrayList<>(table.get(0).size());

        for(int x = 0; x < table.get(0).size(); ++x) {
            newRow.add(get(x, y));
        }

        table.add(y, newRow);
        this.h++;
    }

    public void removeColumn(int x) {
        for(ArrayList<Integer> row : table) {
            row.remove(x);
        }

        this.w--;
    }

    public void removeRow(int y) {
        table.remove(y);
        this.h--;
    }
}
