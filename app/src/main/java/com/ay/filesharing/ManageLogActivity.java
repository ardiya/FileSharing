package com.ay.filesharing;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.ay.filesharing.view.FileLogAdapter;
import com.gc.materialdesign.widgets.Dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import file.ActiveLogin;
import file.FileLog;
import file.FileLogManager;
import file.Network;
import file.NetworkList;
import protocol.DistributedFileSharingProtocol;
import util.StateManager;

public class ManageLogActivity extends ActionBarActivity {

	@InjectView(R.id.listView1)
	ListView listViewManageLog;
	ArrayList<FileLog> listLogFiles;
	FileLogAdapter fileLogAdapter;

    @InjectView(R.id.spinner)
    Spinner spinnerNetworkList;
    ArrayList<String> networkLists;
    ArrayAdapter<String> networkAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manage_log);
		ButterKnife.inject(this);

        networkLists=new ArrayList<>();
        networkAdapter = new ArrayAdapter<String>(getApplicationContext(),
                R.layout.simple_spinner_item, networkLists);

        networkAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinnerNetworkList.setAdapter(networkAdapter);
        spinnerNetworkList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ManageLogActivity.this, "Getting log list for network " + spinnerNetworkList.
                        getSelectedItem().toString(), Toast.LENGTH_SHORT).show();
                final String networkName = spinnerNetworkList.getSelectedItem().toString();
                ArrayList<ActiveLogin> als = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
                ActiveLogin activeLogin = null;
                for(ActiveLogin a: als)
                    if(a.ActiveNetwork.equals(networkName))
                        activeLogin = a;
                if(activeLogin!=null)
                    DistributedFileSharingProtocol.getInstance().requestReplaceLogFile(networkName, activeLogin.username, activeLogin.privKey);
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshFileLog(networkName);
                    }
                },1000);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        refreshNetwork(null);

		listLogFiles = new ArrayList<>();
		fileLogAdapter = new FileLogAdapter(getApplicationContext(), listLogFiles);
		listViewManageLog.setAdapter(fileLogAdapter);
        listViewManageLog.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String networkName = spinnerNetworkList.getSelectedItem().toString();
                ArrayList<ActiveLogin> alal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
                if (alal == null) {
                    alal = new ArrayList();
                }
                ActiveLogin ul = null;
                for (int i = 0; i < alal.size(); i++) {
                    ActiveLogin temp = alal.get(i);
                    if (temp.ActiveNetwork.equals(networkName)) {
                        ul = alal.get(i);
                    }
                }
                if (ul == null) {
                    return false;
                }
                final ActiveLogin u = ul;
                final FileLog fileLog = listLogFiles.get(position);
                Dialog dialog = new Dialog(ManageLogActivity.this, "Confirmation",
                        String.format("Are You sure to delete %s on user %s", fileLog.getFilename(), fileLog.getUsernameTo()));
                dialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DistributedFileSharingProtocol.getInstance().requestDeleteFile(fileLog.getFilename(), networkName, u);
                        Dialog ok = new Dialog(ManageLogActivity.this, "Success", "Delete command sent");
                        ok.show();
                        ok.getButtonAccept().setText("OK");
                        ok.getButtonCancel().setVisibility(View.INVISIBLE);
                    }
                });
                dialog.show();
                return true;
            }
        });

	}

    public void refreshNetwork(View v) {
        DistributedFileSharingProtocol.getInstance().requestNetworkName();
        List<String> networkList = (List<String>) StateManager.getItem("AvailableNetwork");
        if (networkList != null) {
            networkLists.clear();
            networkLists.addAll(networkList);
            networkAdapter.notifyDataSetChanged();
        }else{
            //Load From File
            Log.d("Refresh Network List", "Loading From File");
            NetworkList n = new NetworkList();
            ArrayList<Network> networkFromFile = n
                    .getNetworkTable(getApplicationContext());
            if(n!=null && networkFromFile!=null && !networkFromFile.isEmpty()){
                networkLists.clear();
                for(Network net:networkFromFile)
                    networkLists.add(net.NetworkName);
                networkAdapter.notifyDataSetChanged();
                Log.d("Refresh Network List", networkLists.size()+" Network From File");
            }else{
                Log.d("Refresh Network List", "No Data From File");
            }
        }
    }

	public void refreshFileLog(String networkName){
        listLogFiles.clear();
        FileLogManager flm = new FileLogManager(getApplicationContext());
        for (File path : getApplicationContext().getFilesDir().listFiles()) {
            if (path.toString().matches("(.*)" + networkName + "_log.dat")) {
                ArrayList<FileLog> dbLog = flm.getFileLogListFromFile(path.toString());
                listLogFiles.addAll(dbLog);
            }
        }
        fileLogAdapter.notifyDataSetChanged();
	}
}

