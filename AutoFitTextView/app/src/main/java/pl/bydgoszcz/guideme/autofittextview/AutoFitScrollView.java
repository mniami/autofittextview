package pl.bydgoszcz.guideme.autofittextview;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class AutoFitScrollView {
    private static final int MAX_STEPS_COUNT = 50;
    private static KLogger logger = KLogger.from("AutoFitScrollView");
    private WeakReference<ScrollView> scrollViewReference;
    private WeakReference<ViewGroup> internalLayoutReference;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private boolean inChanging = false;
    private int scrollViewWidth;
    private int scrollViewHeight;
    private List<Step> steps = new LinkedList<>();
    boolean isBlockedScrolling = true;

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

    public void reset() {
        if (steps != null) {
            final ViewGroup internalLayout = internalLayoutReference.get();
            final ScrollView scrollView = scrollViewReference.get();

            if (internalLayout != null && scrollView != null) {
                internalLayout.setPivotX(0);
                internalLayout.setPivotY(0);
                internalLayout.setScaleY(1f);
                internalLayout.setScaleX(1f);
                cacheScrollViewSize();
            }
            clearSteps();
        }
    }

    private void initialize() {
        onGlobalLayoutListener = AutoFitScrollView.this::onGlobalLayout;
        View.OnTouchListener onTouchListener = (v, event) -> isBlockedScrolling;

        final ScrollView scrollView = scrollViewReference.get();

        if (scrollView != null) {
            cacheScrollViewSize();
            scrollView.setOnTouchListener(onTouchListener);
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }

    private void onGlobalLayout() {
        logger.fine(() -> "onGlobalLayout");

        final ScrollView scrollView = scrollViewReference.get();
        final ViewGroup internalLayout = internalLayoutReference.get();

        if (scrollView != null && internalLayout != null && !inChanging) {
            process(scrollView, internalLayout);
        }
    }

    private void process(ScrollView scrollView, ViewGroup internalLayout) {
        logger.fine(() -> "process");

        boolean processed = false;

        if (scrollViewHeight == 0 || scrollViewWidth == 0) {
            cacheScrollViewSize();
        }
        if (scrollViewHeight != scrollView.getHeight() || scrollViewWidth != scrollView.getWidth()) {
            logger.fine(() -> "reset, scrollview size has changed");
            reset();
        }

        Step lastStep = steps.size() > 0 ? steps.get(steps.size() - 1) : null;
        if (lastStep != null && lastStep.finalStep) {
            finalizeScaling(internalLayout);
            return;
        }
        if (!scaleDown(scrollView, internalLayout)) {
            if (isBlockedScrolling) {
                processed = scaleUp(scrollView, internalLayout);
            }
        } else {
            processed = true;
        }

        if (!processed) {
            finalizeScaling(internalLayout);
        }
    }

    private void finalizeScaling(ViewGroup internalLayout) {
        logger.fine(() -> "nth todo");
        if (internalLayout.getVisibility() == View.INVISIBLE) {
            internalLayout.setVisibility(View.VISIBLE);
        }
        isBlockedScrolling = true;
    }

    private void cacheScrollViewSize() {
        final ScrollView scrollView = scrollViewReference.get();
        if (scrollView != null) {
            scrollViewWidth = scrollView.getWidth();
            scrollViewHeight = scrollView.getHeight();
        }
    }

    private boolean scaleUp(ScrollView scrollView, ViewGroup internalLayout) {
        // checking
        final int MIN_PIXELS_OF_TOLERANCE = 2;
        final int containerHeight = scrollView.getMeasuredHeight();
        final int internalHeight = internalLayout.getMeasuredHeight();
        float internalScaleY = internalLayout.getScaleY();
        final int containerWidth = scrollView.getMeasuredWidth();
        final int internalHeightScaled = (int) Math.ceil(containerHeight - internalHeight * internalScaleY);

        if (internalHeightScaled + MIN_PIXELS_OF_TOLERANCE < containerHeight) {
            logger.fine(() -> "scale up processing");

            // calculations
            float deltaHeight = (containerHeight - internalHeight * internalScaleY) / 2f;
            final float diffProportion = deltaHeight * 1.0f / containerHeight;

            // outputs
            final float newScale = internalScaleY * (1 + diffProportion);
            final int newContainerWidth = (int) Math.floor(containerWidth / newScale);
            final Step step = new Step(newScale, newContainerWidth, true);

            return resize(scrollView, internalLayout, false, step);
        }
        return false;
    }

    private boolean scaleUpWithStepBack(ScrollView scrollView, ViewGroup internalLayout, Step badScaleUpStep) {
        logger.fine(() -> "Scale up, overflow the container height, step back and try again with the half of it");

        removeLastStep();

        // inputs
        final Step lastGoodScaleUpStep = getLastScaleUpStep();
        final int containerWidth = scrollView.getMeasuredWidth();

        // exceptions with loading scale
        if (lastGoodScaleUpStep == null || !lastGoodScaleUpStep.scaleUp) {
            logger.fine(() -> "Not found last good scale up step, canceling scale up");
            return false;
        } else {
            if (lastGoodScaleUpStep.scale >= badScaleUpStep.scale) {
                logger.fine(()->"Good scale is not good, error");
                return false;
            }
        }

        // outputs
        float newScale = (badScaleUpStep.scale + lastGoodScaleUpStep.scale) / 2f;
        final float scaleChangeDiff = Math.abs(newScale - lastGoodScaleUpStep.scale);
        final float SCALE_MAX_TOLERANCE = 0.00005f;

        if (scaleChangeDiff < SCALE_MAX_TOLERANCE) {
            // the scale diff is really small it means, there is no need to scale up
            return revertToTheLastGoodScaleUp(scrollView, internalLayout, lastGoodScaleUpStep);
        }

        final int newContainerWidth = (int) Math.floor(containerWidth / newScale);
        final Step step = new Step(newScale, newContainerWidth, true);
        return resize(scrollView, internalLayout, false, step);
    }

    private boolean revertToTheLastGoodScaleUp(ScrollView scrollView, ViewGroup internalLayout, Step lastGoodScaleUpStep){
        logger.fine(()->"Last good step is not good enough, removing it and trying one more time");
        lastGoodScaleUpStep.finalStep = true;
        return resize(scrollView, internalLayout, false, lastGoodScaleUpStep);
    }

    private boolean scaleDown(ScrollView scrollView, ViewGroup internalLayout) {
        // inputs
        final int containerHeight = scrollView.getMeasuredHeight();
        final int containerWidth = scrollView.getMeasuredWidth();
        final int internalHeight = internalLayout.getMeasuredHeight();
        final int scaledHeight = (int) Math.ceil(internalHeight * internalLayout.getScaleY());

        if (containerHeight < scaledHeight) {
            final Step lastStep = getLastScaleUpStep();
            if (lastStep != null && lastStep.scaleUp) {
                return scaleUpWithStepBack(scrollView, internalLayout, lastStep);
            }
            logger.fine(() -> "scale down processing");
            // outputs
            final float newScale = 1.0f - 1.0f * (internalHeight - containerHeight) / internalHeight;
            final int newContainerWidth = (int) Math.floor(containerWidth / newScale);
            final Step step = new Step(newScale, newContainerWidth, false);
            return resize(scrollView, internalLayout, false, step);
        }
        return false;
    }

    private void clearSteps() {
        logger.fine(() -> "Clear steps");
        steps.clear();
    }

    private void removeLastStep() {
        if (steps != null && steps.size() > 0) {
            logger.fine(() -> "Remove last step");
            steps.remove(steps.size() - 1);
        }
    }

    private Step getLastScaleUpStep() {
        if (steps.size() > 0) {
            Step step = steps.get(steps.size() - 1);
            if (step.scaleUp) {
                return step;
            }
        }
        return null;
    }

    private boolean resize(ScrollView scrollView, ViewGroup internalLayout, boolean forceChange, Step step) {
        logger.fine(() -> String.format("resize to %s", step.scale));
        addStep(step, forceChange);

        try {
            if (internalLayout.getVisibility() == View.VISIBLE) {
                //internalLayout.setVisibility(View.INVISIBLE);
            }

            internalLayout.setLayoutParams(new ScrollView.LayoutParams(step.width, scrollView.getHeight()));
            internalLayout.setPivotX(0);
            internalLayout.setPivotY(0);
            internalLayout.setScaleY(step.scale);
            internalLayout.setScaleX(step.scale);

            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {

            }
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Step s : steps) {
                sb.append(String.format("%s%s:%s\n", i, s.scaleUp ? "^" : "_", s.scale));
                i++;
            }
            logger.fine(() -> sb.toString());
        } finally {
            inChanging = false;
        }
        return true;
    }

    private boolean addStep(Step newStep, boolean forceChange) {
        if (steps == null) {
            steps = new LinkedList<>();
        }
        Step existedTheSameStep = null;
        if (forceChange) {
            addStepToList(newStep);
            return true;
        } else {
            for (Step step : steps) {
                if (step.equals(newStep)) {
                    existedTheSameStep = step;
                    break;
                }
            }
            if (existedTheSameStep == null) {
                addStepToList(newStep);
            }
            else {
                if (existedTheSameStep.finalStep){
                    return true;
                }
                existedTheSameStep.finalStep = true;
                steps.remove(existedTheSameStep);
                steps.add(existedTheSameStep);
            }
            return existedTheSameStep == null;
        }
    }

    private void addStepToList(Step newStep) {
        if (steps.size() >= MAX_STEPS_COUNT) {
            steps.remove(0);
        }
        steps.add(newStep);
    }

    private static class Step {
        private float scale;
        private int width;
        private boolean scaleUp;
        private boolean finalStep;

        Step(float scale, int width, boolean scaleUp) {
            this.scale = scale;
            this.width = width;
            this.scaleUp = scaleUp;
        }

        @Override
        public String toString() {
            return String.format("%s:%s\n", scaleUp ? "^" : "_", scale);
        }

        public boolean equals(Step step) {
            return step != null && width == step.width && Float.compare(scale, step.scale) == 0;
        }
    }
}
