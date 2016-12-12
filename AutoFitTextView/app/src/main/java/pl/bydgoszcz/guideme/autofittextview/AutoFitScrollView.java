package pl.bydgoszcz.guideme.autofittextview;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class AutoFitScrollView {
    private WeakReference<ScrollView> scrollViewReference;
    private WeakReference<ViewGroup> internalLayoutReference;

    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private View.OnTouchListener onTouchListener;

    protected boolean inChanging = false;
    protected boolean isBlockedScrolling;

    protected int scaleChances = 0;

    protected List<Step> steps;

    public static AutoFitScrollView with(ScrollView scrollView, ViewGroup childView){
        final AutoFitScrollView autoFitTextView = new AutoFitScrollView();
        autoFitTextView.scrollViewReference = new WeakReference<>(scrollView);
        autoFitTextView.internalLayoutReference = new WeakReference<>(childView);
        autoFitTextView.initialize();
        return autoFitTextView;
    }

    private void initialize(){
        onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                AutoFitScrollView.this.onGlobalLayout();
            }
        };
        onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return isBlockedScrolling;
            }
        };

        final ScrollView scrollView = scrollViewReference.get();

        if (scrollView != null) {
            scrollView.setOnTouchListener(onTouchListener);
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }

    private void onGlobalLayout(){
        Log.d("autofit", "onGlobalLayout");

        final ScrollView scrollView = scrollViewReference.get();
        final ViewGroup internalLayout = internalLayoutReference.get();

        if (scrollView != null && internalLayout != null && !inChanging) {
            process(scrollView, internalLayout);
        }
    }

    private void process(ScrollView scrollView, ViewGroup internalLayout) {
        Log.d("autofit", "process ");

        boolean processed = false;

        if (!scaleDown(scrollView, internalLayout)) {
            if (isBlockedScrolling) {
                processed = scaleUp(scrollView, internalLayout);
            }
        } else {
            processed = true;
        }

        if (!processed){
            Log.d("autofit", "nth todo");
            internalLayout.setVisibility(View.VISIBLE);

            scaleChances = 0;
            isBlockedScrolling = true;
            steps.clear();
        }
    }

    private boolean scaleUp(ScrollView scrollView, ViewGroup internalLayout) {
        // checking
        final int childrenHeightSum = getChildrenHeightSum(internalLayout);
        final int containerHeight = scrollView.getMeasuredHeight();
        final int toleranceHeight = (int)(scrollView.getMeasuredHeight() * 0.01f);

        if (childrenHeightSum + toleranceHeight < containerHeight && scaleChances <= 2){
            Log.d("autofit", "scaleUp procesing");
            // inputs
            final float internalScaleY = internalLayout.getScaleY();
            final int containerWidth = scrollView.getMeasuredWidth();

            // calculations
            final int deltaHeight = containerHeight - childrenHeightSum;
            final float diffProportion = deltaHeight * 1.0f / containerHeight;

            // outputs
            final float newScale = internalScaleY * (1 + diffProportion);
            final int newContainerWidth = (int) Math.floor(containerWidth / newScale);

            resize(scrollView, internalLayout, newScale, newContainerWidth, false);

            scaleChances++;
            return true;
        }
        return false;
    }

    private boolean scaleDown(ScrollView scrollView, ViewGroup internalLayout) {
        // inputs
        final int containerHeight = scrollView.getMeasuredHeight();
        final int containerWidth = scrollView.getMeasuredWidth();
        final int internalHeight = internalLayout.getMeasuredHeight();
        final int scaledHeight = (int)Math.floor(internalHeight * internalLayout.getScaleY());

        if (containerHeight < scaledHeight) {
            Log.d("autofit", "scaleDown procesing");

            // outputs
            final float newScale = 1.0f - 1.0f * (internalHeight - containerHeight) / internalHeight;
            final int newContainerWidth = (int) Math.floor(containerWidth / newScale);

            resize(scrollView, internalLayout, newScale, newContainerWidth, true);
            return true;
        }
        return false;
    }

    private void resize(ScrollView scrollView, ViewGroup internalLayout, float newScale, int newContainerWidth, boolean forceChange) {
        Log.d("autofit", String.format("resize to %s", newScale));
        // flag

        final Step step = new Step(newScale, newContainerWidth);
        if (addStep(step, forceChange)) {
            inChanging = true;
            if (internalLayout.getVisibility() == View.VISIBLE){
                internalLayout.setVisibility(View.INVISIBLE);
            }
            try {
                internalLayout.setPivotX(0);
                internalLayout.setPivotY(0);
                internalLayout.setScaleY(newScale);
                internalLayout.setScaleX(newScale);

                scrollView.scrollTo(0, 0);
                changeWidth(scrollView, internalLayout, newContainerWidth);

            } finally {
                inChanging = false;
            }
        }
        else {
            Log.d("autofit", String.format("resize canceled, step already exists %s", step));
        }
    }

    private void changeWidth(ScrollView scrollView, ViewGroup internalLayout, int newWidth){
        Log.d("autofit", String.format("changeWidth to %s", newWidth));

        final ScrollView.LayoutParams params = new ScrollView.LayoutParams(newWidth, scrollView.getHeight());
        internalLayout.setLayoutParams(params);
        scrollView.updateViewLayout(internalLayout, params);

        updateChildren(internalLayout);
    }

    private boolean addStep(Step newStep, boolean forceChange) {
        if (steps == null){
            steps = new LinkedList<>();
        }
        if (forceChange){
            steps.add(newStep);
            return true;
        }
        else {
            boolean found = false;
            for (Step step : steps) {
                if (step.equals(newStep)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                steps.add(newStep);
            }
            return !found;
        }
    }

    private void updateChildren(ViewGroup internalLayout) {
        View child;

        for (int i = 0; i < internalLayout.getChildCount(); i++){
            child = internalLayout.getChildAt(i);
            child.setLayoutParams(child.getLayoutParams());

            internalLayout.updateViewLayout(child, child.getLayoutParams());
        }
    }

    private int getChildrenHeightSum(ViewGroup internalLayout) {
        int sum = 0;
        for (int i = 0; i < internalLayout.getChildCount(); i++){
            sum += internalLayout.getChildAt(i).getMeasuredHeight();
        }
        return (int)(sum * internalLayout.getScaleY());
    }

    public void recycle(){
        final ScrollView scrollView = scrollViewReference.get();
        if (scrollView != null) {
            scrollView.setOnTouchListener(null);
            scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }

    private static class Step {
        private float scale;
        private int width;

        public Step(float scale, int width){
            this.scale = scale;
            this.width = width;
        }

        public boolean equals(Step step) {
            return width == step.width && scale == step.scale;
        }
    }
}
