package cn.pharmplus.xview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * XScrollView, modified from {@link XListView}
 * 
 */
public class XScrollView extends ScrollView implements OnScrollListener {
	private static final String		TAG					= "XScrollView";

	private final static int		SCROLL_BACK_HEADER	= 0;
	private final static int		SCROLL_BACK_FOOTER	= 1;

	private final static int		SCROLL_DURATION		= 400;

	// when pull up >= 50px
	//	private final static int		PULL_LOAD_MORE_DELTA	= 50;

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
	private IXScrollViewListener	mListener;

	private LinearLayout			mLayout;
	private LinearLayout			mContentLayout;

	private XHeaderView				mHeader;
	// header view content, use it to calculate the Header's height. And hide it
	// when disable pull refresh.
	private RelativeLayout			mHeaderContent;
	private TextView				mHeaderTime;
	private int						mHeaderHeight;
	private int						mFooterHeight;

	private XFooterView				mFooterView;

	private boolean					mEnablePullRefresh	= true;
	private boolean					mPullRefreshing		= false;

	private boolean					mEnablePullLoad		= false;
	private boolean					mEnableAutoLoad		= false;
	private boolean					mPullLoading		= false;
	private boolean					mNoMoreData			= false;

	private Callbacks				mCallbacks;

	public static interface Callbacks {
		public void onScrollChanged();
	}

	public XScrollView(Context context) {
		super(context);
		//initWithContext(context);
	}

	public XScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		//initWithContext(context);
	}

	public XScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		//initWithContext(context);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		initView(this.getContext());
	}

	private void initView(Context context) {

		//have no child view
		if (getChildCount() <= 0) {
			initWithExtraLayout(context);
			return;
		}

		View child = getChildAt(0);
		//have child & child is LinearLayout
		if (child instanceof LinearLayout) {
			mScroller = new Scroller(context, new DecelerateInterpolator());
			// XScrollView need the scroll event, and it will dispatch the event to user's listener (as a proxy).
			this.setOnScrollListener(this);
			ViewConfiguration config = ViewConfiguration.get(context);
			mTouchSlop = config.getScaledTouchSlop();

			// init header view
			mHeader = new XHeaderView(context);
			mHeaderContent = (RelativeLayout) mHeader.findViewById(R.id.header_content);
			mHeaderTime = (TextView) mHeader.findViewById(R.id.header_hint_time);
			((LinearLayout) child).addView(mHeader, 0);

			// init footer view
			mFooterView = new XFooterView(context);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			params.gravity = Gravity.CENTER;
			((LinearLayout) child).addView(mFooterView);

			mContentLayout = (LinearLayout) child;

			initHearderFooterHeight();
		}
		else {
			throw new IllegalStateException("XScrollView can host only one direct child ,must be LinearLayout");
		}
	}

	private void initWithExtraLayout(Context context) {
		mLayout = (LinearLayout) View.inflate(context, R.layout.vw_xscrollview_layout, null);
		mContentLayout = (LinearLayout) mLayout.findViewById(R.id.content_layout);

		mScroller = new Scroller(context, new DecelerateInterpolator());
		// XScrollView need the scroll event, and it will dispatch the event to
		// user's listener (as a proxy).
		this.setOnScrollListener(this);

		ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();

		// init header view
		mHeader = new XHeaderView(context);
		mHeaderContent = (RelativeLayout) mHeader.findViewById(R.id.header_content);
		mHeaderTime = (TextView) mHeader.findViewById(R.id.header_hint_time);
		LinearLayout headerLayout = (LinearLayout) mLayout.findViewById(R.id.header_layout);
		headerLayout.addView(mHeader);

		// init footer view
		mFooterView = new XFooterView(context);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		params.gravity = Gravity.CENTER;
		LinearLayout footLayout = (LinearLayout) mLayout.findViewById(R.id.footer_layout);
		footLayout.addView(mFooterView, params);

		initHearderFooterHeight();

		this.addView(mLayout);
	}

	private void initHearderFooterHeight() {
		// init header height
		ViewTreeObserver observer = mHeader.getViewTreeObserver();
		if (null != observer) {
			observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@SuppressWarnings("deprecation")
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

	/**
	 * Set the content ViewGroup for XScrollView.
	 * 
	 * @param content
	 */
	public void setContentView(ViewGroup content) {
		if (mLayout == null) {
			return;
		}

		if (mContentLayout == null) {
			mContentLayout = (LinearLayout) mLayout.findViewById(R.id.content_layout);
		}

		if (mContentLayout.getChildCount() > 0) {
			mContentLayout.removeAllViews();
		}
		mContentLayout.addView(content);
	}

	/**
	 * Set the content View for XScrollView.
	 * 
	 * @param content
	 */
	public void setView(View content) {
		if (mLayout == null) {
			return;
		}

		if (mContentLayout == null) {
			mContentLayout = (LinearLayout) mLayout.findViewById(R.id.content_layout);
		}
		mContentLayout.addView(content);
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
			mFooterView.setPadding(0, 0, 0, mFooterView.getHeight() * (-1));
			mFooterView.setOnClickListener(null);

			mContentLayout.removeView(mFooterView);
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

	public void setCallbacks(Callbacks listener) {
		mCallbacks = listener;
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
	public void setIXScrollViewListener(IXScrollViewListener listener) {
		mListener = listener;
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
			OnXScrollListener l = (OnXScrollListener) mScrollListener;
			l.onXScrolling(this);
		}
	}

	private void updateHeaderHeight(float delta) {
		//DebugLog.w("mHeaderHeight = " + mHeaderHeight);
		//DebugLog.w("height = " + (int) delta + mHeader.getVisibleHeight());
		mHeader.setVisibleHeight((int) delta + mHeader.getVisibleHeight());
		if (mCallbacks != null) {
			mCallbacks.onScrollChanged();
		}
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
		post(new Runnable() {
			@Override
			public void run() {
				XScrollView.this.fullScroll(ScrollView.FOCUS_UP);
			}
		});
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
		if (mCallbacks != null) {
			mCallbacks.onScrollChanged();
		}
		// trigger computeScroll
		invalidate();
	}

	private void updateFooterHeight(float delta) {
		/*int height = mFooterView.getBottomMargin() + (int) delta;

		if (mEnablePullLoad && !mPullLoading && !mNoMoreData) {
			if (height > PULL_LOAD_MORE_DELTA) {
				// height enough to invoke load more.
				mFooterView.setState(XFooterView.STATE_READY);
			}
			else {
				mFooterView.setState(XFooterView.STATE_NORMAL);
			}
		}
		mFooterView.setBottomMargin(height);*/

		int height = (int) delta + mFooterView.getVisibleHeight();
		//DebugLog.w("mFooterHeight = " + mFooterHeight);
		//DebugLog.w("height = " + height);
		if (mEnablePullLoad && !mPullLoading && !mNoMoreData) {

			if (!mNoMoreData && isVerticalFull()) {
				if (height > mFooterHeight) {
					mFooterView.show();
					// height enough to invoke load more.
					mFooterView.setState(XFooterView.STATE_READY);
				}
				else {
					mFooterView.setState(XFooterView.STATE_NORMAL);
				}
			}
			else if (mNoMoreData && isVerticalFull()) {
				if (height > mFooterHeight) {
					mFooterView.show();
				}
				else {
					//mFooterView.hide();
				}
			}
			else {
				setPullLoadEnable(false);
			}
		}

		mFooterView.setVisibleHeight(height);

		// scroll to bottom
		post(new Runnable() {
			@Override
			public void run() {
				XScrollView.this.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}

	//the scroll view is filled up the vertical screen or not
	private boolean isVerticalFull() {
		//DebugLog.w("mContentLayout = " + mContentLayout.getHeight());
		//DebugLog.w("getScreenHeight(getContext()) = " + getScreenHeight(getContext()));
		if (mContentLayout.getHeight() < getScreenHeight(getContext())) {
			return false;
		}
		return true;
	}

	public int getScreenHeight(Context context) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(outMetrics);
		return outMetrics.heightPixels;
	}

	private void resetFooterHeight() {
		/*int bottomMargin = mFooterView.getBottomMargin();

		if (bottomMargin > 0) {
			mScrollBack = SCROLL_BACK_FOOTER;
			mScroller.startScroll(0, bottomMargin, 0, -bottomMargin, SCROLL_DURATION);
			invalidate();
		}*/

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

	public void setNoMoreData(boolean noMoreData) {

		mNoMoreData = noMoreData;
		if (noMoreData) {
			mFooterView.setState(XFooterView.STATE_NO_MORE);
		}
		else {
			mFooterView.setState(XFooterView.STATE_NORMAL);
		}
	}

	public void setLoadStatus() {
		if (mNoMoreData)
			return;
		if (!mPullLoading) {
			mPullLoading = true;
			mFooterView.setState(XFooterView.STATE_LOADING);
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

	private boolean	mIsBeingDragged	= false;	//???????
	private float	mLastMotionY	= -1;
	private int		mTouchSlop;

	public final boolean isRefreshing() {
		return mPullRefreshing || mPullLoading;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			mIsBeingDragged = false;
			return false;
		}

		//	        if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
		//	            return true;
		//	        }

		switch (action) {
		case MotionEvent.ACTION_MOVE: {
			// If we're refreshing, and the flag is set. Eat all MOVE events
			if (isRefreshing()) {
				return true;
			}

			if (!isVerticalFull()) {
				final float y = ev.getY(), x = ev.getX();
				final float diff, absDiff;

				diff = y - mLastMotionY;
				absDiff = Math.abs(diff);

				if (absDiff > mTouchSlop) {
					if (diff >= 1f && isTop()) {
						mLastMotionY = y;
						mIsBeingDragged = true;
					}
					else if (diff <= -1f && isBottom()) {
						mLastMotionY = y;
						mIsBeingDragged = true;
					}
				}
			}
			break;
		}
		case MotionEvent.ACTION_DOWN: {
			mLastMotionY = ev.getY();
			mIsBeingDragged = false;
			break;
		}
		}
		//DebugLog.d("XScrollView", "onInterceptTouchEvent --> mIsBeingDragged = " + mIsBeingDragged);
		return mIsBeingDragged || super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		//DebugLog.w("getHeight() = " + getHeight());
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

			if (isTop() && (mHeader.getVisibleHeight() > 0 || deltaY > 0)) {
				// the first item is showing, header has shown or pull down.
				updateHeaderHeight(deltaY / OFFSET_RADIO);
				invokeOnScrolling();

			}
			else if (isBottom() && (mFooterView.getVisibleHeight() > 0 || deltaY < 0)) {
				// last item, already pulled up or want to pull up.
				updateFooterHeight(-deltaY / OFFSET_RADIO);

			}
			break;

		default:
			// reset
			mLastY = -1;
			if (mIsBeingDragged) {
				mIsBeingDragged = false;
			}
			resetHeaderOrBottom();
			break;
		}

		return super.onTouchEvent(ev);
	}

	private void resetHeaderOrBottom() {
		if (isTop()) {
			// invoke refresh
			if (mEnablePullRefresh && mHeader.getVisibleHeight() > mHeaderHeight) {
				mPullRefreshing = true;
				mHeader.setState(XHeaderView.STATE_REFRESHING);
				refresh();
			}
			resetHeaderHeight();

		}
		else if (isBottom()) {
			// invoke load more.
			if (mEnablePullLoad && mFooterView.getVisibleHeight() > mFooterHeight && !mNoMoreData) {
				startLoadMore();
			}
			resetFooterHeight();
		}
	}

	private boolean isTop() {
		return getScrollY() <= 0 || mHeader.getVisibleHeight() > mHeaderHeight || mContentLayout.getTop() > 0;
	}

	private boolean isBottom() {
		return Math.abs(getScrollY() + getHeight() - computeVerticalScrollRange()) <= 5 || (getScrollY() > 0 && null != mFooterView && mFooterView.getVisibleHeight() > 0);
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			if (mScrollBack == SCROLL_BACK_HEADER) {
				mHeader.setVisibleHeight(mScroller.getCurrY());
			}
			else {
				mHeader.setVisibleHeight(0);
				mFooterView.setVisibleHeight(mScroller.getCurrY());
			}
			if (mCallbacks != null) {
				mCallbacks.onScrollChanged();
			}
			postInvalidate();
			invokeOnScrolling();
		}
		super.computeScroll();
	}

	public void setOnScrollListener(OnScrollListener l) {
		mScrollListener = l;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mScrollListener != null) {
			mScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		// Grab the last child placed in the ScrollView, we need it to
		// determinate the bottom position.
		View view = getChildAt(getChildCount() - 1);

		if (null != view) {
			// Calculate the scroll diff
			int diff = (view.getBottom() - (view.getHeight() + view.getScrollY()));

			// if diff is zero, then the bottom has been reached
			if (diff == 0 && mEnableAutoLoad) {
				// notify that we have reached the bottom
				startLoadMore();
			}
		}

		super.onScrollChanged(l, t, oldl, oldt);

		if (mCallbacks != null) {
			mCallbacks.onScrollChanged();
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// send to user's listener
		if (mScrollListener != null) {
			mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
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
	 */
	public interface IXScrollViewListener {
		public void onRefresh();

		public void onLoadMore();
	}

	public int measureHeight(ListView mListView) {
		// get ListView adapter
		ListAdapter adapter = mListView.getAdapter();
		if (null == adapter) {
			return 0;
		}

		int totalHeight = 0;

		for (int i = 0, len = adapter.getCount(); i < len; i++) {
			View item = adapter.getView(i, null, mListView);
			if (null == item)
				continue;
			// measure each item width and height
			item.measure(0, 0);
			// calculate all height
			totalHeight += item.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = mListView.getLayoutParams();

		if (null == params) {
			params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		// calculate ListView height
		params.height = totalHeight + (mListView.getDividerHeight() * (adapter.getCount() - 1));

		mListView.setLayoutParams(params);

		return params.height;
	}
}
