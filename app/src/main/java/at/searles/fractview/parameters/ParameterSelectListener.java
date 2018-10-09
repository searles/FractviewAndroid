package at.searles.fractview.parameters;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;

import at.searles.fractal.FractalProvider;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.dialogs.ColorDialogFragment;
import at.searles.fractview.parameters.dialogs.CplxDialogFragment;
import at.searles.fractview.parameters.dialogs.ExprDialogFragment;
import at.searles.fractview.parameters.dialogs.IntegerDialogFragment;
import at.searles.fractview.parameters.dialogs.RealDialogFragment;
import at.searles.fractview.parameters.dialogs.ScaleDialogFragment;
import at.searles.fractview.parameters.palettes.PaletteActivity;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

public class ParameterSelectListener implements AdapterView.OnItemClickListener {

    private final FractalProviderFragment fragment;

    public ParameterSelectListener(FractalProviderFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FractalProvider.ParameterEntry item = ((ParameterAdapter) parent.getAdapter()).getItem(position);

        switch (item.type) {
            case Bool: {
                boolean newValue = !(Boolean) item.value;

                // in the case of non-individual parameters, owner may be null
                // but it is never accessed.
                fragment.setParameter(item.key, item.owner, newValue);
                ((CheckedTextView) view).setChecked(newValue);

                ((ParameterAdapter) parent.getAdapter()).notifyDataSetChanged();

                return;
            }
            case Int: {
                IntegerDialogFragment ft = IntegerDialogFragment.newInstance("Edit integer number " + id, item.key, item.owner, (Integer) item.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Real: {
                RealDialogFragment ft = RealDialogFragment.newInstance("Edit decimal number " + id, item.key, item.owner, (Double) item.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Cplx: {
                CplxDialogFragment ft = CplxDialogFragment.newInstance("Edit complex number " + id, item.key, item.owner, (Cplx) item.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Expr: {
                ExprDialogFragment ft = ExprDialogFragment.newInstance("Edit expression " + id, item.key, item.owner, (String) item.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Scale: {
                ScaleDialogFragment ft = ScaleDialogFragment.newInstance("Edit scale " + id, item.key, item.owner, (Scale) item.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Color: {
                ColorDialogFragment ft = ColorDialogFragment.newInstance("Edit color " + id, item.key, item.owner, (Integer) item.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Palette: {
                // start new activity
                Palette value = (Palette) item.value;

                Intent i = new Intent(parent.getContext(), PaletteActivity.class);

                i.putExtra(PaletteActivity.PALETTE_LABEL, BundleAdapter.toBundle(value));

                // add information which palette it actually is.
                i.putExtra(PaletteActivity.ID_LABEL, item.key);
                i.putExtra(PaletteActivity.OWNER_LABEL, item.owner);

                fragment.startActivityForResult(i, PaletteActivity.PALETTE_ACTIVITY_RETURN);
            }
            return;
            default:
                throw new IllegalArgumentException();
        }
    }
}
