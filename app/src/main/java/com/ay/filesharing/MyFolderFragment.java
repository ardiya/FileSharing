package com.ay.filesharing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.ay.filesharing.view.FileAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import file.ActiveLogin;
import util.StateManager;

public class MyFolderFragment extends Fragment {

	static ArrayList<File> listFile = new ArrayList<>();
	static FileAdapter fileAdapter;
	static Stack<String> pathHistory = new Stack<>();
	static ListView listViewFile;
    static LinearLayout layoutHistory;
	static String homeDir = "/";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_myfolder, container, false);
		listViewFile = (ListView) rootView.findViewById(R.id.listView1);
        layoutHistory = (LinearLayout) rootView.findViewById(R.id.layoutHistory);

		if (pathHistory.isEmpty())
			pathHistory.push(homeDir);
		if (fileAdapter == null)
			fileAdapter = new FileAdapter(getActivity(),
					R.layout.view_file, listFile);
		listViewFile.setAdapter(fileAdapter);
		listViewFile.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parentView, View view, int position,
					long id) {
				File file = fileAdapter.getItem(position);
				if(file.isDirectory()){
					pathHistory.push(pathHistory.peek() + file.getName() + "/");
					refreshData();
				}else{
					Intent shareIntent = new Intent(getActivity(), FileUploadActivity.class);
					shareIntent.setAction(Intent.ACTION_SEND);
					Uri uri = Uri.fromFile(file);
					shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
					shareIntent.setType("*/*");
					startActivity(shareIntent);
				}
			}
		});
		refreshCurrentUser();
		ImageButton btHome = (ImageButton) rootView.findViewById(R.id.imageButton1);
		btHome.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pathHistory.push(homeDir);
				refreshData();				
			}
		});
		return rootView;
	}
	
	public void refreshCurrentUser(){
        Log.d("MyFolderFragment", "refreshCurrentUser");
		ActiveLogin currentActiveLogin = (ActiveLogin) StateManager.getItem("currentActiveLogin");
		if(currentActiveLogin==null){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshCurrentUser();
                }
            },3000);
		}else{
            Log.d("MyFolderFragment", "oldDir="+homeDir+", newDir="+currentActiveLogin.homeDirectory);
			homeDir = currentActiveLogin.homeDirectory;
			pathHistory.clear();
			pathHistory.push(homeDir);
			refreshData();
		}
	}

	public void refreshData() {
		listFile.clear();
		String dir = pathHistory.peek();
        if(dir==null||dir.trim().equals("")) return;
		try{
            String s = String.format("Refreshing path %s with homeDir %s", dir, homeDir);
            Log.d("MyFolderFragment", s);
            //Toast.makeText(getActivity(), s , Toast.LENGTH_LONG).show();
        }catch(Exception e){}
		String pathText = dir.replace(homeDir, "");
		if(!pathText.startsWith("/")) pathText = "/" + pathText;
        generateLayoutHistory(pathText);
		File[] files = new File(dir).listFiles();
		
		if (files != null){
			listFile.addAll(Arrays.asList(files));
		}
		fileAdapter.notifyDataSetChanged();
	}
    public void generateLayoutHistory(String pathText){
        try{
            getActivity().getApplicationContext();
        }catch(Exception e) {
            Log.d("MyFolderActivity", "Failed to generate layout history for path "+pathText);
            return;
        }
        layoutHistory.removeAllViews();
        String [] paths = pathText.split("/");
        String path = homeDir;
        for(int i = 1; i<paths.length;i++){
            final Button button = new Button(getActivity());
            button.setText(paths[i]);
            final String finalPath = (path+=paths[i]+"/");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pathHistory.push(finalPath);
                    refreshData();
                }
            });
            layoutHistory.addView(button);
        }
    }

    public boolean onBackPressed(){
        if(pathHistory.size()>1){
            pathHistory.pop();
            refreshData();
            return true;
        }
        return false;
    }
}
