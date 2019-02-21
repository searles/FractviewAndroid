package at.searles.fractview.provider.view.parameters;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;

import at.searles.fractal.ParameterTable;
import at.searles.fractview.SourceEditorActivity;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.parameters.dialogs.ColorDialogFragment;
import at.searles.fractview.parameters.dialogs.CplxDialogFragment;
import at.searles.fractview.parameters.dialogs.ExprDialogFragment;
import at.searles.fractview.parameters.dialogs.IntegerDialogFragment;
import at.searles.fractview.parameters.dialogs.RealDialogFragment;
import at.searles.fractview.parameters.dialogs.ScaleDialogFragment;
import at.searles.fractview.parameters.palettes.PaletteActivity;
import at.searles.fractview.provider.FractalProviderFragment;
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
        ParameterTable.Entry item = ((ParameterAdapter) parent.getAdapter()).getItem(position);

        switch (item.parameter.type) {
            case Bool: {
                boolean newValue = !(Boolean) item.parameter.value;

                // in the case of non-individual parameters, id may be null
                // but it is never accessed.
                fragment.setParameterValue(item.key, item.owner, newValue);
                ((CheckedTextView) view).setChecked(newValue);

                ((ParameterAdapter) parent.getAdapter()).notifyDataSetChanged();

                return;
            }
            case Int: {
                IntegerDialogFragment ft = IntegerDialogFragment.newInstance("Edit integer number " + item.parameter.description, item.key, item.owner, ((Number) item.parameter.value).intValue());
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Real: {
                RealDialogFragment ft = RealDialogFragment.newInstance("Edit decimal number " + item.parameter.description, item.key, item.owner, ((Number) item.parameter.value).doubleValue());
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Cplx: {
                CplxDialogFragment ft = CplxDialogFragment.newInstance("Edit complex number " + item.parameter.description, item.key, item.owner, (Cplx) item.parameter.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Expr: {
                ExprDialogFragment ft = ExprDialogFragment.newInstance("Edit expression " + item.parameter.description, item.key, item.owner, (String) item.parameter.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Scale: {
                ScaleDialogFragment ft = ScaleDialogFragment.newInstance("Edit scale " + item.parameter.description, item.key, item.owner, (Scale) item.parameter.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Color: {
                ColorDialogFragment ft = ColorDialogFragment.newInstance("Edit color " + item.parameter.description, item.key, item.owner, (Integer) item.parameter.value);
                fragment.getChildFragmentManager().beginTransaction().add(ft, "editor").commit();
            }
            return;
            case Palette: {
                // start new activity
                Palette value = (Palette) item.parameter.value;

                Intent i = new Intent(parent.getContext(), PaletteActivity.class);

                i.putExtra(PaletteActivity.PALETTE_LABEL, BundleAdapter.toBundle(value));

                // add information which palette it actually is.
                i.putExtra(PaletteActivity.ID_LABEL, item.key);
                i.putExtra(PaletteActivity.DESCRIPTION_LABEL, item.parameter.description);
                i.putExtra(PaletteActivity.OWNER_LABEL, item.owner);

                fragment.startActivityForResult(i, PaletteActivity.PALETTE_ACTIVITY_RETURN);
            }
            return;
            case Source: {
                // Source must be read from fractal
                String source = fragment.getSourceByOwner(item.owner);

                Intent i = new Intent(parent.getContext(), SourceEditorActivity.class);

                i.putExtra(SourceEditorActivity.SOURCE_LABEL, source);

                // add information which source it is.
                // key is of no use because there is only one.
                i.putExtra(SourceEditorActivity.ID_LABEL, item.owner);

                fragment.startActivityForResult(i, SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN);
            }
            return;
            default:
                throw new IllegalArgumentException();
        }
    }
}
