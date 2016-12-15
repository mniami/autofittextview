# AutoFitTextView
Android component that solve the problem with not auto fiting content to the boundaries. Working on scaling instead of font size changing. Can operate on any view inside the container which is ScrollView.

## Component condition
Please notice it is early version of this product.

## Problem solving
Auto fit your internal layout content to the boundries defined by wrapped **ScrollView**.

## The reason behind
I cannot find any good solution that fit my internal content to the boundries so I decided to write it from sratch by my own.

## Usage
XML usage

	<ScrollView
        android:id="@+id/scrollView"
        android:layout_width="match_parent"
        android:layout_below="@id/title"
        android:background="@color/colorAccent"
        android:layout_height="100dp">

        <LinearLayout
            android:id="@+id/textLayout"
            android:background="@color/colorPrimaryDark"
            android:layout_width="match_parent"
            android:layout_height="wrap_content">

            <TextView
                android:id="@+id/textView1"
                android:textSize="34dp"
                android:textColor="@color/white"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"/>

        </LinearLayout>
    </ScrollView>
	
### In code

in *onCreate* method:
<pre>AutoFitTextView autoFitTextView = AutoFitTextView.with(scrollView, textLayout);</pre>

in *onDisposed* method:
<pre>autoFitTextView.recycle()</pre>

## Result
*textView1* content is fited into the boundaries of fixed ScrollView.







