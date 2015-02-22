package com.ay.filesharing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ay.filesharing.view.FileAdapter;
import com.gc.materialdesign.views.ButtonRectangle;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class FolderChooserActivity extends ActionBarActivity {

	public static final String TAG_PATH = "path";
	static Stack<String> pathHistory = new Stack<String>();
	@InjectView(R.id.listView1)
	ListView listViewFile;
	@InjectView(R.id.button1)
    ButtonRectangle buttonSelect;
	ArrayList<File> listFile = new ArrayList<File>();
	FileAdapter fileAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_folder_chooser);
		ButterKnife.inject(this);
		if (pathHistory.isEmpty())
			pathHistory.push("/");
		if (fileAdapter == null)
			fileAdapter = new FileAdapter(getApplicationContext(),
					R.layout.view_file, listFile);
		refreshData();
		listViewFile.setAdapter(fileAdapter);
		listViewFile.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int postition, long arg3) {
                final File dir = listFile.get(postition);
                pathHistory.push(pathHistory.peek() + dir.getName() + "/");
                Log.d("click listFile.size()", listFile.size() + "");
                refreshData();
            }
        });
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		pathHistory.clear();
		if (pathHistory.isEmpty())
			pathHistory.push("/");
	}

	private void refreshData() {
		listFile.clear();
		String dir = pathHistory.peek();
		buttonSelect.setText(String.format("Select '%s'", dir));
		File[] files = new File(dir).listFiles();
		if (files != null)
			for (File f : files)
				if (f.isDirectory())
					listFile.add(f);
		fileAdapter.notifyDataSetChanged();
	}

	public void select(View v) {
		Intent data = new Intent();
		data.putExtra(TAG_PATH, pathHistory.peek());
		if (getParent() == null) {
			setResult(ActionBarActivity.RESULT_OK, data);
		} else {
			getParent().setResult(ActionBarActivity.RESULT_OK, data);
		}
		finish();
	}

    @Override
    public void onBackPressed() {
        if (pathHistory != null && pathHistory.size() > 1){
            pathHistory.pop();
            refreshData();
        } else {
            super.onBackPressed();
        }
    }
}
