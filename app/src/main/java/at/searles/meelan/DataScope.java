package at.searles.meelan;

import android.util.Log;

public class DataScope {

	private static final int MAX_MEMORY = 192;

	private DataScope parent;
	private int dataIndex;

	static int memoryRequirement = 0;

	public DataScope(DataScope parent) {
		this.parent = parent;
		dataIndex = parent.dataIndex;
	}

	public DataScope() {
		parent = null;
		dataIndex = 0;
	}

	public DataScope newScope() {
		return new DataScope(this);
	}

	public int nextDataIndex(Type t) throws CompileException {
		int ret = dataIndex;
		dataIndex += t.size();

		if(dataIndex > memoryRequirement) {
			memoryRequirement = dataIndex;
			Log.d("MEM", "Require " + memoryRequirement);
		}

		if(dataIndex > MAX_MEMORY) {
			throw new CompileException("Not enough memory!");
		}

		return ret;
	}
}
