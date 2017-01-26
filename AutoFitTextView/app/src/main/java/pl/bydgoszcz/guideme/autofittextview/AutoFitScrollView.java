package pl.bydgoszcz.guideme.autofittextview;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AutoFitScrollView {
    private static final int MAX_STEPS_COUNT = 50;
    private static final int PX_TOLERANCE = 3; //consider dp

    private WeakReference<ScrollView> scrollViewReference;
    private WeakReference<ViewGroup> internalLayoutReference;

    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

    private static KLogger logger = KLogger.from("AutoFitScrollView");
    private boolean inChanging = false;
    private int scrollViewWidth;
    private int scrollViewHeight;

    boolean isBlockedScrolling = true;
    boolean isScaling = false;

    private List<Step> steps;

    private AutoFitScrollView() {
        steps = new ArrayList<Step>();
        addStepToList(new Step(1));
    }

    public static AutoFitScrollView with(ScrollView scrollView, ViewGroup childView) {
        final AutoFitScrollView autoFitTextView = new AutoFitScrollView();
        autoFitTextView.scrollViewReference = new WeakReference<>(scrollView);
        autoFitTextView.internalLayoutReference = new WeakReference<>(childView);
        autoFitTextView.initialize();
        return autoFitTextView;
    }

    public void recycle() {
        final ScrollView scrollView = scrollViewReference.get();
        if (scrollView != null) {
            scrollView.setOnTouchListener(null);
            scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }

    public void reset(){
        if (steps != null){
            steps.clear();
            addStepToList(new Step(1));
        }
        if (internalLayoutReference != null && scrollViewReference != null) {
            final ScrollView scrollView = scrollViewReference.get();
            final ViewGroup internalLayout = internalLayoutReference.get();

            if (scrollView != null){
                scrollViewWidth = scrollView.getWidth();
                scrollViewHeight = scrollView.getHeight();
            }
            if (internalLayout != null) {
                internalLayout.setPivotX(0);
                internalLayout.setPivotY(0);
                internalLayout.setScaleY(1f);
                internalLayout.setScaleX(1f);
            }
        }
    }

    private void initialize() {
        onGlobalLayoutListener = AutoFitScrollView.this::onGlobalLayout;
        View.OnTouchListener onTouchListener = (v, event) -> isBlockedScrolling;

        final ScrollView scrollView = scrollViewReference.get();

        if (scrollView != null) {
            scrollView.setOnTouchListener(onTouchListener);
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }

    private void onGlobalLayout() {
        logger.fine(() -> "onGlobalLayout");

        final ScrollView scrollView = scrollViewReference.get();
        final ViewGroup internalLayout = internalLayoutReference.get();

        if (scrollView != null && internalLayout != null && !inChanging) {
            if (hasScrollViewSizeChanged(scrollView)){
                reset();
            }

            if (process(scrollView, internalLayout)){
                scrollViewWidth = scrollView.getWidth();
                scrollViewHeight = scrollView.getHeight();
                isScaling = false;
                internalLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean hasScrollViewSizeChanged(ScrollView scrollView) {
        return scrollView.getWidth() != scrollViewWidth || scrollView.getHeight() != scrollViewHeight;
    }

    private boolean process(ScrollView scrollView, ViewGroup internalLayout) {
        logger.fine(() -> "process");

        //check if requires scaling
        final int availableSpace = availableSpace(scrollView, internalLayout);

        steps.get(0).availableSpace = availableSpace;
        //check if we reached px tolerance and still have available space
        //reaching px tolerance without available space may cut content
        //TODO consider removing
        if (availableSpace >= 0 && availableSpace < PX_TOLERANCE) {
            return true;
        }

        if (steps.size() >= MAX_STEPS_COUNT) {
            if (availableSpace > 0) {
                return true;
            }
            //ensure last step is downscaling
            for (int i = 0; i < steps.size(); i++) {
                final Step step = steps.get(i);
                if (Step.StepDirection.DOWN.equals(step.stepDirection) && step.availableSpace >= 0) {
                    addStepToList(step);
                    resize(scrollView, internalLayout, step.scale);
                    break;
                }
            }
            return true;
        }

        if (availableSpace < 0) {
            scaleDown(scrollView, internalLayout);
        } else {
            scaleUp(scrollView, internalLayout);
        }
        return false;
    }

    private void scaleUp(ScrollView scrollView, ViewGroup internalLayout) {
        final Step previousStep = steps.get(0);
        if (Step.StepDirection.START.equals(previousStep.stepDirection)) {
            //If step would be to upscale there is no need to do anything
            return;
        }

        logger.fine(() -> "scale up processing");
        final float dScale = adjustDScale(previousStep.stepDirection, Step.StepDirection.UP, previousStep.dScale);
        final float newScale = previousStep.scale + dScale;

        final Step newStep = new Step(newScale, Step.StepDirection.UP, dScale);
        addStepToList(newStep);
        resize(scrollView, internalLayout, newScale);
    }

    private void scaleDown(ScrollView scrollView, ViewGroup internalLayout) {
        final Step previousStep = steps.get(0);
        logger.fine(() -> "scale down processing");
        final float dScale = adjustDScale(previousStep.stepDirection, Step.StepDirection.DOWN, previousStep.dScale);
        final float newScale = previousStep.scale - dScale;

        final Step newStep = new Step(newScale, Step.StepDirection.DOWN, dScale);
        addStepToList(newStep);
        resize(scrollView, internalLayout, newScale);
    }

    private int availableSpace(ScrollView scrollView, ViewGroup internalLayout) {
        final int childrenHeightSum = getChildrenHeightSum(internalLayout);
        final int containerHeight = scrollView.getMeasuredHeight();

        return containerHeight - childrenHeightSum;
    }

    private float adjustDScale(final Step.StepDirection previousDirection,
                               final Step.StepDirection newDirection,
                               float previousDScale ) {
        if (Step.StepDirection.START.equals(previousDirection)) {
            return previousDScale;
        }
        if (previousDirection.equals(newDirection)) {
            return previousDScale;
        }
        return previousDScale * Step.SCALE_STEP_Q;
    }

    private boolean resize(ScrollView scrollView, ViewGroup internalLayout, float newScale) {
        logger.fine(() -> String.format("resize to %s", newScale));

        inChanging = true;
        isScaling = true;
        final int containerWidth = scrollView.getMeasuredWidth();
        final int newInternalWidth = (int) Math.floor(containerWidth / newScale);
        try {
            if (internalLayout.getVisibility() == View.VISIBLE){
                internalLayout.setVisibility(View.INVISIBLE);
            }

            internalLayout.setLayoutParams(new ScrollView.LayoutParams(newInternalWidth, scrollView.getHeight()));
            internalLayout.setPivotX(0);
            internalLayout.setPivotY(0);
            internalLayout.setScaleY(newScale);
            internalLayout.setScaleX(newScale);

            scrollView.scrollTo(0, 0);

        } finally {
            inChanging = false;
        }
        return true;
    }

    private void addStepToList(Step newStep) {
        steps.add(0, newStep);
    }

    private int getChildrenHeightSum(ViewGroup internalLayout) {
        int sum = 0;
        for (int i = 0; i < internalLayout.getChildCount(); i++) {
            sum += internalLayout.getChildAt(i).getMeasuredHeight();
        }
        return (int) (sum * internalLayout.getScaleY());
    }

    private static class Step {

        private static final float SCALE_INITIAL_STEP = 0.1f;
        private static final float SCALE_STEP_Q = 0.5f;

        private enum StepDirection{
            START,
            UP,
            DOWN
        }

        private float scale;
        private float dScale;
        private StepDirection stepDirection;
        private int availableSpace=-1;

        Step(float scale){
            this(scale, StepDirection.START, SCALE_INITIAL_STEP);
        }

        Step(float scale, StepDirection stepDirection, float dScale) {
            this.scale = scale;
            this.dScale = dScale;
            this.stepDirection = stepDirection;
        }
    }
}
