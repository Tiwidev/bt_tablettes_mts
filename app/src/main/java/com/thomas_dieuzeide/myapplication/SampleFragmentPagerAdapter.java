package com.thomas_dieuzeide.myapplication;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.ViewGroup;

/**
 * Created by Thomas_Dieuzeide on 12/28/2015.
 */
public class SampleFragmentPagerAdapter extends FragmentPagerAdapter {
    final int PAGE_COUNT = 4;
    private String tabTitles[] = new String[] { "Client", "Travaux", "Pièces","Résumé" };
    private Context context;
    private String key;
    private int session, vision;
    private Fragment mCurrentFragment;
    public SampleFragmentPagerAdapter(FragmentManager fm, Context context, String key, int session, int vision) {
        super(fm);
        this.context = context;
        this.key = key;
        this.session = session;
        this.vision = vision;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        return FragmentPage.newInstance(position+1,key,session,vision);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (mCurrentFragment != object) {
            mCurrentFragment = (Fragment) object;
        }
        super.setPrimaryItem(container, position, object);
    }

    public Fragment getCurrentFragment() {
        return mCurrentFragment;
    }

// ...

    @Override
    public CharSequence getPageTitle(int position) {
        // Replace blank spaces with image icon
        SpannableString sb = new SpannableString("" + tabTitles[position]);

        return sb;
    }
}
