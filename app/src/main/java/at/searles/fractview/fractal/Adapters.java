package at.searles.fractview.fractal;

import android.os.Parcel;
import android.util.Log;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is rather a namespace that contains the Adapter-Interface plus some static adapters.
 * Preferred way is to extend Adapter.
 */
public class Adapters {

	private Adapters() { throw new IllegalArgumentException(); }

	public interface Adapter<A> {
		A fromParcel(Parcel parcel);
		void toParcel(A a, Parcel parcel, int flags);

		/**
		 * o must be an instance of one of the following
		 * (copy-paste fron JSONObject.put(String name, Object o)
		 * a JSONObject, JSONArray, String, Boolean, Integer, Long, Double, NULL, or null. May not be NaNs or infinities
		 * @param o
		 * @return
		 */
		A fromJSON(Object o) throws JSONException;
		Object toJSON(A a) throws JSONException;
	}

	public static final Adapter<String> stringAdapter = new Adapter<String>() {
		@Override
		public String fromParcel(Parcel parcel) {
			return parcel.readString();
		}

		@Override
		public void toParcel(String s, Parcel parcel, int flags) {
			parcel.writeString(s);

		}

		@Override
		public String fromJSON(Object o) throws JSONException {
			if(o instanceof String) return (String) o;
			throw new JSONException("not a string");
		}

		@Override
		public Object toJSON(String s) throws JSONException {
			return s;
		}
	};

	public static Adapter<Scale> scaleAdapter = new Adapter<Scale>() {
		// A scale consists of 6 doubles.
		// Format for Parcel: 6 doubles in a row.
		// Format for JSON: JSONArray with 6 doubles

		@Override
		public Scale fromParcel(Parcel parcel) {
			return new Scale(
					parcel.readDouble(),
					parcel.readDouble(),
					parcel.readDouble(),
					parcel.readDouble(),
					parcel.readDouble(),
					parcel.readDouble());
		}

		@Override
		public void toParcel(Scale scale, Parcel parcel, int flags) {
			parcel.writeDouble(scale.xx);
			parcel.writeDouble(scale.xy);
			parcel.writeDouble(scale.yx);
			parcel.writeDouble(scale.yy);
			parcel.writeDouble(scale.cx);
			parcel.writeDouble(scale.cy);
		}

		@Override
		public Scale fromJSON(Object o) throws JSONException {
			if(o instanceof JSONArray) {
				JSONArray arr = (JSONArray) o;
				return new Scale(
						arr.getDouble(0),
						arr.getDouble(1),
						arr.getDouble(2),
						arr.getDouble(3),
						arr.getDouble(4),
						arr.getDouble(5)
				);
			}

			throw new JSONException("JSONArray exprected");
		}

		@Override
		public Object toJSON(Scale scale) throws JSONException {
			return new JSONArray().put(scale.xx).put(scale.xy).put(scale.yx).put(scale.yy).put(scale.cx).put(scale.cy);
		}
	};



	public static final Adapter<Palette> paletteAdapter = new Adapter<Palette>() {
		// Format: Palette is a 2-dimensional array of values where
		// each row has the same amount of values.
		// Each color is represented by an RGB-Integer.
		// Format thus:
		// one integer "width" for number of colors per row,
		// one for "height" and then simply an array.
		// In parcel, width and height are the first two values of the array.
		@Override
		public Palette fromParcel(Parcel parcel) {
			int[] p = parcel.createIntArray();

			int w = p[0];
			int h = p[1];

			int[][] argbs = new int[h][w];

			for(int y = 0; y < h; ++y)
				for(int x = 0; x < w; ++x)
					argbs[y][x] = p[2 + y * w + x];

			return new Palette(argbs);
		}

		@Override
		public void toParcel(Palette p, Parcel parcel, int flags) {
			int[] paletteData = new int[2 + p.height() * p.width()];
			paletteData[0] = p.width();
			paletteData[1] = p.height();

			for(int y = 0; y < p.height(); ++y)
				for(int x = 0; x < p.width(); ++x)
					paletteData[2 + y * p.width() + x] = p.argb(x, y);

			parcel.writeIntArray(paletteData);
		}

		@Override
		public Palette fromJSON(Object o) throws JSONException {
			try {
				if (o instanceof JSONObject) {
					JSONObject obj = (JSONObject) o;
					int h = obj.getInt("height");
					int w = obj.getInt("width");

					int argbs[][] = new int[h][w];
					JSONArray data = obj.getJSONArray("colors");

					for (int y = 0; y < h; ++y)
						for (int x = 0; x < w; ++x) {
							argbs[y][x] = data.getInt(y * w + x);
						}

					return new Palette(argbs);
				}
			} catch(NumberFormatException e) {
				throw new JSONException("data-field must contain hex-numbers");
			}

			throw new JSONException("JSONObject expected");
		}

		@Override
		public Object toJSON(Palette palette) throws JSONException {
			JSONObject obj = new JSONObject();

			int h = palette.height();
			int w = palette.width();

			obj.put("height", h);
			obj.put("width", w);

			Log.d("Ad", h + " x " + w);

			JSONArray arr = new JSONArray();

			for (int y = 0; y < h; ++y)
				for (int x = 0; x < w; ++x) {
					arr.put(palette.argb(x, y));
				}

			obj.put("colors", arr);

			return obj;
		}
	};

	public static Adapter<Boolean> boolAdapter = new Adapter<Boolean>() {
		@Override
		public Boolean fromParcel(Parcel parcel) {
			return parcel.readInt() == 1;
		}

		@Override
		public void toParcel(Boolean value, Parcel parcel, int flags) {
			parcel.writeInt(value ? 1 : 0);
		}

		@Override
		public Boolean fromJSON(Object o) throws JSONException {
			if(o instanceof Boolean) return (Boolean) o;
			throw new JSONException("Boolean expected");
		}

		@Override
		public Object toJSON(Boolean b) throws JSONException {
			// this one's easy
			return b;
		}
	};

	public static final Adapter<Integer> intAdapter = new Adapter<Integer>() {
		@Override
		public Integer fromParcel(Parcel parcel) {
			return parcel.readInt();
		}

		@Override
		public void toParcel(Integer value, Parcel parcel, int flags) {
			parcel.writeInt(value);
		}


		@Override
		public Integer fromJSON(Object o) throws JSONException {
			if(o instanceof Integer) return (Integer) o;
			throw new JSONException("Integer expected");
		}

		@Override
		public Object toJSON(Integer i) throws JSONException {
			// this one's easy
			return i;
		}
	};
	
	public static final Adapter<Double> realAdapter = new Adapter<Double>() {
		@Override
		public Double fromParcel(Parcel parcel) {
			return parcel.readDouble();
		}

		@Override
		public void toParcel(Double value, Parcel parcel, int flags) {
			parcel.writeDouble(value);
		}

		@Override
		public Double fromJSON(Object o) throws JSONException {
			if(o instanceof Number) return ((Number) o).doubleValue();
			throw new JSONException("Double expected");
		}

		@Override
		public Object toJSON(Double d) throws JSONException {
			return d;
		}
	};

	public static final Adapter<Cplx> cplxAdapter = new Adapter<Cplx>() {
		// Parcel: Two doubles in a row.
		// JSon: Array with two doubles.

		@Override
		public Cplx fromParcel(Parcel parcel) {
			double re = parcel.readDouble();
			double im = parcel.readDouble();
			return new Cplx(re, im);
		}

		@Override
		public void toParcel(Cplx cplx, Parcel parcel, int flags) {
			parcel.writeDouble(cplx.re());
			parcel.writeDouble(cplx.im());
		}

		@Override
		public Cplx fromJSON(Object o) throws JSONException {
			if(o instanceof JSONArray) {
				JSONArray arr = (JSONArray) o;
				double re = arr.getDouble(0);
				double im = arr.getDouble(1);
				return new Cplx(re, im);
			}

			throw new JSONException("Array expected");
		}

		@Override
		public Object toJSON(Cplx c) throws JSONException {
			JSONArray arr = new JSONArray();
			arr.put(c.re());
			arr.put(c.im());
			return arr;
		}
	};

	/*public static <A> Adapter<ListMap<A>> listMapAdapter(final Adapter<A> adapter) {
	// FIXME Do I need ListMaps?
		return new Adapter<ListMap<A>>() {
			@Override
			public ListMap<A> fromParcel(Parcel parcel) {
				int size = parcel.readInt();

				ListMap<A> lm = new ListMap<A>();

				for(int i = 0; i < size; ++i) {
					String label = parcel.readString();
					A a = adapter.fromParcel(parcel);

					lm.add(label, a);
				}

				return lm;
			}

			@Override
			public void toParcel(ListMap<A> l, Parcel parcel, int flags) {
				parcel.writeInt(l.size()); // size

				for(String label : l) {
					parcel.writeString(label);
					adapter.toParcel(l.get(label), parcel, flags);
				}
			}

			@Override
			public ListMap<A> fromJSON(Object o) throws JSONException {
				// Order is important, hence, this is an array
				return null;
			}

			@Override
			public Object toJSON(ListMap<A> strings) throws JSONException {
				return null;
			}
		};
	};*/

	public static <A> Adapter<Map<String, A>> mapAdapter(final Adapter<A> adapter) {
		return new Adapter<Map<String, A>>() {
			@Override
			public Map<String, A> fromParcel(Parcel parcel) {
				int size = parcel.readInt();

				Map<String, A> map = new HashMap<>();

				for(int i = 0; i < size; ++i) {
					String label = parcel.readString();
					A a = adapter.fromParcel(parcel);

					map.put(label, a);
				}

				return map;
			}

			@Override
			public void toParcel(Map<String, A> m, Parcel parcel, int flags) {
				parcel.writeInt(m.size());

				for(Map.Entry<String, A> entry : m.entrySet()) {
					parcel.writeString(entry.getKey());
					adapter.toParcel(entry.getValue(), parcel, flags);
				}
			}

			@Override
			public Map<String, A> fromJSON(Object o) throws JSONException {
				// this one is easy
				if(o instanceof JSONObject) {
					Map<String, A> map = new HashMap<>();

					JSONObject obj = (JSONObject) o;

					Iterator<String> keys = obj.keys();

					while(keys.hasNext()) {
						String key = keys.next();
						A value = adapter.fromJSON(obj.get(key));

						map.put(key, value);
					}

					return map;
				}

				throw new JSONException("JSONObject expected");
			}

			@Override
			public Object toJSON(Map<String, A> map) throws JSONException {
				JSONObject obj = new JSONObject();

				for(Map.Entry<String, A> entry : map.entrySet())
					obj.put(entry.getKey(), adapter.toJSON(entry.getValue()));

				return obj;
			}
		};
	};
}
