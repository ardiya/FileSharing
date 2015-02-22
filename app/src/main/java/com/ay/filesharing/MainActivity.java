package com.ay.filesharing;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import file.ActiveLogin;
import protocol.DistributedFileSharingProtocol;
import util.SqliteUtil;
import util.StateManager;



public class MainActivity extends ActionBarActivity implements
		ActionBar.TabListener {

	ArrayList<ActiveLogin> listActiveLogin;
	ActiveLogin currentActiveLogin;
	ArrayList<String> sListActiveNetwork;
	ArrayAdapter<String> activeNetworkAdapter;
    Spinner spinnerActionBar;
	SectionsPagerAdapter sectionsPagerAdapter;
	ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(getApplicationContext(), FileSharingService.class));
		setContentView(R.layout.activity_main);
		loadDBActiveLogin();
		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setCustomView(R.layout.spinner_layout);
		spinnerActionBar = (Spinner) actionBar.getCustomView().findViewById(R.id.spinner1);
		if(sListActiveNetwork==null)sListActiveNetwork = new ArrayList<String>();
		activeNetworkAdapter = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.simple_spinner_item, sListActiveNetwork);
		activeNetworkAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_action_bar);
		spinnerActionBar.setAdapter(activeNetworkAdapter);
		spinnerActionBar.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                currentActiveLogin = listActiveLogin.get(position);
                notifyUpdateActiveLogin(currentActiveLogin);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                if (!listActiveLogin.isEmpty()) {
                    currentActiveLogin = listActiveLogin.get(0);
                    notifyUpdateActiveLogin(currentActiveLogin);
                }else{
                    spinnerActionBar.setSelection(listActiveLogin.size()-1);
                }
            }
            private void notifyUpdateActiveLogin(ActiveLogin currentActiveLogin) {
                StateManager.setItem("currentActiveLogin", currentActiveLogin);
                DistributedFileSharingProtocol.getInstance().requestRefreshUser(currentActiveLogin);
                if(myFolderFragment!=null) myFolderFragment.refreshCurrentUser();
                if(logFragment!=null) logFragment.refreshLogList();
                if(listUserFragment!=null) listUserFragment.refreshActiveUser();
            }
        });
		actionBar.setDisplayShowCustomEnabled(true);
		refreshActiveLogin();
		
		sectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(sectionsPagerAdapter);

		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
						if(position==2&&logFragment!=null){
                            Log.d("setOnPageChangeListener: position", ""+position);
                            logFragment.refreshLogList();
                        }
					}
				});

		for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(actionBar.newTab()
                    .setText(sectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }
		
	}

    @Override
    public void onBackPressed() {
        try {
            Fragment fragment = sectionsPagerAdapter.getItem(viewPager.getCurrentItem());
            if(fragment instanceof MyFolderFragment) {
                if(!myFolderFragment.onBackPressed())
                    super.onBackPressed();
            }else
                super.onBackPressed();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if(listActiveLogin==null) return true;
		boolean isAdmin = false, isClientAdmin = false, isCreator = false;
		for(ActiveLogin al : listActiveLogin){
			Log.d("MainActivity", "listActiveLogin["+al.username+"]"+al.role);
			if(al.role.equals("admin")){
				isAdmin = true;
			}else if(al.role.equals("creator")){
				isCreator = true;
			}else if(al.role.equals("clientadmin")){
				isClientAdmin = true;
			}
		}
		Log.d("MainActivity", "isAdmin:"+isAdmin);
		Log.d("MainActivity", "isCreator:"+isCreator);
		Log.d("MainActivity", "isClientAdmin:"+isClientAdmin);
		if(isAdmin){
			getMenuInflater().inflate(R.menu.menu_manage_log, menu);
		}
		if(isAdmin || isCreator){
			getMenuInflater().inflate(R.menu.menu_manage_network, menu);
		}
		if(isAdmin || isCreator || isClientAdmin){
			getMenuInflater().inflate(R.menu.menu_manage_user, menu);
		}
		getMenuInflater().inflate(R.menu.menu_logout, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			callUpdatePreference();
			return true;
		}
		else if(id== R.id.action_login){
			callLogin();
			return true;
		}else if(id==R.id.action_logout){
			if(listActiveLogin != null && listActiveLogin.contains(currentActiveLogin))
			{
				listActiveLogin.remove(currentActiveLogin);
				StateManager.setItem("ActiveLogin", listActiveLogin);
				writeDBActiveLogin();
				refreshActiveLogin();
			}
            return true;
		}
        else if(id == R.id.action_open_shared_folder){
            String location = currentActiveLogin.homeDirectory;
            try {
                Log.d("opening shared folder", location);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse(location), "resource/folder");
                startActivity(i);
            }catch(Exception e){
                try{
                    Intent intent = new Intent();
                    intent.setAction("com.sec.android.app.myfiles.VIEW");
                    intent.putExtra("folderPath", location);
                    startActivity(intent);
                }catch(Exception e2) {
                    Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if(id==R.id.action_manage_log){
            startActivity(new Intent(getApplicationContext(), ManageLogActivity.class));
        }else if(id==R.id.action_manage_network){
            startActivity(new Intent(getApplicationContext(), ManageNetworkActivity.class));
        }else if(id==R.id.action_manage_user){
            Intent intent = new Intent(getApplicationContext(), ManageUserActivity.class);
            try{
                intent.putExtra(ManageUserActivity.TAG_SELECTED_NETWORK, currentActiveLogin.ActiveNetwork);
            }catch (Exception e){}
            startActivity(intent);
        }
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}
	
	void writeDBActiveLogin(){
		SqliteUtil util = new SqliteUtil(getApplicationContext());
		try {
			Log.d("writeDBActiveLogin", "Start");

			util.clear(ActiveLogin.class);
			for(ActiveLogin al :listActiveLogin)
				util.insert(al);
			
			Log.d("writeDBActiveLogin", "Finished");
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			util.close();
		}
		
	}
	void loadDBActiveLogin(){
		SqliteUtil util = new SqliteUtil(getApplicationContext());
		try {
			Log.d("loadDBActiveLogin", "Loading");
			ArrayList<ActiveLogin> query = (ArrayList) util.query(ActiveLogin.class).queryForAll();
			if(query!=null && query.size()>0){
				StateManager.setItem("ActiveLogin", query);
			}
			Log.d("loadDBActiveLogin", "Finished, "+ query==null?"null":query.size() +" data");
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			util.close();
		}
	}
	@SuppressLint("NewApi")
	void refreshActiveLogin(){
		listActiveLogin = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
		if(sListActiveNetwork==null) {
			sListActiveNetwork=new ArrayList<String>();
		}
		sListActiveNetwork.clear();
		if(listActiveLogin==null || listActiveLogin.isEmpty()){
			callLogin();
			return;
		}
		for(Iterator<ActiveLogin> i = listActiveLogin.iterator();i.hasNext();)
		{
			ActiveLogin l = i.next();
			sListActiveNetwork.add(
					String.format("%s(%s)",l.ActiveNetwork, l.username)
					);
		}
		activeNetworkAdapter.notifyDataSetChanged();
		if(listActiveLogin != null && listActiveLogin.size() > 0){
			writeDBActiveLogin();
		}
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
			Log.v("MainActivity", "invalidateOptionsMenu");
			invalidateOptionsMenu();
		}
	}
	public void callLogin(){
		startActivityForResult(new Intent(getApplicationContext(), LoginActivity.class),
				77);
	}
	public void callUpdatePreference(){
		Intent i = new Intent(getApplicationContext(), PreferencesActivity.class);
		i.putExtra(PreferencesActivity.TAG_ACTIVE_LOGIN, currentActiveLogin);
		startActivityForResult(i, 12);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity[onActivityResult]", String.format("requestCode:%d, resultCode:%d", requestCode, resultCode));
		switch(requestCode){
		case 77:
			refreshActiveLogin();
			break;
		case 12:
			refreshActiveLogin();
            if(myFolderFragment!=null) myFolderFragment.refreshCurrentUser();
			break;
		}
		
	}
	private LogFragment logFragment;
	private ListUserFragment listUserFragment;
	private MyFolderFragment myFolderFragment;
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0){
				if (listUserFragment==null){
					listUserFragment = new ListUserFragment();
				}
				return listUserFragment;
			}else if(position == 2){
				if(logFragment==null){
					logFragment = new LogFragment();
				}
				return logFragment;
			}
			else{
				if(myFolderFragment==null)
                    myFolderFragment = new MyFolderFragment();
				return myFolderFragment;
			}
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
	}


}
