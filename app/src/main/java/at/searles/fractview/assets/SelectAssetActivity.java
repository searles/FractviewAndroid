package at.searles.fractview.assets;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import at.searles.fractal.data.FractalData;
import at.searles.fractview.R;
import at.searles.fractview.favorites.FavoritesActivity;
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

    public static final String IN_FRACTAL_LABEL = "inFractalData";

    private FractalData input; // FIXME
    private AssetsListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.assets_list_layout);

        Intent intent = getIntent();
        FractalData input = BundleAdapter.fractalFromBundle(intent.getBundleExtra(IN_FRACTAL_LABEL));
        adapter = new AssetsListAdapter(this, input);

        initListView();
    }

    private void initListView() {
        RecyclerView lv = findViewById(R.id.assetsListView);
        lv.setAdapter(adapter);
        lv.setLayoutManager(new LinearLayoutManager(this));
//
//        lv.setOnChildClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
//                ParametersEntry selected = adapter.getItem(index);
//                returnFractal(selected, false);
//            }
//        });
//
//        lv.setOnChildLongC(new AdapterView.OnItemLongClickListener() {
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
    }

    public void returnFractal(FractalData data) {
        Intent intent = new Intent();

        // FIXME move that constant somewhere else.
        intent.putExtra(FavoritesActivity.FRACTAL_INDENT_LABEL, BundleAdapter.toBundle(data));
        setResult(1, intent);
        finish();
    }

}
