package com.aptoide.amethyst;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aptoide.amethyst.adapter.BaseAdapter;
import com.aptoide.amethyst.adapter.MoreSearchAdapter;
import com.aptoide.amethyst.analytics.Analytics;
import com.aptoide.amethyst.database.AptoideDatabase;
import com.aptoide.amethyst.fragments.store.BaseWebserviceFragment;
import com.aptoide.amethyst.models.EnumStoreTheme;
import com.aptoide.amethyst.models.search.SearchApkConverter;
import com.aptoide.amethyst.ui.MoreActivity;
import com.aptoide.amethyst.ui.listeners.EndlessRecyclerOnScrollListener;
import com.aptoide.amethyst.utils.AptoideUtils;
import com.aptoide.dataprovider.webservices.models.v7.ListSearchApps;
import com.aptoide.models.displayables.HeaderRow;
import com.aptoide.models.displayables.ProgressBarRow;
import com.aptoide.models.displayables.SearchApk;
import com.aptoide.models.stores.Store;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.util.Collections;
import java.util.List;

/**
 * Created by fabio on 20-11-2015.
 */
public class MoreSearchActivity extends MoreActivity {

	private EnumStoreTheme storeTheme = null;
	private AppBarLayout mAppBar;

    @Override
    protected Fragment getFragment(Bundle args) {
        MoreSearchFragment fragment = new MoreSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void bindViews() {
        super.bindViews();
         mAppBar = (AppBarLayout)findViewById(R.id.appbar);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mToolbar != null) {
            mToolbar.setTitle(AptoideUtils.StringUtils.getFormattedString(this, R.string.search_activity_title, getIntent().getExtras().getString(SearchActivity.SEARCH_QUERY)));
            try {
                storeTheme = (EnumStoreTheme) getIntent().getExtras().get(SearchActivity.SEARCH_STORE_THEME);
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
            setupStoreTheme(storeTheme);
        }
    }

    private void setupStoreTheme(EnumStoreTheme storeTheme) {
        if (storeTheme != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(storeTheme.getColor700tint()));
            }
            mAppBar.setBackgroundColor(getResources().getColor(storeTheme.getStoreHeader()));
            mToolbar.setBackgroundColor(getResources().getColor(storeTheme.getStoreHeader()));
        }
    }

    static public class MoreSearchFragment extends BaseWebserviceFragment {
		private ImageView searchButton;
		private EditText searchQuery;
		private ScrollView noSearchResultLayout;
		private boolean trustedAppsOnly;
		private SearchApkConverter searchApkConverter;
        private AptoideDatabase database;

        @Override
        public void setLayoutManager(RecyclerView recyclerView) {
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        }

        private String query;
        private String storeName;
        private boolean mLoading;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			searchApkConverter = new SearchApkConverter(BUCKET_SIZE);
            database = new AptoideDatabase(Aptoide.getDb());
		}

		@Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            query = getArguments().getString(SearchActivity.SEARCH_QUERY);
            storeName = getArguments().getString(SearchActivity.SEARCH_STORE_NAME);
			trustedAppsOnly = getArguments().getBoolean(SearchActivity.SEARCH_ONLY_TRUSTED_APPS);
            super.onViewCreated(view, savedInstanceState);
            getRecyclerView().addOnScrollListener(new EndlessRecyclerOnScrollListener((LinearLayoutManager) getRecyclerView().getLayoutManager()) {
                @Override
                public void onLoadMore() {
                    mLoading = true;
                    displayableList.add(new ProgressBarRow(BUCKET_SIZE));
                    adapter.notifyItemInserted(adapter.getItemCount());
                    executeEndlessSpiceRequest();
                }

                @Override
                public int getOffset() {
                    return offset;
                }

                @Override
                public boolean isLoading() {
                    return mLoading;
                }
            });
        }


        @Override
        protected BaseAdapter getAdapter() {
            if (adapter == null) {
                return new MoreSearchAdapter(displayableList);
            }
            return adapter;
        }

        @Override
        protected String getBaseContext() {
            return SearchActivity.CONTEXT;
        }

        @Override
        protected void executeSpiceRequest(boolean useCache) {
            mLoading = true;
            long cacheExpiryDuration = useCache ? DurationInMillis.ONE_HOUR * 6 : DurationInMillis.ALWAYS_EXPIRED;
            spiceManager.execute(AptoideUtils.RepoUtils.buildSearchAppsRequest(query, trustedAppsOnly, SearchActivity.SEARCH_LIMIT, offset, formatStore(storeName)), query + offset + SearchActivity.SEARCH_QUERY + storeName, cacheExpiryDuration, listener);
        }

        RequestListener<ListSearchApps> listener = new RequestListener<ListSearchApps>() {
            @Override
            public void onRequestFailure(SpiceException spiceException) {
                handleErrorCondition(spiceException);
            }

            @Override
            public void onRequestSuccess(ListSearchApps listSearchApps) {
                handleSuccessCondition();
                adapter = getAdapter();
                getRecyclerView().setAdapter(adapter);

                List<SearchApk> apkList = searchApkConverter.convert(listSearchApps.datalist.list, offset, true);
                if (!apkList.isEmpty()) {
                    if (isStoreSearch()) {
                        displayableList.add(new HeaderRow(AptoideUtils.StringUtils.getFormattedString(getContext(), R.string.results_in_store, storeName), false, BUCKET_SIZE));
                    } else {
                        displayableList.add(new HeaderRow(getString(R.string.results_subscribed), false, BUCKET_SIZE));
                    }
                    displayableList.addAll(apkList);
                }
				offset = listSearchApps.datalist.next.intValue();
                getAdapter().notifyDataSetChanged();
                swipeContainer.setEnabled(false);
                mLoading = false;
                if (displayableList.size() <= 0) {
                    getRecyclerView().setVisibility(View.GONE);
                    noSearchResultLayout.setVisibility(View.VISIBLE);
                    searchQuery.setText(query);
                    searchButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startSearch();
                        }
                    });
                    searchQuery.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                                startSearch();
                                return true;
                            }
                            return false;
                        }
                    });
                    Analytics.Search.noSearchResultEvent(query);
                }
            }
        };

        private void startSearch() {
            Intent intent = new Intent(getContext(), SearchActivity.class);
            intent.putExtra(SearchActivity.SEARCH_QUERY, searchQuery.getText().toString());
            getContext().startActivity(intent);
        }

        private void executeEndlessSpiceRequest() {
            long cacheExpiryDuration = useCache ? DurationInMillis.ONE_HOUR * 6 : DurationInMillis.ALWAYS_EXPIRED;
            spiceManager.execute(AptoideUtils.RepoUtils.buildSearchAppsRequest(query, trustedAppsOnly, SearchActivity.SEARCH_LIMIT, offset, formatStore(storeName)), MoreSearchActivity.class.getSimpleName() + query + offset, cacheExpiryDuration, new RequestListener<ListSearchApps>() {
                @Override
                public void onRequestFailure(SpiceException spiceException) {
                    if (mLoading && !displayableList.isEmpty()) {
                        displayableList.remove(displayableList.size() - 1);
                        getAdapter().notifyItemRemoved(displayableList.size());
                    }
                }

                @Override
                public void onRequestSuccess(ListSearchApps listSearchApps) {

                    if (mLoading && !displayableList.isEmpty()) {
                        displayableList.remove(displayableList.size() - 1);
                        getAdapter().notifyItemRemoved(displayableList.size());
                    }

                    List<SearchApk> apkList = searchApkConverter.convert(listSearchApps.datalist.list, offset, true);
                    if (!apkList.isEmpty()) {
                        displayableList.addAll(apkList);
                    }
					offset = listSearchApps.datalist.next.intValue();
                    getAdapter().notifyDataSetChanged();
                    swipeContainer.setEnabled(false);
                    mLoading = false;
                }
            });

        }

        @Override
        protected void bindViews(View view) {
            super.bindViews(view);

            searchButton = (ImageView )view.findViewById(R.id.ic_search_button);
            searchQuery = (EditText )view.findViewById(R.id.search_text);
            noSearchResultLayout = (ScrollView )view.findViewById(R.id.no_search_results_layout);
        }

        private boolean isStoreSearch() {
            return storeName != null && !TextUtils.isEmpty(storeName);
        }

        private List<Store> formatStore(String storeName) {

            if (isStoreSearch()) {
                final Store store = database.getSubscribedStore(storeName);
                if (store != null) {
                    return Collections.singletonList(store);
                }
            } else {
                final List<Store> stores = database.getSubscribedStores();
                if (!stores.isEmpty()) {
                    return stores;
                }
            }

            return null;
        }
    }
}
