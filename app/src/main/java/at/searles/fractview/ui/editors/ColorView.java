package at.searles.fractview.ui.editors;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import at.searles.math.Commons;

public class ColorView extends View {

	static final float SQRT_3_4 = (float) Math.sqrt(0.75);

	public static interface ColorListener {
		void onColorChanged(int color);
	}

	// This view consists of three parts:
	// first, the color cube,
	// then two seek bars: vertical (white to black)
	// and horizontal: (transparent to opaque)
	final float dotRad = 10f;
	final int barWidth = 50;

	ColorCube cube;
	BrightnessBar bar;

	Paint dotPaint;

	float brightness;     // ranges from 0 to 1.
	// fixme float alpha;
	PointF cubeSelection; // ranges from -1 to 1 in both coordinates.

	int selectedTriangleIndex = -1;

	private int color;

	private ColorListener listener;

	// fixme add alpha bar?


	public ColorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayerType(View.LAYER_TYPE_SOFTWARE, null); // for composedshader

		cube = new ColorCube();
		bar = new BrightnessBar();

		this.dotPaint = new Paint();
		dotPaint.setStyle(Paint.Style.STROKE);
		dotPaint.setStrokeWidth(5f);

		cubeSelection = new PointF();

		touchController = new TouchController();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// thanks to http://stackoverflow.com/questions/12266899/onmeasure-custom-view-explanation

		// fixme constraints: right brightness bar must have dotRad on top and
		// bottom for the rectangle.

		// height of cube is 2*base + 2*dotRad

		int desiredHeight = 1024;

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
				width = height; // we use a square
			} else if(widthMode == MeasureSpec.AT_MOST) {
				if(widthSize < height) {
					width = height = widthSize; // height-mode is not 'exactly'
				} else { // height <= widthSize
					width = height;
				}
			}
		}

		// okay, we got width and height.
		// subtract width of bar from both

		float cubeX0 = dotRad;
		float cubeY0 = dotRad;
		float cubeX1 = width - barWidth - 2 * dotRad;  // keep space for brightness
		float cubeY1 = height - barWidth - 2 * dotRad; // keep space for alpha

		float x0 = (cubeX0 + cubeX1) / 2f;
		float y0 = (cubeY0 + cubeY1) / 2f;
		float base = Math.min(cubeX1 - cubeX0, cubeY1 - cubeY0) / 2f;

		// subtract dotRad from base so that you see the full circle
		this.cube.initLayout(base, x0, y0);
		this.bar.initLayout(width - barWidth, 0, width, height - barWidth);

		//MUST CALL THIS
		setMeasuredDimension(width, height);
	}

	public void setListener(ColorListener l) {
		this.listener = l;
	}

	@Override
	public void onDraw(Canvas canvas) {
		cube.draw(canvas);
		bar.draw(canvas);

		if(brightness > 0.7f) {
			// 0.7 because 1/sqrt(2) is the actual value (there was this science-youtube-view on
			// rgb color gradients)
			dotPaint.setColor(Color.BLACK);
		} else {
			dotPaint.setColor(Color.WHITE);
		}

		// fixme radius
		canvas.drawCircle(cubeSelection.x * cube.base + cube.x0, cubeSelection.y * cube.base + cube.y0, dotRad, dotPaint);

		float y = brightness * (bar.y0 - bar.y1) + bar.y1;
		canvas.drawRect(bar.x0, y - dotRad, bar.x1, y + dotRad, dotPaint);

		// draw triangles
		/*for(int i = 0; i < 6; ++i) {
			canvas.drawLine(x0, y0, (float) (x0 + 1000 * Math.sin(i * Math.PI / 3.)), (float) (y0 + 1000 * Math.cos(i * Math.PI / 3.)), dotPaint);
		}*/

	}

	// FIXME the following two methods should be inside TouchController

	/**
	 * This one first normalizes the selection according to the selected triangle.
	 * @param x
	 * @param y
     */
	void setCubeSelection(float x, float y) {
		cubeSelection.set((x - cube.x0) / cube.base, (y - cube.y0) / cube.base);

		// pick triangle
		if(selectedTriangleIndex == -1) {
			selectedTriangleIndex = cube.selectedTriangle(cubeSelection.x, cubeSelection.y);
		}

		// normalize x, y
		cube.normalize(cubeSelection, selectedTriangleIndex);

		// all values are fine, fetch color
		this.color = cube.color(cubeSelection, brightness);

		// update bar
		bar.initCubeSelection();

		// and background color
		setBackgroundColor(this.color);

		// and redraw.
		invalidate();

		// tell others what has happened.
		if(listener != null) listener.onColorChanged(color);
	}


	private void updateBarSelection(float y) {
		float d = 1f - (bar.y0 - y) / (bar.y0 - bar.y1);

		// clamp.
		this.brightness = d < 0f ? 0f : d > 1f ? 1f : d;

		cube.updateGradients();
		this.color = cube.color(cubeSelection, brightness);
		setBackgroundColor(color);
		invalidate();
		if(listener != null) listener.onColorChanged(color);
	}


	TouchController touchController;

	// pointer ids for cube element and bar element
	class TouchController {

		int cubeId = -1;
		int barId = -1;

		void down(int id, float x, float y) {
			Log.d("CV", "down: " + id + ", " + x + ":" + y);
			if(cube.inBounds(x, y) && cubeId == -1 && barId != id) {
				// check which triangle it is. The triangles are numbered clockwise.
				// 0 is up 12 to 2, 1 is from 2 to 4 etc...
				cubeId = id;
				setCubeSelection(x, y);
			} else if(bar.inBounds(x, y) && barId == -1 && cubeId != id) {
				barId = id;
				updateBarSelection(y);
			}
		}

		void move(int id, float x, float y) {
			// FIXME Check in which triangle I am
			Log.d("CV", "move: " + id + ", " + x + ":" + y);
			if(id == cubeId) {
				setCubeSelection(x, y);
			} else if(id == barId) {
				updateBarSelection(y);
			} else {
				// id is completely new, try whether down can do sth useful
				down(id, x, y);
			}
		}

		void up(int id, float x, float y) {
			Log.d("CV", "up: " + id + ", " + x + ":" + y);
			if(id == cubeId) {
				setCubeSelection(x, y);
				cubeId = -1;
				selectedTriangleIndex = -1;
			} else if(id == barId) {
				updateBarSelection(y);
				barId = -1;
			}
		}

		void cancel() {
			cubeId = barId = -1;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int index = event.getActionIndex();
		int action = event.getActionMasked();
		int id = event.getPointerId(index);

		switch(action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				touchController.down(id, event.getX(index), event.getY(index));
				return true;
			case MotionEvent.ACTION_MOVE:
				for(index = 0; index < event.getPointerCount(); ++index) {
					touchController.move(event.getPointerId(index), event.getX(index), event.getY(index));
				}
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				touchController.up(id, event.getX(index), event.getY(index));
				return true;
			case MotionEvent.ACTION_CANCEL:
				touchController.cancel();
			default:
				return super.onTouchEvent(event);
		}

		// fixme:
		// if down event, get area that is clicked to decide whether it is a seekbar the cube
		// if move event then based on id decide who will handle it
		// this way, multitouch is possible.

		//if(event.getAction() == MotionEvent.ACTION_DOWN) {
		// fixme: get id

		//


		//this.setBackgroundColor(cube.color(cubeSelection, brightness));
		//}
		//return true;
		//return super.onTouchEvent(event);
	}

	public void setColor(int color) {
		// we do not call the listener here.
		this.color = color;
		setBackgroundColor(this.color);

		float l = cube.colorCoords(color, cubeSelection);
		bar.initCubeSelection();

		// warning: l may be NaN
		if(!Float.isNaN(l)) {
			this.brightness = l;
			cube.updateGradients();
		}

		invalidate();
	}


	class BrightnessBar {
		Paint paintBW, paintColor;
		float x0, y0, x1, y1;

		BrightnessBar() {
			this.paintColor = new Paint();
			this.paintBW = new Paint();
		}

		void initLayout(float x0, float y0, float x1, float y1) {
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
			this.paintBW.setShader(new LinearGradient(
					x0, y0, x0, y1, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP));
			initCubeSelection();
		}

		void initCubeSelection() {
			int color0 = cube.color(cubeSelection, 1f);
			int color1 = cube.color(cubeSelection, 0f);

			this.paintColor.setShader(new LinearGradient(
					x0, y0, x0, y1, color0, color1, Shader.TileMode.CLAMP));
		}

		void draw(Canvas canvas) {
			float mx = (x0 + x1) / 2f;
			canvas.drawRect(mx, y0, x1, y1, paintBW);
			canvas.drawRect(x0, y0, mx, y1, paintColor);
		}

		boolean inBounds(float x, float y) {
			return x0 <= x && x <= x1 &&
					y0 <= y && y <= y1;
		}
	}

	class ColorCube {
		Paint paint;

		Path[] paths = new Path[6];
		ComposeShader[] shaders = new ComposeShader[6];

		float x0, y0;
		float height; // height is the distance from center to midpoint
		float base; // base is the radius (distance from center to corner)

		// points normalized:
		final float gx = 0f, gy = -1f;
		final float cx = SQRT_3_4, cy = -0.5f;
		final float bx = SQRT_3_4, by = 0.5f;
		final float mx = 0f, my = 1f;
		final float rx = -SQRT_3_4, ry = 0.5f;
		final float yx = -SQRT_3_4, yy = -0.5f;

		ColorCube() {
			this.paint = new Paint();
			paint.setStyle(Paint.Style.FILL);
		}

		// size of the cube is w: 2*height(base), h: 2*base
		void initLayout(float base, float x0, float y0) {
			// the smaller len is the one I use
			this.base = base;
			height = base * SQRT_3_4;
			this.x0 = x0;
			this.y0 = y0;

			for(int i = 0; i < 6; ++i) {
				paths[i] = new Path();
				paths[i].moveTo(x0, y0);
			}

			paths[0].lineTo(x0 + base * gx, y0 + base * gy);
			paths[1].lineTo(x0 + base * cx, y0 + base * cy);
			paths[2].lineTo(x0 + base * bx, y0 + base * by);
			paths[3].lineTo(x0 + base * mx, y0 + base * my);
			paths[4].lineTo(x0 + base * rx, y0 + base * ry);
			paths[5].lineTo(x0 + base * yx, y0 + base * yy);

			paths[0].lineTo(x0 + base * cx, y0 + base * cy);
			paths[1].lineTo(x0 + base * bx, y0 + base * by);
			paths[2].lineTo(x0 + base * mx, y0 + base * my);
			paths[3].lineTo(x0 + base * rx, y0 + base * ry);
			paths[4].lineTo(x0 + base * yx, y0 + base * yy);
			paths[5].lineTo(x0 + base * gx, y0 + base * gy);

			for(int i = 0; i < 6; ++i) {
				// and back.
				paths[i].lineTo(x0, y0);
			}

			updateGradients();
		}

		void updateGradients() {
			LinearGradient red;
			LinearGradient green;
			LinearGradient blue;

			int r = 0xffff0000;
			int g = 0xff00ff00;
			int b = 0xff0000ff;
			int black = 0xff000000;

			// fixme: does the next rounding cause problems?
			// fixme test by removing the webcolorEditText-mutual boolean.
			int c2 = Math.min(255, Math.max(0, (int) (255f * brightness)));
			int r2 = 0xff000000 | (c2 << 16);
			int g2 = 0xff000000 | (c2 << 8);
			int b2 = 0xff000000 | c2;

			float dd = 1f - brightness;

			// basic geometry: two gradients are always in the same direction while the other color varies
			// based on the brightness around an angle of 60 deg.

			// for the sake of readability, I normalize the color points here:
			float gx2 = x0 + base * gx, gy2 = y0 + base * gy;
			float cx2 = x0 + base * cx, cy2 = y0 + base * cy;
			float bx2 = x0 + base * bx, by2 = y0 + base * by;
			float mx2 = x0 + base * mx, my2 = y0 + base * my;
			float rx2 = x0 + base * rx, ry2 = y0 + base * ry;
			float yx2 = x0 + base * yx, yy2 = y0 + base * yy;

			// w: red and blue are same direction
			red = new LinearGradient(x0, y0, (rx2 + yx2) / 2f, (ry2 + yy2) / 2f, r2, r, Shader.TileMode.CLAMP);
			blue = new LinearGradient(x0, y0, (rx2 + yx2) / 2f, (ry2 + yy2) / 2f, b2, black, Shader.TileMode.CLAMP);

			// the geometry is rather complicated with this one.
			// connect x0/y0 with the corresponding green-value on the line y-r. => p.
			// the line X = n_p * string + r is the line at which the gradient for g reaches 0
			// while on X = n_p * string + y is the one where it is one.
			// it remains to find appropriate points.
			// here I pick y fixed.

			float px = yx2 * brightness + rx2 * dd;
			float py = yy2 * brightness + ry2 * dd;

			float vx = x0 - px; // vector perpendicular to gradient
			float vy = y0 - py;

			float aa =  vx * yx2 + vy * yy2; // line y -> direction gradient is vx * x + vy * y = aa
			float bb = -vy * rx2 + vx * ry2; // line r -> direction g=0 is    -vy * x + vx * y = bb

			// the intersection of both lines is the point I am looking for.

			float det = vx * vx + vy * vy;
			float x = (aa * vx - bb * vy) / det;
			float y = (vx * bb + vy * aa) / det;

			green = new LinearGradient(yx2, yy2, x, y, g, black, Shader.TileMode.CLAMP);

			shaders[4] = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

			red = new LinearGradient(x0, y0, (rx2 + mx2) / 2f, (ry2 + my2) / 2f, r2, r, Shader.TileMode.CLAMP);
			green = new LinearGradient(x0, y0, (rx2 + mx2) / 2f, (ry2 + my2) / 2f, g2, black, Shader.TileMode.CLAMP);

			px = mx2 * brightness + rx2 * dd;
			py = my2 * brightness + ry2 * dd;

			vx = x0 - px; vy = y0 - py;

			aa =  vx * mx2 + vy * my2;
			bb = -vy * rx2 + vx * ry2;

			det = vx * vx + vy * vy;
			x = (aa * vx - bb * vy) / det;
			y = (vx * bb + vy * aa) / det;

			blue = new LinearGradient(mx2, my2, x, y, b, black, Shader.TileMode.CLAMP);

			shaders[3] = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

			blue = new LinearGradient(x0, y0, (mx2 + bx2) / 2f, (my2 + by2) / 2f, b2, b, Shader.TileMode.CLAMP);
			green = new LinearGradient(x0, y0, (mx2 + bx2) / 2f, (my2 + by2) / 2f, g2, black, Shader.TileMode.CLAMP);

			px = mx2 * brightness + bx2 * dd;
			py = my2 * brightness + by2 * dd;

			vx = x0 - px; vy = y0 - py;

			aa =  vx * mx2 + vy * my2;
			bb = -vy * bx2 + vx * by2;

			det = vx * vx + vy * vy;
			x = (aa * vx - bb * vy) / det;
			y = (vx * bb + vy * aa) / det;

			red = new LinearGradient(mx2, my2, x, y, r, black, Shader.TileMode.CLAMP);

			shaders[2] = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

			blue = new LinearGradient(x0, y0, (cx2 + bx2) / 2f, (cy2 + by2) / 2f, b2, b, Shader.TileMode.CLAMP);
			red = new LinearGradient(x0, y0, (cx2 + bx2) / 2f, (cy2 + by2) / 2f, r2, black, Shader.TileMode.CLAMP);

			px = cx2 * brightness + bx2 * dd;
			py = cy2 * brightness + by2 * dd;

			vx = x0 - px; vy = y0 - py;

			aa =  vx * cx2 + vy * cy2;
			bb = -vy * bx2 + vx * by2;

			det = vx * vx + vy * vy;
			x = (aa * vx - bb * vy) / det;
			y = (vx * bb + vy * aa) / det;

			green = new LinearGradient(cx2, cy2, x, y, g, black, Shader.TileMode.CLAMP);

			shaders[1] = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

			green = new LinearGradient(x0, y0, (cx2 + gx2) / 2f, (cy2 + gy2) / 2f, g2, g, Shader.TileMode.CLAMP);
			red = new LinearGradient(x0, y0, (cx2 + gx2) / 2f, (cy2 + gy2) / 2f, r2, black, Shader.TileMode.CLAMP);

			px = cx2 * brightness + gx2 * dd;
			py = cy2 * brightness + gy2 * dd;

			vx = x0 - px; vy = y0 - py;

			aa =  vx * cx2 + vy * cy2;
			bb = -vy * gx2 + vx * gy2;

			det = vx * vx + vy * vy;
			x = (aa * vx - bb * vy) / det;
			y = (vx * bb + vy * aa) / det;

			blue = new LinearGradient(cx2, cy2, x, y, b, black, Shader.TileMode.CLAMP);

			shaders[0] = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

			green = new LinearGradient(x0, y0, (yx2 + gx2) / 2f, (yy2 + gy2) / 2f, g2, g, Shader.TileMode.CLAMP);
			blue = new LinearGradient(x0, y0, (yx2 + gx2) / 2f, (yy2 + gy2) / 2f, b2, black, Shader.TileMode.CLAMP);

			px = yx2 * brightness + gx2 * dd;
			py = yy2 * brightness + gy2 * dd;

			vx = x0 - px; vy = y0 - py;

			aa =  vx * yx2 + vy * yy2;
			bb = -vy * gx2 + vx * gy2;

			det = vx * vx + vy * vy;
			x = (aa * vx - bb * vy) / det;
			y = (vx * bb + vy * aa) / det;

			red = new LinearGradient(yx2, yy2, x, y, r, black, Shader.TileMode.CLAMP);

			shaders[5] = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);
		}

		void draw(Canvas canvas) {
			paint.setStyle(Paint.Style.FILL);

			for(int i = 0; i < 6; ++i) {
				paint.setShader(shaders[i]);
				canvas.drawPath(paths[i], paint);
				paint.setShader(null);

				if(selectedTriangleIndex != -1 && i != selectedTriangleIndex) {
					// in this case not selected triangles are drawn pale.
					paint.setColor(0x80808080);
					canvas.drawPath(paths[i], paint);
				}
			}
		}

		// called from outside to set the selected color
		// returns the brightness; NaN if brightness is the whole
		// range from 0 to 1.
		float colorCoords(int color, PointF point) {
			// fixme float alpha = Color.alpha(color) / 255f;
			float r = Color.red(color) / 255f;
			float g = Color.green(color) / 255f;
			float b = Color.blue(color) / 255f;

			// first, determine triangle
			// set some values etc...
			float x1, y1, x2, y2, d, e, l;

			// fixme make independent of concrete color-cube values!
			if(g >= b && b >= r) {
				// gc (green cyan-triangle)
				x1 = gx; y1 = gy; // gx = 0, gy =
				x2 = cx; y2 = cy;
				e = g - r;
				d = 1 - (b - r) / e;
				l = r / (1.f - e); // will be new brightness
			} else if(b >= g && g >= r) {
				// cb
				x1 = cx; y1 = cy;
				x2 = bx; y2 = by;
				e = b - r;
				d = (g - r) / e;
				l = r / (1.f - e); // will be new brightness
			} else if(b >= r && r >= g) {
				// bm
				x1 = bx; y1 = by;
				x2 = mx; y2 = my;
				e = b - g;
				d = 1 - (r - g) / e;
				l = g / (1.f - e); // will be new brightness
			} else if(r >= b && b >= g) {
				// mr
				x1 = mx; y1 = my;
				x2 = rx; y2 = ry;
				e = r - g;
				d = (b - g) / e;
				l = g / (1.f - e); // will be new brightness
			} else if(r >= g && g >= b) {
				// ry
				x1 = rx; y1 = ry;
				x2 = yx; y2 = yy;
				e = r - b;
				d = 1 - (g - b) / e;
				l = b / (1.f - e); // will be new brightness
			} else if(g >= r && r >= b) {
				// yg
				x1 = yx; y1 = yy;
				x2 = gx; y2 = gy;
				e = g - b;
				d = (r - b) / e;
				l = b / (1.f - e); // will be new brightness
			} else {
				throw new IllegalArgumentException("bug");
			}

			if(e <= 0.0) {
				// this means center
				// and now also x/y is easy
				point.set(0, 0);
			} else {
				// px/py is easy because of d
				float px = x1 * d + x2 * (1 - d);
				float py = y1 * d + y2 * (1 - d);

				if(e >= 1.0) {
					point.set(px, py);
				} else {
					point.set(
							px * e,
							py * e
					);
				}
			}

			return l;
		}

		/**
		 * Return the point that is closest to the triangle with index triangleIndex
		 * @param p the point
		 * @param triangleIndex if -1 then the best matching triangle for p is used.
         * @return p with normalized coordinates
         */
		void normalize(PointF p, int triangleIndex) {
			// first check
			if (triangleIndex == -1) triangleIndex = selectedTriangle(p.x, p.y);

			// the following are the corners of the triangle (the third is 0/0)
			float x1, y1, x2, y2;

			switch (triangleIndex) {
				case 0: { // ne
					x1 = gx;
					y1 = gy;
					x2 = cx;
					y2 = cy;
				}
				break;
				case 1: { // e
					x1 = cx;
					y1 = cy;
					x2 = bx;
					y2 = by;
				}
				break;
				case 2: { // se
					x1 = bx;
					y1 = by;
					x2 = mx;
					y2 = my;
				}
				break;
				case 3: { // sw
					x1 = mx;
					y1 = my;
					x2 = rx;
					y2 = ry;
				}
				break;
				case 4: { // w
					x1 = rx;
					y1 = ry;
					x2 = yx;
					y2 = yy;
				}
				break;
				case 5: { // nw
					x1 = yx;
					y1 = yy;
					x2 = gx;
					y2 = gy;
				}
				break;
				default:
					throw new IllegalArgumentException("BUG: bad triangle");
			}

			float[] q = Commons.norm(p.x, p.y, 0f, 0f, x1, y1, x2, y2, null);
			p.x = q[0];
			p.y = q[1];
		}


		/**
		 * Return color according to selection
		 * @param selection the selection. it should be inside the correct triangle.
		 * @param brightness brightness is set externally
         * @return
         */
		int color(PointF selection, float brightness) {
			// objective: Find color for given selection point.

			float x = selection.x;
			float y = selection.y;

			int index = selectedTriangleIndex == -1 ? selectedTriangle(x, y) : selectedTriangleIndex;

			// the following are the corners of the triangle (the third is 0/0)
			float x1, y1, x2, y2;

			switch(index) {
				case 0: { // ne
					x1 = gx; y1 = gy;
					x2 = cx; y2 = cy;
				} break;
				case 1: { // e
					x1 = cx; y1 = cy;
					x2 = bx; y2 = by;
				} break;
				case 2: { // se
					x1 = bx; y1 = by;
					x2 = mx; y2 = my;
				} break;
				case 3: { // sw
					x1 = mx; y1 = my;
					x2 = rx; y2 = ry;
				} break;
				case 4: { // w
					x1 = rx; y1 = ry;
					x2 = yx; y2 = yy;
				} break;
				case 5: { // nw
					x1 = yx; y1 = yy;
					x2 = gx; y2 = gy;
				} break;
				default: throw new IllegalArgumentException("bad triangle: " + x + ", " + y);
			}

			float det = (x1 - x2) * (-y) - (y1 - y2) * (-x);

			if (det == 0) {
				// it is some kind of gray
				// fixme check 256
				int color = Math.round(255 * brightness); // (int) Math.min(brightness * 256, 255);

				return Color.rgb(color, color, color);
			}

			// from https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection#Given_two_points_on_each_line
			// fixme put this into a MathUtility
			float d0 = (x1 * y2 - y1 * x2);
			//float d1 = 0f;//(zerox * y - zeroy * x);

			float px = -(d0 * x) / det;
			float py = -(d0 * y) / det;

			// get length(x1/y1 - px/py) / base (range 0 [close to x1/y1] to 1 [close to x2/y2])
			float dx = px - x1;
			float dy = py - y1;
			float d = (float) Math.sqrt(dx * dx + dy * dy);

			// get length(x0/y0 - x/y) / length(x0/y0 - px/py) (range 0 [close to center] to 1 [close to color of p])
			float len1 = (float) Math.sqrt(px * px + py * py);

			float len2 = (float) Math.sqrt(x * x + y * y);

			// if e > 1 then it is out of range. set e = 1
			float e = Math.min(1f, len2 / len1);

			// brightness is the rgb-value at x0/y0.
			float r = 0, g = 0, b = 0;

			switch(index) {
				case 0: // gc
					r = brightness * (1 - e);
					g = e + brightness * (1 - e);
					b = d * e + brightness * (1 - e);
					break;
				case 1: // cb
					r = brightness * (1 - e);
					g = (1 - d) * e + brightness * (1 - e);
					b = e + brightness * (1 - e);
					break;
				case 2: // bm
					r = d * e + brightness * (1 - e);
					g = brightness * (1 - e);
					b = e + brightness * (1 - e);
					break;
				case 3: // mr
					r = e + brightness * (1 - e);
					g = brightness * (1 - e);
					b = (1 - d) * e + brightness * (1 - e);
					break;
				case 4: // ry
					r = e + brightness * (1 - e);
					g = d * e + brightness * (1 - e);
					b = brightness * (1 - e);
					break;
				case 5: // yg
					r = (1 - d) * e + brightness * (1 - e);
					g = e + brightness * (1 - e);
					b = brightness * (1 - e);
					break;
				default:
					throw new IllegalArgumentException("bug: no such triangle index " + index);
			}

			// return color
			// fixme reconsider 256
			return Color.rgb(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
		}

		int selectedTriangle(float x, float y) {
			// triangle index 0 is g/c to 5=y/g
			float len = (float) Math.sqrt(x * x + y * y);

			if(len == 0.f) return 0;

			x /= len;
			y /= len;

			if(x > 0.f) {
				// from top it is base/2 - base - base/2
				// it is 0, 1 or
				if(y < -0.5f) return 0;
				else if(y < 0.5f) return 1;
				else return 2;
			} else {
				if(y < -0.5f) return 5;
				else if(y < 0.5f) return 4;
				else return 3;
			}
		}

		boolean inBounds(float x, float y) {
			return x0 - base * SQRT_3_4 <= x && x <= x0 + base * SQRT_3_4 &&
					y0 - base <= y && y <= y0 + base;
		}
	}

}