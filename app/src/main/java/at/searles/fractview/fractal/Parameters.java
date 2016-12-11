package at.searles.fractview.fractal;

import android.os.Parcel;
import at.searles.math.Cplx;
import at.searles.math.color.Palette;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * This class represents non-default values. It therefore interacts a lot
 * with the ParametersActivity and it is a parcelable/json-part to store
 * a concrete fractal.
 *
 * This class does not contain scale because it is more specialized, and changing the scale
 * allows other things like reusing data in some cases. Thus, scale will not be added here.
 */
public class Parameters {


	static final Adapters.Adapter<Map<String, String>> ea = Adapters.mapAdapter(Adapters.stringAdapter);
	static final Adapters.Adapter<Map<String, Boolean>> ba = Adapters.mapAdapter(Adapters.boolAdapter);
	static final Adapters.Adapter<Map<String, Integer>> ia = Adapters.mapAdapter(Adapters.intAdapter);
	static final Adapters.Adapter<Map<String, Double>> ra = Adapters.mapAdapter(Adapters.realAdapter);
	static final Adapters.Adapter<Map<String, Cplx>> ca = Adapters.mapAdapter(Adapters.cplxAdapter);
	// No need for color because it is an int.
	static final Adapters.Adapter<Map<String, Palette>> pa = Adapters.mapAdapter(Adapters.paletteAdapter);

	final Map<String, String> exprs;
	final Map<String, Boolean> bools;
	final Map<String, Integer> ints;
	final Map<String, Double> reals;
	final Map<String, Cplx> cplxs;
	final Map<String, Integer> colors; // use intAdapter
	final Map<String, Palette> palettes;

	private Parameters(Map<String, String> exprs, Map<String, Boolean> bools, Map<String, Integer> ints, Map<String, Double> reals, Map<String, Cplx> cplxs, Map<String, Integer> colors, Map<String, Palette> palettes) {
		this.exprs = exprs;
		this.bools = bools;
		this.ints = ints;
		this.reals = reals;
		this.cplxs = cplxs;
		this.colors = colors;
		this.palettes = palettes;
	}

	public Parameters() {
		this(
				new TreeMap<String, String>(),
				new TreeMap<String, Boolean>(),
				new TreeMap<String, Integer>(),
				new TreeMap<String, Double>(),
				new TreeMap<String, Cplx>(),
				new TreeMap<String, Integer>(),
				new TreeMap<String, Palette>()
				);
	}


	public Parameters addExpr(String id, String entry) {
		exprs.put(id, entry);
		return this;
	}

	public Parameters removeExpr(String id) {
		exprs.remove(id);
		return this;
	}

	public Parameters addBool(String id, boolean entry) {
		bools.put(id, entry);
		return this;
	}

	public Parameters removeBool(String id) {
		bools.remove(id);
		return this;
	}

	public Parameters addInt(String id, int entry) {
		ints.put(id, entry);
		return this;
	}

	public Parameters removeInts(String id) {
		ints.remove(id);
		return this;
	}

	public Parameters addReal(String id, double entry) {
		reals.put(id, entry);
		return this;
	}

	public Parameters removeReal(String id) {
		reals.remove(id);
		return this;
	}

	public Parameters addCplx(String id, Cplx entry) {
		cplxs.put(id, entry);
		return this;
	}

	public Parameters removeCplx(String id) {
		cplxs.remove(id);
		return this;
	}

	public Parameters addColor(String id, int entry) {
		colors.put(id, entry);
		return this;
	}

	public Parameters removeColor(String id) {
		colors.remove(id);
		return this;
	}


	public Parameters addPalette(String id, Palette entry) {
		palettes.put(id, entry);
		return this;
	}

	public Parameters removePalette(String id) {
		palettes.remove(id);
		return this;
	}

	public static final Adapters.Adapter<Parameters> ADAPTER = new Adapters.Adapter<Parameters>() {
		@Override
		public Parameters fromParcel(Parcel parcel) {
			Map<String, String> exprs = ea.fromParcel(parcel);
			Map<String, Boolean> bools = ba.fromParcel(parcel);
			Map<String, Integer> ints = ia.fromParcel(parcel);
			Map<String, Double> reals = ra.fromParcel(parcel);
			Map<String, Cplx> cplxs = ca.fromParcel(parcel);
			Map<String, Integer> colors = ia.fromParcel(parcel);
			Map<String, Palette> palettes = pa.fromParcel(parcel);

			Parameters dm = new Parameters(
					exprs,
					bools,
					ints,
					reals,
					cplxs,
					colors,
					palettes
			);

			return dm;
		}

		@Override
		public void toParcel(Parameters dm, Parcel parcel, int flags) {
			ea.toParcel(dm.exprs, parcel, flags);
			ba.toParcel(dm.bools, parcel, flags);
			ia.toParcel(dm.ints, parcel, flags);
			ra.toParcel(dm.reals, parcel, flags);
			ca.toParcel(dm.cplxs, parcel, flags);
			ia.toParcel(dm.colors, parcel, flags);
			pa.toParcel(dm.palettes, parcel, flags);
		}

		@Override
		public Parameters fromJSON(Object o) throws JSONException {
			if(o instanceof JSONObject) {
				JSONObject obj = (JSONObject) o;

				Object ee = obj.opt("exprs");
				Object bb = obj.opt("bools");
				Object ii = obj.opt("ints");
				Object rr = obj.opt("reals");
				Object cc = obj.opt("cplxs");
				Object ll = obj.opt("colors");
				Object pp = obj.opt("palettes");

				Map<String, String> exprs = ee == null ? new LinkedHashMap<String, String>() : ea.fromJSON(ee);
				Map<String, Boolean> bools = bb == null ? new LinkedHashMap<String, Boolean>() : ba.fromJSON(bb);
				Map<String, Integer> ints = ii == null ? new LinkedHashMap<String, Integer>() : ia.fromJSON(ii);
				Map<String, Double> reals = rr == null ? new LinkedHashMap<String, Double>() : ra.fromJSON(rr);
				Map<String, Cplx> cplxs = cc == null ? new LinkedHashMap<String, Cplx>() : ca.fromJSON(cc);
				Map<String, Integer> colors = ll == null ? new LinkedHashMap<String, Integer>() : ia.fromJSON(ll);
				Map<String, Palette> palettes = pp == null ? new LinkedHashMap<String, Palette>() : pa.fromJSON(pp);

				Parameters dm = new Parameters(exprs, bools, ints, reals, cplxs, colors, palettes);

				return dm;
			}

			throw new JSONException("not a JSONObject");
		}

		@Override
		public Object toJSON(Parameters dm) throws JSONException {
			JSONObject obj = new JSONObject();

			if(!dm.exprs.isEmpty()) obj.put("exprs", ea.toJSON(dm.exprs));
			if(!dm.bools.isEmpty()) obj.put("bools", ba.toJSON(dm.bools));
			if(!dm.ints.isEmpty()) obj.put("ints", ia.toJSON(dm.ints));
			if(!dm.reals.isEmpty()) obj.put("reals", ra.toJSON(dm.reals));
			if(!dm.cplxs.isEmpty()) obj.put("cplxs", ca.toJSON(dm.cplxs));
			if(!dm.colors.isEmpty()) obj.put("colors", ia.toJSON(dm.colors));
			if(!dm.palettes.isEmpty()) obj.put("palettes", pa.toJSON(dm.palettes));

			return obj;
		}
	};



	public boolean isEmpty() {
		// only show in json if not empty
		return exprs.isEmpty() && bools.isEmpty() && ints.isEmpty() && reals.isEmpty() &&
				cplxs.isEmpty() && colors.isEmpty() && palettes.isEmpty();
	}

	public boolean contains(String id) {
		return exprs.containsKey(id) || bools.containsKey(id) || ints.containsKey(id) || reals.containsKey(id) ||
				cplxs.containsKey(id) || colors.containsKey(id) || palettes.containsKey(id);
	}

	public Parameters merge(Parameters other) {
		// create new Parameters with all of this parameters and all of other's; where other's overwrite this one's
		Parameters ret = new Parameters();

		ret.exprs.putAll(this.exprs);
		ret.exprs.putAll(other.exprs);
		ret.bools.putAll(this.bools);
		ret.bools.putAll(other.bools);
		ret.ints.putAll(this.ints);
		ret.ints.putAll(other.ints);
		ret.reals.putAll(this.reals);
		ret.reals.putAll(other.reals);
		ret.cplxs.putAll(this.cplxs);
		ret.cplxs.putAll(other.cplxs);
		ret.colors.putAll(this.colors);
		ret.colors.putAll(other.colors);
		ret.palettes.putAll(this.palettes);
		ret.palettes.putAll(other.palettes);

		return ret;
	}
}
