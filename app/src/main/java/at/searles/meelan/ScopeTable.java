package at.searles.meelan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScopeTable {

	ScopeTable parent;
	Map<String, Tree> externs;
	//ExternalData external;

	// entries that should not be mapped contain value null.
	Map<String, Tree> table = new HashMap<>();

	public ScopeTable(/*ExternalData external,*/ ScopeTable parent) {
		//this.external = external;
		this.parent = parent;

		externs = parent == null ? new HashMap<String, Tree>() : parent.externs;
	}

	/*public ScopeTable(ExternalData external) {
		this(external, null);
	}*/

	public ScopeTable() {
		this(/*null,*/ null);
	}

	public void addExtern(String id, Tree expr) {
		externs.put(id, expr);
	}

	public void addToTable(String id, Tree expr) {
		// first check corresponds to behaviour in java
		if(topLevelScope(id)) throw new IllegalArgumentException(id + " already defined in scope");

		table.put(id, expr);
	}

	public void addDef(String id, Tree inlineExpr) {
		addToTable(id, inlineExpr);
	}

	/**
	 * Creates a new register and puts it into the table/scope.
	 *
	 * @param id
	 * @param type
	 * @param inlineInit
	 * @return Instance of RegDecl
	 */
	public Tree addVar(String id, String type, Tree inlineInit) throws CompileException {
		// create new untyped register
		Value.Reg reg = new Value.Reg(null, id); // no data scope yet.

		if(type != null) {
			Type t = Type.get(type); // throws an exception if type does not exist
			reg.initType(t);
		}

		addToTable(id, reg);

		return new Tree.RegDecl(reg, inlineInit);
	}

	public ScopeTable createFuncTable(List<String> args) {
		ScopeTable table = newScope();
		for(String arg : args) {
			table.addDef(arg, null);
		}

		return table;
	}

	public void addMasked(String id) {
		addDef(id, null);
	}

	public ScopeTable newScope() {
		return new ScopeTable(/*external,*/ this);
	}

	/*public Tree externDef(String id, String type, Tree defaultValue) {
	Extern defs are inserted into scopetable before.
		// fixme I could add the buffer-position and thereby modify the source code
		// fixme to the edited value.
		if(external == null) throw new CompileException("No reference for external values");

		return external.add(id, type, defaultValue).inline(this);
	}*/

	/**
	 * @param id
	 * @param def if it is masked, this will be returned
	 * @return null if id is not in scopetable (which is usually a syntax error)
	 */
	public Tree getId(String id, Tree def) {
		if(table.containsKey(id)) {
			Tree ret = table.get(id);
			// it might be null if it is masked, therefore provide "def".
			return ret == null ? def : ret;
		} else if(parent != null) {
			return parent.getId(id, def);
		} else {
			return null;
		}
	}

	/**
	 * Returns true if id is defined in the current scope.
	 * false means that it either is not defined, or it is
	 * defined in a higher scope.
	 * @param id
	 * @return
	 */
	public boolean topLevelScope(String id) {
		return this.table.containsKey(id);
	}

	// The next method is removed because it was error prone and not intuitive.
	// Now, externs behave different from defs in the meaning that
	// defs are true closures, using currently available data while
	// externs are purely syntactic without data. To demonstrate the
	// difference:
	// var a = 2; extern sth expr = "a + 1"; { var a = 1; sth } returns 2, while
	// var a = 2; def sth = a + 1; { var a = 1; sth } returns 3.



	/**
	 * This one allows us to replace entries by their inlined version.
	 * Useful to decide later where an entry should be defined.
	 * @param id
	 */
	void pullUpExtern(String id, boolean maskVars) throws CompileException {
		if(externs.containsKey(id)) {
			Tree t = externs.get(id);

			// so, simply add it as a def:
			// this.addDef(id, new Tree.Scope(t));
			// this does not work because t must be inlined.
			// solutions for this problem: keep defs for later.
			// but then, still not inlining is a problem because
			// Apps might be preserved.
			// So, I must inline this right here.
			// but I must mask variables to avoid double-initialization
			// but this tempers with the part in Tree because I
			// only insert such th
			this.addDef(id, new Tree.Scope(t.inline(this, maskVars)));
		}

		// if it is not in extern, it might be just something else.
		// like a constant that was already added to others
		// so simply ignore.
	}
}
