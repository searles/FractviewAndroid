package at.searles.fractview.ui;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import at.searles.fractview.BitmapFragment;
import at.searles.fractview.MultiTouchController;
import at.searles.fractview.fractal.Fractal;
import at.searles.math.Scale;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class ScaleableImageView extends ImageView {

	public static final float SCALE_ON_DOUBLE_TAB = 3f; // scale factor on double tapping

	public static final Paint GRID_PAINT_1 = new Paint();
	public static final Paint GRID_PAINT_2 = new Paint();

	static {
		GRID_PAINT_1.setColor(0xffffffff);
		GRID_PAINT_2.setColor(0xff000000);

		GRID_PAINT_1.setStyle(Paint.Style.STROKE);
		GRID_PAINT_2.setStyle(Paint.Style.STROKE);

		GRID_PAINT_1.setStrokeWidth(3f);
		GRID_PAINT_2.setStrokeWidth(1f);
	}

	// Here, we also have some gesture control
	// Scroll-Events are handled as multitouch scale-events
	// double tab zooms at tabbed position

	// FIXME bitmap fragment is needed for the following reasons:
	// FIXME: first, matrices for view transformations
	// FIXME: second, setting Scale
	// FIXME: and third, checking whether it is still running.

	BitmapFragment bitmapFragment;

	Matrix view2bitmap = new Matrix();
	Matrix bitmap2view = new Matrix();

	/**
	 * We use this one to store the last transformation
	 * to also apply it to the picture if it was not updated yet.
	 */
	LinkedList<Matrix> lastScale = new LinkedList<>();

	GestureDetector detector;
	MultiTouch multitouch;
	boolean showGrid;

	public ScaleableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTouch();
	}

	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d("SIV", "onMeasure called now");

		int width;
		int height;

		float vw = MeasureSpec.getSize(widthMeasureSpec);
		float vh = MeasureSpec.getSize(heightMeasureSpec);

		/*int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);*/

		if(bitmapFragment == null || bitmapFragment.getBitmap() == null) {
			setMeasuredDimension((int) vw, (int) vh);
			return;
		}

		// fixme this works but it can be improved.

		RectF viewRect = new RectF(0f, 0f, vw, vh);
		RectF bitmapRect;

		float bw = bitmapFragment.width(); // fixme: bitmapFragment requires drawer in the background.
		float bh = bitmapFragment.height();

		if(vw > vh) {
			// if width of view is bigger, match longer side to it
			bitmapRect = new RectF(0f, 0f, Math.max(bw, bh), Math.min(bw, bh));
		} else {
			bitmapRect = new RectF(0f, 0f, Math.min(bw, bh), Math.max(bw, bh));
		}

		// fixme create this one directly.
		bitmap2view.setRectToRect(bitmapRect, viewRect, Matrix.ScaleToFit.CENTER);

		// Check orientation
		if(vw > vh ^ bw > bh) {
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

		//MUST CALL THIS
		setMeasuredDimension(width, height);
	}

	/**
	 * The following method should be called whenever the imageview changes its size
	 * or the bitmap is changed.
	 */
	/*public void initMatrices() {
		// Set initMatrix + inverse to current view-size
		float vw = getWidth();
		float vh = getHeight();

		// Right after creation, this view might not
		// be inside anything and therefore have size 0.
		if(vw <= 0 && vh <= 0) return; // do nothing.

		// fixme put into scale.
		float bw = bitmapFragment.width();
		float bh = bitmapFragment.height();

		RectF viewRect = new RectF(0f, 0f, vw, vh);
		RectF bitmapRect;

		if(vw > vh) {
			// if width of view is bigger, match longer side to it
			bitmapRect = new RectF(0f, 0f, Math.max(bw, bh), Math.min(bw, bh));
		} else {
			bitmapRect = new RectF(0f, 0f, Math.min(bw, bh), Math.max(bw, bh));
		}

		defaultMatrix.setRectToRect(bitmapRect, viewRect, Matrix.ScaleToFit.CENTER);

		// Check orientation
		if(vw > vh ^ bw > bh) {
			// Turn centerImageMatrix by 90 degrees
			Matrix m = new Matrix();
			m.postRotate(90f);
			m.postTranslate(bh, 0);

			defaultMatrix.preConcat(m);
		}

		currentImageMatrix = defaultMatrix;

		setImageMatrix(defaultMatrix);
	}*/

	final float[] gridPoints = new float[]{
			//0, 0,
			-1, -1,
			-1, 1,
			1, 1,
			1, -1,

			-1, 0,
			1, 0,
			0, -1,
			0, 1,
	};

	float[] viewGridPoints = new float[gridPoints.length];

	@Override
	public void onDraw(@NotNull Canvas canvas) {
		/*if(bitmapFragment != null && getDrawable() != null) {
			// fixme this is ugly...
			BitmapDrawable bd = (BitmapDrawable) getDrawable();
			if(bd.getBitmap() == null) setImageBitmap(bitmapFragment.getBitmap());
		}*/

		// draw image
		super.onDraw(canvas);

		if(showGrid) {
			invNormAll(gridPoints, viewGridPoints);

			float len = Math.min(getWidth(), getHeight());

			// outside square
			canvas.drawLine(viewGridPoints[0], viewGridPoints[1], viewGridPoints[2], viewGridPoints[3], GRID_PAINT_1);
			canvas.drawLine(viewGridPoints[2], viewGridPoints[3], viewGridPoints[4], viewGridPoints[5], GRID_PAINT_1);
			canvas.drawLine(viewGridPoints[4], viewGridPoints[5], viewGridPoints[6], viewGridPoints[7], GRID_PAINT_1);
			canvas.drawLine(viewGridPoints[6], viewGridPoints[7], viewGridPoints[0], viewGridPoints[1], GRID_PAINT_1);

			// inside cross
			canvas.drawLine(viewGridPoints[8], viewGridPoints[9], viewGridPoints[10], viewGridPoints[11], GRID_PAINT_1);
			canvas.drawLine(viewGridPoints[12], viewGridPoints[13], viewGridPoints[14], viewGridPoints[15], GRID_PAINT_1);

			// now same with other dash
			canvas.drawLine(viewGridPoints[0], viewGridPoints[1], viewGridPoints[2], viewGridPoints[3], GRID_PAINT_2);
			canvas.drawLine(viewGridPoints[2], viewGridPoints[3], viewGridPoints[4], viewGridPoints[5], GRID_PAINT_2);
			canvas.drawLine(viewGridPoints[4], viewGridPoints[5], viewGridPoints[6], viewGridPoints[7], GRID_PAINT_2);
			canvas.drawLine(viewGridPoints[6], viewGridPoints[7], viewGridPoints[0], viewGridPoints[1], GRID_PAINT_2);

			// inside cross
			canvas.drawLine(viewGridPoints[8], viewGridPoints[9], viewGridPoints[10], viewGridPoints[11], GRID_PAINT_2);
			canvas.drawLine(viewGridPoints[12], viewGridPoints[13], viewGridPoints[14], viewGridPoints[15], GRID_PAINT_2);

			// and show outer area


		}
	}

	public void setBitmapFragment(BitmapFragment bitmapFragment) {
		this.bitmapFragment = bitmapFragment;
	}

	/*public void updateBitmap() {
		setImageBitmap(bitmapFragment.getBitmap());
		this.requestLayout(); // size might have changed. Force it to relayout
	}*/



	/*@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}*/

	/**
	 * Called when the bitmap is updated according to the last transformation or
	 * when a post-edit happend that did not restart the drawing
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
		if(bitmapFragment == null) return false;

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
		bitmapFragment.bitmap2norm.mapPoints(pts);
		return new PointF(pts[0], pts[1]);
	}

	/** Inverse of norm
	 */
	void invNormAll(float[] src, float[] dst) {
		bitmapFragment.norm2bitmap.mapPoints(dst, src);
		bitmap2view.mapPoints(dst);
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
		bitmapFragment.edit(new Runnable() {
			@Override
			public void run() {
				Fractal fb = bitmapFragment.fractal();
				bitmapFragment.setScale(fb.scale().relative(sc));
			}
		});

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
			// fixme lastScale!
			if(multitouch.isScrollEvent) multitouch.cancel();

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

		/*@Override
		public void onShowPress(MotionEvent motionEvent) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent motionEvent) {
			return false;
		}*/

		@Override
		public boolean onScroll(MotionEvent startEvt, MotionEvent currentEvt, float vx, float vy) {
			// fixme
			//Log.d("OGL", motionEvent1.toString());

			multitouch.scroll(currentEvt);
			return true;
		}

		/*@Override
		public void onLongPress(MotionEvent motionEvent) {

		}

		@Override
		public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
			return false;
		}*/
	};


	class MultiTouch {
		/**
		 * Controller for the image. It is possible to use only one, I guess, but scaling
		 * and everything is rather difficult.
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

			m.preConcat(bitmapFragment.bitmap2norm);
			m.postConcat(bitmapFragment.norm2bitmap);

			m.postConcat(bitmap2view);

			return m;
		}

		/**
		 * Cancel entire action
		 */
		void cancel() {
			controller = null;
			isScrollEvent = false;
			setImageMatrix(viewMatrix());
		}

		/**
		 * Call this on the first finger-down.
		 * @param event
		 */
		void initDown(MotionEvent event) {
			int index = event.getActionIndex();
			int id = event.getPointerId(index);

			if(controller != null) {
				throw new IllegalStateException("controller not null");
			}

			// do not display the point-dragging view while dragging...
			// fixme interactiveView.setVisibility(View.INVISIBLE);

			PointF p = new PointF(event.getX(index), event.getY(index));

			controller = new MultiTouchController();
			controller.addPoint(id, norm(p));

			// isScrollEvent is false here!
		}

		/**
		 *
		 * @param event
		 * @return
		 */
		void finalUp(MotionEvent event) {
			if(!isScrollEvent) {
				cancel();
				return; // not a scrollevent
			}

			isScrollEvent = false;

			int index = event.getActionIndex();
			int id = event.getPointerId(index);

			controller.removePoint(id);

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
			controller = null;

			// the matrix of the image view will be updated
			// after we receive the first call to the update-method.

			// for now, we will set the latest view matrix.
			setImageMatrix(viewMatrix());
		}

		void down(MotionEvent event) {
			int index = event.getActionIndex();
			int id = event.getPointerId(index);

			PointF p = new PointF(event.getX(index), event.getY(index));
			controller.addPoint(id, norm(p));
		}

		void up(MotionEvent event) {
			int index = event.getActionIndex();
			int id = event.getPointerId(index);

			controller.removePoint(id);
		}

		void scroll(MotionEvent event) {
			isScrollEvent = true;

			int index = event.getActionIndex();

			for(index = 0; index < event.getPointerCount(); ++index) {
				PointF pos = new PointF(event.getX(index), event.getY(index));
				int id = event.getPointerId(index);

				controller.movePoint(id, norm(pos));
			}

			/*
			Original:
			m = controller.getMatrix();
			m.postConcat(currentImageMatrix);
			// ie, imagematrix = bitmap2view * m
			 */

			ScaleableImageView.this.setImageMatrix(viewMatrix());
			ScaleableImageView.this.invalidate();
		}
	}
}
