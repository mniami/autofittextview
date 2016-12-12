package pl.bydgoszcz.guideme.autofittextview;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;
import android.view.ViewTreeObserver;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AutoFitScrollViewTest {
    private static final String SAMPLE_TEXT = "<h1>LONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXT\n" +
            "LONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXT\n" +
            "LONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXTLONG TEXT</h1>";
    private AutoFitScrollView autoFitTextView;
    private TestActivity testActivity;
    private boolean testFinished;
    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setUp() {
        testActivity = activityRule.getActivity();

        testActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                autoFitTextView = AutoFitScrollView.with(testActivity.scrollView, testActivity.linearLayout);
            }
        });
    }

    @After
    public void tearDown() {
    }

    @Test
    public void test1() throws InterruptedException {
        while (!testActivity.created) {
            Thread.sleep(100);
        }
        testActivity.scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                System.out.print("hello");
            }
        });
        testActivity.textView1.setText(Html.fromHtml(SAMPLE_TEXT));
        while (!autoFitTextView.isBlockedScrolling) {
            Thread.sleep(100);
        }
    }
}
