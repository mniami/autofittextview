package pl.bydgoszcz.guideme.autofittextview;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import java.lang.ref.WeakReference;

public class AutoFitTextView {
    private WeakReference<ScrollView> scrollViewReference;
    private WeakReference<ViewGroup> internalLayoutReference;

    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private View.OnTouchListener onTouchListener;

    protected boolean inChanging = false;
    protected boolean isBlockedScrolling;

    protected int scaleChances = 0;

    public static AutoFitTextView with(ScrollView scrollView, ViewGroup childView){
        final AutoFitTextView autoFitTextView = new AutoFitTextView();
        autoFitTextView.scrollViewReference = new WeakReference<>(scrollView);
        autoFitTextView.internalLayoutReference = new WeakReference<>(childView);
        autoFitTextView.initialize();
        return autoFitTextView;
    }

    private void initialize(){
        onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                AutoFitTextView.this.onGlobalLayout();
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

        boolean processed;

        if (!scaleDown(scrollView, internalLayout)) {
            processed = scaleUp(scrollView, internalLayout);
        }
        else {
            processed = true;
        }

        if (!processed){
            Log.d("autofit", "nth todo");
            internalLayout.setVisibility(View.VISIBLE);

            scaleChances = 0;
            isBlockedScrolling = true;
        }
    }

    private boolean scaleUp(ScrollView scrollView, ViewGroup internalLayout) {
        // checking
        final int childrenHeightSum = getChildrenHeightSum(internalLayout);
        final int containerHeight = scrollView.getMeasuredHeight();
        final int toleranceHeight = (int)(scrollView.getMeasuredHeight() * 0.1f);

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

            resize(scrollView, internalLayout, newScale, newContainerWidth);

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

            resize(scrollView, internalLayout, newScale, newContainerWidth);
            return true;
        }
        return false;
    }

    private void resize(ScrollView scrollView, ViewGroup internalLayout, float newScale, int newContainerWidth) {
        // flag
        inChanging = true;

        try {
            if (internalLayout.getVisibility() == View.VISIBLE){
                internalLayout.setVisibility(View.INVISIBLE);
            }
            final ScrollView.LayoutParams params = new ScrollView.LayoutParams(newContainerWidth, scrollView.getHeight());
            internalLayout.setLayoutParams(params);
            scrollView.updateViewLayout(internalLayout, params);

            updateChildren(internalLayout);

            internalLayout.setPivotX(0);
            internalLayout.setPivotY(0);
            internalLayout.setScaleY(newScale);
            internalLayout.setScaleX(newScale);

            scrollView.scrollTo(0, 0);
        }
        finally {
            inChanging = false;
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
}
