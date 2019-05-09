package at.searles.fractview.palettes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import at.searles.math.color.ColorCommons;
import at.searles.math.color.Colors;

public class ColorView extends View {

	// fixme preserve state on rotation

	private final Triangle triangle;
	private final HueCircle circle;
	private float dotRadius;
	private final Paint dotPaint;

	private ArrayList<Listener> listeners;

	private float hue;

	// color is calculated as comp colorFraction + whiteFraction.
	// colorFraction is related to saturation while whiteFraction
	// corresponds to brightness. This model avoids divisions
	// and is easier to calculate since it directly corresponds to
	// barymetric coordinates in the triangle.

	private float colorFraction;
	private float whiteFraction;

	public ColorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayerType(View.LAYER_TYPE_SOFTWARE, null); // for composedshader

		this.listeners = new ArrayList<>();

		this.triangle = new Triangle();
		this.circle = new HueCircle();

		this.dotPaint = new Paint();
		dotPaint.setStyle(Paint.Style.STROKE);
		dotPaint.setColor(Color.BLACK); // FIXME Color
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// thanks to http://stackoverflow.com/questions/12266899/onmeasure-custom-view-explanation
		int desiredHeight = 1296; // fixme 2 inch

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int width = widthSize;
		int height = heightSize;

		//Measure Height
		if (heightMode == MeasureSpec.EXACTLY) {
			if(widthMode == MeasureSpec.UNSPECIFIED) {
				width = widthSize;
			} else if(widthMode == MeasureSpec.AT_MOST) {
				width = Math.min(height, widthSize);
			}
		} else {
			if (heightMode == MeasureSpec.AT_MOST) {
				height = Math.min(desiredHeight, heightSize);
			} else {
				height = desiredHeight;
			}

			if(widthMode == MeasureSpec.UNSPECIFIED) {
				//noinspection SuspiciousNameCombination
				width = height; // we use a square
			} else if(widthMode == MeasureSpec.AT_MOST) {
				if(widthSize < height) {
					//noinspection SuspiciousNameCombination
					width = height = widthSize; // height-mode is not 'exactly'
				} else { // height <= widthSize
					//noinspection SuspiciousNameCombination
					width = height;
				}
			}
		}

		float diameter = Math.min(width, height);
		this.circle.setRadius(diameter / 2.f, diameter * 2 / 5); // fixme
		this.triangle.setRadius(this.circle.innerRadius);

		this.dotRadius = diameter / 64f; // fixme
		this.dotPaint.setStrokeWidth(diameter / 120.f); // fixme

		//MUST CALL THIS
		setMeasuredDimension(width, height);
	}

	@Override
	public void onDraw(Canvas canvas) {
		circle.draw(canvas);
		triangle.draw(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// fixme assign pointer id to selection.
		int action = event.getActionMasked();

		for(int i = 0; i < event.getPointerCount(); ++i) {
			int id = event.getPointerId(i);
			switch(action) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_POINTER_DOWN:
					if(!circle.select(event.getX(i), event.getY(i), id)) {
						// both should be mutually exclusive
						triangle.select(event.getX(i), event.getY(i), id);
					}
					break;
				case MotionEvent.ACTION_MOVE:
					circle.dragTo(event.getX(i), event.getY(i), id);
					triangle.dragTo(event.getX(i), event.getY(i), id);
					break;
				case MotionEvent.ACTION_POINTER_UP:
				case MotionEvent.ACTION_UP:
					circle.unselect(id);
					triangle.unselect(id);
					break;
				default:
					super.onTouchEvent(event);
			}
		}

		return true; // fixme
	}

	public void setColor(int rgb) {
		float[] rgbf = Colors.int2rgb(rgb, new float[3]);

		this.hue = ColorCommons.hue(rgbf[0], rgbf[1], rgbf[2]);
		this.whiteFraction = Math.min(Math.min(rgbf[0], rgbf[1]), rgbf[2]);
		this.colorFraction = Math.max(Math.max(rgbf[0], rgbf[1]), rgbf[2]) - whiteFraction;

		triangle.hueUpdated();

		invalidate();
	}

	public int getColor() {
		float r = ColorCommons.red(hue);
		float g = ColorCommons.green(hue);
		float b = ColorCommons.blue(hue);

		r = r * colorFraction + whiteFraction;
		g = g * colorFraction + whiteFraction;
		b = b * colorFraction + whiteFraction;

		return Colors.rgb2int(r, g, b);
	}

	public void addColorEditedListener(Listener listener) {
		this.listeners.add(listener);
	}

	public void removeColorEditedListener(Listener listener) {
		this.listeners.remove(listener);
	}

	class HueCircle {
		float innerRadius;
		float outerRadius;

		private Paint huePaint;
		private Path hueCircle;
		private int selectedId = -1;

		void setRadius(float outerRadius, float innerRadius) {
			this.outerRadius = outerRadius;
			this.innerRadius = innerRadius;

			initGradients();

			invalidate();
		}

		boolean select(float x, float y, int id) {
			return setSelectedPoint(x, y, id, true);
		}

		boolean dragTo(float x, float y, int id) {
			return setSelectedPoint(x, y, id, false);
		}

		boolean unselect(int id) {
			if(selectedId != id) {
				return false;
			}

			selectedId = -1;
			return true;
		}

		boolean setSelectedPoint(float x, float y, int id, boolean init) {
			if(!init && selectedId != id) {
				return false;
			}

			float dx = (x - getWidth() / 2.f);
			float dy = (y - getHeight() / 2.f);

			if(init) {
				float rad2 = dx * dx + dy * dy;

				if(rad2 < innerRadius * innerRadius || outerRadius * outerRadius < rad2) {
					return false;
				}

				selectedId = id;
			}

			// selected is true, update hue.
			float h = (float) (Math.atan2(dy, dx) / (2 * Math.PI));

			hue = h < 0.f ? h + 1 : h;

			triangle.hueUpdated();
			invalidate();

			// hue was changed
			listeners.forEach(l -> l.onColorChanged(getColor()));

			return true;
		}

		void draw(Canvas canvas) {
			canvas.drawPath(hueCircle, huePaint);

			float x = (float) (Math.cos(hue * 2. * Math.PI) * (outerRadius + innerRadius) / 2.f);
			float y = (float) (Math.sin(hue * 2. * Math.PI) * (outerRadius + innerRadius) / 2.f);

			canvas.drawCircle(x + getWidth() / 2.f, y + getHeight() / 2.f, dotRadius, dotPaint);
		}

		void initGradients() {
			int[] colors = new int[ColorCommons.colorSegmentsCount() + 1];

			for(int i = 0; i < ColorCommons.colorSegmentsCount(); ++i) {
				float hue = ((float) i) / (float) ColorCommons.colorSegmentsCount();
				float r = ColorCommons.red(hue);
				float g = ColorCommons.green(hue);
				float b = ColorCommons.blue(hue);

				colors[i] = Colors.rgb2int(r, g, b);
			}

			colors[ColorCommons.colorSegmentsCount()] = colors[0];

			SweepGradient hueGradient = new SweepGradient(getWidth() / 2.f, getHeight() / 2.f, colors, null);

			huePaint = new Paint();
			huePaint.setStyle(Paint.Style.FILL);
			huePaint.setShader(hueGradient);

			hueCircle = new Path();
			hueCircle.addCircle(getWidth() / 2.f, getHeight() / 2.f, outerRadius, Path.Direction.CW);
			hueCircle.addCircle(getWidth() / 2.f, getHeight() / 2.f, innerRadius, Path.Direction.CW);
			hueCircle.setFillType(Path.FillType.EVEN_ODD);
		}
	}

	class Triangle {
		// the following coordinates are in pixels
		float centerX;
		float centerY;
		float radius;

		// The triangle
		float ax; // corner of color
		float ay;

		float bx; // corner of white
		float by;

		float cx; // corner of white
		float cy;

		Path path;
		Paint paint;
		int selectedId = -1;
		private int draggedCorner = -1;

		Triangle() {
			paint = new Paint();
			paint.setStyle(Paint.Style.FILL);

			path = new Path();
		}

		float x(float x) {
			return x * radius + centerX;
		}

		float y(float y) {
			return y * radius + centerY;
		}

		float ix(float x) {
			return (x - centerX) / radius;
		}

		float iy(float y) {
			return (y - centerY) / radius;
		}

		void draw(Canvas canvas) {
			canvas.drawPath(path, paint);

			float blackFraction = (1 - colorFraction - whiteFraction);
			float px = ax * colorFraction + bx * whiteFraction + cx * blackFraction;
			float py = ay * colorFraction + by * whiteFraction + cy * blackFraction;

			canvas.drawCircle(x(px), y(py), dotRadius, dotPaint);
		}

		boolean select(float x, float y, int id) {
			return setSelectedPoint(x, y, id, true);
		}

		void dragTo(float x, float y, int id) {
			setSelectedPoint(x, y, id, false);
		}

		void unselect(int id) {
			if(selectedId == id) {
				selectedId = -1;
			}
		}

		boolean setSelectedPoint(float vx, float vy, int id, boolean init) {
			if(!init && selectedId != id) {
				// id is not the finger that selected this triangle initially.
				return false;
			}

			float x = ix(vx);
			float y = iy(vy);

			float det = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy);

			float s = ((by - cy) * (x - cx) + (cx - bx) * (y - cy)) / det; // color
			float t = ((cy - ay) * (x - cx) + (ax - cx) * (y - cy)) / det; // white
			float u = 1 - s - t; // black

			if(init) {
				if(!(s >= 0 && t >= 0 && u >= 0)) {
					// outside triangle
					return false;
				}

				// inside triangle
				selectedId = id;
			}

			if(s >= 0 && t >= 0 && u >= 0) {
				// inside triangle
				draggedCorner = -1; // no dragged corner.
			}

			if(t <= 0 && u <= 0) {
				draggedCorner = 0; // A
				s = 1; t = 0; u = 0;
			}

			if(s <= 0 && u <= 0) {
				draggedCorner = 1; // B
				s = 0; t = 1; u = 0;
			}

			if(s <= 0 && t <= 0) {
				draggedCorner = 2; // C
				s = 0; t = 0; u = 1;
			}

			// now, at most one of them is negative

			// check if left/right of one side.
			if(u < 0) {
				s += u / 2.f;
				t += u / 2.f;
				u = 0; // for debugging
			}

			if(t < 0) {
				s += t / 2.f;
				u += t / 2.f;
				t = 0;
			}

			if(s < 0) {
				t += s / 2.f;
				u += s / 2.f;
				s = 0;
			}

			// set color coordinates
			colorFraction = Math.max(0.f, Math.min(1.f, s));
			whiteFraction = Math.max(0.f, Math.min(1.f, t));

			// is there a drag? If yes, update hue.
			if(draggedCorner != -1) { // A is dragged
				// adapt hue
				hue = (float) ((Math.atan2(y, x) + draggedCorner * Math.PI * 2. / 3.) / (2 * Math.PI));

				hue = (float) (hue - Math.floor(hue));

				hueUpdated();
			}

			// color was changed.
			listeners.forEach(l -> l.onColorChanged(getColor()));
			invalidate();

			return true;
		}

		/**
		 * Called to update the triangle to a new value of 'hue'
		 */
		void hueUpdated() {
			float sin120 = (float) (Math.sqrt(3.) / 2.);
			float cos120 = -0.5f;

			// corners of the triangle

			ax = (float) Math.cos(hue * 2 * Math.PI);
			ay = (float) Math.sin(hue * 2 * Math.PI);

			cx = ax * cos120 - ay * sin120;
			cy = ax * sin120 + ay * cos120;

			bx = cx * cos120 - cy * sin120;
			by = cx * sin120 + cy * cos120;

			updateGeometry();
		}

		void updateGeometry() {
			// Triangle
			path.rewind();

			path.moveTo(x(ax), y(ay));
			path.lineTo(x(bx), y(by));
			path.lineTo(x(cx), y(cy));
			path.close();

			// Color of triangle
			LinearGradient cbGradient = new LinearGradient(x(ax), y(ay), x(-ax / 2.f), y(-ay / 2.f), ColorCommons.color(hue), Color.BLACK, Shader.TileMode.CLAMP);
			LinearGradient wbGradient = new LinearGradient(x(bx), y(by), x(-bx / 2.f), y(-by / 2.f), Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);

			ComposeShader shader = new ComposeShader(cbGradient, wbGradient, PorterDuff.Mode.ADD);

			paint.setShader(shader);
		}
		void setRadius(float radius) {
			this.centerX = getWidth() / 2.f;
			this.centerY = getHeight() / 2.f;

			this.radius = radius;

			this.updateGeometry();
		}
	}

	public interface Listener {
		void onColorChanged(int color);
	}
}