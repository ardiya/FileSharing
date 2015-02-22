package com.ay.filesharing;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

import protocol.DistributedFileSharingProtocol;

import networking.ftp.FTPListener;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ay.filesharing.view.FTPFileAdapter;

import file.ActiveLogin;
import file.ActiveUser;
import file.FTPUser;
import file.FileLogStatus;

public class FileTransferActivity extends ActionBarActivity {

	public static final String TAG_FTP_PATH = "path";
	public static final String TAG_FTP_CLIENT = "ftpclient";
	private ActiveUser ftpClient;
	public static final String TAG_ACTIVE_LOGIN = "activelogin";
	private ActiveLogin activeLogin;
	static Stack<String> pathHistory = new Stack<>();
	
	ArrayList<FTPFile> listFile = new ArrayList<>();
	FTPFileAdapter adapter;
	ListView lv;
    LinearLayout layoutHistory;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_transfer);
		Bundle extras = getIntent().getExtras();
		ftpClient = (ActiveUser) extras.get(TAG_FTP_CLIENT);
		activeLogin = (ActiveLogin) extras.get(TAG_ACTIVE_LOGIN);
		if(adapter==null)adapter = new FTPFileAdapter(getApplicationContext(), 
				R.layout.view_file, listFile);
        layoutHistory = (LinearLayout) findViewById(R.id.layoutHistory);
		lv = (ListView) findViewById(R.id.listView1);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
				    int position, long id) {
				FTPFile ftpFile = listFile.get(position);
				if(ftpFile.getType() == FTPFile.TYPE_DIRECTORY){
					pathHistory.push(pathHistory.peek()+ftpFile.getName()+"/");
					new refreshListFile().execute();
				}else{
					Toast.makeText(getApplicationContext(), "Downloading...", Toast.LENGTH_LONG).show();
					new DownloadFile(ftpFile).execute();
				}
				
			}
		});
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
				    int position, long id) {
				Toast.makeText(getApplicationContext(), "LONG ITEM CLICK", Toast.LENGTH_SHORT).show();
				return false;
			}
		});
		if(pathHistory.isEmpty()) pathHistory.push("/");
		
		new refreshListFile().execute();
	}

	public void home(View v){
		pathHistory.push("/");
		new refreshListFile().execute();
	}

    @Override
    public void onBackPressed() {
        if(pathHistory.size()>1){
            pathHistory.pop();
            try{
                new refreshListFile().execute();
            }catch(Exception e){
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
        else
            super.onBackPressed();
    }

    public void generateLayoutHistory(String pathText){

        layoutHistory.removeAllViews();
        String [] paths = pathText.split("/");
        String path = "/";
        for(int i = 1; i<paths.length;i++){
            final Button button = new Button(getApplicationContext());
            button.setText(paths[i]);
            final String finalPath = (path+=paths[i]+"/");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pathHistory.push(finalPath);
                    new refreshListFile().execute();
                }
            });
            layoutHistory.addView(button);
        }
    }

	class refreshListFile extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPostExecute(Void result) {
			adapter.notifyDataSetChanged();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    generateLayoutHistory(pathHistory.peek());
                }
            });
			super.onPostExecute(result);
		}
		@Override
		protected Void doInBackground(Void... arg0) {
			FTPClient client = new FTPClient();
	        try {
	            client.connect(ftpClient.ip, 2225);
	            try{
	            	client.login(ftpClient.ftpUser, ftpClient.ftpPassword);
	            }catch(FTPException ex){
	            	FTPUser sessionKey = DistributedFileSharingProtocol.getInstance().requestSessionKey(ftpClient, activeLogin);
	            	ftpClient.ftpUser = sessionKey.username;
	            	ftpClient.ftpPassword = sessionKey.password;
	            	client.login(ftpClient.ftpUser, ftpClient.ftpPassword);
	            }
	            client.changeDirectory(pathHistory.peek());
	            listFile.clear();
	            listFile.addAll(Arrays.asList(client.list()));
	        }
            catch(it.sauronsoftware.ftp4j.FTPException e){
                if(e.getCode()==550){
                    //No Such Directory
                    pathHistory.pop();
                }
            }
	        catch(Exception e){
                Log.d("pathHistory.peek()", pathHistory.peek());
	        	e.printStackTrace();
	        }
			return null;
		}
		
	}
	class DownloadFile extends AsyncTask<Void, Void, String>{

		FTPFile ftpFile;
		public DownloadFile(FTPFile ftpFile){
			this.ftpFile = ftpFile;
		}
		@Override
		protected String doInBackground(Void... params) {
			FTPClient client = new FTPClient();
			String filename = ftpFile.getName();
			File localFile = new File(activeLogin.homeDirectory+filename);
			Log.d("localFilePath", localFile.getPath());
			if(localFile.exists()){
				Log.e("FileTransfer", filename+" already exist");
			}
	        try {
	            client.connect(ftpClient.ip, 2225);
	            try{
	            	client.login(ftpClient.ftpUser, ftpClient.ftpPassword);
	            }catch(FTPException ex){
	            	FTPUser sessionKey = DistributedFileSharingProtocol.getInstance().requestSessionKey(ftpClient, activeLogin);
	            	ftpClient.ftpUser = sessionKey.username;
	            	ftpClient.ftpPassword = sessionKey.password;
	            	client.login(ftpClient.ftpUser, ftpClient.ftpPassword);
	            }
	            FileLogStatus s = new FileLogStatus();
	            s.setId(100000 + new Random().nextInt(900000));
                s.setFilename(ftpFile.getName());
                s.setFrom(ftpClient.username);
                s.setType("download");
                s.setNetworkName(ftpClient.network);
                s.setTo(activeLogin.username);
                s.setProgress(Double.valueOf(0));
                s.setStatus("Transfering");
                s.setSize(ftpFile.getSize());
                s.setPath(localFile.getPath());
                s.setFtpclient(client);
            	client.download(pathHistory.peek()+ftpFile.getName(), localFile, new FTPListener(ftpFile.getSize(), s, getApplicationContext(), "Download in progress"));
            	return "Download Done";
	        }catch(Exception e){
	        	e.printStackTrace();
	        	return e.getLocalizedMessage();
	        }
		}
		
	}
}
