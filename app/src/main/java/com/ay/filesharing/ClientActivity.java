package com.ay.filesharing;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.transition.Slide;
import android.transition.Transition;
import android.view.Window;

import file.ActiveLogin;
import file.ActiveUser;


public class ClientActivity extends ActionBarActivity implements ActionBar.TabListener {

    private SectionsPagerAdapter sectionsPagerAdapter;

    private ViewPager viewPager;

    private ActiveLogin activeLogin;
    private ActiveUser client;

    public static final String TAG_CLIENT = "client";
    public static final String TAG_ACTIVE_LOGIN = "activelogin";
    public static final String TAG_START = "start";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(sectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        Bundle extras = getIntent().getExtras();
        viewPager.setCurrentItem(extras.getInt(TAG_START));
        activeLogin = (ActiveLogin) extras.get(TAG_ACTIVE_LOGIN);
        client = (ActiveUser) extras.get(TAG_CLIENT);
        setTitle(client.username);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putAll(getIntent().getExtras());
            if(position == 0){
                FileTransferFragment fileTransferFragment = new FileTransferFragment();
                fileTransferFragment.setArguments(bundle);
                return fileTransferFragment;
            }else{
                ChatFragment chatFragment = new ChatFragment();
                chatFragment.setArguments(bundle);
                return chatFragment;
            }

        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "File Transfer";
                case 1:
                    return "Chat";
            }
            return null;
        }
    }
}
