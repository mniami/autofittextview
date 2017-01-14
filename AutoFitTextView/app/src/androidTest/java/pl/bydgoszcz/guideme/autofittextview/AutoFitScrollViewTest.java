package pl.bydgoszcz.guideme.autofittextview;

import android.support.annotation.NonNull;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static junit.framework.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AutoFitScrollViewTest {
    private AutoFitScrollView autoFitTextView;
    private TestActivity testActivity;
    private boolean initialized, finished;
    @NonNull
    @Rule
    public final ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setUp() {
        testActivity = activityRule.getActivity();

        testActivity.runOnUiThread(() -> {
            autoFitTextView = AutoFitScrollView.with(testActivity.scrollView, testActivity.linearLayout);
            initialized = true;
        });
    }

    @Test
    public void test40() throws InterruptedException {
        for (int i = 40; i < 300; i += 5) {
            testContent(i, createContent(i));
        }
    }

    private String createContent(int wordsCount) {
        final String word = "Hello ";
        StringBuilder sb = new StringBuilder(wordsCount * word.length());
        for (int i = 0; i < wordsCount; i++) {
            sb.append(word);
        }
        return sb.toString();
    }

    private void testContent(int wordsCount, final String content) throws InterruptedException {
        while (!testActivity.created && initialized) {
            Thread.sleep(100);
        }
        testActivity.runOnUiThread(() -> {
            //noinspection deprecation
            testActivity.textView1.setText(Html.fromHtml(content));
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            finished = true;
        });
        while (!finished) {
            Thread.sleep(100);
        }
        finished = false;
        final float internalLayoutHeight = testActivity.linearLayout.getHeight() * testActivity.linearLayout.getScaleY();
        final float containerLayoutHeight = testActivity.scrollView.getHeight() * 1.1f;

        assertTrue("Words count: " + wordsCount + " " + Arrays.toString(new float[]{internalLayoutHeight, containerLayoutHeight}), internalLayoutHeight < containerLayoutHeight);
    }
}
