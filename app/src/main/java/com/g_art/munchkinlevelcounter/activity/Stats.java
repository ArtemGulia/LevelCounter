package com.g_art.munchkinlevelcounter.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.g_art.munchkinlevelcounter.R;
import com.g_art.munchkinlevelcounter.adapter.StatsPageAdapter;
import com.g_art.munchkinlevelcounter.application.MyApplication;
import com.g_art.munchkinlevelcounter.model.Player;
import com.g_art.munchkinlevelcounter.util.StoredPlayers;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class Stats extends AppCompatActivity implements ActionBar.TabListener {

    public final static String PLAYER_KEY = "playersList";

    private ViewPager viewPager;
    private ActionBar actionBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Obtain the shared Tracker instance.
        MyApplication application = (MyApplication) getApplication();
        Tracker mTracker = application.getDefaultTracker();
        mTracker.send(new HitBuilders.EventBuilder()
                .setAction("Stats opened")
                .setCategory("Screen")
                .setLabel("Stats")
                .build());

        ActionBar.Tab lvlTab, gearTab, powerTab;

        StoredPlayers mStored = StoredPlayers.getInstance(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));

        String PREFS_NO_DATA = getString(R.string.no_data);

        Intent intent = getIntent();
        String BUNDLE_STATS_KEY = "bundleStats";
        Bundle bundle = intent.getBundleExtra(BUNDLE_STATS_KEY);
        ArrayList<Player> playersList;
        String PLAYER_KEY = "playersList";
        if (bundle != null && !bundle.isEmpty()) {
            playersList = bundle.getParcelableArrayList(PLAYER_KEY);
            if (playersList != null) {
                mStored.clearStoredPlayers();
                mStored.savePlayers(playersList);
            } else {
                Toast.makeText(this, getString(R.string.prev_stas), Toast.LENGTH_SHORT).show();
                playersList = mStored.loadPlayers(PREFS_NO_DATA);
            }
        } else {
            playersList = mStored.loadPlayers(PREFS_NO_DATA);
        }

        Bundle fragBundle = new Bundle();
        fragBundle.putParcelableArrayList(PLAYER_KEY, playersList);

        // Initialization
        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getSupportActionBar();
        StatsPageAdapter statsAdapter = new StatsPageAdapter(getSupportFragmentManager(), fragBundle);
        viewPager.setAdapter(statsAdapter);
        viewPager.setOffscreenPageLimit(2);

        /**
         * on swiping the viewpager make respective tab selected
         * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        lvlTab = actionBar.newTab().setText(getResources().getText(R.string.lvl_tab));
        lvlTab.setTabListener(this);

        gearTab = actionBar.newTab().setText(getResources().getText(R.string.gear_tab));
        gearTab.setTabListener(this);

        powerTab = actionBar.newTab().setText(getResources().getText(R.string.power_tab));
        powerTab.setTabListener(this);

        actionBar.addTab(lvlTab);
        actionBar.addTab(gearTab);
        actionBar.addTab(powerTab);

    }


    /**
     * Navigates to parent activity
     */
    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
