package com.slidingviewgroupdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


	private android.widget.TextView tvsettop;
	private android.widget.TextView tvdel;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);
		this.tvdel = (TextView) findViewById(R.id.tv_del);
		this.tvsettop = (TextView) findViewById(R.id.tv_set_top);
		tvdel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(MainActivity.this, "删除", Toast.LENGTH_SHORT).show();
			}
		});
		tvsettop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(MainActivity.this, "置顶", Toast.LENGTH_SHORT).show();
			}
		});
    }

}
