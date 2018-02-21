package at.searles.fractview;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;
import java.util.Stack;

import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractview.ui.DialogHelper;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

public abstract class FractalProviderFragment extends Fragment {

	private static final CharSequence[] scaleOptions = { "Reset to Default", "Copy", "Paste", "Center on Origin", "Orthogonalize", "Straighten" };
	private static final CharSequence[] boolOptions = { "Reset to Default" };
	private static final CharSequence[] cplxOptions = { "Reset to Default", "Copy", "Paste", "Set to Center" };
	private static final CharSequence[] defaultOptions = { "Reset to Default", "Copy", "Paste" };

	private static final String KEY_LABEL = "label";
	private static final String DIALOG_KEY = "dialog";
	// This fragment takes over the responsibility to host (multiple) fractals 
	// and provides facilities for editing them.

	public interface Listener {
		void structureModified(FractalProviderFragment src);
		void parameterModified(FractalProviderFragment src, String label);
		void providerSizeChanged(FractalProviderFragment src);
	}

	public interface Change {
		void apply() throws CannotSetParameterException;
	}

	private List<Listener> listeners;

	/**
	 * If true, then the modification is created by
	 * a change in the history, thus no undo-event is
	 * added
	 */
	private boolean isUndoChange;
	private Stack<Change> undoStack;
	
	public void addListener(Listener l) {
		this.listeners.add(l);
	}
	
	public void removeListener(Listener l) {
		this.listeners.remove(l);
	}

	protected void fireStructureModified() {
		// in the future allow more sophisticated things like picking a listener based on an index
		for(Listener l : listeners) {
			l.structureModified(this);
		}
	}

	protected void fireParameterModified(String label) {
		// in the future allow more sophisticated things like picking a listener based on an index
		for(Listener l : listeners) {
			l.parameterModified(this, label);
		}
	}

	protected void fireProviderSizeChanged() {
		// if number of fractals in this fractal provider was modified.
		for(Listener l : listeners) {
			l.providerSizeChanged(this);
		}
	}
	
	// Read methods
	public abstract Fractal get(int index);

	public abstract String sourceCode();

	public abstract int size();

	public abstract Iterable<String> parameters();

	public abstract Fractal.Type type(String label);

	public abstract Object value(String label);

	public abstract boolean isDefault(String label);

	/**
	 * Called if the value at label will be modified.
	 * This adds an undo-operation to the undo-stack unless
	 * #isUndoChange is true.
	 * @param label The label of the argument.
	 */
	protected void registerUndoChangeFor(String label) {
		if(isUndoChange) {
			return;
		}

		boolean wasDefault = isDefault(label);
		Object previousValue = wasDefault ? null : value(label);

		undoStack.push(new Change() {
			@Override
			public void apply() throws CannotSetParameterException {
				if(wasDefault) {
					reset(label);
				} else {
					setValue(label, previousValue);
				}
			}
		});
	}

	// Write methods; They invoke 'registerChange'.
	public abstract void reset(String label);

	public abstract void setValue(String label, Object value) throws CannotSetParameterException;

	public abstract void setSourceCode(String source, boolean resetParameters) throws CannotSetParameterException;

	public abstract void setFractal(int index, Fractal fractal) throws CannotSetParameterException;

	public boolean undoLastChange() {
		if(undoStack.isEmpty()) {
			return false;
		}

		try {
			isUndoChange = true;
			Change change = undoStack.pop();
			change.apply();
		} catch (CannotSetParameterException e) {
			throw new IllegalArgumentException(e);
		} finally {
			isUndoChange = false;
		}

		return true;
	}

	/**
	 * Creates a callback to this fragment. The requestCode
	 * must be the index in this argument.
	 * @param fragment
	 * @return
	 */
	public EditableDialogFragment.Callback createCallback(FractalProviderFragment fragment) {
		return new EditableDialogFragment.Callback() {
			@Override
			public void apply(String label, Object o) {
				if (o == null) {
					// There was an error in the input
					DialogHelper.error(getActivity(), "Bad input");
					return;
				}

				Fractal.Type type = type(label);

				try {
					setValue(label, o);
				} catch(CannotSetParameterException e) {
					DialogHelper.error(getActivity(), "Bad value");

					// and reopen dialog.
					EditableDialogFragment ft = EditableDialogFragment.newInstance(
							label, "Error in Expression!",
							EditableDialogFragment.Type.Name).setInitVal(o);

					ft.show(getActivity().getFragmentManager(), DIALOG_KEY);
				}

			}
		};
	}

	// all write methods fire a "fractalModified()" event.

	// UI methods
	public void setSourceCode(final String source) {
		try {
			setSourceCode(source, true);
		} catch(CannotSetParameterException e) {
			DialogHelper.confirm(getActivity(),
					e.getLocalizedMessage(),
					"Reset parameters?",
					new Runnable() {
						@Override
						public void run() {
							try {
								setSourceCode(source, false);
							} catch (CannotSetParameterException e) {
								DialogHelper.error(getActivity(), e.getLocalizedMessage());
							}
						}
					}
			);
		}
	}

	public void setFromIntent(Intent intent) throws CannotSetParameterException {
		// label and value are in intent
		String label = intent.getStringExtra(KEY_LABEL);
		setValue(label, intent.getParcelableExtra(label));
	}

	public void startEditorForParameter(String label) {
		Fractal.Type type = type(label);

		switch(type) {
                /*case Label: {
                    // this should not happen...
                } return;*/
			case Bool: {
				throw new IllegalArgumentException("no editor for this");
			}
			case Scale: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(
						label, "Edit Scale", EditableDialogFragment.Type.Scale)
						.setInitVal(value(label));

				ft.show(getFragmentManager(), DIALOG_KEY);
			} return;
			case Expr: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(
						label, "Edit Expression", EditableDialogFragment.Type.Name)
						.setInitVal(value(label));

				ft.show(getFragmentManager(), DIALOG_KEY);
			} return;
			case Int: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(
						label, "Edit Integer Value", EditableDialogFragment.Type.Int)
						.setInitVal(value(label));

				ft.show(getFragmentManager(), DIALOG_KEY);
			} return;
			case Real: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(
						label, "Edit Real Value", EditableDialogFragment.Type.Real)
						.setInitVal(value(label));

				ft.show(getFragmentManager(), DIALOG_KEY);
			}
			return;
			case Cplx: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(
						label, "Edit Complex Value", EditableDialogFragment.Type.Cplx)
						.setInitVal(value(label));

				ft.show(getFragmentManager(), DIALOG_KEY);
			}
			return;
			case Color: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(
						label, "Edit Color", EditableDialogFragment.Type.Color)
						.setInitVal(value(label));

				ft.show(getFragmentManager(), DIALOG_KEY);
			}
			return;
			case Palette: {
				// start new activity
				Palette value = (Palette) value(label);

				Intent i = new Intent(getActivity(), PaletteActivity.class);

				i.putExtra(PaletteActivity.PALETTE_LABEL, BundleAdapter.paletteToBundle(value));
				i.putExtra(PaletteActivity.ID_LABEL, label); // label should also be in here.

				startActivityForResult(i, PaletteActivity.PALETTE_ACTIVITY_RETURN);
			}
			return;
			default:
				throw new IllegalArgumentException();
		}

	}



	private void onSettingError(CannotSetParameterException e) {
		Activity activity = getActivity();

		if(activity != null) {
			DialogHelper.error(activity, e.getLocalizedMessage());
		}
	}

	private static Scale ratio(Scale original) {
		// TODO: Orientation: Use diagonals.

		// FIXME Check these
		// Step 1: make x/y-vectors same length
		double xx = original.xx();
		double xy = original.xy();

		double yx = original.yx();
		double yy = original.yy();

		double lenx = Math.sqrt(xx * xx + xy * xy);
		double leny = Math.sqrt(yx * yx + yy * yy);

		double mlen = Math.max(lenx, leny);

		xx *= mlen / lenx;
		xy *= mlen / lenx;
		yx *= mlen / leny;
		yy *= mlen / leny;

		double vx = (xx + yx) / 2;
		double vy = (xy + yy) / 2;

		double ax = vx + vy;
		double ay = vx - vy;

		// fixme find proper orientation
		return new Scale(ax, ay, -ay, ax, original.cx(), original.cy());
	}

	private static Scale straight(Scale original) {
		double xlen = Math.hypot(original.xx(), original.xy());
		double ylen = Math.hypot(original.yx(), original.yy());
		return new Scale(xlen, 0, 0, ylen, original.cx(), original.cy());
	}

	private boolean onOptionClick(String label, int which) {
		// Option 0: Reset
		// Option 1: Copy
		// Option 2: Paste

		switch (which) {
			case 0: // Reset
				reset(label);
				return true;
			case 1:
				// TODO
				return true;
			case 2:
				// TODO
				return true;
			default:
				return false;
		}
	}

	private void onScaleOptionClick(String label, int which) {
		if(onOptionClick(label, which)) return;

		Scale original = (Scale) value(label);

		switch (which) {
			case 3: // Origin
				setValue(label, new Scale(original.xx(), original.xy(), original.yx(), original.yy(), 0, 0));
				break;
			case 4: // Ratio
				setValue(label, ratio(original));
				break;
			case 5: // Straighten
				setValue(label, straight(original));
				break;
		}

		fireParameterModified(label);
	}

	private void onBoolOptionClick(String label, int which) {
		switch (which) {
			case 0: // Reset
				reset(label);
				break;
		}

		fireParameterModified(label);
	}

	private void onCplxOptionClick(String label, int which) {
		if(onOptionClick(label, which)) return;

		Scale original = (Scale) value(Fractal.SCALE_KEY);

		switch (which) {
			case 3: // Center
				setValue(label, new Cplx(original.cx(), original.cy()));
				break;
		}

		fireParameterModified(label);
	}

	public boolean startOptionsEditor(String label) {
		Fractal.Type type = type(label);

		String dialogTitle = "Select an option for \"" + label + "\"";

		Context context = getActivity();

		switch (type) {
			case Scale: {
				DialogHelper.showOptionsDialog(context, dialogTitle, scaleOptions, true, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						onScaleOptionClick(label, which);
					}
				});

				return true;
			}
			case Cplx: {
				DialogHelper.showOptionsDialog(context, dialogTitle, cplxOptions, true, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						onCplxOptionClick(label, which);
					}
				});

				return true;
			}
			case Bool: {
				DialogHelper.showOptionsDialog(context, dialogTitle, boolOptions, true, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						onBoolOptionClick(label, which);
					}
				});

				return true;
			}
			default: {
				DialogHelper.showOptionsDialog(context, dialogTitle, defaultOptions, true, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						onOptionClick(label, which);
					}
				});

				return true;
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PaletteActivity.PALETTE_ACTIVITY_RETURN) {
			if (resultCode == 1) { // = "Ok"
				Bundle bundle = data.getBundleExtra(PaletteActivity.PALETTE_LABEL);
				String label = data.getStringExtra(KEY_LABEL);

				setValue(label, BundleAdapter.bundleToPalette(bundle));
			}
		} else if(requestCode == SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN) {
			if (resultCode == 1) { // = "Ok"
				String source = data.getStringExtra(SourceEditorActivity.SOURCE_LABEL);

				setSourceCode(source, false);
			}
		}

	}

	public void startSourceEditor() {
		String source = sourceCode();
		Intent i = new Intent(getActivity(), SourceEditorActivity.class);

		i.putExtra(SourceEditorActivity.SOURCE_LABEL, source);

		startActivityForResult(i, SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN);
	}


	protected class CannotSetParameterException extends RuntimeException {
		public CannotSetParameterException(String msg) {
			super(msg);
		}

		public CannotSetParameterException(Throwable cause) {
			super(cause);
		}
	}

    // the following is used by parameterAdapter because it sets up
	// the views in a listview.
	
	interface ParameterSelector {
		public void selectParameter(String label);
	}

	public static class ParameterEditorSelector implements FractalProviderFragment.ParameterSelector {
		private final FractalProviderFragment fragment;

		public ParameterEditorSelector(FractalProviderFragment fragment) {
			this.fragment = fragment;
		}

		// Check out ActivityOption!
		// creates editors...
		public void selectParameter(String label) {
			fragment.startOptionsEditor(label);
		}
	}
		
	public static class ParameterShowOptionsSelector implements FractalProviderFragment.ParameterSelector {
		private final FractalProviderFragment fragment;
		// add copy/paste functionality right here.
		// only show 'reset' if 'isDefault' is false.
		
		public ParameterShowOptionsSelector(FractalProviderFragment fragment) {
			this.fragment = fragment;
		}

		@Override
		public void selectParameter(String label) {
			fragment.startEditorForParameter(label);
		}
	}
}
