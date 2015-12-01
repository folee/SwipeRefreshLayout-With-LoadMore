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
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.demievil.example.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.pharmplus.xview.DebugLog;
import cn.pharmplus.xview.XListView;

public class ListViewActivity extends AppCompatActivity implements XListView.IXListViewListener {

	private SwipeRefreshLayout				mRefreshLayout;
	private XListView						mListView;
	private SimpleAdapter					mAdapter;
	private ArrayList<Map<String, Object>>	mData	= new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_listview);
		DebugLog.d("onCreate");
		mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
		mListView = (XListView) findViewById(R.id.list);

		mListView.setPullLoadEnable(true);
		mListView.setPullRefreshEnable(false);

		initAdapter();
		mListView.setAdapter(mAdapter);

		mRefreshLayout.setColorSchemeResources(R.color.google_blue, R.color.google_green, R.color.google_red, R.color.google_yellow);

		//use SwipeRefreshLayout OnRefreshListener to refresh data
		mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				simulateFetchingData();
			}
		});

		//use XListView IXListViewListener.onLoadMore to load more data
		mListView.setXListViewListener(this);
	}

	private void initAdapter() {
		for (int i = 0; i < 20; i++) {
			Map<String, Object> listItem = new HashMap<>();
			listItem.put("img", R.mipmap.ic_launcher);
			listItem.put("text", "Item " + i);
			mData.add(listItem);
		}
		mAdapter = new SimpleAdapter(this, mData, R.layout.list_item, new String[] { "img", "text" }, new int[] { R.id.img, R.id.text });
	}

	/**
	 * simulate getting new data when pull to refresh
	 */
	private void getNewTopData() {
		Map<String, Object> listItem = new HashMap<>();
		listItem.put("img", R.mipmap.ic_launcher);
		listItem.put("text", "New Top Item " + mData.size());
		mData.add(0, listItem);
	}

	/**
	 * simulate load more data to bottom
	 */
	private void getNewBottomData() {
		int size = mData.size();
		for (int i = 0; i < 3; i++) {
			Map<String, Object> listItem = new HashMap<>();
			listItem.put("img", R.mipmap.ic_launcher);
			listItem.put("text", "New Bottom Item " + (size + i));
			mData.add(listItem);
		}
	}

	/**
	 * ListView simulate update ListView and stop refresh after a time-consuming
	 * task
	 */
	private void simulateFetchingData() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				getNewTopData();
				mRefreshLayout.setRefreshing(false);
				mAdapter.notifyDataSetChanged();
				Toast.makeText(ListViewActivity.this, "Refresh Finished!", Toast.LENGTH_SHORT).show();
			}
		}, 2000);
	}

	@Override
	public void onRefresh() {

	}

	/**
	 * ListView simulate update ListView and stop load more after after a
	 * time-consuming task
	 */
	@Override
	public void onLoadMore() {
		mRefreshLayout.setRefreshing(true);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				getNewBottomData();
				mRefreshLayout.setRefreshing(false);
				mAdapter.notifyDataSetChanged();
				mListView.loadFinish();
				Toast.makeText(ListViewActivity.this, "Load Finished!", Toast.LENGTH_SHORT).show();
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
		//手动刷新
		//refresh manually
		case R.id.action_refresh:
			if (mRefreshLayout != null) {
				mListView.setSelection(0);
				startRefreshIconAnimation(item);
				mRefreshLayout.setRefreshing(true);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						getNewTopData();
						mAdapter.notifyDataSetChanged();
						mRefreshLayout.setRefreshing(false);
						stopRefreshIconAnimation(item);
						Toast.makeText(ListViewActivity.this, "Refresh Finished!", Toast.LENGTH_SHORT).show();
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
