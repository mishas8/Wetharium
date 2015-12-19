package com.exsoft.weatharium.view;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;

import java.util.ArrayList;

/**
 * Created by M.A. on 02.12.2015.
 */
public class TabsAdapter extends FragmentPagerAdapter implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {

    private final Context mContext;
    private final TabHost mTabHost;
    private final ViewPager mViewPager;

    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

    static final class TabInfo {

        private final String tag;
        private final Class<?> clss;
        private final Bundle args;

        TabInfo(final String _tag, final Class<?> _class, final Bundle _args) {
            tag = _tag;
            clss = _class;
            args = _args;
        }
    }

    class DummyTabFactory implements TabHost.TabContentFactory {

        private final Context mContext;

        public DummyTabFactory(final Context context) {
            mContext = context;
        }

        @Override
        public View createTabContent(final String tag) {
            View v = new View(mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }
    }

    public TabsAdapter(final FragmentActivity activity, final TabHost tabHost, final ViewPager pager) {
        super(activity.getSupportFragmentManager());
        mContext = activity;
        mTabHost = tabHost;
        mViewPager = pager;

        mTabHost.setOnTabChangedListener(this);
        mViewPager.setAdapter(this);
        mViewPager.setOnPageChangeListener(this);
        // Задаем мин. количество страниц (Используем фиксированное значение, чтобы не разрушались страницы при скроле)
        mViewPager.setOffscreenPageLimit(3);
    }

    public void addTab(final TabHost.TabSpec tabSpec, final Class<?> clss, final Bundle args) {
        tabSpec.setContent(new DummyTabFactory(mContext));
        String tag = tabSpec.getTag();
        TabInfo info = new TabInfo(tag, clss, args);
        mTabs.add(info);
        mTabHost.addTab(tabSpec);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem(final int position) {
        TabInfo info = mTabs.get(position);
        return Fragment.instantiate(mContext, info.clss.getName(), info.args);
    }

    @Override
    public void onTabChanged(final String tabId) {
        int position = mTabHost.getCurrentTab();
        mViewPager.setCurrentItem(position);
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
    }



    @Override
    public void onPageSelected(final int position) {
        // Unfortunately when TabHost changes the current tab, it kindly
        // also takes care of putting focus on it when not in touch mode.
        // The jerk.
        // This hack tries to prevent this from pulling focus out of our
        // ViewPager.
        TabWidget widget = mTabHost.getTabWidget();
        int oldFocusability = widget.getDescendantFocusability();
        widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        mTabHost.setCurrentTab(position);
        widget.setDescendantFocusability(oldFocusability);
    }

    @Override
    public void onPageScrollStateChanged(final int state) {

    }

}