package at.searles.fractview.parameters.options;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import at.searles.fractal.ParameterTable;
import at.searles.fractview.R;
import at.searles.fractview.provider.FractalProviderFragment;
import at.searles.fractview.provider.view.parameters.ParameterLongSelectListener;

public class OptionsDialog implements DialogInterface.OnClickListener {

    private final int ITEM_VIEW_TYPE = 0;
    private final int CHECKABLE_VIEW_TYPE = 1;
    private final int VIEW_TYPE_COUNT = 2;

    private final String title;
    private final FractalProviderFragment provider;
    private final ParameterTable.Entry item;

    private List<ParameterLongSelectListener.Action> actions;
    private List<ParameterLongSelectListener.CheckableAction> checkableActions;

    public OptionsDialog(String title, FractalProviderFragment provider, ParameterTable.Entry item) {
        this.title = title;
        this.provider = provider;
        this.item = item;

        initActions();
    }

    private void initActions() {
        actions = Arrays.stream(Actions.values()).filter(action -> action.isApplicable(provider, item)).collect(Collectors.toList());
        checkableActions = Arrays.stream(CheckedActions.values()).filter(action -> action.isApplicable(provider, item)).collect(Collectors.toList());
    }

    public void show() {
        // show these simple dialogs to reset or center values.
        AlertDialog.Builder builder = new AlertDialog.Builder(provider.getContext());

        if(title != null) {
            builder.setTitle(title);
        }

        builder.setAdapter(new OptionsAdapter(), this);

        builder.setNegativeButton(R.string.closeDialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.setCancelable(true);
        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which < actions.size()) {
            actions.get(which).apply(provider, item);
            dialog.dismiss();
        } else {
            ParameterLongSelectListener.CheckableAction checkableAction = checkableActions.get(which - actions.size());
            checkableAction.setChecked(!checkableAction.isChecked(provider, item), provider, item);
        }
    }

    private class OptionsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return actions.size() + checkableActions.size();
        }

        @Override
        public Object getItem(int position) {
            return position < actions.size() ? actions.get(position) : checkableActions.get(position - actions.size());
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return position < actions.size() ? ITEM_VIEW_TYPE : CHECKABLE_VIEW_TYPE;
        }

        @Override
        public int getViewTypeCount() {
            // items and checked items
            return VIEW_TYPE_COUNT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);

            switch (viewType) {
                case ITEM_VIEW_TYPE:
                    return createItemView(position, convertView, parent);
                case CHECKABLE_VIEW_TYPE:
                    return createCheckableView(position - actions.size(), convertView, parent);
            }

            throw new IllegalArgumentException("unknown view id: " + viewType);
        }

        private View createItemView(int index, View view, ViewGroup parent) {
            if (view == null) {
                view = provider.getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            ParameterLongSelectListener.Action action = actions.get(index);

            TextView textView = view.findViewById(android.R.id.text1);

            textView.setText(action.title());

            return view;
        }

        private View createCheckableView(int index, View view, ViewGroup parent) {
            if (view == null) {
                view = provider.getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_checked, parent, false);
            }

            ParameterLongSelectListener.CheckableAction action = checkableActions.get(index);

            CheckedTextView checkedTextView = view.findViewById(android.R.id.text1);

            checkedTextView.setText(action.title());
            checkedTextView.setChecked(action.isChecked(provider, item));

            return view;
        }
    }
}
