package pl.bydgoszcz.guideme.autofittextview;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class TestActivity extends Activity {
    private AutoFitScrollView autoFitTextView;
    private TextView textView2;
    protected TextView textView1;
    protected LinearLayout linearLayout;
    protected ScrollView scrollView;
    protected boolean created;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_layout);

        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        linearLayout = (LinearLayout) findViewById(R.id.textLayout);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        super.onCreate(savedInstanceState);
        autoFitTextView = AutoFitScrollView.with(scrollView, linearLayout);
        textView1.setText(Html.fromHtml(getString(R.string.long_text_1)));
        textView2.setText(Html.fromHtml(getString(R.string.long_text_1)));
        SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);

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
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setText(Html.fromHtml(getString(R.string.long_text_1)));
            }
        });
        button = (Button)findViewById(R.id.button_2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setText(Html.fromHtml(getString(R.string.long_text_2)));
            }
        });
        button = (Button)findViewById(R.id.button_3);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setText(Html.fromHtml(getString(R.string.long_text_3)));
            }
        });
        button = (Button)findViewById(R.id.button_4);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setText(Html.fromHtml(getString(R.string.long_text_4)));
            }
        });
        created = true;
    }

    private void setText(Spanned spanned) {
        textView1.setText(spanned);
        textView2.setText(spanned);
    }

    @Override
    protected void onDestroy() {
        autoFitTextView.recycle();
        super.onDestroy();
    }
}
