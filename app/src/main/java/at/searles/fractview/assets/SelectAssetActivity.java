package at.searles.fractview.assets;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import at.searles.fractal.data.FractalData;
import at.searles.fractview.R;
import at.searles.fractview.fractal.BundleAdapter;

/**
 * This activity allows to select from a set of parameter sets.
 * Since it must decide whether it is applicable to a given program
 * it needs a list of compatible parameters. It contains
 * one special entry 'default' followed by all compatible ones.
 *
 * It returns an instance of Parameters.
 */
public class SelectAssetActivity extends Activity {

    private static final String IN_FRACTAL_LABEL = "inFractalData";

    private FractalData input;

    private SourceListAdapter sourceEntriesAdapter;
    private ParameterListAdapter parametersEntriesAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parameters_list_layout);

        Intent intent = getIntent();
        this.input = BundleAdapter.fractalFromBundle(intent.getBundleExtra(IN_FRACTAL_LABEL));

        sourceEntriesAdapter = new SourceListAdapter(this, this.input.source);
        parametersEntriesAdapter = new ParameterListAdapter(this, this.input.data);

        Button closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // end this activity.
                SelectAssetActivity.this.finish();
            }
        });
    }

//    private void initListView() {
//        ListView lv = (ListView) findViewById(R.id.parametersListView);
//
//        lv.setAdapter(adapter);
//
//        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
//                ParametersEntry selected = adapter.getItem(index);
//                returnFractal(selected, false);
//            }
//        });
//
//        // TODO Check SingleChoiceSelection whether it is helpful in this case.
//
//        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int index, long id) {
//                ParametersEntry selected = adapter.getItem(index);
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(SelectAssetActivity.this);
//
//                String[] items = {
//                        "Merge Parameters",
//                        "Use Default Parameters" };
//
//                builder.setItems(items,
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                switch (which) {
//                                    case 0: { // Merge
//                                        returnFractal(selected, true);
//                                    }
//                                    break;
//                                    case 1: { // Default
//                                        returnFractal(selected, false);
//                                    }
//                                    break;
//                                    default:
//                                        throw new IllegalArgumentException("no such selection: " + which);
//                                }
//                            }
//                        });
//
//                builder.setCancelable(true);
//                builder.show();
//
//                return true;
//            }
//        });
//    }

//    private void returnFractal(ParameterEntry selected, boolean merge) {
//        Fractal outFractal = inFractal.copyNewData(selected.parameters, merge);
//
//        Intent data = new Intent();
//
//        data.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(outFractal));
//        setResult(1, data);
//        finish();
//    }

}
