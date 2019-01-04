package at.searles.fractview.favorites;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractal.gson.Serializers;
import at.searles.fractview.R;
import at.searles.fractview.ui.DialogHelper;

/**
 * This activity does not require any input, and on success it
 * returns to the caller FractalData of the picked fractal.
 *
 * The activity consists of a list with all favorites. It allows
 * the caller to delete and export single and multiple favorites.
 */
public class FavoritesActivity extends Activity {

    private static final int IMPORT_COLLECTION_CODE = 111;
    public static final String FRACTAL_INDENT_LABEL = "fractal";

    private FavoritesListAdapter adapter;
    private FavoritesAccessor accessor;

    /**
     * Selected elements
     */
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fractal_list_activity_layout);

        this.accessor = new FavoritesAccessor(this);
        this.adapter = new FavoritesListAdapter(this, accessor);

        initListView();
    }

    private void initListView() {
        this.listView = findViewById(R.id.fractalListView);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new ItemClickedListener(adapter));

        // Enable selection mode
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new ItemsSelectedListener(listView, accessor));
    }

    // ===== Default menu =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_favorites, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_import_collection: {
                importCollection();
            }
            return true;
            case R.id.action_select_all: {
                // FIXME is there a faster way?
                for (int i = 0; i < adapter.getCount(); ++i) {
                    listView.setItemChecked(i, true);
                }
            }
            return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void importCollection() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, IMPORT_COLLECTION_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            if (requestCode == IMPORT_COLLECTION_CODE) {
                // Fetch elements
                Uri uri = data.getData();

                try {
                    FavoriteEntry.Collection collection = importFromUri(uri);
                    if (collection == null) {
                        DialogHelper.error(this, "Could not read file!");
                        return;
                    }

                    accessor.importEntries(this, collection);
                } catch(Throwable th) {
                    th.printStackTrace();
                    DialogHelper.error(this, th.getLocalizedMessage());
                }
            }
        }
    }

    private FavoriteEntry.Collection importFromUri(Uri uri) throws FileNotFoundException {
        try(InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if(inputStream == null) {
                throw new IllegalArgumentException("it would be nice if the documentation of " +
                        "openInputStream would tell us why it can return null.");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            return Serializers.serializer().fromJson(reader, FavoriteEntry.Collection.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ===== If sth is selected =====

    /**
     * Select entries in adapter
     *
     * @param keys keys of the entries that should be selected
     */
    public void selectKeys(Set<String> keys) {
        // find indices and set checked-status.
        for (int i = 0; i < accessor.entriesCount(); ++i) {
            listView.setItemChecked(i, keys.contains(accessor.keyAt(i)));
        }
    }





}
