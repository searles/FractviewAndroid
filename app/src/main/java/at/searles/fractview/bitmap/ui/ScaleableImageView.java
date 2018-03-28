package at.searles.fractview.bitmap.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

import at.searles.fractview.Commons;
import at.searles.math.Scale;

public class ScaleableImageView extends View {

	/**
	 * Scale factor on double tapping
	 */
	public static final float SCALE_ON_DOUBLE_TAB = 3f;

	public static final float LEFT_UP_INDICATOR_LENGTH = 40f;

	/**
	 * The grid is painted from two kinds of lines. These are the paints
	 */
	private static final Paint[] GRID_PAINTS = new Paint[]{new Paint(), new Paint(), new Paint()};
	private static final Paint BOUNDS_PAINT = new Paint();
	private static final Paint TEXT_PAINT = new Paint(); // for error messages

	private static final Paint IMAGE_PAINT = new Paint();

	static {
		IMAGE_PAINT.setAntiAlias(false);
		IMAGE_PAINT.setFilterBitmap(false);
		IMAGE_PAINT.setDither(false);

		GRID_PAINTS[0].setColor(0xffffffff);
		GRID_PAINTS[0].setStyle(Paint.Style.STROKE);
		GRID_PAINTS[0].setStrokeWidth(5f);

		GRID_PAINTS[1].setColor(0xff000000);
		GRID_PAINTS[1].setStyle(Paint.Style.STROKE);
		GRID_PAINTS[1].setStrokeWidth(3f);

		GRID_PAINTS[2].setColor(0xffffffff);
		GRID_PAINTS[2].setStyle(Paint.Style.STROKE);
		GRID_PAINTS[2].setStrokeWidth(1f);

		BOUNDS_PAINT.setColor(0xaa000000); // semi-transparent black
		BOUNDS_PAINT.setStyle(Paint.Style.FILL_AND_STROKE);

		TEXT_PAINT.setTextAlign(Paint.Align.CENTER);
		TEXT_PAINT.setTextSize(96); // fixme hardcoded...
	}

	/**
	 * To save the state of this view over rotation etc...
	 */
	static class ViewState extends BaseSavedState {

		/**
		 * Should the grid be shown or not?
		 */
		boolean showGrid = false;
		boolean rotationLock = false;
		boolean confirmZoom = false;
		boolean deactivateZoom = false;

		ViewState(Parcelable in) {
			super(in);
		}

		private ViewState(Parcel in) {
			super(in);
			this.showGrid = in.readInt() == 1;
			this.rotationLock = in.readInt() == 1;
			this.confirmZoom = in.readInt() == 1;
			this.deactivateZoom = in.readInt() == 1;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);

			dest.writeInt(showGrid ? 1 : 0);
			dest.writeInt(rotationLock ? 1 : 0);
			dest.writeInt(confirmZoom ? 1 : 0);
			dest.writeInt(deactivateZoom ? 1 : 0);
		}

		public static final Creator<ViewState> CREATOR = new Creator<ViewState>() {
			@Override
			public ViewState createFromParcel(Parcel in) {
				return new ViewState(in);
			}

			@Override
			public ViewState[] newArray(int size) {
				return new ViewState[size];
			}
		};
	}

	public static interface Listener {
		void scaleRelative(Scale m);
	}

	private boolean showGrid;
	private boolean rotationLock;
	private boolean confirmZoom;
	private boolean deactivateZoom;

	// Here, we also have some gesture control
	// Scroll-Events are handled as multitouch scale-events
	// double tab zooms at tabbed position

	// === The following fields are not preserved over rotation ===

	private Matrix view2bitmap = new Matrix();
	private Matrix bitmap2view = new Matrix();

	private Listener listener;

	/**
	 * We use this one to store the last transformation
	 * to also apply it to the picture if it was not updated yet.
	 */
	private LinkedList<Matrix> lastScale = new LinkedList<>();

	/**
	 * To detect gestures (3 finger drag etc...)
	 */
	private GestureDetector detector;

	/**
	 * Multitouch object that always reacts to finger input
	 */
	private MultiTouch multitouch;

	/**
	 * Image matrix. This one is modified according to the selection.
	 * @param context
	 * @param attrs
     */
	private Matrix imageMatrix;

	private Bitmap bitmap;

	/**
	 *
	 * @param context
	 * @param attrs
     */
	public ScaleableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTouch();
	}

	private void setImageMatrix(Matrix matrix) {
		this.imageMatrix = matrix;

	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void setBitmap(Bitmap bitmap) {
		multitouch.cancel();
		this.bitmap = bitmap;
		requestLayout();
	}


	/**
	 * Toggle show-grid flag
	 * @param showGrid if true, the grid will be shown.
     */
	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
		invalidate();
	}

	public boolean getShowGrid() {
		return showGrid;
	}

	public void setRotationLock(boolean rotationLock) {
		multitouch.cancel();
		this.rotationLock = rotationLock;
		// does not change the view.
	}

	public boolean getRotationLock() {
		return rotationLock;
	}

	public void setConfirmZoom(boolean confirmZoom) {
		multitouch.cancel();
		this.confirmZoom = confirmZoom;
	}

	public boolean getConfirmZoom() {
		return confirmZoom;
	}

	public void setDeactivateZoom(boolean deactivateZoom) {
		multitouch.cancel();
		this.deactivateZoom = deactivateZoom;
	}

	public boolean getDeactivateZoom() {
		return deactivateZoom;
	}

	public boolean backButtonAction() {
		// cancel selection if it exists
		if(multitouch != null && multitouch.controller != null) {
			multitouch.cancel();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		//begin boilerplate code that allows parent classes to save state
		Parcelable superState = super.onSaveInstanceState();

		ViewState vs = new ViewState(superState);
		vs.showGrid = this.showGrid;
		vs.rotationLock = this.rotationLock;
		vs.confirmZoom = this.confirmZoom;
		vs.deactivateZoom = this.deactivateZoom;

		return vs;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		//begin boilerplate code so parent classes can restore state
		if(!(state instanceof ViewState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		ViewState vs = (ViewState)state;
		super.onRestoreInstanceState(vs.getSuperState());
		//end

		setShowGrid(vs.showGrid);
		setRotationLock(vs.rotationLock);
		setConfirmZoom(vs.confirmZoom);
		setDeactivateZoom(vs.deactivateZoom);
	}

	/**
	 * If the view measurements are the ones in the arguments, what would be the width?
	 * In case the image is flipped then this one returns the scaled height.
	 * @param viewWidth
	 * @param viewHeight
     * @return
     */
	private float scaledBitmapWidth(float viewWidth, float viewHeight) {
		if(bitmap == null) return viewWidth;

		float bitmapWidth = bitmap.getWidth();
		float bitmapHeight = bitmap.getHeight();

		/* Just some thoughts:
		The scaled rectangle of the bitmap should fit into the view.
		So, the ratio is the min-ratio.
		 */

		if(flipBitmap(viewWidth, viewHeight)) {
			float ratio = Math.min(viewWidth / bitmapHeight, viewHeight / bitmapWidth);
			return bitmapHeight * ratio;
		} else {
			float ratio = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
			return bitmapWidth * ratio;
		}
	}

	private float scaledBitmapHeight(float viewWidth, float viewHeight) {
		if(bitmap == null) return viewHeight;

		float bitmapWidth = bitmap.getWidth();
		float bitmapHeight = bitmap.getHeight();

		if(flipBitmap(viewWidth, viewHeight)) {
			float ratio = Math.min(viewWidth / bitmapHeight, viewHeight / bitmapWidth);
			return bitmapWidth * ratio;
		} else {
			float ratio = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
			return bitmapHeight * ratio;
		}
	}

	/**
	 * Should the bitmap be rotated by 90 degrees to maximize
	 * the filled fractal area?
	 * @param viewWidth
	 * @param viewHeight
     * @return
     */
	private boolean flipBitmap(float viewWidth, float viewHeight) {
		if(bitmap == null) return false;

		float bitmapWidth = bitmap.getWidth();
		float bitmapHeight = bitmap.getHeight();

		// maximize filled area
		if(bitmapWidth > bitmapHeight) {
			return viewWidth < viewHeight;
		} else {
			return bitmapWidth < bitmapHeight && viewWidth > viewHeight;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d("SIV", "onMeasure called now");

		int width;
		int height;

		float vw = MeasureSpec.getSize(widthMeasureSpec);
		float vh = MeasureSpec.getSize(heightMeasureSpec);

		if(bitmap == null) {
			setMeasuredDimension((int) vw, (int) vh);
			return;
		}

		float bw = bitmap.getWidth();
		float bh = bitmap.getHeight();

		if(vw > vh) {
			// if width of view is bigger, match longer side to it
			RectF viewRect = new RectF(0f, 0f, vw, vh);
			RectF bitmapRect = new RectF(0f, 0f, Math.max(bw, bh), Math.min(bw, bh));
			bitmap2view.setRectToRect(bitmapRect, viewRect, Matrix.ScaleToFit.CENTER);
		} else {
			RectF viewRect = new RectF(0f, 0f, vw, vh);
			RectF bitmapRect = new RectF(0f, 0f, Math.min(bw, bh), Math.max(bw, bh));
			bitmap2view.setRectToRect(bitmapRect, viewRect, Matrix.ScaleToFit.CENTER);
		}

		// Check orientation
		if(flipBitmap(vw, vh)) {
			// fixme create this one directly
			// Turn centerImageMatrix by 90 degrees
			Matrix m = new Matrix();
			m.postRotate(90f);
			m.postTranslate(bh, 0);

			bitmap2view.preConcat(m);
		}

		bitmap2view.invert(view2bitmap);

		setImageMatrix(multitouch.viewMatrix());

		width = (int) vw;
		height = (int) vh;

		Log.d("SIV", "dimensions are " + width + " x " + height + ", matrices are " + bitmap2view + ", " + view2bitmap);

		setMeasuredDimension(width, height);
	}

	@Override
	public void onDraw(@NotNull Canvas canvas) {
		// avoid crash if the image was not created yet.
		if(bitmap == null) return;

		// draw image
		canvas.drawBitmap(bitmap, imageMatrix, null);

		// remove bounds
		float w = getWidth(), h = getHeight();

		float bw = scaledBitmapWidth(getWidth(), getHeight());
		float bh = scaledBitmapHeight(getWidth(), getHeight());

		float cx = w / 2.f;
		float cy = h / 2.f;

		if(showGrid) {
			float minlen = Math.min(bw, bh) / 2.f;

			for (int i = 0; i < GRID_PAINTS.length; ++i) {
				Paint gridPaint = GRID_PAINTS[i];

				// outside grid
				canvas.drawLine(0, cy - minlen, w, cy - minlen, gridPaint);
				canvas.drawLine(0, cy + minlen, w, cy + minlen, gridPaint);
				canvas.drawLine(cx - minlen, 0, cx - minlen, h, gridPaint);
				canvas.drawLine(cx + minlen, 0, cx + minlen, h, gridPaint);

				// inside cross
				canvas.drawLine(0, h / 2.f, w, h / 2.f, gridPaint);
				canvas.drawLine(w / 2.f, 0, w / 2.f, h, gridPaint);

				// and a circle inside
				canvas.drawCircle(w / 2.f, h / 2.f, minlen, gridPaint);

				// and also draw quaters with thinner lines
				if(i != 0) {
					canvas.drawLine(0, cy - minlen / 2.f, w, cy - minlen / 2.f, gridPaint);
					canvas.drawLine(0, cy + minlen / 2.f, w, cy + minlen / 2.f, gridPaint);
					canvas.drawLine(cx - minlen / 2.f, 0, cx - minlen / 2.f, h, gridPaint);
					canvas.drawLine(cx + minlen / 2.f, 0, cx + minlen / 2.f, h, gridPaint);

				}
			}
		}

		// draw in total 4 transparent rectangles to indicate the drawing area
		canvas.drawRect(-1, -1, w, cy - bh / 2.f, BOUNDS_PAINT); // top
		canvas.drawRect(-1, -1, cx - bw / 2.f, h, BOUNDS_PAINT); // left
		canvas.drawRect(-1, cy + bh / 2.f, w, h, BOUNDS_PAINT);  // bottom
		canvas.drawRect(cx + bw / 2.f, -1, w, h, BOUNDS_PAINT);  // right

		if(flipBitmap(w, h)) {
			// draw an indicator in the left upper corner of the bitmap
			// which is in this case
			for (Paint gridPaint : GRID_PAINTS) {
				// three lines in total
				canvas.drawLine(
						w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
						w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 1.5f,
						gridPaint
				);

				canvas.drawLine(
						w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
						w - LEFT_UP_INDICATOR_LENGTH * 1.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
						gridPaint
				);

				canvas.drawLine(
						w - LEFT_UP_INDICATOR_LENGTH * 1.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
						w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 1.5f,
						gridPaint
				);
			}
		}
	}

	/**
	 * Called when the bitmap is updated according to the last transformation or
	 * when a post-edit happend that did not restart the drawing. This is necessary
	 * because I might be scaling the image while it is modified according to a
	 * pending change.
	 */
	public void removeLastScale() {
		Log.d("SIV", "remove last scale");
		if(!lastScale.isEmpty()) {
			Matrix l = lastScale.removeLast();
			// update the viewMatrix.
			setImageMatrix(multitouch.viewMatrix());
			invalidate();
		}
	}

	/**
	 * Called when a scale is commited
	 */
	public void combineLastScales() {
		if(lastScale.size() >= 2) {
			// more than or eq two, because one scale is currently waiting for
			// its preview to be finished (which is then handled by removeLastScale)
			Matrix l1 = lastScale.removeLast();
			Matrix l0 = lastScale.removeLast();

			l0.preConcat(l1);

			lastScale.addLast(l0);
		}
	}

	/**
	 * Initialize objects used for multitouch and other events.
	 */
	void initTouch() {
		this.detector = new GestureDetector(getContext(), gestureListener);
		this.multitouch = new MultiTouch();

		// and I need a touchevent
	}

	@Override
	public boolean onTouchEvent(@NotNull MotionEvent event) {
		// gesture detector handles scroll
		// no action without bitmap fragment.
		// or if deactivateZoom is set.
		if(deactivateZoom) {
			return false;
		}

		boolean ret = false;

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_CANCEL: multitouch.cancel(); ret = true; break;
			case MotionEvent.ACTION_DOWN: multitouch.initDown(event); ret = true; break;
			case MotionEvent.ACTION_POINTER_DOWN: multitouch.down(event); ret = true; break;
			case MotionEvent.ACTION_POINTER_UP: multitouch.up(event); ret = true; break;
			case MotionEvent.ACTION_UP: multitouch.finalUp(event); ret = true; break;
		}

		ret |= detector.onTouchEvent(event);

		return ret || super.onTouchEvent(event);
	}

	/**
	 * Map point from view coordinates to norm
	 * @param p Immutable point to modify
	 * @return returns the mapped point.
	 */
	PointF norm(PointF p) {
		float[] pts = new float[]{p.x, p.y};

		view2bitmap.mapPoints(pts);
		Commons.bitmap2norm(bitmap.getWidth(), bitmap.getHeight()).mapPoints(pts);
		return new PointF(pts[0], pts[1]);
	}

	/**
	 * Transform the image using matrix n
	 * @param n
	 */
	void addScale(Matrix n) {
		Log.d("SIV", "Adding a new scale: " + n);
		// and use it for lastScale.
		lastScale.addFirst(n); // add at the end (?)

		// the inverted one will be applied to the current scale
		Matrix m = new Matrix();
		n.invert(m);

		float[] values = new float[9];
		m.getValues(values);

		final Scale sc = Scale.fromMatrix(values);

		// update scale and restart calculation.
		listener.scaleRelative(sc);

		// If there are multiple scales pending, then
		// we combine them into one. This is important for the preview.
		// In the preview-listener-method, the last scale is removed anyways.
		combineLastScales();
	}

	final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
		@Override
		public boolean onDown(MotionEvent motionEvent) {
			// must be true, otherwise no touch events.
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent event) {
			if(confirmZoom) {
				// not active when 'confirm zoom' is set.
				return false;
			}

			if(multitouch.isScrollEvent) {
				multitouch.cancel();
			}

			int index = event.getActionIndex();

			final PointF p = norm(new PointF(event.getX(index), event.getY(index)));

			Matrix m = new Matrix();
			m.setValues(new float[]{
					SCALE_ON_DOUBLE_TAB, 0, p.x * (1 - SCALE_ON_DOUBLE_TAB),
					0, SCALE_ON_DOUBLE_TAB, p.y * (1 - SCALE_ON_DOUBLE_TAB),
					0, 0, 1
			});

			addScale(m);

			return true;
		}


		@Override
		public boolean onSingleTapUp(MotionEvent motionEvent) {
			if(confirmZoom && multitouch.controller != null) {
				multitouch.confirm();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean onScroll(MotionEvent startEvt, MotionEvent currentEvt, float vx, float vy) {
			multitouch.scroll(currentEvt);
			return true;
		}
	};


	class MultiTouch {
		/**
		 * Controller for the image. It is possible to use only one, I guess,
		 * but scaling and everything is rather difficult.
		 */
		MultiTouchController controller = null;

		boolean isScrollEvent = false;

		/**
		 * returns the current view-matrix
		 * @return
		 */
		Matrix viewMatrix() {
			Matrix m;

			if(isScrollEvent) {
				// fixme avoid creating a new matrix all the time
				m = controller.getMatrix();
			} else {
				// fixme avoid creating a new matrix all the time
				m = new Matrix();
			}

			for(Matrix l : lastScale) {
				m.preConcat(l);
			}

			m.preConcat(Commons.bitmap2norm(bitmap.getWidth(), bitmap.getHeight()));
			m.postConcat(Commons.norm2bitmap(bitmap.getWidth(), bitmap.getHeight()));

			m.postConcat(bitmap2view);

			return m;
		}

		/**
		 * Cancel entire action
		 */
		void cancel() {
			// controller might not be null if a finger is on
			// the display but did not move
			controller = null;

			if(isScrollEvent) {
				isScrollEvent = false;
				setImageMatrix(viewMatrix());
				invalidate();
			}
		}

		/**
		 * Call this on the first finger-down.
		 * @param event
		 */
		void initDown(MotionEvent event) {
			if(controller == null) {
				controller = new MultiTouchController(rotationLock);
			} else if(!confirmZoom) {
				// FIXME this happened, but I don't know why!
				Log.e(getClass().getName(), "Huh? Controller is set, but it is the first down-event?");
			}

			down(event);
		}

		void down(MotionEvent event) {
			if(controller != null) {
				int index = event.getActionIndex();
				int id = event.getPointerId(index);

				PointF p = new PointF(event.getX(index), event.getY(index));
				controller.addPoint(id, norm(p));
			}
		}


		/**
		 *
		 * @param event
		 * @return
		 */
		void finalUp(MotionEvent event) {
			if(!isScrollEvent) {
				cancel();
				// no dragging here...
			} else {
				up(event);

				if (!confirmZoom) {
					confirm();
				}
			}
		}

		void up(MotionEvent event) {
			if(controller != null) {
				int index = event.getActionIndex();
				int id = event.getPointerId(index);

				controller.removePoint(id);
			}
		}

		boolean confirm() {
			if(controller == null) {
				return false;
			}

			if(!controller.isDone()) {
				throw new IllegalStateException("action up but not done...");
			}

			// next for the preferences
			// convert it to a Scale

			// fetch controller-matrix
			Matrix n = controller.getMatrix();

			// and edit the bitmap fragment.
			addScale(n);

			// thanks for all that work, dear controller.
			isScrollEvent = false;
			controller = null;

			// the matrix of the image view will be updated
			// after we receive the first call to the update-method.

			// for now, we will set the latest view matrix.
			// it will be reset later when the first preview has been generated.
			setImageMatrix(viewMatrix());

			return true;
		}

		void scroll(MotionEvent event) {
			isScrollEvent = controller != null;

			if(isScrollEvent) {
				for (int index = 0; index < event.getPointerCount(); ++index) {
					PointF pos = new PointF(event.getX(index), event.getY(index));
					int id = event.getPointerId(index);

					controller.movePoint(id, norm(pos));
				}

				ScaleableImageView.this.imageMatrix = viewMatrix();
				ScaleableImageView.this.invalidate();
			}
		}
	}
}
