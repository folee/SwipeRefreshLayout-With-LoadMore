package cn.pharmplus.xview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * XListView, it's based on <br>
 * <a href="https://github.com/Maxwin-z/XListView-Android">XListView(Maxwin)</a>
 * 
 */
public class XListView extends ListView implements OnScrollListener {
	// private static final String TAG = "XListView";

	private final static int		SCROLL_BACK_HEADER	= 0;
	private final static int		SCROLL_BACK_FOOTER	= 1;

	private final static int		SCROLL_DURATION		= 400;

	// when pull up >= 50px
	//	private final static int	PULL_LOAD_MORE_DELTA	= 50;

	// support iOS like pull
	private final static float		OFFSET_RADIO		= 1.8f;

	private float					mLastY				= -1;

	// used for scroll back
	private Scroller				mScroller;
	// user's scroll listener
	private OnScrollListener		mScrollListener;
	// for mScroller, scroll back from header or footer.
	private int						mScrollBack;

	// the interface to trigger refresh and load more.
	private IXListViewListener		mListener;
	private HeadViewStretchListener	mHeadViewStretchListener;

	private XHeaderView				mHeader;
	// header view content, use it to calculate the Header's height. And hide it
	// when disable pull refresh.
	private RelativeLayout			mHeaderContent;
	private TextView				mHeaderTime;
	private int						mHeaderHeight;
	private int						mFooterHeight;

	private LinearLayout			mFooterLayout;
	private XFooterView				mFooterView;
	private boolean					mIsFooterReady		= false;

	private boolean					mEnablePullRefresh	= true;
	private boolean					mPullRefreshing		= false;

	private boolean					mEnablePullLoad		= true;
	private boolean					mEnableAutoLoad		= false;
	private boolean					mPullLoading		= false;
	private boolean					mNoMoreData			= false;

	// total list items, used to detect is at the bottom of ListView
	private int						mTotalItemCount;
	private int						mVisibleItemCount;

	public XListView(Context context) {
		super(context);
		initWithContext(context);
	}

	public XListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initWithContext(context);
	}

	public XListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initWithContext(context);
	}

	private void initWithContext(Context context) {
		mScroller = new Scroller(context, new DecelerateInterpolator());
		super.setOnScrollListener(this);

		// init header view
		mHeader = new XHeaderView(context);
		mHeaderContent = (RelativeLayout) mHeader.findViewById(R.id.header_content);
		mHeaderTime = (TextView) mHeader.findViewById(R.id.header_hint_time);
		addHeaderView(mHeader);

		// init footer view
		mFooterView = new XFooterView(context);
		mFooterLayout = new LinearLayout(context);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		params.gravity = Gravity.CENTER;
		mFooterLayout.addView(mFooterView, params);

		// init header height
		ViewTreeObserver observer = mHeader.getViewTreeObserver();
		if (null != observer) {
			observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@SuppressWarnings("deprecation")
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@Override
				public void onGlobalLayout() {
					mHeaderHeight = mHeaderContent.getHeight();
					ViewTreeObserver observer = getViewTreeObserver();

					if (null != observer) {
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
							observer.removeGlobalOnLayoutListener(this);
						}
						else {
							observer.removeOnGlobalLayoutListener(this);
						}
					}
				}
			});
		}

		// init footer height
		ViewTreeObserver footerObserver = mFooterView.getViewTreeObserver();
		if (null != footerObserver) {
			footerObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@SuppressWarnings("deprecation")
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@Override
				public void onGlobalLayout() {
					mFooterHeight = mFooterView.getVisibleHeight();//getHeight();
					ViewTreeObserver observer = getViewTreeObserver();

					if (null != observer) {
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
							observer.removeGlobalOnLayoutListener(this);
						}
						else {
							observer.removeOnGlobalLayoutListener(this);
						}
					}
				}
			});
		}
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		// make sure XFooterView is the last footer view, and only add once.
		if (!mIsFooterReady) {
			mIsFooterReady = true;
			if (mEnablePullLoad) {
				addFooterView(mFooterLayout);
			}
			resetFooterHeight();
		}

		super.setAdapter(adapter);
	}

	/**
	 * Enable or disable pull down refresh feature.
	 * 
	 * @param enable
	 */
	public void setPullRefreshEnable(boolean enable) {
		mEnablePullRefresh = enable;

		// disable, hide the content
		mHeaderContent.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
	}

	/**
	 * Enable or disable pull up load more feature.
	 * 
	 * @param enable
	 */
	public void setPullLoadEnable(boolean enable) {
		mEnablePullLoad = enable;

		mFooterView.hide();
		if (!mEnablePullLoad) {
			mFooterView.setBottomMargin(0);
			//mFooterView.hide();
			mFooterView.setPadding(0, 0, 0, 0);
			mFooterView.setOnClickListener(null);
			removeFooterView(mFooterLayout);
		}
		else {
			mPullLoading = false;
			mFooterView.setPadding(0, 0, 0, 0);
			//mFooterView.show();
			mFooterView.setState(XFooterView.STATE_NORMAL);
			// both "pull up" and "click" will invoke load more.
			mFooterView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startLoadMore();
				}
			});
		}
	}

	/**
	 * Enable or disable auto load more feature when scroll to bottom.
	 * 
	 * @param enable
	 */
	public void setAutoLoadEnable(boolean enable) {
		mEnableAutoLoad = enable;
	}

	/**
	 * data refresh or load finish
	 * 
	 * @param time
	 *            -- data update time
	 */
	public void loadFinish(String time) {
		stopRefresh();
		stopLoadMore();
		setRefreshTime(time);
	}

	public void loadFinish() {
		stopRefresh();
		stopLoadMore();
	}

	/**
	 * Stop refresh, reset header view.
	 */
	public void stopRefresh() {
		if (mPullRefreshing) {
			mHeader.setState(XHeaderView.STATE_DONE);
			mHeader.postDelayed(new Runnable() {
				@Override
				public void run() {
					mPullRefreshing = false;
					resetHeaderHeight();
				}
			}, 500);
		}
	}

	/**
	 * Stop load more, reset footer view.
	 */
	public void stopLoadMore() {
		if (mPullLoading) {
			mPullLoading = false;
			mHeader.postDelayed(new Runnable() {
				@Override
				public void run() {
					resetFooterHeight();
					if (!mEnablePullRefresh)
						resetHeaderHeight();
				}
			}, 500);
			if (!mNoMoreData)
				mFooterView.setState(XFooterView.STATE_NORMAL);
		}
	}

	/**
	 * Set last refresh time
	 * 
	 * @param time
	 */
	public void setRefreshTime(String time) {
		mHeaderTime.setText(time);
	}

	/**
	 * Set listener.
	 * 
	 * @param listener
	 */
	public void setXListViewListener(IXListViewListener listener) {
		mListener = listener;
	}

	public void setHeadViewStretchListener(HeadViewStretchListener listener) {
		mHeadViewStretchListener = listener;
	}

	/**
	 * Auto call back refresh.
	 */
	public void autoRefresh() {
		mHeader.setVisibleHeight(mHeaderHeight);

		if (mEnablePullRefresh && !mPullRefreshing) {
			// update the arrow image not refreshing
			if (mHeader.getVisibleHeight() > mHeaderHeight) {
				mHeader.setState(XHeaderView.STATE_READY);
			}
			else {
				mHeader.setState(XHeaderView.STATE_NORMAL);
			}
		}

		mPullRefreshing = true;
		mHeader.setState(XHeaderView.STATE_REFRESHING);
		refresh();
	}

	private void invokeOnScrolling() {
		if (mScrollListener instanceof OnXScrollListener) {
			OnXScrollListener listener = (OnXScrollListener) mScrollListener;
			listener.onXScrolling(this);
		}
	}

	public int getHeaderVisibleHeight() {
		if (mHeader != null) {
			return mHeader.getVisibleHeight();
		}
		else {
			return -1;
		}
	}

	private void updateHeaderHeight(float delta) {
		if (getHeaderVisibleHeight() > 0 && mHeadViewStretchListener != null) {
			mHeadViewStretchListener.stretchStart();
		}

		mHeader.setVisibleHeight((int) delta + mHeader.getVisibleHeight());

		if (mEnablePullRefresh && !mPullRefreshing) {
			// update the arrow image unrefreshing
			if (mHeader.getVisibleHeight() > mHeaderHeight) {
				mHeader.setState(XHeaderView.STATE_READY);
			}
			else {
				mHeader.setState(XHeaderView.STATE_NORMAL);
			}
		}

		// scroll to top each time
		setSelection(0);
	}

	private void resetHeaderHeight() {
		int height = mHeader.getVisibleHeight();
		if (height == 0)
			return;

		// refreshing and header isn't shown fully. do nothing.
		if (mPullRefreshing && height <= mHeaderHeight)
			return;

		// default: scroll back to dismiss header.
		int finalHeight = 0;
		// is refreshing, just scroll back to show all the header.
		if (mPullRefreshing && height > mHeaderHeight) {
			finalHeight = mHeaderHeight;
		}

		mScrollBack = SCROLL_BACK_HEADER;
		mScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);

		// trigger computeScroll
		invalidate();
	}

	private void resetFooterHeight() {

		int height = mFooterView.getVisibleHeight();

		if (height == 0)
			return;

		// refreshing and header isn't shown fully. do nothing.
		if (mPullLoading && height <= mFooterHeight)
			return;

		// default: scroll back to dismiss header.
		int finalHeight = 0;
		// is refreshing, just scroll back to show all the header.
		if (mPullLoading && height > mFooterHeight) {
			finalHeight = mFooterHeight;
		}

		mScrollBack = SCROLL_BACK_FOOTER;
		mScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);

		// trigger computeScroll
		invalidate();
	}

	private void updateFooterHeight(float delta) {
		int height = (int) delta + mFooterView.getVisibleHeight();//mFooterView.getBottomMargin() + (int) delta;

		if (mEnablePullLoad && !mPullLoading) {

			if (!mNoMoreData /*&& mVisibleItemCount <= mTotalItemCount - 1*/&& isVerticalFull()) {
				if (height > mFooterHeight) {
					mFooterView.show();
					// height enough to invoke load more.
					mFooterView.setState(XFooterView.STATE_READY);
				}
				else {
					mFooterView.setState(XFooterView.STATE_NORMAL);
				}
			}
			else if (mNoMoreData /*&& mVisibleItemCount <= mTotalItemCount - 1*/&& isVerticalFull()) {
				if (height > mFooterHeight) {
					mFooterView.show();
				}
				else {
					//mFooterView.hide();
				}
			}
			else if (!isVerticalFull()) {
				mFooterView.hide();
				return;
				//setPullLoadEnable(false);
			}
			else {
				return;
			}
		}

		mFooterView.setVisibleHeight(height);
		//mFooterView.setBottomMargin(height);

		// scroll to bottom
		// setSelection(mTotalItemCount - 1);
	}

	/*private void resetFooterHeight() {
		int bottomMargin = mFooterView.getBottomMargin();

		if (bottomMargin > 0) {
			mScrollBack = SCROLL_BACK_FOOTER;
			mScroller.startScroll(0, bottomMargin, 0, -bottomMargin, SCROLL_DURATION);
			invalidate();
		}
	}*/

	/**
	 * the scroll view is filled up the vertical screen or not
	 * 
	 * @return
	 */
	private boolean isVerticalFull() {
		if (getVisibleHeight(mVisibleItemCount) == getTotalHeight(mTotalItemCount)) {
			return false;
		}
		return true;
	}

	public void resetXListView() {
		setPullRefreshEnable(mEnablePullRefresh);
		setPullLoadEnable(mEnablePullLoad);
		setNoMoreData(false);
		visibleHeight = 0;
		totalHeight = 0;
	}

	int	visibleHeight	= 0;

	public int getVisibleHeight(int visibleCount) {
		ListAdapter adapter = getAdapter();
		if (null == adapter) {
			return 0;
		}
		if (visibleHeight <= 0) {

			for (int i = 0, len = visibleCount; i < len; i++) {
				View item = adapter.getView(i, null, this);
				if (null == item)
					continue;
				item.measure(0, 0);
				visibleHeight += item.getMeasuredHeight();
			}
		}
		return visibleHeight;
	}

	int	totalHeight	= 0;

	public int getTotalHeight(int totalCount) {
		ListAdapter adapter = getAdapter();
		if (null == adapter) {
			return 0;
		}
		if (totalHeight <= 0) {

			for (int i = 0, len = totalCount; i < len; i++) {
				View item = adapter.getView(i, null, this);
				if (null == item)
					continue;
				item.measure(0, 0);
				totalHeight += item.getMeasuredHeight();
			}
		}
		return totalHeight;
	}

	public void resetLoadParam() {
		visibleHeight = 0;
		totalHeight = 0;
	}

	public void setNoMoreData(boolean noMoreData) {
		mNoMoreData = noMoreData;
		if (noMoreData) {
			mFooterView.setState(XFooterView.STATE_NO_MORE);
		}
		else {
			mFooterView.setState(XFooterView.STATE_NORMAL);
		}
	}

	private void startLoadMore() {
		if (mNoMoreData)
			return;
		if (!mPullLoading) {
			mPullLoading = true;
			mFooterView.setState(XFooterView.STATE_LOADING);
			loadMore();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mLastY == -1) {
			mLastY = ev.getRawY();
		}

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastY = ev.getRawY();
			break;

		case MotionEvent.ACTION_MOVE:
			final float deltaY = ev.getRawY() - mLastY;
			mLastY = ev.getRawY();

			if (getFirstVisiblePosition() == 0 && (mHeader.getVisibleHeight() > 0 || deltaY > 0)) {
				// the first item is showing, header has shown or pull down.
				updateHeaderHeight(deltaY / OFFSET_RADIO);
				invokeOnScrolling();

			}
			else if (getLastVisiblePosition() == mTotalItemCount - 1 && (mFooterView.getBottomMargin() > 0 || deltaY < 0)) {
				// last item, already pulled up or want to pull up.
				updateFooterHeight(-deltaY / OFFSET_RADIO);
			}
			break;

		default:
			// reset
			mLastY = -1;
			if (getFirstVisiblePosition() == 0) {
				// invoke refresh
				if (mEnablePullRefresh && mHeader.getVisibleHeight() > mHeaderHeight) {
					mPullRefreshing = true;
					mHeader.setState(XHeaderView.STATE_REFRESHING);
					refresh();
				}
			}
			else if (getLastVisiblePosition() == mTotalItemCount - 1) {
				// invoke load more.
				if (mEnablePullLoad && mFooterView.getVisibleHeight() > mFooterHeight && !mNoMoreData) {
					startLoadMore();
				}
			}
			resetHeaderHeight();
			resetFooterHeight();

			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			//DebugLog.w("mScrollBack = " + mScrollBack );
			if (mScrollBack == SCROLL_BACK_HEADER) {
				mHeader.setVisibleHeight(mScroller.getCurrY());
			}
			else {
				mHeader.setVisibleHeight(0);
				mFooterView.setVisibleHeight(mScroller.getCurrY());

				if (mHeadViewStretchListener != null) {
					mHeadViewStretchListener.stretchEnd();
				}
			}

			postInvalidate();
			invokeOnScrolling();
		}

		super.computeScroll();
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		mScrollListener = l;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mScrollListener != null) {
			mScrollListener.onScrollStateChanged(view, scrollState);
		}

		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			if (mEnableAutoLoad && getLastVisiblePosition() == getCount() - 1) {
				startLoadMore();
			}
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// send to user's listener
		mTotalItemCount = totalItemCount;
		mVisibleItemCount = visibleItemCount;
		if (mScrollListener != null) {
			mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}
		if (getHeaderVisibleHeight() <= 0 && mHeadViewStretchListener != null) {
			mHeadViewStretchListener.stretchEnd();
		}
	}

	private void refresh() {
		if (mEnablePullRefresh && null != mListener) {
			mListener.onRefresh();
		}
	}

	private void loadMore() {
		if (mEnablePullLoad && null != mListener) {
			mListener.onLoadMore();
		}
	}

	/**
	 * You can listen ListView.OnScrollListener or this one. it will invoke
	 * onXScrolling when header/footer scroll back.
	 */
	public interface OnXScrollListener extends OnScrollListener {
		public void onXScrolling(View view);
	}

	/**
	 * Implements this interface to get refresh/load more event.
	 * 
	 * @author markmjw
	 */
	public interface IXListViewListener {
		public void onRefresh();

		public void onLoadMore();
	}

	/**
	 * get the status of list view when it start stretch or stop.
	 *
	 * @author Emerson
	 */
	public interface HeadViewStretchListener {
		public void stretchStart();

		public void stretchEnd();
	}
}
