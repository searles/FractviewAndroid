package at.searles.meelan;

import at.searles.math.Cplx;
import at.searles.math.Quat;

import java.util.List;

public interface Compiler {
	void id(String id);
	void app(String fn, List<Tree> args);
	void union(String id, String type);
	void var(String id, String type, Tree init);

	void intval(int i);
	void realval(double d);
	void cplxval(Cplx c);
	void quadval(Quat f);
}