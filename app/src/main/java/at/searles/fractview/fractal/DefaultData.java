package at.searles.fractview.fractal;

import at.searles.math.Cplx;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;
import at.searles.meelan.ExternalData;
import at.searles.meelan.Tree;
import at.searles.meelan.Value;

import java.util.*;

/**
 * This class contains default values that are implemented inside the source
 * code in the extern-declarations.
 */
public class DefaultData implements ExternalData {

	public enum Type { EXPR, BOOL, INT, REAL, CPLX, COLOR, PALETTE };

	Map<String, Type> elements = new LinkedHashMap<>();

	// LinkedHashMap keeps them in the order of the program
	final Map<String, String> exprs = new LinkedHashMap<>();
	final Map<String, Boolean> bools = new LinkedHashMap<>();
	final Map<String, Integer> ints = new LinkedHashMap<>();
	final Map<String, Double> reals = new LinkedHashMap<>();
	final Map<String, Cplx> cplxs = new LinkedHashMap<>();
	final Map<String, Integer> colors = new LinkedHashMap<>();
	final Map<String, Palette> palettes = new LinkedHashMap<>();

	@Override
	public void add(String id, String sType, Tree init) throws CompileException {
		if(elements.containsKey(id))
			throw new CompileException("the extern field \"" + id + "\" is already defined.");

		Type type;

		try {
			// FIXME I get bug reports that 'int' was not found???
			type = Type.valueOf(sType.toUpperCase());
		} catch(IllegalArgumentException e) {
			throw new CompileException("the type \"" + sType + "\" for extern field \"" + id + "\" does not exist.");
		}


		switch (type) {
			case EXPR:
				if(init instanceof Value.StringVal) {
					exprs.put(id, ((Value.StringVal) init).value);
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not an expr but a " + init.getClass() + "!");
				}
				break;
			case BOOL:
				if(init instanceof Value.Bool) {
					bools.put(id, ((Value.Bool) init).value);
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not a bool but a " + init.getClass() + "!");
				}
				break;
			case INT:
				if(init instanceof Value.Int) {
					ints.put(id, ((Value.Int) init).value);
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not an int but a " + init.getClass() + "!");
				}
				break;
			case REAL:
				double d;
				if(init instanceof Value.Int) {
					d = ((Value.Int) init).value;
				} else if(init instanceof Value.Real) {
					d = ((Value.Real) init).value;
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not a real but a " + init.getClass() + "!");
				}

				reals.put(id, d);
				break;
			case CPLX:
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

				cplxs.put(id, c);

				break;
			case COLOR:
				// again, this one is an integer
				if(init instanceof Value.Int) {
					colors.put(id, ((Value.Int) init).value);
				} else {
					throw new CompileException("extern " + id + " = " + init + " is not a color but a " + init.getClass() + "!");
				}
				break;
			case PALETTE:
				// this one is a bit different
				palettes.put(id, toPalette(init));
				break;
		}

		elements.put(id, type);
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
