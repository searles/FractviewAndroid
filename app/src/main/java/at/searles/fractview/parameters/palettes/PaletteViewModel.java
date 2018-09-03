package at.searles.fractview.parameters.palettes;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import at.searles.math.color.Palette;

/**
 * Model for NewPaletteView
 */
public class PaletteViewModel {
    private int w;
    private int h;

    private ArrayList<ArrayList<Integer>> table;

    PaletteViewModel(Palette p) {
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

    Palette createPalette() {
        int colors[] = new int[h * w];

        for (int y = 0; y < h; ++y) {
            ArrayList<Integer> row = table.get(y);
            for (int x = 0; x < w; ++x) {
                colors[y * w + x] = row.get(x);
            }
        }

        return new Palette(w, h, colors);
    }

    /**
     * A stream of infinitely many random colors
     */
    private Iterator<Integer> randomColorStream = new Iterator<Integer>() {
        Random rnd = new Random();
        float[] tmp = new float[3];

        @Override
        public boolean hasNext() {
            return true;
        }

        int rndColor() {
            // tmp[0] will be hue [0-360]
            // tmp[1] will be sat [0-1]
            // tmp[2] will be val [0-1]

            // hue: 0 red, 60 yellow, 120 green, 240 is blue.
            // Since yellow is culturally more important, I will scale
            // it up.
            float hue = rnd.nextFloat();

            if(hue < 0.5f) {
                // gradient red-yellow-green
                hue *= 360;
            } else {
                // gradient green-blue-red
                hue = hue * 480 - 120;
            }

            float sat = rnd.nextFloat();

            float val = rnd.nextFloat();
            val = 1 - val * val; // more natural

            // FIXME maybe more for others.

            tmp[0] = hue; tmp[1] = sat; tmp[2] = val;

            return Color.HSVToColor(tmp);
        }

        @Override
        public Integer next() {
            return rndColor(); // create random color in lab
        }
    };

    int randomColor() {
        return randomColorStream.next();
    }

    /**
     * Set Width adds columns. If a color is missing, a random color is picked.
     */
    public PaletteViewModel setWidth(int width) {
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

    public PaletteViewModel setHeight(int height) {
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

    void rotateAll(int dx, int dy) {
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

    void rotateUp(int x) {
        int i = get(x, 0);

        for(int y = 1; y < height(); ++y) {
            set(x, y - 1, get(x, y));
        }

        set(x, height() - 1, i);
    }

    void rotateDown(int x) {
        int i = get(x, height() - 1);

        for(int y = height() - 2; y >= 0; --y) {
            set(x, y + 1, get(x, y));
        }

        set(x, 0, i);
    }

    void rotateLeft(int y) {
        int i = get(0, y);

        for(int x = 1; x < width(); ++x) {
            set(x - 1, y, get(x, y));
        }

        set(width() - 1, y, i);
    }

    void rotateRight(int y) {
        int i = get(width() - 1, y);

        for(int x = width() - 2; x >= 0; --x) {
            set(x + 1, y, get(x, y));
        }

        set(0, y, i);
    }

    void moveDown(int y) {
        if(height() == 1 || y >= height() - 1 || y < 0) return;

        for(int x = 0; x < width(); ++x) {
            int i = get(x, y);
            set(x, y, get(x, y + 1));
            set(x, y + 1, i);
        }
    }

    void moveUp(int y) {
        moveDown(y - 1);
    }

    void moveRight(int x) {
        if(width() == 1 || x >= width() - 1 || x < 0) return;

        for(int y = 0; y < height(); ++y) {
            int i = get(x, y);
            set(x, y, get(x + 1, y));
            set(x + 1, y, i);
        }
    }

    void moveLeft(int colIndex) {
        moveRight(colIndex - 1);
    }

    void duplicateColumn(int x) {
        for(ArrayList<Integer> row : table) {
            row.add(x, row.get(x));
        }
        this.w++;
    }

    void duplicateRow(int y) {
        ArrayList<Integer> newRow = new ArrayList<>(table.get(0).size());

        for(int x = 0; x < table.get(0).size(); ++x) {
            newRow.add(get(x, y));
        }

        table.add(y, newRow);
        this.h++;
    }

    void removeColumn(int x) {
        for(ArrayList<Integer> row : table) {
            row.remove(x);
        }

        this.w--;
    }

    void removeRow(int y) {
        table.remove(y);
        this.h--;
    }
}
