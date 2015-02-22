package com.ay.filesharing;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.gc.materialdesign.widgets.Dialog;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.InjectView;
import cryptography.EncryptionUtility;
import file.ActiveLogin;
import file.Network;
import file.NetworkList;
import file.NetworkUserDetail;
import file.NetworkUserTable;
import file.SignatureContainer;
import file.User;
import protocol.DistributedFileSharingProtocol;
import util.DatatypeConverter;
import util.ObjectConverter;
import util.StateManager;

public class LoginActivity extends ActionBarActivity {
	private BroadcastReceiver rec;

	List<String> networkLists;
	ArrayAdapter<String> networkAdapter;

	@InjectView(R.id.spinner1)
	Spinner spinnerNetworkList;
	@InjectView(R.id.editText1)
	EditText editTextUsername;
	@InjectView(R.id.editText2)
	EditText editTextPassword;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		ButterKnife.inject(this);

		networkLists = new ArrayList<String>();
		networkLists.add("No Network");

		networkAdapter = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.simple_spinner_item, networkLists);

		networkAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		spinnerNetworkList.setAdapter(networkAdapter);
		spinnerNetworkList
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						String network = (String) spinnerNetworkList.getSelectedItem();
						DistributedFileSharingProtocol.getInstance().requestGetNetworkUserTable(network);
                        DistributedFileSharingProtocol.getInstance().requestDeletedUserList();
                        Log.d("LoginActivity", "Request get network user table of network "+network);
						Toast.makeText(getApplicationContext(), network,Toast.LENGTH_SHORT).show();
						StateManager.deleteItem("adminip");
						DistributedFileSharingProtocol.getInstance().requestNetworkAdminIP(
								network);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		rec = createBroadcastReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(rec,
				new IntentFilter("com.ay.filesharing.MainActivity"));

	}

	private BroadcastReceiver createBroadcastReceiver() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String method = (String) intent.getExtras().get("method");
				Log.e("MainActivity", "BroadcastReceived method " + method);
				switch (method) {
				case "updateNetworkList":
					List<String> networkList = (List<String>) StateManager.getItem("AvailableNetwork");
					
					if (networkList != null) {
						networkLists.clear();
						networkLists.addAll(networkList);
					}
					networkAdapter.notifyDataSetChanged();
					break;
				}
			}
		};
	}

	public void login(View v) {
		DistributedFileSharingProtocol.getInstance()
				.requestGetNetworkUserTable(
						spinnerNetworkList.getSelectedItem().toString());
		new ValidateUserLogin().execute();
	}

	public void signUp(View w) {
		StateManager.deleteItem("adminip");
		Pattern emailpattern = Pattern
				.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
						+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

		Matcher matcher = emailpattern.matcher(editTextUsername.getText());
		if (!matcher.matches()) {
            Dialog err = new Dialog(LoginActivity.this, "Username Not Valid", "Acceptable username format is XX@XXX.X");
            err.show();
            err.getButtonAccept().setText("OK");
            err.getButtonCancel().setVisibility(View.INVISIBLE);
		} else if (editTextPassword.getText().length() < 3) {
            Dialog err = new Dialog(LoginActivity.this, "Password Not Valid", "Password needs to be at least 3 characters");
            err.show();
            err.getButtonAccept().setText("OK");
            err.getButtonCancel().setVisibility(View.INVISIBLE);
		} else {
			String networkName = spinnerNetworkList.getSelectedItem()
					.toString();
			DistributedFileSharingProtocol.getInstance().requestNetworkAdminIP(
					networkName);
			new RegisterUser().execute();
		}
	}

	public void refresh(View v) {
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

	class RegisterUser extends AsyncTask<String, Void, String> {

		ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(LoginActivity.this);
			dialog.setMessage("Loading...");
			dialog.show();
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(String result) {
			if (dialog != null)
				dialog.dismiss();
			if (result == null) {
                Dialog err = new Dialog(LoginActivity.this, "Error", "Sorry, Admin is currently offline");
                err.show();
                err.getButtonAccept().setText("OK");
                err.getButtonCancel().setVisibility(View.INVISIBLE);
            }else {
                Dialog err = new Dialog(LoginActivity.this, "Success", "Sign up success, please wait for admin activation");
                err.show();
                err.getButtonAccept().setText("OK");
                err.getButtonCancel().setVisibility(View.INVISIBLE);

			}
			super.onPostExecute(result);
		}

		@Override
		protected String doInBackground(String... arg0) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			String adminip = (String) StateManager.getItem("adminip");
			if (adminip == null) {
				return null;
			} else {
				DistributedFileSharingProtocol.getInstance().requestRegister(
						spinnerNetworkList.getSelectedItem().toString(),
						editTextUsername.getText().toString(),
						editTextPassword.getText().toString(), adminip);
				return adminip;
			}
		}

	}

	class ValidateUserLogin extends AsyncTask<Void, Void, String> {

		ProgressDialog dialog;
		ActiveLogin activeLogin;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(LoginActivity.this);
			dialog.setMessage("Loading...");
			dialog.show();
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(String result) {
			if (dialog != null)
				dialog.dismiss();
			if (result != "OK") {
                Dialog err = new Dialog(LoginActivity.this, "Error", result);
                err.show();
                err.getButtonAccept().setText("OK");
                err.getButtonCancel().setVisibility(View.INVISIBLE);
			} else {
				if(activeLogin.homeDirectory==null || activeLogin.homeDirectory.equals(""))
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Intent i = new Intent(LoginActivity.this, PreferencesActivity.class);
                            i.putExtra(PreferencesActivity.TAG_ACTIVE_LOGIN, activeLogin);
                            startActivity(i);
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                else{
                    setResult(RESULT_OK);
                    finish();
                }

				
			}
			super.onPostExecute(result);
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String username = editTextUsername.getText().toString();
			String networkname = (String) spinnerNetworkList.getSelectedItem()
					.toString();
			ArrayList<SignatureContainer> sca = NetworkUserTable
					.getNetworkUserTable(getApplicationContext(), networkname,
							null);
            if(sca==null||sca.isEmpty()) {
                DistributedFileSharingProtocol.getInstance().requestGetNetworkUserTable(networkname);
                return "Failed to get network User table, requesting to admin of network '"+networkname+"'";
            }else{
                Log.d("LoginActivity", "sca.size():"+sca.size());
                int index = -1;
                for (int it = 0; it < sca.size(); it++) {
                    Log.d("it", ""+it);
                    SignatureContainer sc = sca.get(it);
                    NetworkUserDetail nUd = (NetworkUserDetail) ObjectConverter
                            .StringToObject(sc.object);
                    Log.d("Looping sca", "Searching user in network " + nUd.networkName);
                    ArrayList<User> usr = nUd.userlist;
                    // Search in array


                    for (int i = 0; i < usr.size(); i++) {
                        Log.d("usr.get(i).username", usr.get(i).username);
                        if (usr.get(i).username.equals(username)) {
                            Log.d("Found", "index=" + i);
                            index = i;
                            break;
                        }
                    }
                    if (index != -1){
                        User u = usr.get(index);
                        byte[] enprivKey = DatatypeConverter
                                .parseBase64Binary(u.privateKey);
                        byte[] password = EncryptionUtility.createKeyFromPassword(
                                editTextPassword.getText().toString(),
                                u.privateKeyHash).getEncoded();
                        byte[] privKey = EncryptionUtility.decryptSymetric(
                                enprivKey, password);
                        if (privKey != null) {
                            try {
                                Log.v("privKey", DatatypeConverter.printBase64Binary(privKey));
                                if (u.privateKeyHash.equals(
                                        EncryptionUtility.hash(privKey))) {
                                    ActiveLogin al = new ActiveLogin();
                                    al.ActiveNetwork = networkname;
                                    al.username = username;
                                    al.privKey = privKey;
                                    al.role = u.role;
                                    al.pubKey = DatatypeConverter
                                            .parseBase64Binary(u.publicKey);
                                    SharedPreferences settings = getSharedPreferences("filesharing", 0);
                                    String dir =settings.getString(al.username+"@"+al.ActiveNetwork, null);
                                    if(dir!=null)
                                        al.homeDirectory = dir;

                                    ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager
                                            .getItem("ActiveLogin");
                                    if (aal == null) {
                                        aal = new ArrayList<ActiveLogin>();
                                    }
                                    for (int i = 0; i < aal.size(); i++) {
                                        ActiveLogin temp = aal.get(i);
                                        if (temp.ActiveNetwork.equals(networkname)) {
                                            return "You have been logged in in this network";
                                        }
                                    }
                                    this.activeLogin = al;
                                    boolean found = false;
                                    for (ListIterator<ActiveLogin> i = aal.listIterator(); i.hasNext(); ) {
                                        ActiveLogin cal = i.next();
                                        if (cal.ActiveNetwork.equals(activeLogin.ActiveNetwork) && cal.username.equals(cal.username)) {
                                            i.set(activeLogin);
                                            found = true;
                                        }
                                    }
                                    if (!found) {
                                        aal.add(activeLogin);
                                        Log.d("Login", "ActiveLogin Not Found, adding");
                                        StateManager.setItem("ActiveLogin", aal);
                                    }
                                    return "OK";
                                } else {
                                    Log.d("failed u.privateKeyHash", "'"
                                            + u.privateKeyHash + "'");
                                    Log.d("failed privKey",
                                            "'" + EncryptionUtility.hash(privKey)
                                                    + "'");
                                    return "Incorrect Password";
                                }
                            } catch (NoSuchAlgorithmException ex) {
                                ex.printStackTrace();
                                return "Your phone does not support encryption algorithm";
                            }
                        } else {
                            return "Incorrect Password(Failed to Generate Private Key)";
                        }
                    }
                }
                if(index == -1) {
                    DistributedFileSharingProtocol.getInstance().requestGetNetworkUserTable(networkname);
                    return "Invalid Username";
                }
            }
	 		return "Unknown Error";
		}

	}

}
