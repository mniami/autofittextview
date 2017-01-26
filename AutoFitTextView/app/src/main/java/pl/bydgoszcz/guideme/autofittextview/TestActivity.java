package pl.bydgoszcz.guideme.autofittextview;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("deprecation")
public class TestActivity extends Activity {
    private AutoFitScrollView autoFitTextView;
    private TextView textView2;
    private TextView textViewStatus;
    TextView textView1;
    LinearLayout linearLayout;
    ScrollView scrollView;
    boolean created;
    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_layout);

        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        linearLayout = (LinearLayout) findViewById(R.id.textLayout);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        super.onCreate(savedInstanceState);
        autoFitTextView = AutoFitScrollView.with(scrollView, linearLayout);
        textView1.setText(Html.fromHtml(getString(R.string.long_text_1)));
        textView2.setText(Html.fromHtml(getString(R.string.long_text_1)));
        SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        delaySetStatus();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150 + progress * 14);
                scrollView.setLayoutParams(params);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        Button button = (Button)findViewById(R.id.button_1);
        button.setOnClickListener(v -> setText(Html.fromHtml(getString(R.string.long_text_1))));
        button = (Button)findViewById(R.id.button_2);
        button.setOnClickListener(v -> setText(Html.fromHtml(getString(R.string.long_text_2))));
        button = (Button)findViewById(R.id.button_3);
        button.setOnClickListener(v -> setText(Html.fromHtml(getString(R.string.long_text_3))));
        button = (Button)findViewById(R.id.button_4);
        button.setOnClickListener(v -> setText(Html.fromHtml(getString(R.string.long_text_4))));
        created = true;
    }

    private void delaySetStatus(){
        handler.postDelayed(()->{
            if (textViewStatus != null && autoFitTextView != null){
                Object tag = textViewStatus.getTag();
                if (tag != null && (boolean)tag != autoFitTextView.isScaling || tag == null) {
                    String status = autoFitTextView.isScaling ? "Scaling" : "Finished";
                    textViewStatus.setText(status);
                    textViewStatus.setTag(autoFitTextView.isScaling);
                }
            }
            delaySetStatus();
        }, 100);
    }

    private void setText(Spanned spanned) {
        autoFitTextView.reset();

        textView1.setText(spanned);
        textView2.setText(spanned);
    }

    @Override
    protected void onDestroy() {
        autoFitTextView.recycle();
        super.onDestroy();
    }
}
