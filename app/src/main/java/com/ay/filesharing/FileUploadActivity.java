package com.ay.filesharing;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.ay.filesharing.view.SelectUserAdapter;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Random;

import file.ActiveLogin;
import file.ActiveUser;
import file.FTPUser;
import file.FileLogStatus;
import it.sauronsoftware.ftp4j.FTPClient;
import networking.ftp.FTPListener;
import protocol.DistributedFileSharingProtocol;
import util.SqliteUtil;
import util.StateManager;

public class FileUploadActivity extends ActionBarActivity {

	ListView listViewActiveUsers;
	SelectUserAdapter selectUserAdapter;
	ArrayList<ActiveUser> activeUsers;
	File file;
	ArrayList<ActiveLogin> listActiveLogin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_upload);
		startService(new Intent(getApplicationContext(), FileSharingService.class));
        setTitle("Select Users to Share");
		//load active login
		listActiveLogin = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
		if(listActiveLogin==null){
			SqliteUtil util = new SqliteUtil(getApplicationContext());
			try {
				Log.d("loadDBActiveLogin", "Loading");
				
				listActiveLogin = (ArrayList) util.query(ActiveLogin.class).queryForAll();
				if(listActiveLogin != null && !listActiveLogin.isEmpty()){
					StateManager.setItem("ActiveLogin", listActiveLogin);
				}else{
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    this.finish();
                }
				Log.d("loadDBActiveLogin", "Finished");
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				util.close();
			}
		}
		//load active user
		listViewActiveUsers = (ListView) findViewById(R.id.listView1);
		activeUsers = (ArrayList<ActiveUser>) StateManager.getItem("activeuser");
		if(activeUsers == null) {
			activeUsers = new ArrayList<>();
			ActiveUser object = new ActiveUser();
			object.username="Scroll down to refresh";
			activeUsers.add(object);
			new Thread(){
				@Override
				public void run() {
					if(listActiveLogin!=null)
						for(ActiveLogin activeLogin:listActiveLogin)
							DistributedFileSharingProtocol.getInstance().requestActiveUser(activeLogin.username,activeLogin.privKey);
				}
			}.start();
			new android.os.Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					ArrayList<ActiveUser> aLa = (ArrayList<ActiveUser>) StateManager.getItem("activeuser");
					if (aLa != null && !aLa.isEmpty()) {
						activeUsers.clear();
						activeUsers.addAll(aLa);
						selectUserAdapter.notifyDataSetChanged();
					}
				}
			}, 1000);
		}
		selectUserAdapter = new SelectUserAdapter(getApplicationContext(), R.layout.view_select_user, activeUsers, listActiveLogin);
		listViewActiveUsers.setAdapter(selectUserAdapter);
		
		//setup refresh gesture
		
		final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
		swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

					@Override
					public void onRefresh() {
						Log.d("FileUploadActivity", "Refreshing Active User");

						DistributedFileSharingProtocol protocol = DistributedFileSharingProtocol
								.getInstance();
						for(ActiveLogin activeLogin:listActiveLogin){
							Log.e("RequestingActiveUser", activeLogin.username);
							protocol.requestActiveUser(activeLogin.username,activeLogin.privKey);
						}
						new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                ArrayList<ActiveUser> aLa = (ArrayList<ActiveUser>) StateManager.getItem("activeuser");
                                ArrayList<ActiveUser> clone = (ArrayList<ActiveUser>) aLa.clone();
                                if(aLa!=null) {
                                    activeUsers.clear();
                                    activeUsers.addAll(clone);
                                    selectUserAdapter.notifyDataSetChanged();
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
		
		// show path
		try{
			Intent intent = getIntent();
		    String action = intent.getAction();
		    String type = intent.getType();
			if (Intent.ACTION_SEND.equals(action) && type != null) {
				Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
				if(uri != null){
					file = new File(new URI(uri.toString()));
				}else{
					Toast.makeText(this, "No URI to send file", Toast.LENGTH_LONG).show();
					finish();
				}
			}
		}catch(Exception e){
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	public void upload(View v){
		for(ActiveUser u: selectUserAdapter.getActiveUsers()){
			Log.d("Upload", u.username);
			new UploadFile(u).execute();
		}
		Toast.makeText(getApplicationContext(), "Sending to "+ selectUserAdapter.getActiveUsers().size()+" user(s)", Toast.LENGTH_SHORT).show();
	}
	class UploadFile extends AsyncTask<Void, Void, Void>{
		ActiveUser user;
		UploadFile(ActiveUser user){
			this.user = user;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			
			ActiveLogin activeLogin = null;
			for(ActiveLogin a: listActiveLogin){
				if(a.ActiveNetwork.equals(user.network)) {
					activeLogin = a;
					Log.d("found:activeLogin", a.ActiveNetwork);
				}
			}
			try {
				FTPUser ftpUser = null;
                int i = 0;
                do {
                    ftpUser = DistributedFileSharingProtocol.getInstance().requestSessionKey(user, activeLogin);
                    Thread.sleep(1000*i);
                    i++;
                    if(i>10) return null;
                } while(ftpUser==null);
				FTPClient client = new FTPClient();
				client.connect(user.ip, 2225);
				client.login(ftpUser.username, ftpUser.password);
				
				FileLogStatus s = new FileLogStatus();
	            s.setId(100000 + new Random().nextInt(900000));
                s.setFilename(file.getName());
                s.setFrom(activeLogin.username);
                s.setType("upload");
                s.setNetworkName(activeLogin.ActiveNetwork);
                s.setTo(user.username);
                s.setProgress(Double.valueOf(0));
                s.setStatus("Transfering");
                s.setSize(file.length());
                s.setPath(file.getPath());
                s.setFtpclient(client);
                FTPListener ftpListener = new FTPListener(file.length(), s, getApplicationContext(), "Sending to " + user.username);
				client.upload(file, ftpListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
}
