package at.searles.fractview.ui.editors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import at.searles.fractview.ui.PaletteActivity;
import at.searles.math.color.Colors;

public class PaletteView extends View {
	// draw grid where left hand side and top two are fixed while
	// the palette itself is draggable.
	// advantage: memory! Drawing is really
	// easy of this one.

	// fixme fixed white is not nice.

	// fixme onmeasure

	// fixme: on long tab on column move it?


	PaletteViewModel model;

	int offsetX;
	int offsetY;

	Paint colorPaint;
	Paint headerPaint1;
	Paint headerPaint2;
	Paint scrollbarPaint;
	//Paint selectedPaint;
	//Paint unselectedPaint;

	GestureDetector detector;

	// assertion: table always contains at least one element

	public PaletteView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		// FIXME: Maybe read attributes in the following way?
		/*
		        TypedArray array = getTheme().obtainStyledAttributes(new int[] {
                android.R.attr.colorBackground,
                android.R.attr.textColorPrimary,
        });

        backgroundColor = array.getColor(0, 0xFF00FF);
        textColor = array.getColor(1, 0xFF00FF);
        array.recycle();

		 */

		init();

		initGestureListener(context);
		//detector.
	}


	public void setModel(PaletteViewModel model) {
		this.model = model;
		model.setView(this);
	}

	int heightBox;
	int headerWidth;
	int widthBox;
	//int y0;
	int padding;
	Rect bounds;

	//TreeSet<Integer> selectedColumns;
	//TreeSet<Integer> selectedRows;

	private void init() {
		colorPaint = new Paint();
		colorPaint.setAntiAlias(true);
		colorPaint.setStyle(Paint.Style.FILL);
		colorPaint.setTypeface(Typeface.MONOSPACE);
		// FIXME Text Size!!!
		colorPaint.setTextSize(16);

		headerPaint1 = new Paint();
		headerPaint1.setStyle(Paint.Style.STROKE);
		headerPaint1.setStrokeWidth(3f); // fixme
		headerPaint1.setColor(0xff000000); // fixme
		headerPaint2 = new Paint();
		headerPaint2.setStyle(Paint.Style.STROKE);
		headerPaint2.setStrokeWidth(3f); // fixme
		headerPaint2.setColor(0xffffffff); // fixme

		scrollbarPaint = new Paint();
		scrollbarPaint.setStyle(Paint.Style.STROKE);
		scrollbarPaint.setStrokeWidth(6f); // fixme
		scrollbarPaint.setColor(0xffffffff);


		/*selectedPaint = new Paint();
		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setStrokeWidth(padding); // fixme
		selectedPaint.setColor(0xffaa0000); // fixme

		// unselectedPaint is only used for header
		unselectedPaint = new Paint();
		unselectedPaint.setStyle(Paint.Style.STROKE);
		unselectedPaint.setStrokeWidth(padding / 2); // fixme
		unselectedPaint.setColor(0xff00aa00); // fixme*/

		// init text size
		bounds = new Rect();

		colorPaint.getTextBounds("#00000000", 0, 9, bounds);

		// fixme do the following 3 in a different way
		padding = 12;
		heightBox = bounds.height() * 5;
		widthBox = bounds.width() + 4 * bounds.height();
		headerWidth = heightBox; // heightbox is used as universal size

		offsetX = headerWidth;
		offsetY = heightBox;

		colorPaint.setTextAlign(Paint.Align.CENTER);

		// we use bounds of the heightBox of the font.
		//selectedRows = new TreeSet<Integer>();
		//selectedColumns = new TreeSet<Integer>();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width;
		int height;

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		// fixme
		//Measure Height
		if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
			height = MeasureSpec.getSize(heightMeasureSpec);
		} else {
			height = Math.max(3, model.h) * (heightBox + padding) + padding + heightBox;
		}
		if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
			width = MeasureSpec.getSize(widthMeasureSpec);
		} else {
			width = Math.max(3, model.w) * (widthBox + padding) + padding + headerWidth;
		}

		//MUST CALL THIS
		setMeasuredDimension(width, height);
	}

	void getTopHeaderRect(int x, Rect bounds) {
		getRect(x, 0, bounds);
		bounds.top = padding / 2;
		bounds.bottom = bounds.top + heightBox;
	}

	void getLeftHeaderRect(int y, Rect bounds) {
		getRect(0, y, bounds);
		bounds.left = padding / 2;
		bounds.right = bounds.left + headerWidth;
	}

	int totalWidth() {
		return model.w * (widthBox + padding) + headerWidth + padding;
	}

	int totalHeight() {
		return model.h * (heightBox + padding) + heightBox + padding;
	}

	void getRect(int x, int y, Rect bounds) {
		int l = x * (widthBox + padding) + 3 * padding / 2 + offsetX; // two times padding
		int u = y * (heightBox + padding) + 3 * padding / 2 + offsetY; // because of header
		bounds.set(l, u, l + widthBox, u + heightBox);
	}

	float getX(float px) {
		return (px - offsetX - 3 * padding / 2) / (widthBox + padding);
	}

	float getY(float py) {
		return (py - offsetY - 3 * padding / 2) / (heightBox + padding);

	}

	void clampOffset() {
		// do not drag the view farer than necessary
		int width = getWidth(); // width of view
		int height = getHeight(); // height of view

		int totalWidth = totalWidth(); // required space of color part
		int totalHeight = totalHeight(); // ignore header.

		if(totalWidth + offsetX - headerWidth < width) offsetX = width - totalWidth + headerWidth;
		if(totalHeight + offsetY - heightBox < height) offsetY = height - totalHeight + heightBox;

		if(offsetX > headerWidth || width >= totalWidth) offsetX = headerWidth;
		if(offsetY > heightBox || height >= totalHeight) offsetY = heightBox;
	}



	@Override
	public void onDraw(Canvas canvas) {
		// draw color buttons
		for(int y = 0; y < model.h; ++y) {
			//boolean isSelected = selectedRows.contains(y);
			for(int x = 0; x < model.w; ++x) {
				int color = model.get(x, y);

				colorPaint.setColor(color);
				getRect(x, y, bounds);

				canvas.drawRect(bounds, colorPaint);

				//if(isSelected || selectedColumns.contains(x)) {
				//	canvas.drawRect(bounds, selectedPaint);
				//}

				int textColor = Colors.brightness(color) > 0.7 ? 0xff000000 : 0xffffffff;
				colorPaint.setColor(textColor);

				canvas.drawText(Colors.toColorString(model.get(x, y)), bounds.centerX(), bounds.centerY() + heightBox / 10, colorPaint);
			}
		}

		// draw left first column
		for(int y = 0; y < model.h; ++y) {
			getLeftHeaderRect(y, bounds);
			canvas.drawRect(bounds.left, bounds.top, bounds.right - 3, bounds.bottom - 3, headerPaint1);
			canvas.drawRect(bounds.left + 3, bounds.top + 3, bounds.right, bounds.bottom, headerPaint2);
		}

		// draw header top
		for(int x = 0; x < model.w; ++x) {
			getTopHeaderRect(x, bounds);
			canvas.drawRect(bounds.left, bounds.top, bounds.right - 3, bounds.bottom - 3, headerPaint1);
			canvas.drawRect(bounds.left + 3, bounds.top + 3, bounds.right, bounds.bottom, headerPaint2);
		}

		// finally, if scrolling is possible/necessary
		// draw on bottom/right scroll markers to indicate position.
		// width is padding / 2.
		int width = getWidth(); // width of view
		int height = getHeight(); // height of view

		int totalWidth = totalWidth(); // required space of color part
		int totalHeight = totalHeight(); // ignore header.

		if(width < totalWidth) {
			// draw marker on bottom
			// range for drawing is headerWidth to width
			// range for offset is
			// int lo = headerWidth;
			// int hi = width - totalWidth + headerWidth;
			float ratio = ((float) width) / (float) totalWidth;
			int len = (int) (width * ratio); // length of line

			// line starts between 0 and (width - len).
			// depending on offsetX

			// offsetX from width - totalWidth + headerWidth to headerWidth
			// offsetX - headerWidth from width - totalWidth to 0
			// offsetX - headerWidth + totalWidth - width from 0 to totalWidth - width

			float dR = ((float) (offsetX - headerWidth + totalWidth - width)) / (float) (totalWidth - width);

			int x0 = (int) ((width - len) * (1.f - dR));

			canvas.drawLine(x0, height - padding / 2, x0 + len, height - padding / 2, scrollbarPaint);
		}

		if(height < totalHeight) {
			// draw marker on right
			// range for drawing is heightBox to height
			// int lo = heightBox;
			// int hi = height - totalHeight + heightBox;
			float ratio = ((float) height) / (float) totalHeight;
			int len = (int) (height * ratio); // length of line

			float dR = ((float) (offsetY - heightBox + totalHeight - height)) / (float) (totalHeight - height);

			int y0 = (int) ((height - len) * (1.f - dR));

			canvas.drawLine(width - padding / 2, y0, width - padding / 2, y0 + len, scrollbarPaint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return detector.onTouchEvent(event) || super.onTouchEvent(event);
	}

	void selectTablePart(final int index, final boolean isRow) {
		// delete is only allowed if there is more than one element to be deleted
		// move is not possible in these cases either
		boolean hasDeleteMoveOption = (isRow && model.h > 1) || (!isRow && model.w > 1);

		CharSequence options[];

		if(hasDeleteMoveOption) {
			if(isRow) {
				// fixme language
				options = new CharSequence[] {"insert above", "insert below", "delete", "move up", "move down"};
			} else {
				options = new CharSequence[] {"insert left", "insert right", "delete", "move left", "move right"};
			}
		} else {
			if(isRow) {
				options = new CharSequence[] {"insert above", "insert below"};
			} else {
				options = new CharSequence[] {"insert left", "insert right"};
			}
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		//builder.setTitle("Pick a color");
		builder.setItems(options, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0: {// insert before
						if (isRow) model.addRow(index);
						else model.addColumn(index);
					}
					break;
					case 1: {// insert after
						if (isRow) model.addRow(index + 1);
						else model.addColumn(index + 1);
					}
					break;
					case 2: {// delete
						if (isRow) model.removeRow(index);
						else model.removeColumn(index);
					}
					break;
					case 3: {// move ahead
						if (isRow) model.moveRow(index, (index + model.h - 1) % model.h);
						else model.moveColumn(index, (index + model.w - 1) % model.w);
					}
					break;
					case 4: {// move behind
						if (isRow) model.moveRow(index, (index + 1) % model.h);
						else model.moveColumn(index, (index + 1) % model.w);
					}
					break;
					default:
						throw new IllegalArgumentException("no such selection: " + which);
				}
			}
		});
		builder.setCancelable(true);
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	void initGestureListener(final Context context) {
		GestureDetector.SimpleOnGestureListener l = new GestureDetector.SimpleOnGestureListener() {
			// fixme what was this for? float lastX, lastY;

			@Override
			public boolean onSingleTapConfirmed(MotionEvent motionEvent) {

				float px = motionEvent.getX();
				float py = motionEvent.getY();

				if(px <= headerWidth) {
					// a row was selected.
					final int y = (int) getY(py);
					if(y >= 0 && y < model.h) {
						selectTablePart(y, true);
						/*if(selectedRows.contains(y)) {
							selectedRows.remove(y);
						} else {
							selectedRows.add(y);
						}

						invalidate();*/
					}
				} else if(py <= heightBox) {
					// a column was selected
					final int x = (int) getX(px);
					if(x >= 0 && x < model.w) {
						/*if(selectedColumns.contains(x)) {
							selectedColumns.remove(x);
						} else {
							selectedColumns.add(x);
						}

						invalidate();*/
						selectTablePart(x, false);
					}
				} else {
					final int x = (int) getX(px);
					final int y = (int) getY(py);

					if(x >= 0 && x < model.w && y >= 0 && y < model.h) {
						// create color editor that modifies the model
						// this way we survive rotations (since model is stuck
						// to the editor which is again stuck to the dialog fragment)

						((PaletteActivity) context).editColorAt(x, y);
					}
				}

				return true;
			}

			/*@Override
			public boolean onDoubleTap(MotionEvent motionEvent) {
				Log.d("GESTURE", "double tab");
				return true; // true;
			}

			@Override
			public boolean onDoubleTapEvent(MotionEvent motionEvent) {
				//Log.d("GESTURE", "double tab event");
				return false;//true;
			}*/

			@Override
			public boolean onDown(MotionEvent motionEvent) {
				//Log.d("GESTURE", "down");
				// is not called for multiple fingers
				return true;
			}

			/*@Override
			public void onShowPress(MotionEvent motionEvent) {
				//Log.d("GESTURE", "show press");
			}

			@Override
			public boolean onSingleTapUp(MotionEvent motionEvent) {
				//Log.d("GESTURE", "single tap up");
				return false;//true;
			}*/

			@Override
			public boolean onScroll(MotionEvent event1, MotionEvent event2, float dx, float dy) {
				offsetX -= dx;
				offsetY -= dy;

				// check bounds of offset

				// fixme put the following into some 'invalidate'-function, because
				// it is also important on rotation,
				clampOffset();

				invalidate();

				return true;
			}

			/*@Override
			public void onLongPress(MotionEvent motionEvent) {
				// Log.d("GESTURE", "long press");
			}

			@Override
			public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
				// Log.d("GESTURE", "fling");
				// FIXME can be used to animate sth?
				return false;//true;
			}*/
		};

		detector = new GestureDetector(context, l);
		detector.setOnDoubleTapListener(l);
	}

}
