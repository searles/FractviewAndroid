package at.searles.fractview.ui.editors;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

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

	// fixme should the dot in the colorcube be actually drawn in the colorcube?
	// na. selection I handle in here.
	Paint dotPaint;

	// fixme: the following should be independent from the
	// actual size of the width.
	float brightness;     // ranges from 0 to 1.
	float alpha;
	PointF cubeSelection; // ranges from -1 to 1 in both coordinates.

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

	void updateSelectionPoint(float x, float y) {
		cubeSelection.set((x - cube.x0) / cube.base, (y - cube.y0) / cube.base);
		bar.initCubeSelection();
		this.color = cube.color(cubeSelection, brightness);
		setBackgroundColor(this.color);
		invalidate();
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
				cubeId = id;
				updateSelectionPoint(x, y);
			} else if(bar.inBounds(x, y) && barId == -1 && cubeId != id) {
				barId = id;
				updateBarSelection(y);
			}
		}

		void move(int id, float x, float y) {
			Log.d("CV", "move: " + id + ", " + x + ":" + y);
			if(id == cubeId) {
				updateSelectionPoint(x, y);
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
				updateSelectionPoint(x, y);
				cubeId = -1;
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

		Path w;
		Path nw;
		Path ne;
		Path e;
		Path se;
		Path sw;

		ComposeShader wShader;
		ComposeShader nwShader;
		ComposeShader neShader;
		ComposeShader eShader;
		ComposeShader seShader;
		ComposeShader swShader;

		float x0, y0;
		float height;
		float base;

		// points normalized:
		final float gx = 0f, gy = -1f;
		final float cx = SQRT_3_4, cy = -0.5f;
		final float bx = SQRT_3_4, by = 0.5f;
		final float mx = 0f, my = 1f;
		final float rx = -SQRT_3_4, ry = 0.5f;
		final float yx = -SQRT_3_4, yy = -0.5f;

		public ColorCube() {
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

			w = new Path();
			sw = new Path();
			se = new Path();
			e = new Path();
			ne = new Path();
			nw = new Path();

			w.moveTo(x0, y0);
			sw.moveTo(x0, y0);
			se.moveTo(x0, y0);
			e.moveTo(x0, y0);
			ne.moveTo(x0, y0);
			nw.moveTo(x0, y0);

			w.lineTo(x0 + base * rx, y0 + base * ry);
			sw.lineTo(x0 + base * mx, y0 + base * my);
			se.lineTo(x0 + base * bx, y0 + base * by);
			e.lineTo(x0 + base * cx, y0 + base * cy);
			ne.lineTo(x0 + base * gx, y0 + base * gy);
			nw.lineTo(x0 + base * yx, y0 + base * yy);

			w.lineTo(x0 + base * yx, y0 + base * yy);
			sw.lineTo(x0 + base * rx, y0 + base * ry);
			se.lineTo(x0 + base * mx, y0 + base * my);
			e.lineTo(x0 + base * bx, y0 + base * by);
			ne.lineTo(x0 + base * cx, y0 + base * cy);
			nw.lineTo(x0 + base * gx, y0 + base * gy);

			w.lineTo(x0, y0);
			sw.lineTo(x0, y0);
			se.lineTo(x0, y0);
			e.lineTo(x0, y0);
			ne.lineTo(x0, y0);
			nw.lineTo(x0, y0);

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

			wShader = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

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

			swShader = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

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

			seShader = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

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

			eShader = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

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

			neShader = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);

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

			nwShader = new ComposeShader(new ComposeShader(red, green, PorterDuff.Mode.ADD), blue, PorterDuff.Mode.ADD);
		}

		void draw(Canvas canvas) {
			paint.setStyle(Paint.Style.FILL);
			paint.setShader(wShader);
			canvas.drawPath(w, paint);
			paint.setShader(swShader);
			canvas.drawPath(sw, paint);
			paint.setShader(seShader);
			canvas.drawPath(se, paint);
			paint.setShader(eShader);
			canvas.drawPath(e, paint);
			paint.setShader(neShader);
			canvas.drawPath(ne, paint);
			paint.setShader(nwShader);
			canvas.drawPath(nw, paint);
		}

		// called from outside to set the selected color
		// returns the brightness; NaN if brightness is the whole
		// range from 0 to 1.
		public float colorCoords(int color, PointF point) {
			float alpha = Color.alpha(color) / 255f;
			float r = Color.red(color) / 255f;
			float g = Color.green(color) / 255f;
			float b = Color.blue(color) / 255f;

			// first, determine triangle
			// set some values etc...
			int index;

			float x1, y1, x2, y2, d, e, l;

			// fixme make independent of concrete color-cube values!
			if(g >= b && b >= r) {
				// gc (green cyan-triangle)
				index = 0;
				x1 = gx; y1 = gy; // gx = 0, gy =
				x2 = cx; y2 = cy;
				e = g - r;
				d = 1 - (b - r) / e;
				l = r / (1.f - e); // will be new brightness
			} else if(b >= g && g >= r) {
				// cb
				index = 1;
				x1 = cx; y1 = cy;
				x2 = bx; y2 = by;
				e = b - r;
				d = (g - r) / e;
				l = r / (1.f - e); // will be new brightness
			} else if(b >= r && r >= g) {
				// bm
				index = 2;
				x1 = bx; y1 = by;
				x2 = mx; y2 = my;
				e = b - g;
				d = 1 - (r - g) / e;
				l = g / (1.f - e); // will be new brightness
			} else if(r >= b && b >= g) {
				// mr
				index = 3;
				x1 = mx; y1 = my;
				x2 = rx; y2 = ry;
				e = r - g;
				d = (b - g) / e;
				l = g / (1.f - e); // will be new brightness
			} else if(r >= g && g >= b) {
				// ry
				index = 4;
				x1 = rx; y1 = ry;
				x2 = yx; y2 = yy;
				e = r - b;
				d = 1 - (g - b) / e;
				l = b / (1.f - e); // will be new brightness
			} else if(g >= r && r >= b) {
				// yg
				index = 5;
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


		// if out of range, selection is modified
		int color(PointF selection, float brightness) {
			// selection is normalized
			float x = selection.x;
			float y = selection.y;

			int index = selectedTriangle(x, y);

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

			float e = len2 / len1;

			// if e > 1 then it is out of range. set e = 1 and selection to px/py
			if(e > 1) {
				selection.x = px;
				selection.y = py;
				e = 1;
			}

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