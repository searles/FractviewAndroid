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
import java.util.List;

import at.searles.fractview.Commons;
import at.searles.fractview.bitmap.ui.imageview.Plugin;
import at.searles.math.Scale;

public class ScalableImageView extends View {

	private static final int ROTATION_LOCK_MASK = 0x02;
	private static final int CONFIRM_ZOOM_MASK = 0x04;
	private static final int DEACTIVATE_ZOOM_MASK = 0x08;

	/**
	 * Scale factor on double tapping
	 */
	public static final float SCALE_ON_DOUBLE_TAB = 3f;

	private static final Paint BOUNDS_PAINT = boundsPaint();

	private static Paint boundsPaint() {
		Paint boundsPaint = new Paint();
		boundsPaint.setColor(0xaa000000); // semi-transparent black
		boundsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		return boundsPaint;
	}

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

	private List<Plugin> plugins;

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
	 * Image matrix. This one is modified according to the selection. Use this to convert
	 * Points from image to view.
     */
	private Matrix imageMatrix;

	/**
	 * Inverse of it.
	 */
	private Matrix inverseImageMatrix;

	private Bitmap bitmap;

	public ScalableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.plugins = new LinkedList<>();
		initTouch();
	}

	public float[] viewToBitmap(float[] screenPoint) {
		inverseImageMatrix.mapPoints(screenPoint);
		return screenPoint;
	}

	public float[] bitmapToView(float[] viewPoint) {
		imageMatrix.mapPoints(viewPoint);
		return viewPoint;
	}

	public void addPlugin(Plugin plugin) {
		plugins.add(plugin);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void setBitmap(Bitmap bitmap) {
		multitouch.cancel();
		this.bitmap = bitmap;
		requestLayout();
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

		// TODO Read https://trickyandroid.com/saving-android-view-state-correctly/

		ViewState vs = new ViewState(superState);
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

		setRotationLock(vs.rotationLock);
		setConfirmZoom(vs.confirmZoom);
		setDeactivateZoom(vs.deactivateZoom);
	}

	/**
	 * If the view measurements are the ones in the arguments, what would be the width?
	 * In case the image is flipped then this one returns the scaled height.
     */
	public float scaledBitmapWidth(float viewWidth, float viewHeight) {
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

	public float scaledBitmapHeight(float viewWidth, float viewHeight) {
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
	 * Returns true if the bitmap should be rotated by 90 degrees to maximize
	 * the filled fractal area.
     */
	public boolean flipBitmap(float viewWidth, float viewHeight) {
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

		float vw = MeasureSpec.getSize(widthMeasureSpec);
		float vh = MeasureSpec.getSize(heightMeasureSpec);

		if(bitmap == null) {
			setMeasuredDimension((int) vw, (int) vh);
			return;
		}

		float bw = bitmap.getWidth();
		float bh = bitmap.getHeight();

		if(vw > vh) {
			// fixme use matrices
			// if width of view is bigger, match longer side to it
			RectF viewRect = new RectF(0f, 0f, vw, vh);
			RectF bitmapRect = new RectF(0f, 0f, Math.max(bw, bh), Math.min(bw, bh));
			bitmap2view.setRectToRect(bitmapRect, viewRect, Matrix.ScaleToFit.CENTER);
		} else {
			// fixme use matrices
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

		setImageMatrix(multitouch.createViewMatrix());

		setMeasuredDimension((int) vw, (int) vh);
	}

	private void setImageMatrix(Matrix newImageMatrix) {
		if(this.inverseImageMatrix == null) {
			this.inverseImageMatrix = new Matrix();
		}

		if(!newImageMatrix.invert(this.inverseImageMatrix)) {
			// in this case, it is better to keep the old one.
			Log.e(getClass().getName(), "could not invert image matrix");
			return;
		}

		this.imageMatrix = newImageMatrix;
		invalidate();
	}

	@Override
	public void onDraw(@NotNull Canvas canvas) {
		// avoid crash if the image was not created yet.
		if(bitmap == null) {
			return;
		}

		if(imageMatrix == null) {
			Log.e(getClass().getName(), "image matrix is null in onDraw");
		}

		// draw image
		canvas.drawBitmap(bitmap, imageMatrix, null);

		// remove bounds
		float w = getWidth(), h = getHeight();

		float cx = w / 2.f;
		float cy = h / 2.f;

		float bw = scaledBitmapWidth(getWidth(), getHeight());
		float bh = scaledBitmapHeight(getWidth(), getHeight());

		// draw in total 4 transparent rectangles to indicate the drawing area
		canvas.drawRect(-1, -1, w, cy - bh / 2.f, BOUNDS_PAINT); // top
		canvas.drawRect(-1, -1, cx - bw / 2.f, h, BOUNDS_PAINT); // left
		canvas.drawRect(-1, cy + bh / 2.f, w, h, BOUNDS_PAINT);  // bottom
		canvas.drawRect(cx + bw / 2.f, -1, w, h, BOUNDS_PAINT);  // right

		// finally the plugins
		// XXX consider using reverse order
		for(Plugin plugin : plugins) {
			plugin.onDraw(canvas);
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
			Matrix l = lastScale.removeLast(); // FIXME what is this?
			// update the createViewMatrix.
			setImageMatrix(multitouch.createViewMatrix());
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
	}

	@Override
	public boolean onTouchEvent(@NotNull MotionEvent event) {
		for(Plugin plugin : plugins) {
			if(plugin.onTouchEvent(event)) {
				return true;
			}
		}

		// gesture detector handles scroll
		// no action without bitmap fragment.
		// or if deactivateZoom is set.
		if(deactivateZoom || bitmap == null) {
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
	 * Map point from view coordinates to screenToNormalized
	 * @param p Immutable point to modify
	 * @return returns the mapped point.
	 */
	public PointF screenToNormalized(PointF p) {
		// FIXME this is a bit overkill...
		float[] pts = new float[]{p.x, p.y};

		view2bitmap.mapPoints(pts); // fixme
		Commons.bitmap2norm(bitmap.getWidth(), bitmap.getHeight()).mapPoints(pts);

		return new PointF(pts[0], pts[1]);
	}

	public PointF normalizedToScreen(PointF p) {
		float[] pts = new float[]{p.x, p.y};

		Commons.norm2bitmap(bitmap.getWidth(), bitmap.getHeight()).mapPoints(pts);
		bitmap2view.mapPoints(pts); // FIXME replace

		return new PointF(pts[0], pts[1]);
	}

	public PointF normalizedToBitmap(PointF p) {
		// FIXME overkill.
		float[] pts = new float[]{p.x, p.y};

		Commons.norm2bitmap(bitmap.getWidth(), bitmap.getHeight()).mapPoints(pts);

		return new PointF(pts[0], pts[1]);
	}

	/**
	 * Transform the image using matrix n
	 * @param n
	 */
	void addScale(Matrix n) {
		Log.d("SIV", "Adding a new scale: " + n);
		// and use it for lastScale.
		lastScale.addFirst(n);

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
		combineLastScales(); // FIXME shouldn't this happen before scaleRelative???
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

			final PointF p = screenToNormalized(new PointF(event.getX(index), event.getY(index)));

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

		MultiTouchController controller = null;

		boolean isScrollEvent = false;

		/**
		 * returns the current view-matrix
		 */
		Matrix createViewMatrix() {
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
				ScalableImageView.this.setImageMatrix(createViewMatrix());
			}
		}

		/**
		 * Call this on the first finger-down.
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
				controller.addPoint(id, screenToNormalized(p));
			}
		}

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

		void confirm() {
			Log.d(getClass().getName(), "confirming touch event");

			if(controller == null) {
				return;
			}

			if(!controller.isDone()) {
				Log.e(getClass().getName(), "Controller not done: " + controller);
				// still continue.
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
			ScalableImageView.this.setImageMatrix(createViewMatrix());
		}

		void scroll(MotionEvent event) {
			isScrollEvent = controller != null;

			if(isScrollEvent) {
				for (int index = 0; index < event.getPointerCount(); ++index) {
					PointF pos = new PointF(event.getX(index), event.getY(index));
					int id = event.getPointerId(index);

					controller.movePoint(id, screenToNormalized(pos));
				}

				ScalableImageView.this.setImageMatrix(createViewMatrix());
			}
		}
	}

	/**
	 * To save the state of this view over rotation etc...
	 */
	static class ViewState extends BaseSavedState {

		// FIXME use a key-value pair, also to store data of plugins.

		/**
		 * Should the grid be shown or not?
		 */
		boolean rotationLock = false;
		boolean confirmZoom = false;
		boolean deactivateZoom = false;

		ViewState(Parcelable in) {
			super(in);
		}

		private ViewState(Parcel in) {
			super(in);

			int masked = in.readInt();

			this.rotationLock = (masked & ROTATION_LOCK_MASK) != 0;
			this.confirmZoom = (masked & CONFIRM_ZOOM_MASK) != 0;
			this.deactivateZoom = (masked & DEACTIVATE_ZOOM_MASK) != 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);

			int masked = 0;

			masked |= rotationLock ? ROTATION_LOCK_MASK : 0;
			masked |= confirmZoom ? CONFIRM_ZOOM_MASK : 0;
			masked |= deactivateZoom ? DEACTIVATE_ZOOM_MASK : 0;

			dest.writeInt(masked);
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

	public interface Listener {
		void scaleRelative(Scale m);
	}

}
