package csci201.classy.MainActivity;

/**
 * Created by kfait on 11/16/2016.
 */

//pager adapter used to display fragments under two tabs: class watcher and class selector

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import csci201.classy.Tabs.SelectorFragment;
import csci201.classy.Tabs.WatcherFragment;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                WatcherFragment tab1 = WatcherFragment.newInstance("1","2");
                return tab1;
            case 1:
                SelectorFragment tab2 = SelectorFragment.newInstance("1", "2");
                return tab2;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
