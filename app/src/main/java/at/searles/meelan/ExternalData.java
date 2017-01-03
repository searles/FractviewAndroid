package at.searles.meelan;

public interface ExternalData {
	void add(String id, String type, Tree init) throws CompileException;
}
