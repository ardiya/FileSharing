package com.ay.filesharing;

import java.util.ArrayList;

import protocol.DistributedFileSharingProtocol;
import util.StateManager;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;

import file.ActiveLogin;
import file.ActiveUser;


@SuppressLint("NewApi")
public class ListUserFragment extends Fragment {

	ArrayList<ActiveUser> list;
	ListUserAdapter listUserAdapter;
	SwipeListView swipeListView;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_listuser, container,
				false);
		swipeListView = (SwipeListView) rootView.findViewById(R.id.swipelist);
		if(list ==null){
			ActiveUser u = new ActiveUser();
			u.username = "Swipe Down to Refresh";
			list = new ArrayList<ActiveUser>();
			list.add(u);
		}
		listUserAdapter = new ListUserAdapter(getActivity(), this.list);
		
		DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
		//set up swipe right for Chat + FileTransfer
		swipeListView.setOffsetRight(displayMetrics.widthPixels - 128 * displayMetrics.density);
		swipeListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            swipeListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                      long id, boolean checked) {
                    mode.setTitle("Selected (" + swipeListView.getCountSelected() + ")");
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        default:
                            return false;
                    }
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    swipeListView.unselectedChoiceStates();
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }
            });
        }
        swipeListView.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onOpened(int position, boolean toRight) {
            }

            @Override
            public void onClosed(int position, boolean fromRight) {
            }

            @Override
            public void onListChanged() {
            }

            @Override
            public void onMove(int position, float x) {
            }

            @Override
            public void onStartOpen(int position, int action, boolean right) {
            }

            @Override
            public void onStartClose(int position, boolean right) {
            }

            @Override
            public void onClickFrontView(int position) {
            }

            @Override
            public void onClickBackView(int position) {
            }

            @Override
            public void onDismiss(int[] reverseSortedPositions) {
            }

        });
        
		swipeListView.setAdapter(listUserAdapter);

		//set SwipeLayout for refresh List User
		final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) rootView
				.findViewById(R.id.swipe_container);
		swipeLayout
				.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

					@Override
					public void onRefresh() {
						DistributedFileSharingProtocol protocol = DistributedFileSharingProtocol
								.getInstance();
						ActiveLogin activeLogin = (ActiveLogin) StateManager.getItem("currentActiveLogin");
						if(activeLogin==null)
						{
							list.clear();
							ActiveUser u = new ActiveUser();
							u.username = "No ActiveLogin";
							list.add(u);
							listUserAdapter.notifyDataSetChanged();
							return;
						}
                        StateManager.deleteItem("activeuser");
						protocol.requestActiveUser(activeLogin.username,activeLogin.privKey);
						final ActiveLogin finalActiveLogin = activeLogin;
						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								ArrayList<ActiveUser> aLa = (ArrayList<ActiveUser>) StateManager
										.getItem("activeuser");
								if (aLa != null) {
									for(ActiveUser u:aLa)
										Log.d("activeuser", u.username+" "+u.ip);
									list.clear();
									for(ActiveUser au:aLa)
										if(au.network.equals(finalActiveLogin.ActiveNetwork))
											list.add(au);
                                    if(list.isEmpty()){
                                        ActiveUser u = new ActiveUser();
                                        u.username = "No Active User";
                                        list.add(u);
                                    }
									listUserAdapter.notifyDataSetChanged();
								}
								swipeLayout.setRefreshing(false);
							}
						}, 5000);
					}
				});
        swipeLayout.setColorScheme(R.color.light_blue_500,
                R.color.red_500,
                R.color.green_500,
                R.color.yellow_500);
        refreshActiveUser();
		return rootView;
	}

    public void refreshActiveUser() {
        Log.d("ListUserFragment", "refreshActiveUser called");
        new Thread(){
            public void run() {
                StateManager.deleteItem("activeuser");
                ActiveLogin activeLogin = (ActiveLogin) StateManager.getItem("currentActiveLogin");
                if(activeLogin!=null)
                    DistributedFileSharingProtocol.getInstance().requestActiveUser(activeLogin.username,activeLogin.privKey);
            }
        }.start();
        Runnable r = new Runnable() {
            @Override
            public void run() {
            ArrayList<ActiveUser> aLa = (ArrayList<ActiveUser>) StateManager
                    .getItem("activeuser");
            if (aLa != null) {
                list.clear();
                ActiveLogin activeLogin = (ActiveLogin) StateManager.getItem("currentActiveLogin");
                for(ActiveUser au:aLa)
                    if(au.network.equals(activeLogin.ActiveNetwork))
                        list.add(au);
                if(list.isEmpty()){
                    ActiveUser u = new ActiveUser();
                    u.username = "No Active User";
                    list.add(u);
                }
                listUserAdapter.notifyDataSetChanged();
            }
            Log.d("ListUserFragment", "refreshActiveUser Runnable end");
            }
        };
        new Handler().postDelayed(r, 500);
        new Handler().postDelayed(r, 1500);
    }
}
