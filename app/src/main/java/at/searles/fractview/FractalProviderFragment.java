public class FractalProviderFragment extends Fragment {
	// This fragment takes over the responsibility to host (multiple) fractals 
	// and provides facilities for editing them.

	List<Listener> ...
	Stack<Change> undoStack;
	
	public void addListener(Listener l) {
	}
	
	public void removeListener(Listener l) {
	}
	
	protected void fireFractalModified() {
		// in the future allow more sophisticated things like picking a listener based on an index
	}
	
	protected void fireProviderSizeChanged() {
		// if size was modified.
	}
	
	// Read methods
	public Fractal get(int index) {
	}
	
	public int size() {
	}
	
	public Iterable<String> parameters() {
	}
	
	public Fractal.Type type(String label) {
	}
	
	public Object value(String label) {
	}
	
	public boolean isDefault(String label) {
	}
	
	// the only actual mutable method.
	protected void doChange(Change change, Change undoChange) {
		
	}
	
	// Write methods; They invoke 'registerChange'.
	public void externReset(String label) {
		 doChange(change, undoChange);
	}
	
	public void setExternValue(String label, Object value) throws CannotSetParameterException {
		 doChange(change, undoChange);
	}
	
	public void setSourceCode(String source, boolean resetParameters) throws CannotSetParameterException {
		 doChange(change, undoChange);
	}
	
	public void setFractal(int index, Fractal fractal) throws CannotSetParameterException {
		 doChange(change, undoChange);
	}
	
	public void undoLastChange(boolean notify) {
		
	}
	
	// all write methods fire a "fractalModified()" event.
	
	// UI methods
	public void setExternFromIntent(Intent intent) throws CannotSetParameterException {
		// label and value are in intent
		setExternValue(label, value);
	}

	public void startEditorForParameter(String label) {
		Type type = externType(label);
		
		switch(type) {
			...
		}
	}

	public void startSourceEditor() {
		String source = ;
		startActivityForResult(...);
		
		// 
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// if source code
		// or if palette
		// anything else?
	}
	
	// the following is used by parameterAdapter because it sets up 
	// the views in a listview.
	
	interface ParameterSelector {
		public void selectParameter(String label);
	}

	public static class ParameterEditorSelector implements FractalProviderFragment.ParameterSelector {
		public ParameterEditorSelector(FractalProviderFragment fragment) {
			this.fragment = fragment;
		}

		// Check out ActivityOption!
		// creates editors...
		public void selectParameter(String label) {
			Type type = externType();
			
			switch(type) {
				... start editors, either fragment, or startActivityForResult.
			}
		}
	}
		
	public static class ParameterShowOptionsSelector implements FractalProviderFragment.ParameterSelector {
		// add copy/paste functionality right here.
		// only show 'reset' if 'isDefault' is false.
		
		public ParameterShowOptionsSelector(FractalProviderFragment fragment) {
			this.fragment = fragment;
		}
	}
}
