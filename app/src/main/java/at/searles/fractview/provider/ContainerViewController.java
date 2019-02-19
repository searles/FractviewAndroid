package at.searles.fractview.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import java.util.Map;
import java.util.TreeMap;

import at.searles.fractview.R;
import at.searles.fractview.bitmap.ui.CalculatorView;

class ContainerViewController {

    // the next ones are recreated when the view is created.

    private LinearLayout containerView;
    private Map<Integer, CalcViewGroup> views; // mapping id -> button
    private int selectedId;
    private FractalProviderFragment parent;

    ContainerViewController(FractalProviderFragment parent) {
        this.parent = parent;
        this.views = new TreeMap<>();
        this.selectedId = -1;
    }

    View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, Map<Integer, CalculatorWrapper> calculatorWrappers) {
        this.containerView = (LinearLayout) inflater.inflate(R.layout.fractalview_layout, container);

        for(CalculatorWrapper wrapper : calculatorWrappers.values()) {
            addView(wrapper);
        }

        return this.containerView;
    }

    void onDestroyView() {
        this.containerView = null;
        this.views.clear();
    }

    void addWrapper(CalculatorWrapper wrapper) {
        if(containerView != null) {
            addView(wrapper);
        }
    }

    void remove(int id) {
        CalcViewGroup viewGroup = views.remove(id);

        containerView.removeView(viewGroup.selectorButton);
        containerView.removeView(viewGroup.calculatorView);

        updateViewPadding();
    }

    private void addView(CalculatorWrapper wrapper) {
        RadioButton selectorButton = new RadioButton(containerView.getContext());

        selectorButton.setText("View " + wrapper.id()); // fixme
        selectorButton.setOnClickListener(src -> {
            parent.setSelectedId(wrapper.id());
            updateSelection();
        });

        CalculatorView calcView = wrapper.createView();

        containerView.addView(selectorButton);
        containerView.addView(calcView);

        CalcViewGroup viewGroup = new CalcViewGroup(selectorButton, calcView);
        views.put(wrapper.id(), viewGroup);

        updateViewPadding();
    }

    /**
     * Fetch selection from parent.
     */
    void updateSelection() {
        if(views.isEmpty()) {
            return;
        }

        if(selectedId != parent.selectedId()) {
            // unselect old one if it exists
            CalcViewGroup viewGroup = views.get(selectedId);

            if(viewGroup != null) {
                viewGroup.selectorButton.setChecked(false);
            }

            selectedId = parent.selectedId();
            views.get(selectedId).selectorButton.setChecked(true);
        }
    }

    private void updateViewPadding() {
        if(views.size() == 1) {
            // hide selector and set padding to 0
            CalcViewGroup viewGroup = views.values().iterator().next();
            viewGroup.selectorButton.setVisibility(View.GONE);
            viewGroup.calculatorView.setPadding(0, 0, 0, 0);
        } else {
            for(CalcViewGroup viewGroup : views.values()) {
                viewGroup.selectorButton.setVisibility(View.VISIBLE);
                // fixme use dpi instead
                viewGroup.calculatorView.setPadding(20, 0, 0, 10);
            }
        }
    }

    public void invalidate() {
        if(containerView != null) {
            containerView.invalidate();
        }
    }

    class CalcViewGroup {
        final RadioButton selectorButton;
        final CalculatorView calculatorView;

        CalcViewGroup(RadioButton selectorButton, CalculatorView calculatorView) {
            this.selectorButton = selectorButton;
            this.calculatorView = calculatorView;
        }
    }
}
