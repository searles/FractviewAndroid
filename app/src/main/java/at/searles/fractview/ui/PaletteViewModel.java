package at.searles.fractview.ui;

import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import at.searles.math.color.Palette;

/**
 * Model for a palette view
 */
public class PaletteViewModel {
	private int w;
	private int h;

	private ArrayList<ArrayList<Integer>> table = new ArrayList<ArrayList<Integer>>();

	private PaletteView view = null;

	public PaletteViewModel(Palette palette) {
		this.w = palette.width();
		this.h = palette.height();

		table.clear();

		for (int y = 0; y < h; ++y) {
			ArrayList<Integer> row = new ArrayList<Integer>(w);
			table.add(row);
			for (int x = 0; x < w; ++x) {
				row.add(palette.argb(x, y));
			}
		}
	}

	void setView(PaletteView view) {
		this.view = view;
		if (view != null) view.invalidate();
	}

	public int get(int x, int y) {
		return table.get(y).get(x);
	}

	public void set(int x, int y, int color) {
		table.get(y).set(x, color);
		if (view != null) view.invalidate();
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

	public void removeColumn(int columnIndex) {
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

	public void addRow(int rowIndex) {
		ArrayList<Integer> row = new ArrayList<Integer>();

		Random rnd = new Random();
		float[] hsv = new float[3];

		for (int x = 0; x < w; ++x) {
			hsv[0] = rnd.nextFloat() * 360;
			hsv[1] = (float) Math.sqrt(rnd.nextFloat());
			hsv[2] = (float) Math.sqrt(rnd.nextFloat());
			row.add(Color.HSVToColor(hsv));
		}
		table.add(rowIndex, row);
		h++;
		if (view != null) view.invalidate();
	}

	public void addColumn(int columnIndex) {
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

	public void moveRow(int rowIndex, int dstIndex) {
		ArrayList<Integer> tmp = table.get(rowIndex);
		table.set(rowIndex, table.get(dstIndex));
		table.set(dstIndex, tmp);
		if (view != null) view.invalidate();
	}

	public void moveColumn(int columnIndex, int dstIndex) {
		for (ArrayList<Integer> row : table) {
			int tmp = row.get(columnIndex);
			row.set(columnIndex, row.get(dstIndex));
			row.set(dstIndex, tmp);
		}
		if (view != null) view.invalidate();
	}

	public int width() {
		return w;
	}

	public int height() {
		return h;
	}
}
