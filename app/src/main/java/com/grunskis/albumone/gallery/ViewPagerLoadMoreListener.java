package com.grunskis.albumone.gallery;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

public abstract class ViewPagerLoadMoreListener extends ViewPager.SimpleOnPageChangeListener {
    private static final int ITEMS_REMAINING_THRESHOLD = 5;
    private static final int ITEMS_PER_PAGE = 10;

    private boolean loading = true;
    private int previousTotalItemCount = 0;

    private PagerAdapter mPagerAdapter;
    private int currentPage;

    ViewPagerLoadMoreListener(PagerAdapter pagerAdapter) {
        mPagerAdapter = pagerAdapter;
        currentPage = mPagerAdapter.getCount() / ITEMS_PER_PAGE;
    }

    @Override
    public void onPageSelected(int position) {
        int numItems = mPagerAdapter.getCount();

        if (loading && (numItems > previousTotalItemCount)) {
            loading = false;
            previousTotalItemCount = numItems;
        }

        if (!loading && (position + ITEMS_REMAINING_THRESHOLD) > numItems - 1) {
            currentPage++;
            onLoadMore(currentPage);
            loading = true;
        }
    }

    public abstract void onLoadMore(int page);
}
