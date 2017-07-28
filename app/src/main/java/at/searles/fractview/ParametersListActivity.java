package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

import at.searles.fractal.Fractal;
import at.searles.fractal.ParameterEntry;
import at.searles.fractal.android.BundleAdapter;
import at.searles.meelan.CompileException;

/**
 * This activity fetches a fractal and overrides its data/parameters
 * with a preset value, then it returns the new fractal. Data
 * are always merged here.
 */
public class ParametersListActivity extends Activity {

    private Fractal inFractal;
    private ParameterListAdapter adapter;

    // FIXME: First current, then assets, then elements stored in shared preference.
    // FIXME: Show only those, that are compilable with source


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parameters_list_layout);

        Intent intent = getIntent();
        this.inFractal = BundleAdapter.bundleToFractal(intent.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));

        // parse fractal
        try {
            inFractal.parse();
        } catch (CompileException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }

        adapter = new ParameterListAdapter(this, inFractal);

        initListView();

        Button closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // end this activity.
                ParametersListActivity.this.finish();
            }
        });
    }

    private void initListView() {
        ListView lv = (ListView) findViewById(R.id.parametersListView);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                ParameterEntry selected = adapter.getItem(index);
                returnFractal(selected, false);
            }
        });

        // TODO Check SingleChoiceSelection whether it is helpful in this case.

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int index, long id) {
                ParameterEntry selected = adapter.getItem(index);

                AlertDialog.Builder builder = new AlertDialog.Builder(ParametersListActivity.this);

                String[] items = {
                        "Merge Parameters",
                        "Use Default Parameters" };

                builder.setItems(items,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: { // Merge
                                        returnFractal(selected, true);
                                    }
                                    break;
                                    case 1: { // Default
                                        returnFractal(selected, false);
                                    }
                                    break;
                                    default:
                                        throw new IllegalArgumentException("no such selection: " + which);
                                }
                            }
                        });

                builder.setCancelable(true);
                builder.show();

                return true;
            }
        });
    }

    private void returnFractal(ParameterEntry selected, boolean merge) {
        Fractal outFractal = inFractal.copyNewData(selected.parameters, merge);

        Intent data = new Intent();

        data.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(outFractal));
        setResult(1, data);
        finish();
    }

    private static class ParameterListAdapter extends FractalListAdapter<ParameterEntry> {

        private ParameterEntry inEntry;
        private ArrayList<ParameterEntry> entries;

        ParameterListAdapter(Activity context, Fractal inFractal) {
            super(context);
            inEntry = new ParameterEntry("Current", null, "", inFractal.parameterMap());

            this.entries = new ArrayList<>();

            for(ParameterEntry entry : ParameterEntry.entries(context.getAssets())) {
                // Only those that work out of the box
                if(inFractal.parameters().containsAll(entry.parameters.keySet())) {
                    entries.add(entry);
                }
            }
        }

        @Override
        public int getCount() {
            return 1 + entries.size();
        }

        @Override
        public ParameterEntry getItem(int position) {
            if(position == 0) {
                return inEntry;
            } else {
                return entries.get(position - 1);
            }
        }

        @Override
        public String getTitle(int position) {
            return getItem(position).title;
        }

        @Override
        public Bitmap getIcon(int position) {
            return getItem(position).icon;
        }

        @Override
        public String getDescription(int position) {
            return getItem(position).description;
        }
    }
}
