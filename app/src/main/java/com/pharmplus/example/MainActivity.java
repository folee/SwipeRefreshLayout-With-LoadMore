package com.pharmplus.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.demievil.example.R;

public class MainActivity extends AppCompatActivity {

	private final static String				TAG		= "MainActivity";
	private Button btn_listview;
	private Button btn_scrollview;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(TAG, "onCreate");
        btn_listview = (Button) findViewById(R.id.btn_listview);
        btn_scrollview = (Button) findViewById(R.id.btn_scrollview);

        btn_listview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplication(), ListViewActivity.class));
            }
        });
        btn_scrollview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplication(), ScrollViewActivity.class));
            }
        });
	}

}
