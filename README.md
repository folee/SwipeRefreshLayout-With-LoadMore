## SwipeRefreshLayout ##
An extension of android.support.v4.widget.SwipeRefreshLayout with loading more function

## Note ##
- [x] Support ListView
- [ ] Support RecyclerView (Todo)
- [x] Custom your FooterView and load more action
- [x] Start and stop refresh or load more manually


## Demo ##
[Download apk](/demo.apk)

## Screenshot ##
![Gif](/demo.gif)

## How to Use ##
In your `build.gradle`:
```groovy
dependencies {
    compile 'com.demievil.library:refreshlayout:1.0.0@aar'
}
```
[Click to check the latest version](https://jcenter.bintray.com/com/demievil/library/refreshlayout/)

## Simple Example ##
- Use RefreshLayout in your layout
````xml
<com.demievil.library.RefreshLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/swipe_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <ListView
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#FFFFFF"
        android:dividerHeight="1px"
        android:footerDividersEnabled="false"
        tools:listitem="@layout/list_item" />
</com.demievil.library.RefreshLayout>
````
- Customize your footer layout to indicate a loading progress such as:
````xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/footer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FFFFFF"
    android:gravity="center"
    android:padding="8dp">

    <TextView
        android:id="@+id/text_more"
        android:layout_width="match_parent"
        android:layout_height="40dp"
        android:layout_gravity="center|top"
        android:gravity="center"
        android:text="more..."
        android:textSize="16sp" />

    <ProgressBar
        android:id="@+id/load_progress_bar"
        style="@android:style/Widget.DeviceDefault.Light.ProgressBar"
        android:layout_width="wrap_content"
        android:layout_height="40dp"
        android:layout_gravity="center|top"
        android:indeterminate="true"
        android:visibility="gone" />
</FrameLayout>
````
- Get instance and use it: 
````java
    RefreshLayout mRefreshLayout;
    ListView mListView;
    View footerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRefreshLayout = (RefreshLayout) findViewById(R.id.swipe_container);
        mListView = (ListView) findViewById(R.id.list);
        footerLayout = getLayoutInflater().inflate(R.layout.listview_footer, null);

        mListView.addFooterView(footerLayout);
        mRefreshLayout.setChildView(mListView);
	
        mListView.setAdapter(mAdapter);

        mRefreshLayout.setColorSchemeResources(R.color.google_blue,
                R.color.google_green,
                R.color.google_red,
                R.color.google_yellow);

        mRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
	         // start to refresh
            }
        });
        mRefreshLayout.setOnLoadListener(new RefreshLayout.OnLoadListener() {
            @Override
            public void onLoad() {
                // start to load   
            }
        });
    }
````

- You can start and stop refresh or load more as follows:
````java
//start and stop refresh manually
mRefreshLayout.setRefreshing(true);
mRefreshLayout.setRefreshing(false);

//start and stop load more manually
mRefreshLayout.setLoading(true); //this will jump to bottom anf callback OnLoadListener.onLoad()
mRefreshLayout.setLoading(false); //this will reset laod more coordinates
````

## Thanks ##
[SwipeRefreshLayout](https://developer.android.com/reference/android/support/v4/widget/SwipeRefreshLayout.html)

## License ##
> Copyright (c) 2015 Demievil
> 
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
> 
>    http://www.apache.org/licenses/LICENSE-2.0
> 
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.

