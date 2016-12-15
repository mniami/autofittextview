package pl.bydgoszcz.guideme.autofittextview;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;
import android.view.ViewTreeObserver;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.logging.Logger;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

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

        testActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoFitTextView = AutoFitScrollView.with(testActivity.scrollView, testActivity.linearLayout);
                autoFitTextView.logger = mock(Logger.class);

                doAnswer(new Answer<Void>() {
                    @Nullable
                    public Void answer(@NonNull InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        System.out.println(Arrays.toString(args));
                        return null;
                    }
                }).when(autoFitTextView.logger).fine(anyString());

                testActivity.scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        //System.out.print("hello");
                    }
                });
                initialized = true;
            }
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
        testActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //noinspection deprecation
                testActivity.textView1.setText(Html.fromHtml(content));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                finished = true;
            }
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
