package at.searles.fractview.fractal;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.*;
import at.searles.parsing.ParsingError;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class Fractal implements Parcelable {

	/**
	 * Scale of this fractal
	 */
	private Scale scale;

	/**
	 * Source code of the program
	 */
	private String sourceCode;

	/**
	 * Parameters of the program (these are the external-parts in the program)
	 */
	private Parameters parameters;

	// FIXME The next 3 ones should rather be bundled in a separate object

	/**
	 * Data that are fetched during compilation
	 */
	private DefaultData defaults = null; // generated

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

			Parameters arguments = optArgs == null ? new Parameters() : Parameters.ADAPTER.fromJSON(optArgs);

			return new Fractal(scale, sourceCode, arguments);
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


		if(!parameters.isEmpty()) obj.put("arguments", Parameters.ADAPTER.toJSON(parameters));

		return obj;
	}

	public void parse() throws ParsingError, CompileException {
		defaults = new DefaultData();
		ast = Meelan.parse(sourceCode, defaults);
		Log.d("FR", ast.toString());
	}

	public boolean isDefaultValue(String id) {
		return !parameters.contains(id);
	}

	public void compile() throws CompileException {
		ScopeTable table = new ScopeTable();

		// Add values to table
		for(String id : exprLabels()) {
			try {
				// extern values must be treated special
				// unless they are constants.
				table.addExtern(id, Meelan.parse(expr(id), null));
			} catch(ParsingError e) {
				throw new CompileException(e.getMessage());
			}
		}

		for(String id : boolLabels()) {
			table.addDef(id, new Value.Bool(bool(id)));
		}

		for(String id : intLabels()) {
			table.addDef(id, new Value.Int(intVal(id)));
		}

		for(String id : realLabels()) {
			table.addDef(id, new Value.Real(real(id)));
		}

		for(String id : cplxLabels()) {
			table.addDef(id, new Value.CplxVal(cplx(id)));
		}

		for(String id : colorLabels()) {
			table.addDef(id, new Value.Int(color(id)));
		}

		// Palettes are special
		// A palette is actually a function with one complex argument.
		Tree.Id xy = new Tree.Id("__xy");

		int paletteIndex = 0;
		for(String id : paletteLabels()) {
			// FIXME this one defines a function
			Tree.FuncDef fn = new Tree.FuncDef("palette_" + id, Collections.singletonList("__xy"),
					Op.__ld_palette.eval(Arrays.asList(new Value.Label(paletteIndex), xy)));

			table.addDef(id, fn);
			paletteIndex++;
		}

		this.code = Meelan.compile(ast, table);
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
		f.ast = ast;
		f.defaults = defaults;
		f.code = code;

		return f;
	}

	public Parameters parameters() {
		return parameters;
	}

	// some mutable methods
	public Scale scale() {
		return scale;
	}

	public void setScale(Scale scale) {
		this.scale = scale;
	}

	public Iterable<String> exprLabels() {
		return defaults.exprs.keySet();
	}

	public String expr(String id) {
		return parameters.exprs.containsKey(id) ? parameters.exprs.get(id) : defaults.exprs.get(id);
	}

	public void resetExpr(String id) {
		parameters.exprs.remove(id);
	}

	public void setExpr(String id, String expr) {
		parameters.exprs.put(id, expr);
	}



	public Iterable<String> boolLabels() {
		return defaults.bools.keySet();
	}

	public boolean bool(String id) {
		return parameters.bools.containsKey(id) ? parameters.bools.get(id) : defaults.bools.get(id);
	}

	public void resetBool(String id) {
		parameters.bools.remove(id);
	}

	public void setBool(String id, boolean bool) {
		parameters.bools.put(id, bool);
	}



	public Iterable<String> intLabels() {
		return defaults.ints.keySet();
	}

	public int intVal(String id) {
		return parameters.ints.containsKey(id) ? parameters.ints.get(id) : defaults.ints.get(id);
	}

	public void resetInt(String id) {
		parameters.ints.remove(id);
	}

	public void setInt(String id, int i) {
		parameters.ints.put(id, i);
	}



	public Iterable<String> realLabels() {
		return defaults.reals.keySet();
	}

	public double real(String id) {
		return parameters.reals.containsKey(id) ? parameters.reals.get(id) : defaults.reals.get(id);
	}

	public void resetReal(String id) {
		parameters.reals.remove(id);
	}

	public void setReal(String id, double real) {
		parameters.reals.put(id, real);
	}



	public Iterable<String> cplxLabels() {
		return defaults.cplxs.keySet();
	}

	public Cplx cplx(String id) {
		return parameters.cplxs.containsKey(id) ? parameters.cplxs.get(id) : defaults.cplxs.get(id);
	}

	public void resetCplx(String id) {
		parameters.cplxs.remove(id);
	}

	public void setCplx(String id, Cplx cplx) {
		parameters.cplxs.put(id, cplx);
	}



	public Iterable<String> colorLabels() {
		return defaults.colors.keySet();
	}

	public int color(String id) {
		return parameters.colors.containsKey(id) ? parameters.colors.get(id) : defaults.colors.get(id);
	}

	public void resetColor(String id) {
		parameters.colors.remove(id);
	}

	public void setColor(String id, int color) {
		parameters.colors.put(id, color);
	}



	public Iterable<String> paletteLabels() {
		return defaults.palettes.keySet();
	}

	public Palette palette(String id) {
		return parameters.palettes.containsKey(id) ? parameters.palettes.get(id) : defaults.palettes.get(id);
	}

	public void resetPalette(String id) {
		parameters.palettes.remove(id);
	}

	public void setPalette(String id, Palette palette) {
		parameters.palettes.put(id, palette);
	}

	/**
	 * Since palettes must be transferred directly, convenience method.
	 * @return
	 */
	public Palette[] palettes() {
		Palette[] array = new Palette[defaults.palettes.size()];

		int i = 0;

		for(String label : paletteLabels()) {
			array[i++] = palette(label);
		}

		return array;
	}


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(sourceCode);
		Adapters.scaleAdapter.toParcel(scale, parcel, flags);
		Parameters.ADAPTER.toParcel(parameters, parcel, flags);
	}

	public static final Parcelable.Creator<Fractal> CREATOR
			= new Parcelable.Creator<Fractal>() {
		public Fractal createFromParcel(Parcel in) {
			String sourceCode = in.readString();
			Scale sc = Adapters.scaleAdapter.fromParcel(in);
			Parameters dm = Parameters.ADAPTER.fromParcel(in);

			return new Fractal(sc, sourceCode, dm);
		}

		public Fractal[] newArray(int size) {
			return new Fractal[size];
		}
	};

	public int[] code() {
		return code;
	}

	public String sourceCode() {
		return sourceCode;
	}

	public Iterable<? extends Map.Entry<String, DefaultData.Type>> parameterMap() {
		return defaults.elements.entrySet();
	}
}
