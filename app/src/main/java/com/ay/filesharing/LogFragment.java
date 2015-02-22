package com.ay.filesharing;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;

import file.ActiveLogin;
import util.SqliteUtil;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.ay.filesharing.view.FileLogStatusAdapter;

import file.FileLogStatus;
import util.StateManager;

public class LogFragment extends Fragment {

	ListView listViewLog;
	FileLogStatusAdapter fileLogStatusAdapter;
	ArrayList<FileLogStatus> listFileLogStatus = new ArrayList<>();

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_log, container,
				false);
		listViewLog = (ListView) rootView.findViewById(R.id.listView1);
		listViewLog.setEmptyView(rootView.findViewById(R.id.empty));
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			listViewLog.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			listViewLog.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					FileLogStatus fileLogStatus = fileLogStatusAdapter.getItem(arg2);
					Log.d("path", fileLogStatus.getFileName()+" "+fileLogStatus.getPath());
					try
					{
						Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
						File file = new File(fileLogStatus.getPath());
						String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
						String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
						                myIntent.setDataAndType(Uri.fromFile(file), mimetype);
						startActivity(myIntent);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});
			listViewLog.setMultiChoiceModeListener(new MultiChoiceModeListener() {

						@Override
						public boolean onPrepareActionMode(ActionMode mode,
								Menu menu) {
							return false;
						}

						@Override
						public void onDestroyActionMode(ActionMode mode) {
							fileLogStatusAdapter.removeSelection();
							listViewLog.setAdapter(fileLogStatusAdapter);
						}

						@Override
						public boolean onCreateActionMode(ActionMode mode,
								Menu menu) {
							Log.d("LogFragmentMultiChoice", "CreatedActionMode");
							mode.getMenuInflater().inflate(R.menu.log, menu);
							return true;
						}

						@Override
						public boolean onActionItemClicked(ActionMode mode,
								MenuItem item) {
							Log.d("LogFragmentMultiChoice", "Clicked");
							switch (item.getItemId()) {
							case R.id.menu_delete:
								SparseBooleanArray selected = fileLogStatusAdapter
										.getSelectedIds();
								for (int i = selected.size() - 1; i >= 0; i--) {
									SqliteUtil util = new SqliteUtil(
											getActivity());
									try {
										util.delete(fileLogStatusAdapter.getItem(i));

										fileLogStatusAdapter.remove(fileLogStatusAdapter.getItem(i));
									} catch (SQLException e) {
										Toast.makeText(getActivity(),
												e.getLocalizedMessage(),
												Toast.LENGTH_LONG).show();
									} finally {
										util.close();
									}
								}
								mode.finish();
								return true;
							default:
								return false;
							}

						}

						@Override
						public void onItemCheckedStateChanged(ActionMode mode,
								int position, long id, boolean checked) {
							final int checkedCount = listViewLog
									.getCheckedItemCount();
							mode.setTitle(checkedCount + " Selected");
							fileLogStatusAdapter.toggleSelection(position);
							View v = getViewByPosition(position, listViewLog);
							LinearLayout l = (LinearLayout) v
									.findViewById(R.id.layout1);
							if (checked)
								l.setBackgroundColor(Color.GRAY);
							else
								l.setBackgroundColor(Color.TRANSPARENT);
						}

						public View getViewByPosition(int pos, ListView listView) {
							final int firstListItemPosition = listView
									.getFirstVisiblePosition();
							final int lastListItemPosition = firstListItemPosition
									+ listView.getChildCount() - 1;

							if (pos < firstListItemPosition
									|| pos > lastListItemPosition) {
								return listView.getAdapter().getView(pos, null,
										listView);
							} else {
								final int childIndex = pos
										- firstListItemPosition;
								return listView.getChildAt(childIndex);
							}
						}

					});
		}
		fileLogStatusAdapter = new FileLogStatusAdapter(getActivity(), listFileLogStatus);
		refreshLogList();
		listViewLog.setAdapter(fileLogStatusAdapter);
        broadcastReceiver = createBroadcastReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver,
                new IntentFilter("com.ay.filesharing.LogFragment"));
		return rootView;
	}

    BroadcastReceiver broadcastReceiver;

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String method = (String) intent.getExtras().get("method");
                Log.e("LogFragment", "BroadcastReceived method " + method);
                switch (method) {
                    case "updateLogList":
                        refreshLogList();
                        break;
                }
            }

        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

	public void refreshLogList() {
		//Toast.makeText(getActivity(), "Refreshing Log List", Toast.LENGTH_SHORT).show();
		ArrayList<FileLogStatus> res;
		SqliteUtil util = new SqliteUtil(getActivity());
		try {
			res = (ArrayList<FileLogStatus>) util.query(FileLogStatus.class)
					.queryForAll();
			if (res != null) {
				listFileLogStatus.clear();
                ActiveLogin activeLogin = (ActiveLogin) StateManager.getItem("currentActiveLogin");
                if(activeLogin!=null) {
                    for (FileLogStatus fls : res)
                        if (fls.getNetworkName().equals(activeLogin.ActiveNetwork))
                            listFileLogStatus.add(fls);
                }else
                    listFileLogStatus.addAll(res);
				fileLogStatusAdapter.notifyDataSetChanged();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			util.close();
		}

	}
}
