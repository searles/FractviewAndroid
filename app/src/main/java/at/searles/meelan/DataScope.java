package at.searles.meelan;

public class DataScope {
	private DataScope parent;
	private int dataIndex;

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

	public int nextDataIndex(Type t) {
		int ret = dataIndex;
		dataIndex += t.size();
		return ret;
	}
}
