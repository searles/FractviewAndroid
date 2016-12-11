package at.searles.fractview.ui;

public class InteractiveView /*extends View*/ {

	/*Map<String, PointF> points;
	public static final float RAD = 50.f;

	PointListener listener;

	Paint unselectedPaintStroke;
	Paint unselectedPaintFill;

	Paint selectedPaintStroke;
	Paint selectedPaintFill;

	public InteractiveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TouchListener l = new TouchListener();
		this.setOnTouchListener(l);

		points = new TreeMap<String, PointF>();

		selectedPaintStroke = new Paint();
		selectedPaintStroke.setColor(0xffffffff);
		selectedPaintStroke.setStyle(Paint.Style.STROKE);
		selectedPaintStroke.setStrokeWidth(4.f);

		selectedPaintFill = new Paint();
		selectedPaintFill.setColor(0x80676767);
		selectedPaintFill.setStyle(Paint.Style.FILL);

		unselectedPaintStroke = new Paint();
		unselectedPaintStroke.setColor(0x80ffffff);
		unselectedPaintStroke.setStyle(Paint.Style.STROKE);
		unselectedPaintStroke.setStrokeWidth(4.f);

		unselectedPaintFill = new Paint();
		unselectedPaintFill.setColor(0x80676767);
		unselectedPaintFill.setStyle(Paint.Style.FILL);

		listener = (PointListener) context;
	}

	public void setPoint(String label, PointF p) {
		points.put(label, p);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		for(Map.Entry<String, PointF> entry : points.entrySet()) {
			PointF p = entry.getValue();
			String label = entry.getKey();

			// FIXME: Show label

			if(draggedItem == entry) {
				canvas.drawCircle(p.x, p.y, RAD * 0.75f + 4, selectedPaintFill);
				canvas.drawCircle(p.x, p.y, RAD * 0.75f, selectedPaintStroke);
			} else {
				canvas.drawCircle(p.x, p.y, RAD * 0.5f + 2, unselectedPaintFill);
				canvas.drawCircle(p.x, p.y, RAD * 0.5f, unselectedPaintStroke);
			}
		}
	}

	// We only allow one point to be dragged at a time.
	Map.Entry<String, PointF> draggedItem;
	float dx, dy; // if it was grabbed a bit on the side keep the side

	boolean grabPoint(float x, float y) {
		if(draggedItem != null) {
			throw new IllegalStateException("old point was not released?");
		}

		// find closest point
		Map.Entry<String, PointF> closest = null;
		float closestDistance = Float.MAX_VALUE;

		for(Map.Entry<String, PointF> entry : points.entrySet()) {
			PointF p = entry.getValue();

			float dx = p.x - x;
			float dy = p.y - y;

			float d = dx * dx + dy * dy;

			if(closestDistance > d) {
				closestDistance = d;
				closest = entry;

				this.dx = dx;
				this.dy = dy;
			}
		}

		if(closestDistance <= RAD * RAD) {
			draggedItem = closest;
			invalidate();
			return true;
		}

		return false;
	}

	boolean releasePoint() {
		if(draggedItem != null) {
			// inform others of the update
			listener.pointMoved(draggedItem.getKey(), draggedItem.getValue());

			draggedItem = null;
			invalidate();

			return true;
		} else {
			return false;
		}
	}

	boolean movePoint(float x, float y) {
		if(draggedItem != null) {
			draggedItem.getValue().set(x + dx, y + dy);
			invalidate();
			return true;
		}

		return false;
	}

	public void clear() {
		points.clear();
	}

	class TouchListener implements View.OnTouchListener {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			int action = event.getActionMasked();

			switch(action) {
				case MotionEvent.ACTION_CANCEL: {
					releasePoint();
					return false;
				}
				case MotionEvent.ACTION_DOWN: {
					return event.getPointerId(0) == 0 && grabPoint(event.getX(), event.getY());
				}
				case MotionEvent.ACTION_UP: {
					return releasePoint();
				}
				case MotionEvent.ACTION_MOVE: {
					int index = event.getActionIndex();
					int id = event.getPointerId(index);

					return id == 0 && movePoint(event.getX(), event.getY());
				}
				default:
					Log.e("Unknown Action", event.toString());
					return false;
			}
		}
	}

	public static interface PointListener {
		void pointMoved(String label, PointF pos);
	}*/
}
