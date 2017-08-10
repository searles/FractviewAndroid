package at.searles.fractview;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.TreeSet;

import at.searles.fractview.ui.ColorView;
import at.searles.fractview.ui.DialogHelper;
import at.searles.math.Scale;
import at.searles.math.color.Colors;

/**
 * Dialog for editable elements. Initial argument must be set in the bundle
 * 'arguments'. This is for sharedpref-dialogs the name of the shared preference.
 */
public class EditableDialogFragment extends DialogFragment {

    public static EditableDialogFragment newInstance(int requestCode, String title,
                                                     boolean callFragment, Type type) {
        Bundle b = EditableDialogFragment.createBundle(requestCode, callFragment, title);
        b.putInt("type", type.ordinal());

        EditableDialogFragment fragment = new EditableDialogFragment();
        fragment.setArguments(b);

        return fragment;
    }

    private Object initialValue;

    public static Bundle createBundle(
            int requestCode,
            boolean callFragment,
            String title) {
        Bundle b = new Bundle();
        b.putInt("request_code", requestCode);
        b.putBoolean("call_fragment", callFragment);
        if(title != null) b.putString("key", title);

        b.putBoolean("closed", false);
        // this one is here to check whether the dialog should still
        // exist.

        return b;
    }

    protected boolean callbackFragment() {
        return getArguments().getBoolean("call_fragment");
    }

    /**
     * @return may return null if there is no key
     */
    protected String title() {
        if(getArguments().containsKey("key")) {
            return getArguments().getString("key");
        } else {
            return null;
        }
    }

    protected int requestCode() {
        return getArguments().getInt("request_code");
    }

    public EditableDialogFragment setInitVal(Object o) {
        this.initialValue = o;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d("EDF", "onCreateDialog was called");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Type type = type();

        Log.d("EDF", "type is " + type);

        View view = type.createView(this);

        builder.setView(view);

        String title = title();
        if(title != null) builder.setTitle(title);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss(); // fixme: Is this one needed?
            }
        });

        if(type.hasPositiveButton()) {
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    Object value = type().getValueInView(getDialog());

                    if(value != null) {
                        getCallback().apply(requestCode(), value);
                    } else {
                        Log.i("EDF", "The value was null, hence no callback");
                    }
                }
            });
        }

        Dialog dialog = builder.create();

        // init values in view of dialog
        if(savedInstanceState == null) {
            // but only in the first call.
            // otherwise it is rotated, then we should preserve the values
            if(initialValue != null) type.setValueInView(initialValue, view);
        }

        return dialog;
    }

    protected Type type() {
        return Type.values()[getArguments().getInt("type")];
    }

    private Callback getCallback() {
        if(callbackFragment()) {
            return (EditableDialogFragment.Callback) getTargetFragment();
        } else {
            return (Callback) getActivity();
        }
    }


    public interface Callback {
        /**
         * Calls back with the input value.
         * @param o The object in the input.
         */
        void apply(int requestCode, Object o);
    }

    /**
     * All the things that can be edited
     */
    public enum Type {
        Scale {
            @Override
            void setValueInView(Object o, View view) {
                EditText editorXX = (EditText) view.findViewById(R.id.xxEditText);
                EditText editorXY = (EditText) view.findViewById(R.id.xyEditText);
                EditText editorYX = (EditText) view.findViewById(R.id.yxEditText);
                EditText editorYY = (EditText) view.findViewById(R.id.yyEditText);
                EditText editorCX = (EditText) view.findViewById(R.id.cxEditText);
                EditText editorCY = (EditText) view.findViewById(R.id.cyEditText);

                Scale sc = (Scale) o;

                editorXX.setText(Double.toString(sc.xx()));
                editorXY.setText(Double.toString(sc.xy()));
                editorYX.setText(Double.toString(sc.yx()));
                editorYY.setText(Double.toString(sc.yy()));
                editorCX.setText(Double.toString(sc.cx()));
                editorCY.setText(Double.toString(sc.cy()));
            }

            @Override
            Object getValueInView(Dialog view) {
                EditText[] editors = new EditText[6];

                editors[0] = ((EditText) view.findViewById(R.id.xxEditText));
                editors[1] = (EditText) view.findViewById(R.id.xyEditText);
                editors[2] = (EditText) view.findViewById(R.id.yxEditText);
                editors[3] = (EditText) view.findViewById(R.id.yyEditText);
                editors[4] = (EditText) view.findViewById(R.id.cxEditText);
                editors[5] = (EditText) view.findViewById(R.id.cyEditText);

                double[] ds = new double[6];

                for(int i = 0; i < 6; ++i) {
                    try {
                        ds[i] = Double.parseDouble(editors[i].getText().toString());
                    } catch(NumberFormatException e) {
                        Toast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        return false;
                    }
                }

                return new Scale(ds[0], ds[1], ds[2], ds[3], ds[4], ds[5]);
            }

            @Override
            View createView(EditableDialogFragment ft) {
                return ft.getActivity().getLayoutInflater().inflate(R.layout.scale_editor, null);
            }
        },
        Int {
            @Override
            void setValueInView(Object o, View view) {
                if(!(o instanceof Integer))
                    throw new ClassCastException("Int expects an Integer of number but got a " + o.getClass());

                EditText v = (EditText) view.findViewById(R.id.intEditText);
                v.setText(o.toString());
            }

            @Override
            Integer getValueInView(Dialog dialog) {
                EditText v = (EditText) dialog.findViewById(R.id.intEditText);

                try {
                    return Integer.parseInt(v.getText().toString());
                } catch(NumberFormatException e) {
                    // error
                    return null;
                }
            }

            @Override
            View createView(EditableDialogFragment ft) {
                return ft.getActivity().getLayoutInflater().inflate(R.layout.int_editor, null);
            }
        },
        Real {
            @Override
            void setValueInView(Object o, View view) {
                if(!(o instanceof Double))
                    throw new ClassCastException("Real expects a Double but got a " + o.getClass());

                EditText v = (EditText) view.findViewById(R.id.realEditText);
                v.setText(o.toString());
            }

            @Override
            Object getValueInView(Dialog dialog) {
                EditText v = (EditText) dialog.findViewById(R.id.realEditText);

                try {
                    return Double.parseDouble(v.getText().toString());
                } catch(NumberFormatException e) {
                    // error
                    return null;
                }
            }

            @Override
            View createView(EditableDialogFragment ft) {
                return ft.getActivity().getLayoutInflater().inflate(R.layout.real_editor, null);
            }
        },
        Cplx {
            @Override
            void setValueInView(Object o, View view) {
                // o is a parcel in this case.
                at.searles.math.Cplx c = (at.searles.math.Cplx) o;

                EditText tx = (EditText) view.findViewById(R.id.xEditText);
                EditText ty = (EditText) view.findViewById(R.id.yEditText);

                tx.setText(Double.toString(c.re()));
                ty.setText(Double.toString(c.im()));
            }

            @Override
            Object getValueInView(Dialog dialog) {
                // TODO would it be better to return a Parcelable here?
                // TODO In case of an activity I can save some code duplication...
                EditText tx = (EditText) dialog.findViewById(R.id.xEditText);
                EditText ty = (EditText) dialog.findViewById(R.id.yEditText);

                try {
                    double re = Double.parseDouble(tx.getText().toString());
                    double im = Double.parseDouble(ty.getText().toString());

                    return new at.searles.math.Cplx(re, im);
                } catch(NumberFormatException e) {
                    // null means error.
                    return null;
                }
            }

            @Override
            View createView(EditableDialogFragment ft) {
                return ft.getActivity().getLayoutInflater().inflate(R.layout.cplx_editor, null);
            }
        },
        Name {
            @Override
            void setValueInView(Object o, View view) {
                EditText v = (EditText) view.findViewById(R.id.editText);
                v.setText((String) o);
            }

            @Override
            Object getValueInView(Dialog dialog) {
                EditText v = (EditText) dialog.findViewById(R.id.editText);
                return v.getText().toString();
            }

            @Override
            View createView(EditableDialogFragment ft) {
                return ft.getActivity().getLayoutInflater().inflate(R.layout.string_editor, null);
            }
        },
        Color {
            @Override
            void setValueInView(Object o, View view) {
                ColorView colorView = (ColorView) view.findViewById(R.id.colorView);
                EditText webcolorEditor = (EditText) view.findViewById(R.id.webcolorEditText);

                webcolorEditor.setText(Colors.toColorString((Integer) o));
                colorView.setColor((Integer) o);
            }

            @Override
            Object getValueInView(Dialog view) {
                ColorView editor = (ColorView) view.findViewById(R.id.colorView);
                return editor.getColor(); // I use the ColorView.
            }

            @Override
            View createView(EditableDialogFragment ft) {
                View view = ft.getActivity().getLayoutInflater().inflate(R.layout.color_editor, null);
                // I initialize the view here.
                ColorView colorView = (ColorView) view.findViewById(R.id.colorView);
                EditText webcolorEditor = (EditText) view.findViewById(R.id.webcolorEditText);

                // I need listeners for both of them.
                colorView.bindToEditText(webcolorEditor);

                return view;
            }
        },
        LoadSharedPref {
            @Override
            void setValueInView(Object o, View view) {
            }

            @Override
            Object getValueInView(Dialog dialog) {
                throw new IllegalArgumentException("in list view this method is useless");
            }

            @Override
            View createView(EditableDialogFragment ft) {
                // there is only one listview in here, so use it right away.
                ListView lv = new ListView(ft.getActivity());

                SharedPreferences prefs =
                        ft.getActivity().getSharedPreferences(
                                ft.getArguments().getString("prefs_name"),
                                Context.MODE_PRIVATE);

                // initialize it.
                initSharedPrefListView(ft, lv, prefs);

                // Single click will dismiss the dialog and return the
                // selected element.
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // something was selected. Therefore, dismiss dialog.
                        String selected = (String) lv.getAdapter().getItem(position);

                        // successfully got sth.
                        ft.dismiss();
                        ft.getCallback().apply(ft.requestCode(), selected);
                    }
                });

                return lv;
            }

            @Override
            public boolean hasPositiveButton() {
                return false;
            }
        },
        SaveSharedPref {
            @Override
            void setValueInView(Object o, View view) {
                EditText saveEdit = (EditText) view.findViewById(R.id.saveNameEditText);
                saveEdit.setText(o.toString());
            }

            @Override
            Object getValueInView(Dialog dialog) {
                EditText saveEdit = (EditText) dialog.findViewById(R.id.saveNameEditText);
                String name = saveEdit.getText().toString();

                if(name.isEmpty()) {
                    Toast.makeText(dialog.getContext(), "Name must not be empty", Toast.LENGTH_LONG).show();
                    return null;
                }

                return name;
            }

            @Override
            View createView(EditableDialogFragment ft) {
                View view = ft.getActivity().getLayoutInflater().inflate(R.layout.save_layout, null);

                ListView lv = (ListView) view.findViewById(R.id.existingListView);
                EditText saveEdit = (EditText) view.findViewById(R.id.saveNameEditText);

                // initialize list view with existing data.
                SharedPreferences prefs =
                        ft.getActivity().getSharedPreferences(
                                ft.getArguments().getString("prefs_name"),
                                Context.MODE_PRIVATE);

                initSharedPrefListView(ft, lv, prefs);

                // Single click will set the textfield
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // something was selected. Therefore, dismiss dialog.
                        String selected = (String) lv.getAdapter().getItem(position);
                        saveEdit.setText(selected);
                    }
                });

                return view;
            }
        },
        ImageSize {
            @Override
            void setValueInView(Object o, View view) {
                // insert current size
                EditText widthView = (EditText) view.findViewById(R.id.widthEditText);
                EditText heightView = (EditText) view.findViewById(R.id.heightEditText);

                // This one reads a parcel
                int[] size = ((int[]) o);

                widthView.setText(Integer.toString(size[0]));
                heightView.setText(Integer.toString(size[1]));
            }

            @Override
            Object getValueInView(Dialog view) {
                // insert current size
                EditText widthView = (EditText) view.findViewById(R.id.widthEditText);
                EditText heightView = (EditText) view.findViewById(R.id.heightEditText);


                boolean setAsDefault = ((CheckBox) view.findViewById(R.id.defaultCheckBox)).isChecked();

                int w, h;

                try {
                    w = Integer.parseInt(widthView.getText().toString());
                } catch(NumberFormatException e) {
                    Toast.makeText(view.getContext(), "invalid width", Toast.LENGTH_LONG).show();
                    return null;
                }

                try {
                    h = Integer.parseInt(heightView.getText().toString());
                } catch(NumberFormatException e) {
                    Toast.makeText(view.getContext(), "invalid height", Toast.LENGTH_LONG).show();
                    return null;
                }

                if(w < 1) {
                    Toast.makeText(view.getContext(), "width must be >= 1", Toast.LENGTH_LONG).show();
                    return null;
                }

                if(h < 1) {
                    Toast.makeText(view.getContext(), "height must be >= 1", Toast.LENGTH_LONG).show();
                    return null;
                }

                return new int[]{w, h, setAsDefault ? 1 : 0};
            }

            @Override
            View createView(EditableDialogFragment ft) {
                View view =
                        ft.getActivity().getLayoutInflater().inflate(R.layout.image_size_editor, null);

                // insert current size
                EditText widthView = (EditText) view.findViewById(R.id.widthEditText);
                EditText heightView = (EditText) view.findViewById(R.id.heightEditText);

                // listener to button
                Button resetButton = (Button) view.findViewById(R.id.resetSizeButton);

                resetButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SharedPreferences prefs =
                                PreferenceManager.getDefaultSharedPreferences(view.getContext());

                        Point dim = new Point();

                        dim.set(prefs.getInt("width", -1), prefs.getInt("height", -1));

                        if (dim.x <= 0 || dim.y <= 0) {
                            dim = MainActivity.screenDimensions(view.getContext());
                        }

                        widthView.setText(Integer.toString(dim.x));
                        heightView.setText(Integer.toString(dim.y));
                    }
                });

                return view;
            }
        },
        Palette {
            @Override
            void setValueInView(Object o, View view) {
                // FIXME?
            }

            @Override
            Object getValueInView(Dialog dialog) {
                // FIXME?
                return null;
            }

            @Override
            View createView(EditableDialogFragment ft) {
                return ft.getActivity().getLayoutInflater().inflate(R.layout.palette_editor, null);
            }
        },
        StringList {
            // TODO

            @Override
            void setValueInView(Object o, View view) {

            }

            @Override
            Object getValueInView(Dialog dialog) {
                return null;
            }

            @Override
            View createView(EditableDialogFragment ft) {
                throw new IllegalArgumentException("not yet implemented");
            }

            public boolean hasPositiveButton() {
                return false;
            }
        }, // selects one element from a string list
        ColorList {
            // TODO

            @Override
            void setValueInView(Object o, View view) {

            }

            @Override
            Object getValueInView(Dialog dialog) {
                return null;
            }

            @Override
            View createView(EditableDialogFragment ft) {
                throw new IllegalArgumentException("not yet implemented");
            }

            public boolean hasPositiveButton() {
                return false;
            }
        }  // selects one element from a color list
        ;

        /**
         * Sets the value in the view
         * @param o The value (must correspond to the Type's documentation).
         * @param view The view (must be the same that is inflated by this Type)
         */
        abstract void setValueInView(Object o, View view);

        // TODO Check whether it wouldn't be better if the next method throws
        //      exception in case of an error
        // TODO Replace dialog by something else so that this could also be used by sth
        //      different than dialogs.


        /**
         * Returns the object that is currently set in this view
         * @param dialog The dialog containing the view
         * @return The object. Not necessarily the same that is set in setValueInView
         * because setValueInView might contain a Parcel. null in case of an error.
         */
        abstract Object getValueInView(Dialog dialog);

        /**
         * In this method a view to show and edit the given type is shown.
         * This returns the entire view because possibly some listeners must
         * be set up.
         * @param ft Fragment that contains the view
         * @return The created view
         */
        abstract View createView(EditableDialogFragment ft);

        /**
         * @return true if this one has a "confirm" button. The list views don't have
         * it.
         */
        public boolean hasPositiveButton() {
            return true;
        }
    }

    private final static String[] SHARED_PREF_LV_OPTIONS = { "Delete", "Rename" };



    /**
     * The next one is for convenience because I need it in load pref and save pref.
     * @param lv Listview containing data of the shared preferences
     * @param prefs The shared preferences
     */
    static void initSharedPrefListView(EditableDialogFragment ft, ListView lv, SharedPreferences prefs) {
        // Create an adapter.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                ft.getActivity(),
                android.R.layout.select_dialog_item);
        lv.setAdapter(adapter);

        initializePreferencesAdapterData(prefs, adapter);

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String selected = adapter.getItem(position);

                DialogHelper.showOptionsDialog(view.getContext(), "Select an option", SHARED_PREF_LV_OPTIONS, true, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: {
                                        SharedPreferences.Editor edit = prefs.edit();
                                        edit.remove(selected);
                                        edit.apply();
                                        dialog.dismiss();
                                        initializePreferencesAdapterData(prefs, adapter);
                                    } break;
                                    case 1: {
                                        openRenamePreferenceDialog(view, selected, prefs, adapter);
                                    } break;
                                }
                            }
                        });
                return true;
            }
        });
    }

    private static void initializePreferencesAdapterData(SharedPreferences prefs, ArrayAdapter<String> adapter) {
        // add all elements to it.
        // but have them sorted.
        adapter.clear();

        TreeSet<String> set = new TreeSet<>();

        for(String entry : prefs.getAll().keySet()) {
            if(entry != null) {
                // Due to a bug there might be a key null.
                // This happens when an empty element is added.
                set.add(entry);
            } else {
                Log.w("EDF", "Palette contains one entry with key null!");
            }
        }

        adapter.addAll(set);
    }

    private static void openRenamePreferenceDialog(final View view, final String selected, final SharedPreferences prefs, ArrayAdapter<String> adapter) {
        DialogHelper.inputText(view.getContext(),
                "Rename " + selected, selected,
                new Commons.KeyAction() {
                    @Override
                    public void apply(String key) {
                        Context context = view.getContext();
                        if (SharedPrefsHelper.renameKey(context, selected, key, prefs) != null) {
                            initializePreferencesAdapterData(prefs, adapter);
                        }
                    }
                });
    }

}
