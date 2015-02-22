package com.ay.filesharing;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

import protocol.DistributedFileSharingProtocol;

import networking.ftp.FTPListener;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.ay.filesharing.view.FTPFileAdapter;

import file.ActiveLogin;
import file.ActiveUser;
import file.FTPUser;
import file.FileLogStatus;

public class FileTransferFragment extends Fragment {

	private ActiveUser ftpClient;

	private ActiveLogin activeLogin;
	static Stack<String> pathHistory = new Stack<>();
	
	ArrayList<FTPFile> listFile = new ArrayList<>();
	FTPFileAdapter ftpFileAdapter;
	ListView listViewFtpFile;
    LinearLayout layoutHistory;
	@Override
	//protected void onCreate(Bundle savedInstanceState) {
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_file_transfer, container,
                false);
		Bundle extras = getArguments();
		ftpClient = (ActiveUser) extras.get(ClientActivity.TAG_CLIENT);
		activeLogin = (ActiveLogin) extras.get(ClientActivity.TAG_ACTIVE_LOGIN);

		if(ftpFileAdapter ==null) ftpFileAdapter = new FTPFileAdapter(getActivity(),
				R.layout.view_file, listFile);
        layoutHistory = (LinearLayout) rootView.findViewById(R.id.layoutHistory);
		listViewFtpFile = (ListView) rootView.findViewById(R.id.listView1);
		listViewFtpFile.setAdapter(ftpFileAdapter);
		listViewFtpFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                FTPFile ftpFile = listFile.get(position);
                if (ftpFile.getType() == FTPFile.TYPE_DIRECTORY) {
                    pathHistory.push(pathHistory.peek() + ftpFile.getName() + "/");
                    new refreshListFile().execute();
                } else {
                    Toast.makeText(getActivity(), "Fetching "+ftpFile.getName()+"...", Toast.LENGTH_LONG).show();
                    new DownloadFile(ftpFile).execute();
                }

            }
        });
		if(pathHistory.isEmpty()) pathHistory.push("/");
		rootView.findViewById(R.id.ImageButton01).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pathHistory.push("/");
                new refreshListFile().execute();
            }
        });
		new refreshListFile().execute();

        return rootView;
	}

    public void generateLayoutHistory(String pathText){

        layoutHistory.removeAllViews();
        String [] paths = pathText.split("/");
        String path = "/";
        for(int i = 1; i<paths.length;i++){
            final Button button = new Button(getActivity());
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
			ftpFileAdapter.notifyDataSetChanged();
            try {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        generateLayoutHistory(pathHistory.peek());
                    }
                });
            }catch (Exception e){ e.printStackTrace(); }
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
            }catch (SocketTimeoutException e){
                Toast.makeText(getActivity(),"Request Timeout", Toast.LENGTH_SHORT).show();
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
            	client.download(pathHistory.peek()+ftpFile.getName(), localFile, new FTPListener(ftpFile.getSize(), s, getActivity(), "Fetching in progress"));
            	return "Done Fetching";
	        }catch(Exception e){
	        	e.printStackTrace();
	        	return e.getLocalizedMessage();
	        }
		}
		
	}
}
