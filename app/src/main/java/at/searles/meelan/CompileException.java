package at.searles.meelan;

import java.util.LinkedList;

public class CompileException extends Exception {
	private LinkedList<Tree> trace = new LinkedList<>();

	public CompileException(String msg) {
		super(msg);
	}

	public CompileException addTraceElement(Tree t) {
		trace.add(t);
		return this;
	}

	public String toString() {
		return trace.toString();
	}
}
