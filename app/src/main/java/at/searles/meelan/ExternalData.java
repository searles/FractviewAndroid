package at.searles.meelan;

public interface ExternalData {
	/**
	 * Called from parser. Adds an element to external data. Additionally,
	 * external values may be embedded inside if-expressions that
	 * depend on external booleans. In this case, dependency is set to
	 * the ID of that boolean. It can be masked then in some underlying
	 * framework (eg some UI) because it does not have any impact.
	 * @param id
	 * @param type
	 * @param init
	 * @throws CompileException
     */
	void add(String id, String type, Tree init) throws CompileException;
}
