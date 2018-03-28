package at.searles.fractal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.searles.fractview.Commons;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;
import at.searles.meelan.ExternalData;
import at.searles.meelan.Meelan;
import at.searles.meelan.Op;
import at.searles.meelan.ScopeTable;
import at.searles.meelan.Tree;
import at.searles.meelan.Value;
import at.searles.parsing.ParsingError;

/*
 * When parsing, an instance of ExternData is created.
 * Additionally, there is a Map of custom parameters.
 *
 * So, ExternData gets a list of parameters, with
 * a type and a default value. The order must
 * be preserved.
 *
 * Parameter also contains data type. Order of parameters
 * is of no importance.
 *
 * LinkedHashMap<String, ExternElement>
 */

public class Fractal implements ExternalData {

	/**
	 * data contains a label Scale that contains the scale of the fractal.
	 */
	public static final String SCALE_KEY = "Scale";

    /**
     * Scale to fall back if there is no other scale defined.
     */ 
    public static final Scale DEFAULT_SCALE = new Scale(2, 0, 0, 2, 0, 0);
    
	/**
	 * Source code of the program
	 */
	private String sourceCode;

	/**
	 * Replacements for default data
	 */
	private Map<String, Parameter> data;

    /**
     * Default data in the order in which they are added.
     * This map is created when the source code is parsed.
     */
    private transient LinkedHashMap<String, Parameter> defaultData = null;

    /**
	 * Abstract Syntax Tree, generated by parse method
	 */
	private transient Tree ast = null; // generated

	/**
	 * Byte code, generated by compile method
	 */
	private transient int[] code = null; // generated

	/**
	 * Simple constructor
	 * @param sourceCode
	 * @param parameters
	 */
	public Fractal(String sourceCode, Map<String, Parameter> parameters) {
		if(sourceCode == null) {
			throw new NullPointerException();
		}

		this.sourceCode = sourceCode;
		this.data = parameters == null ? new HashMap<>() : parameters;
	}

	/**
	 * Returns an iterable of all parameters.
	 * @return
	 */
	public Set<String> parameters() {
		if(defaultData == null) {
			throw new IllegalArgumentException("parse has not been called yet!");
		}

		return defaultData.keySet();
	}


	/**
	 * Returns an iterable of all parameters.
	 * @return
	 */
	public Iterable<Map.Entry<String, Parameter>> nonDefaultParameters() {
		return data.entrySet();
	}

	/**
	 * @param id The name of the parameter
	 * @return true if the parameter with the given name is a member of data.
	 */
	public boolean isDefault(String id) {
		if(defaultData == null) {
			throw new IllegalArgumentException("fractal not compiled");
		} else if(!defaultData.containsKey(id)) {
			throw new IllegalArgumentException("key does not exist");
		} else {
			// Types must be compatible
			return !(data.containsKey(id) && data.get(id).type() == defaultData.get(id).type());
		}
	}

	public Type type(String id) {
		if(defaultData == null) {
			throw new IllegalArgumentException("must parse fractal before this");
		} else {
			Parameter p = defaultData.get(id);

			return p != null ? p.type() : null;
		}
	}

	/**
	 * Returns the value for a given parameter
	 * @param id
	 * @return
	 */
	public Parameter get(String id) {
		if(!isDefault(id)) {
			return data.get(id);
		} else {
			return defaultData.get(id);
		}
	}

	/**
	 * Resets parameter with id to default
	 * @param id
	 */
	public void setToDefault(String id) {
		data.remove(id);
	}

	/**
	 * Resets all parameters to default
	 */
	public void setAllToDefault() {
		data.clear();
	}

	public void setInt(String id, int i) {
		data.put(id, new Parameter(Type.Int, i));
	}

	public void setReal(String id, double d) {
		data.put(id, new Parameter(Type.Real, d));
	}

	public void setCplx(String id, Cplx c) {
		data.put(id, new Parameter(Type.Cplx, c));
	}

	public void setBool(String id, boolean b) {
		data.put(id, new Parameter(Type.Bool, b));
	}

	public void setExpr(String id, String expr) {
		data.put(id, new Parameter(Type.Expr, expr));
	}

	public void setColor(String id, int color) {
		data.put(id, new Parameter(Type.Color, color));
	}

	public void setPalette(String id, Palette p) {
		data.put(id, new Parameter(Type.Palette, p));
	}

	public void setScale(String id, Scale sc) {
		data.put(id, new Parameter(Type.Scale, sc));
	}

	public void setScale(Scale sc) {
		// The default scale that is used for the zoom
		setScale(SCALE_KEY, sc);
	}

	/**
	 * Create a new instance of this class with a different source code
	 * @param newSourceCode the new source code
	 * @param reuseArguments if true, old arguments are reused.
	 * @return fractal with the new source code
	 */
	public Fractal copyNewSource(String newSourceCode, boolean reuseArguments) {
		// This one requires complete new compiling
		return new Fractal(newSourceCode, reuseArguments ? data : new HashMap<>());
	}

	/**
	 * Creates a copy of this fractal with new scale.
	 * @param newScale
	 * @return
	 */
	public Fractal copyNewScale(Scale newScale) {
        HashMap<String, Parameter> newData = new HashMap<String, Parameter>();
        newData.put(SCALE_KEY, new Parameter(Type.Scale, newScale));
		Fractal newFractal = copyNewData(newData, true);
		newFractal.code = this.code; // keep code.
		return newFractal;
	}

	/**
	 * Creates a copy of this fractal with new data.
	 * @param newData
	 * @return
	 */
	public Fractal copyNewData(Map<String, Parameter> newData, boolean merge) {
        if(merge) newData = Commons.merge(newData, data);
        
        // keep default scale
		Fractal f = new Fractal(sourceCode, newData);

		// Source code stayed the same, thus no need to parse again.
		f.defaultData = defaultData; f.ast = ast;

		return f;
    }
    
    // ======== Some convenience methods to obtain data ========
    
	public Scale scale() {
		return (Scale) get(SCALE_KEY).value();
	}

	/**
	 * Since palettes must be transferred directly to the script, convenience method
	 * to collect all palettes
	 * @return
	 */
	public List<Palette> palettes() {
		// Collect all palettes
		LinkedList<Palette> list = new LinkedList<>();

		for(String id : parameters()) {
			Parameter p = get(id);
			if(p.type == Type.Palette) {
				list.add((Palette) p.object);
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


	/**
	 * Parses the source code and thus collects
	 * default parameters.
	 * @throws ParsingError
	 * @throws CompileException
	 */
	public void parse() throws ParsingError, CompileException {
		defaultData = new LinkedHashMap<>();
        
        // First entry is the default scale.
        // This is a work-around if there is no default scale defined in source code.
        defaultData.put(SCALE_KEY, new Parameter(Type.Scale, DEFAULT_SCALE));

		ast = Meelan.parse(sourceCode, this);
		// It is added before so that it would show up as the first element.
	}

	/**
	 * Compiles the source code, replacing
	 * the default parameters by custom parameters.
	 * @throws ParsingError
	 * @throws CompileException
	 */
	public void compile() throws ParsingError, CompileException {
		if(defaultData == null) parse();

		// all parameters are added to the scope table.
		ScopeTable table = new ScopeTable();

		// For palettes there are some treats here:
		// A palette is actually a function with one complex argument.
		Tree.Id xy = new Tree.Id("__xy");

		int paletteIndex = 0; // and we count the number of them.

		for(String id : parameters()) {
			Parameter p = get(id);

			switch(p.type) {
				case Int:
					table.addDef(id, new Value.Int(((Number) p.object).intValue()));
					break;
				case Real:
					table.addDef(id, new Value.Real(((Number) p.object).doubleValue()));
					break;
				case Cplx:
					table.addDef(id, new Value.CplxVal((Cplx) p.object));
					break;
				case Bool:
					table.addDef(id, new Value.Bool((Boolean) p.object));
					break;
				case Expr:
					try {
						// extern values must be treated special
						// unless they are constants.
						table.addExtern(id, Meelan.parse((String) p.object, null));
					} catch(ParsingError e) {
						throw new CompileException(e.getMessage());
					}
					break;
				case Color:
					table.addDef(id, new Value.Int((Integer) p.object));
					break;
				case Palette:
					// Palettes are special
					// A palette is actually a function with one complex argument.
					// this one defines a function
					Tree.FuncDef fn = new Tree.FuncDef("palette_" + id, Collections.singletonList("__xy"),
							Op.__ld_palette.eval(Arrays.asList(new Value.Label(paletteIndex), xy)));

					table.addDef(id, fn);
					paletteIndex++;

					break;
                case Scale: {
                    // FIXME TODO
                } break;
                default:
                    throw new IllegalArgumentException("not implemented yet");
			}
		}

		this.code = Meelan.compile(ast, table);
	}

	@Override
	public void add(String id, String type, Tree init) throws CompileException {
		switch(type) {
			case "int": {
				int i = 0;
				if(init instanceof Value.Int) {
					i = ((Value.Int) init).value;
				} else if(init instanceof Value.Real) {
					// fixme this is a fix for a bug.
					i = (int) ((Value.Real) init).value;
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not an int but a " + init.getClass() + "!");
				}

				defaultData.put(id, new Parameter(Type.Int, i));
			} break;
			case "real": {
				double d = 0.0;
				if(init instanceof Value.Int) {
					d = ((Value.Int) init).value;
				} else if(init instanceof Value.Real) {
					d = ((Value.Real) init).value;
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not a real but a " + init.getClass() + "!");
				}

				defaultData.put(id, new Parameter(Type.Real, d));
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

				defaultData.put(id, new Parameter(Type.Cplx, c));
			} break;
			case "bool": {
				if(init instanceof Value.Bool) {
					defaultData.put(id, new Parameter(Type.Bool, ((Value.Bool) init).value));
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not a bool but a " + init.getClass() + "!");
				}
			} break;
			case "expr": {
				if(init instanceof Value.StringVal) {
					defaultData.put(id, new Parameter(Type.Expr, ((Value.StringVal) init).value));
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not an expr but a " + init.getClass() + "!");
				}
			} break;
			case "color": {
				// again, this one is an integer
				if(init instanceof Value.Int) {
					defaultData.put(id, new Parameter(Type.Color, ((Value.Int) init).value));
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not a color but a " + init.getClass() + "!");
				}
			} break;
			case "palette": {
				// this one is a bit different
				defaultData.put(id, new Parameter(Type.Palette, Commons.toPalette(init)));
			} break;
            case "scale": {
				defaultData.put(id, new Parameter(Type.Scale, Commons.toScale(init)));
            } break;
			default:
				throw new CompileException("Unknown extern type: " + type + " for id " + id);
		}
	}

	public Map<String, Parameter> parameterMap() {
		return data;
	}

	public Fractal copy() {
		return new Fractal(this.sourceCode, this.data);
	}

	public static class ParameterMapBuilder {
		private final Map<String, Parameter> map;


		public ParameterMapBuilder() {
			this.map = new HashMap<>();
		}

		public ParameterMapBuilder add(String key, Fractal.Type type, Object value) {
			map.put(key, new Parameter(type, value));
			return this;
		}

		public Map<String, Parameter> map() {
			return map;
		}
	}

	public static ParameterMapBuilder parameterBuilder() {
		return new ParameterMapBuilder();
	}

	/**
	 * Types of parameters. Scale is a special case because
	 * it should not be part of Parameters.
	 */
	public enum Type { Int, Real, Cplx, Bool, Expr, Color, Palette, Scale };

	public static class Parameter {
		private final Type type;
		private final Object object;

		public Parameter(Type type, Object object) {
			this.type = type;
			this.object = object;
		}

		public Type type() {
			return type;
		}

		public Object value() {
			return object;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Parameter parameter = (Parameter) o;

			if (type != parameter.type) return false;
			return object != null ? object.equals(parameter.object) : parameter.object == null;

		}

		@Override
		public int hashCode() {
			int result = type != null ? type.hashCode() : 0;
			result = 31 * result + (object != null ? object.hashCode() : 0);
			return result;
		}
	}
}
