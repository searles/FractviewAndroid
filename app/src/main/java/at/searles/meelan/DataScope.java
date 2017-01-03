package at.searles.meelan;

public class DataScope {
	DataScope parent;
	int dataIndex;

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

	/**
	 * This one is for unions so that they do not increase
	 * the pointer. Works good, but unions are rather unsafe.
	 * @return
	 */
	public int pullDataIndex() {
		return dataIndex;
	}
}
