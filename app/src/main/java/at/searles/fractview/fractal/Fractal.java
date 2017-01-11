package at.searles.fractview.fractal;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.*;
import at.searles.parsing.ParsingError;
import at.searles.utils.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// FIXME
// FIXME
// FIXME
// FIXME
// FIXME Type checks. What happens if I change the source code
// FIXME of an argument that is already set?
// FIXME Parameters should always (!) be aligned with default as soon as
// FIXME it is bound to a Fractal.
// FIXME Figure out a way how to do that best.
/*
Idea 1:
In 'get' of fractal/parameter, ignore parameter if
its type is different from the one in DefaultParameter.
 */

public class Fractal implements Parcelable {

	/**
	 * Types of parameters. Scale is a special case because
	 * it should not be part of Parameters.
	 */
	public enum Type { Int, Real, Cplx, Bool, Expr, Color, Palette, Scale };

	public static class DefaultParameters implements ExternalData, Iterable<String> {

		/**
		 * Default data in the order in which they are added.
		 */
		private LinkedHashMap<String, Pair<Type, Object>> elements;

		/**
		 * Should be only constructed when I compile a program.
		 */
		DefaultParameters() {
			elements = new LinkedHashMap<>();
		}

		public Iterator<String> iterator() {
			return elements.keySet().iterator();
		}

		public Pair<Type, Object> get(String id) {
			return elements.get(id);
		}

		@Override
		public void add(String id, String type, Tree init) throws CompileException {
			switch(type) {
				case "int": {
					int i;
					if(init instanceof Value.Int) {
						i = ((Value.Int) init).value;
					} else if(init instanceof Value.Real) {
						// fixme this is a fix for a bug.
						i = (int) ((Value.Real) init).value;
						Log.e("FRACTALS", "An int was parsed as a real");
					} else {
						throw new CompileException("extern " + id + " = " + init + " is not an int but a " + init.getClass() + "!");
					}

					elements.put(id, new Pair<>(Type.Int, i));
				} break;
				case "real": {
					double d;
					if(init instanceof Value.Int) {
						d = ((Value.Int) init).value;
					} else if(init instanceof Value.Real) {
						d = ((Value.Real) init).value;
					} else {
						throw new CompileException("extern " + id + " = " + init + " is not a real but a " + init.getClass() + "!");
					}

					elements.put(id, new Pair<>(Type.Real, d));
				} break;
				case "cplx": {
					Cplx c;
					if(init instanceof Value.Int) {
						c = new Cplx(((Value.Int) init).value);
					} else if(init instanceof Value.Real) {
						c = new Cplx(((Value.Real) init).value);
					} else if(init instanceof Value.CplxVal) {
						c = ((Value.CplxVal) init).value;
					} else {
						throw new CompileException("extern " + id + " = " + init + " is not a cplx but a " + init.getClass() + "!");
					}

					elements.put(id, new Pair<>(Type.Cplx, c));
				} break;
				case "bool": {
					if(init instanceof Value.Bool) {
						elements.put(id, new Pair<>(Type.Bool, ((Value.Bool) init).value));
					} else {
						throw new CompileException("extern " + id + " = " + init + " is not a bool but a " + init.getClass() + "!");
					}
				} break;
				case "expr": {
					if(init instanceof Value.StringVal) {
						elements.put(id, new Pair<>(Type.Expr, ((Value.StringVal) init).value));
					} else {
						throw new CompileException("extern " + id + " = " + init + " is not an expr but a " + init.getClass() + "!");
					}
				} break;
				case "color": {
					// again, this one is an integer
					if(init instanceof Value.Int) {
						elements.put(id, new Pair<>(Type.Color, ((Value.Int) init).value));
					} else {
						throw new CompileException("extern " + id + " = " + init + " is not a color but a " + init.getClass() + "!");
					}
				} break;
				case "palette": {
					// this one is a bit different
					elements.put(id, new Pair<>(Type.Palette, toPalette(init)));
				} break;
				default:
					throw new CompileException("Unknown extern type: " + type + " for id " + id);
			}
		}

		private static Palette toPalette(Object t) throws CompileException {
			if(t == null) {
				throw new CompileException("Missing list value");
			}

			LinkedList<List<Integer>> p = new LinkedList<List<Integer>>();

			int w = 0, h = 0;

			if(t instanceof Tree.Vec) {
				for(Tree arg : (Tree.Vec) t) {
					List<Integer> row = new LinkedList<Integer>();

					if(arg instanceof Tree.Vec) {
						for(Tree item : (Tree.Vec) arg) {
							if(item instanceof Value.Int) {
								row.add(((Value.Int) item).value);
							} else {
								throw new CompileException("int was expected here");
							}
						}
					} else if(arg instanceof Value.Int) {
						row.add(((Value.Int) arg).value);
					} else {
						throw new CompileException("int was expected here");
					}

					if(row.isEmpty()) {
						throw new CompileException("no empty row allowed in palette");
					}

					if(w < row.size()) w = row.size();


					p.add(row);
					h++;
				}
			} else if(t instanceof Value.Int) {
				w = h = 1;
				p.add(Collections.singletonList(((Value.Int) t).value));
			}

			// p now contains lists of lists, h and w contain width and height.
			int[][] argbs = new int[h][w];

			int y = 0;

			for(List<Integer> row : p) {
				int x = 0;
				while(x < w) {
					// we circle around if a row is incomplete.
					for(int rgb : row) {
						argbs[y][x] = rgb;
						x++;
						if(x >= w) break;
					}
				}
				y++;
			}

			return new Palette(argbs);
		}
	}

	public static class Parameters implements Parcelable, Iterable<String> {
		private HashMap<String, Pair<Type, Object>> elements;

		public Parameters() {
			elements = new HashMap<>();
		}

		Parameters(Parcel in) {
			this();

			int size = in.readInt(); // number of elements.

			for(int i = 0; i < size; ++i) {
				String id = in.readString();
				int typeIndex = in.readInt();

				Log.d("Parcel Fractal", id + " and " + typeIndex);

				Type type = Type.values()[typeIndex];

				switch(type) {
					case Int:
						add(id, Type.Int, in.readInt());
						break;
					case Real:
						add(id, Type.Real, in.readDouble());
						break;
					case Cplx:
						add(id, Type.Cplx, Adapters.cplxAdapter.fromParcel(in));
						break;
					case Bool:
						// true is 1, false is 0.
						add(id, Type.Bool, in.readInt() != 0);
						break;
					case Expr:
						add(id, Type.Expr, in.readString());
						break;
					case Color:
						add(id, Type.Color, in.readInt());
						break;
					case Palette:
						add(id, Type.Palette, Adapters.paletteAdapter.fromParcel(in));
						break;
				}
			}
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(elements.size()); // first, size
			for(Map.Entry<String, Pair<Type, Object>> entry : elements.entrySet()) {
				// then all elements.

				// first title
				String title = entry.getKey();
				dest.writeString(title);

				Type type = entry.getValue().a;
				// next type
				dest.writeInt(type.ordinal());

				Object v = entry.getValue().b;

				// finally, element.
				switch(entry.getValue().a) {
					case Int:
						dest.writeInt((Integer) v);
						break;
					case Real:
						dest.writeDouble((Double) v);
						break;
					case Cplx:
						Adapters.cplxAdapter.toParcel((Cplx) v, dest, flags);
						break;
					case Bool:
						dest.writeInt((Boolean) v ? 1 : 0);
						break;
					case Expr:
						dest.writeString((String) v);
						break;
					case Color:
						dest.writeInt((Integer) v);
						break;
					case Palette:
						Adapters.paletteAdapter.toParcel((Palette) v, dest, flags);
						break;
				}

			}
		}

		/**
		 * returns an element of this parameters field. If none exists fetches
		 * from defaults.
		 * @param id The id of the parameter
		 * @param defaults Default Parameters for type definitions and default values.
         * @return The value of this parameter (if it is defined and its type matches the one in defaults),
		 * otherwise the one of defaults. null, if it does not exist.
         */
		public Pair<Type, Object> get(String id, DefaultParameters defaults) {
			Pair<Type, Object> value = defaults.get(id);

			if(value == null) return null;
			
			Pair<Type, Object> p = elements.get(id);
			if(p != null && p.a == value.a) {
				return p;
			} else {
				return value;
			}
		}

		/**
		 * If it does not exist, nothing will happen. Otherwise
		 * it is removed which corresponds to a "reset to default".
		 * @param id
		 */
		public Parameters remove(String id) {
			elements.remove(id);
			return this;
		}

		public Parameters add(String id, Type type, Object o) {
			elements.put(id, new Pair<>(type, o));
			return this;
		}

		public boolean isEmpty() {
			return elements.isEmpty();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final Creator<Parameters> CREATOR = new Creator<Parameters>() {
			@Override
			public Parameters createFromParcel(Parcel in) {
				return new Parameters(in);
			}

			@Override
			public Parameters[] newArray(int size) {
				return new Parameters[size];
			}
		};

		public JSONObject toJSON() throws JSONException {
			// this is a relict from before when these classes were a bit more complex.
			// Still on a plus side, it is a space saving way to group this in sections according to type.
			JSONObject ints = new JSONObject();
			JSONObject reals = new JSONObject();
			JSONObject cplxs = new JSONObject();
			JSONObject bools = new JSONObject();
			JSONObject exprs = new JSONObject();
			JSONObject colors = new JSONObject();
			JSONObject palettes = new JSONObject();

			for(Map.Entry<String, Pair<Type, Object>> entry : elements.entrySet()) {
				String id = entry.getKey();

				Object v = entry.getValue().b;

				switch(entry.getValue().a) {
					case Int:
						ints.put(id, (Integer) v);
						break;
					case Real:
						reals.put(id, (Double) v);
						break;
					case Cplx:
						cplxs.put(id, Adapters.cplxAdapter.toJSON((Cplx) v));
						break;
					case Bool:
						bools.put(id, (Boolean) v);
						break;
					case Expr:
						exprs.put(id, (String) v);
						break;
					case Color:
						colors.put(id, (Integer) v);
						break;
					case Palette:
						palettes.put(id, Adapters.paletteAdapter.toJSON((Palette) v));
						break;
				}
			}

			JSONObject obj = new JSONObject();

			if(ints.length() != 0) obj.put("ints", ints);
			if(reals.length() != 0) obj.put("reals", reals);
			if(cplxs.length() != 0) obj.put("cplxs", cplxs);
			if(exprs.length() != 0) obj.put("exprs", exprs);
			if(bools.length() != 0) obj.put("bools", bools);
			if(colors.length() != 0) obj.put("colors", colors);
			if(palettes.length() != 0) obj.put("palettes", palettes);

			return obj;
		}

		public static Parameters fromJSON(Object o) throws JSONException {
			if(o instanceof JSONObject) {
				JSONObject obj = (JSONObject) o;

				JSONObject exprs = (JSONObject) obj.opt("exprs");
				JSONObject bools = (JSONObject) obj.opt("bools");
				JSONObject ints = (JSONObject) obj.opt("ints");
				JSONObject reals = (JSONObject) obj.opt("reals");
				JSONObject cplxs = (JSONObject) obj.opt("cplxs");
				JSONObject colors = (JSONObject) obj.opt("colors");
				JSONObject palettes = (JSONObject) obj.opt("palettes");

				Parameters p = new Parameters();

				if(ints != null) {
					for(Iterator<String> i = ints.keys(); i.hasNext(); ) {
						String id = i.next();
						p.add(id, Type.Int, ints.get(id));
					}
				}

				if(reals != null) {
					for(Iterator<String> i = reals.keys(); i.hasNext(); ) {
						String id = i.next();
						p.add(id, Type.Real, reals.get(id));
					}
				}

				if(cplxs != null) {
					for(Iterator<String> i = cplxs.keys(); i.hasNext(); ) {
						String id = i.next();
						p.add(id, Type.Cplx, cplxs.get(id));
					}
				}

				if(exprs != null) {
					for(Iterator<String> i = exprs.keys(); i.hasNext(); ) {
						String id = i.next();
						p.add(id, Type.Expr, exprs.get(id));
					}
				}

				if(bools != null) {
					for(Iterator<String> i = bools.keys(); i.hasNext(); ) {
						String id = i.next();
						p.add(id, Type.Bool, bools.get(id));
					}
				}

				if(colors != null) {
					for(Iterator<String> i = colors.keys(); i.hasNext(); ) {
						String id = i.next();
						p.add(id, Type.Color, colors.get(id));
					}
				}

				if(palettes != null) {
					for(Iterator<String> i = palettes.keys(); i.hasNext(); ) {
						String id = i.next();
						p.add(id, Type.Palette, palettes.get(id));
					}
				}

				return p;
			}

			throw new JSONException("not a JSONObject");		}

		@Override
		public Iterator<String> iterator() {
			return elements.keySet().iterator();
		}

		/**
		 * Returns a new Parameter object that is a merged one of
		 * other and this. If there is an element in both of them, then
		 * the one of 'this' is used.
		 * @param other the other Parameters
         * @return a new object
         */
		public Parameters merge(Parameters other) {
			Parameters merged = new Parameters();
			merged.elements.putAll(other.elements);
			merged.elements.putAll(this.elements);

			return merged;
		}
	}

	/**
	 * Scale of this fractal
	 */
	private Scale scale;

	/**
	 * Source code of the program
	 */
	private String sourceCode;

	/**
	 * Non-default parameters
	 */
	private Parameters parameters;

	/**
	 * Data that are fetched during compilation
	 */
	private DefaultParameters defaults = null; // generated

	/**
	 * Abstract Syntax Tree
	 */
	private Tree ast = null; // generated

	/**
	 * Result of compiling it.
	 */
	private int[] code = null; // generated

	/**
	 * Simple constructor
	 * @param scale
	 * @param sourceCode
	 * @param parameters
	 */
	public Fractal(Scale scale, String sourceCode, Parameters parameters) {
		if(scale == null || sourceCode == null || parameters == null) {
			throw new NullPointerException();
		}

		this.scale = scale;
		this.sourceCode = sourceCode;
		this.parameters = parameters;
	}

	public Parameters parameters() {
		return parameters;
	}

	public void resetAll() {
		resetScale();
		parameters.elements.clear();
	}

	public void reset(String id) {
		parameters.remove(id);
	}

	public void resetScale() {
		setScale(PresetFractals.INIT_SCALE);
	}

	public Pair<Type, Object> get(String labelId) {
		if(defaults == null) throw new NullPointerException("fractal was not parsed");
		return parameters.get(labelId, defaults);
	}

	public void setInt(String labelId, int i) {
		parameters.add(labelId, Type.Int, i);
	}

	public void setReal(String labelId, double d) {
		parameters.add(labelId, Type.Real, d);
	}

	public void setCplx(String labelId, Cplx c) {
		parameters.add(labelId, Type.Cplx, c);
	}

	public void setBool(String labelId, boolean b) {
		parameters.add(labelId, Type.Bool, b);
	}

	public void setExpr(String labelId, String expr) {
		parameters.add(labelId, Type.Expr, expr);
	}

	public void setColor(String labelId, int color) {
		parameters.add(labelId, Type.Color, color);
	}

	public void setPalette(String labelId, Palette p) {
		parameters.add(labelId, Type.Palette, p);
	}

	/**
	 * Parses the source code and on the way collects the extern
	 * arguments.
	 * @throws ParsingError
	 * @throws CompileException
	 */
	public void parse() throws ParsingError, CompileException {
		defaults = new DefaultParameters();
		ast = Meelan.parse(sourceCode, defaults);
		Log.d("FR", ast.toString());
	}

	/**
	 * Compiles the source code using parameters.
	 * @throws ParsingError
	 * @throws CompileException
	 */
	public void compile() throws ParsingError, CompileException {
		if(defaults == null) parse();

		ScopeTable table = new ScopeTable();

		// For palettes there are some treats here:
		// A palette is actually a function with one complex argument.
		Tree.Id xy = new Tree.Id("__xy");

		int paletteIndex = 0; // and we count the number of them.

		for(String id : defaults) {
			Pair<Type, Object> element = parameters.get(id, defaults);

			switch(element.a) {
				case Int:
					table.addDef(id, new Value.Int((Integer) element.b));
					break;
				case Real:
					table.addDef(id, new Value.Real((Double) element.b));
					break;
				case Cplx:
					table.addDef(id, new Value.CplxVal((Cplx) element.b));
					break;
				case Bool:
					table.addDef(id, new Value.Bool((Boolean) element.b));
					break;
				case Expr:
					try {
						// extern values must be treated special
						// unless they are constants.
						table.addExtern(id, Meelan.parse((String) element.b, null));
					} catch(ParsingError e) {
						throw new CompileException(e.getMessage());
					}
					break;
				case Color:
					table.addDef(id, new Value.Int((Integer) element.b));
					break;
				case Palette:
					// FIXME I guess they don't work now...
					// Palettes are special
					// A palette is actually a function with one complex argument.
					// this one defines a function
					Tree.FuncDef fn = new Tree.FuncDef("palette_" + id, Collections.singletonList("__xy"),
							Op.__ld_palette.eval(Arrays.asList(new Value.Label(paletteIndex), xy)));

					table.addDef(id, fn);
					paletteIndex++;

					break;
			}
		}

		this.code = Meelan.compile(ast, table);
	}

	/**
	 * Factory method from JSON
	 * @param o the json object
	 * @return a valid fractal
	 * @throws JSONException if something was wrong (like json not containing sth)
     */
	public static Fractal fromJSON(Object o) throws JSONException {
		if(o instanceof JSONObject) {
			JSONObject obj = (JSONObject) o;

			// throws an exception if this is not a scale or the field does not exist.
			Scale scale = Adapters.scaleAdapter.fromJSON(obj.get("scale"));

			//
			JSONArray arr = obj.getJSONArray("source");

			StringBuilder sb = new StringBuilder();
			int len = arr.length();

			sb.append(arr.getString(0));

			for(int i = 1; i < len; i++) {
				sb.append("\n").append(arr.getString(i));
			}

			String sourceCode = sb.toString();

			Object optArgs = obj.opt("arguments");

			Parameters parameters = optArgs == null ? new Parameters() : Parameters.fromJSON(optArgs);

			return new Fractal(scale, sourceCode, parameters);
		}

		throw new JSONException("not a JSONObject");
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject obj = new JSONObject();

		obj.put("scale", Adapters.scaleAdapter.toJSON(scale));

		// For better readability, the source code is split up into its lines.
		String[] lines = sourceCode.split("\n");
		JSONArray arr = new JSONArray(Arrays.asList(lines));
		obj.put("source", arr);

		if(!parameters.isEmpty()) obj.put("arguments", parameters.toJSON());

		return obj;
	}

	/**
	 * Returns true if the default value is used for the specific id.
	 * @param id
	 * @return
     */
	public boolean isDefaultValue(String id) {
		return !parameters.elements.containsKey(id);
	}

	/**
	 * Create a new instance of this class with a different source code
	 * @param newSourceCode the new source code
	 * @param reuseArguments if true, old arguments are reused.
	 * @return fractal with the new source code
	 */
	public Fractal copyNewSource(String newSourceCode, boolean reuseArguments) {
		// This one requires complete new compiling
		return new Fractal(scale, newSourceCode, reuseArguments ? parameters : new Parameters());
	}

	public Fractal copyNewScale(Scale newScale) {
		Fractal f = new Fractal(newScale, sourceCode, parameters);

		// take over generated stuff.
		f.defaults = defaults; f.ast = ast; f.code = code;

		return f;
	}

	/**
	 * @return An iterable of all ids of arguments
     */
	public Iterable<String> parameterIds() {
		return defaults.elements.keySet();
	}



	// some mutable methods
	public Scale scale() {
		return scale;
	}

	public void setScale(Scale scale) {
		this.scale = scale;
	}

	/**
	 * Since palettes must be transferred directly, convenience method.
	 * @return
	 */
	public List<Palette> palettes() {
		// Collect all palettes
		LinkedList<Palette> list = new LinkedList<>();

		for(String id : defaults) {
			Pair<Type, Object> element = parameters.get(id, defaults);
			if(element.a == Type.Palette) {
				list.add((Palette) element.b);
			}
		}

		return list;
	}

	public String sourceCode() {
		return sourceCode;
	}

	public int[] code() {
		return code;
	}



	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(sourceCode);
		Adapters.scaleAdapter.toParcel(scale, parcel, flags);
		parcel.writeParcelable(parameters, flags);
	}

	public static final Parcelable.Creator<Fractal> CREATOR =
			new Parcelable.Creator<Fractal>() {
		public Fractal createFromParcel(Parcel in) {
			String sourceCode = in.readString();
			Scale sc = Adapters.scaleAdapter.fromParcel(in);
			Parameters p = in.readParcelable(Fractal.Parameters.class.getClassLoader());

			return new Fractal(sc, sourceCode, p);
		}

		public Fractal[] newArray(int size) {
			return new Fractal[size];
		}
	};
}
