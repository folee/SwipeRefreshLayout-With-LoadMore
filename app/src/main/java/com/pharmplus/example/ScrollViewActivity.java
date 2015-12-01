package com.pharmplus.example;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.demievil.example.R;

import java.util.ArrayList;
import java.util.Map;

import cn.pharmplus.xview.DebugLog;
import cn.pharmplus.xview.XScrollView;

public class ScrollViewActivity extends AppCompatActivity implements XScrollView.IXScrollViewListener{

	private SwipeRefreshLayout mRefreshLayout;
	private XScrollView mScrollView;
	private ArrayList<Map<String, Object>>	mData	= new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scrollview);
		DebugLog.d("onCreate");
		mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
		mScrollView = (XScrollView) findViewById(R.id.scrollview);

		mScrollView.setPullLoadEnable(true);
		mScrollView.setPullRefreshEnable(false);
		//use customed IXScrollViewListener.onLoadMore to load more data
		mScrollView.setIXScrollViewListener(this);

		mRefreshLayout.setColorSchemeResources(R.color.google_blue, R.color.google_green, R.color.google_red, R.color.google_yellow);
		//use SwipeRefreshLayout OnRefreshListener to refresh data
		mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				simulateFetchingData();
			}
		});


	}



	/**
	 * XScrollView simulate update XScrollView and stop refresh after
	 * a time-consuming task
	 */
	private void simulateFetchingData() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mRefreshLayout.setRefreshing(false);
				Toast.makeText(ScrollViewActivity.this, "Refresh Finished!", Toast.LENGTH_SHORT).show();
			}
		}, 2000);
	}

	@Override
	public void onRefresh() {

	}

	/**
	 * XScrollView simulate update XScrollView and stop load more
	 * after after a time-consuming task
	 */
	@Override
	public void onLoadMore() {
		mRefreshLayout.setRefreshing(true);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				//mRefreshLayout.setLoading(false);
				mRefreshLayout.setRefreshing(false);
				mScrollView.loadFinish();
				Toast.makeText(ScrollViewActivity.this, "Load Finished!", Toast.LENGTH_SHORT).show();
			}
		}, 2000);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		final MenuItem refreshItem = menu.findItem(R.id.action_refresh);
		refreshItem.getActionView().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.performIdentifierAction(refreshItem.getItemId(), 0);
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		//refresh manually
		case R.id.action_refresh:
			if (mRefreshLayout != null) {
				startRefreshIconAnimation(item);
				mRefreshLayout.setRefreshing(true);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						mRefreshLayout.setRefreshing(false);
						stopRefreshIconAnimation(item);
						Toast.makeText(ScrollViewActivity.this, "Refresh Finished!", Toast.LENGTH_SHORT).show();
					}
				}, 2000);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void startRefreshIconAnimation(MenuItem item) {
		Animation rotation = AnimationUtils.loadAnimation(this, R.anim.refresh_icon_rotate);
		rotation.setRepeatCount(Animation.INFINITE);
		item.getActionView().startAnimation(rotation);
		item.getActionView().setClickable(false);
	}

	private void stopRefreshIconAnimation(MenuItem item) {
		if (item.getActionView() != null) {
			item.getActionView().clearAnimation();
			item.getActionView().setClickable(true);
		}
	}

}
