package com.thomas_dieuzeide.myapplication;

import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private SampleFragmentPagerAdapter sfpa;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle b = this.getIntent().getExtras();
        String value = b.getString("key");
        int session = b.getInt("session");
        int vision = b.getInt("vision");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("BON: " + value);
        actionBar.setDisplayOptions(actionBar.getDisplayOptions()
                | ActionBar.DISPLAY_SHOW_CUSTOM);
        ImageView imageView = new ImageView(actionBar.getThemedContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageResource(R.drawable.logo);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT
                | Gravity.CENTER_VERTICAL);
        layoutParams.rightMargin = 40;
        imageView.setLayoutParams(layoutParams);
        actionBar.setCustomView(imageView);
        // Get the ViewPager and set it's Page1rAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        sfpa = new SampleFragmentPagerAdapter(getSupportFragmentManager(),
                MainActivity.this,value,session,vision);
        viewPager.setAdapter(sfpa);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onBackPressed() {
        FragmentPage f = (FragmentPage) sfpa.getCurrentFragment();
        Toast.makeText(getApplicationContext(),"Fragment back press:" + f.getMPage() +"",Toast.LENGTH_SHORT).show();
        f.store();
    }
}
