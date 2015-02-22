package com.ay.filesharing;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ay.filesharing.view.InputDialog;
import com.gc.materialdesign.widgets.Dialog;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import cryptography.EncryptionUtility;
import file.ActiveLogin;
import file.DeletedNetworkList;
import file.Network;
import file.NetworkList;
import file.NetworkUserTable;
import file.SignatureContainer;
import file.User;
import util.DatatypeConverter;
import util.StateManager;


public class ManageNetworkActivity extends ActionBarActivity {
    ArrayList<String> networkList;
    ArrayAdapter<String> networkAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_network);
        networkList = new ArrayList<>();
        ListView listView = (ListView) findViewById(R.id.listView1);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String networkName = networkList.get(position);
                Dialog confirm = new Dialog(ManageNetworkActivity.this, "Alert","Are you sure to delete network "+networkName);
                confirm.setOnAcceptButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteNetwork(networkName);
                    }
                });
                confirm.show();
                return true;
            }
        });
        networkAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.view_network, networkList){
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                if(view==null)
                    view = ((LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                            ).inflate(R.layout.view_network, parent, false);
                TextView txt = (TextView) view.findViewById(R.id.textView1);
                txt.setText(getItem(position));
                return view;
            }
        };
        listView.setAdapter(networkAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), ManageUserActivity.class);
                i.putExtra(ManageUserActivity.TAG_SELECTED_NETWORK, networkList.get(position));
                startActivity(i);
            }
        });
        refreshNetworkList();
    }
    public void addNetwork(View v){
        //Request Network Name
        final InputDialog dialogNetwork = new InputDialog(this, "Add Network", "Network Name");
        dialogNetwork.setOnAcceptButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final String inputNetwork = dialogNetwork.getEditText().getText().toString();
                    Log.d("AddNetwork[inputNetwork]", inputNetwork);
                    ArrayList<String> delNetworkArrayList = new DeletedNetworkList().getDeletedNetworkTable(ManageNetworkActivity.this);
                    if (inputNetwork.equals("")) {
                        Dialog err = new Dialog(ManageNetworkActivity.this, "Error", "Network name cannot be empty");
                        err.show();
                        err.getButtonAccept().setText("OK");
                        err.getButtonCancel().setVisibility(View.INVISIBLE);
                    } else if (networkList.contains(inputNetwork)) {
                        Dialog err = new Dialog(ManageNetworkActivity.this, "Error", "This network already exist, please choose a new network name");
                        err.show();
                        err.getButtonAccept().setText("OK");
                        err.getButtonCancel().setVisibility(View.INVISIBLE);
                    } else if (delNetworkArrayList.contains(inputNetwork)) {
                        Dialog err = new Dialog(ManageNetworkActivity.this, "Error", "This network name is not available because has been used and deleted previously, please choose a different network name");
                        err.show();
                        err.getButtonAccept().setText("OK");
                        err.getButtonCancel().setVisibility(View.INVISIBLE);
                    } else {
                        //request Admin Username
                        final InputDialog dialogUsername = new InputDialog(ManageNetworkActivity.this, "Create Admin", "Input username for new admin");
                        dialogUsername.setOnAcceptButtonClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final String inputUsername = dialogUsername.getEditText().getText().toString();
                                if(inputUsername==null||inputUsername.equals("")){
                                    Dialog err = new Dialog(ManageNetworkActivity.this, "Error", "Username cannot be empty");
                                    err.show();
                                    err.getButtonAccept().setText("OK");
                                    err.getButtonCancel().setVisibility(View.INVISIBLE);
                                }else {
                                    //request Admin Password
                                    final InputDialog dialogPassword = new InputDialog(ManageNetworkActivity.this, "Create Admin", "Input password for new admin");
                                    dialogPassword.setOnAcceptButtonClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            final String inputPassword = dialogPassword.getEditText().getText().toString();
                                            if(inputPassword == null || inputPassword.equals("")){
                                                Dialog err = new Dialog(ManageNetworkActivity.this, "Error", "Password cannot be empty");
                                                err.show();
                                                err.getButtonAccept().setText("OK");
                                                err.getButtonCancel().setVisibility(View.INVISIBLE);
                                            }else {
                                                addNetwork(inputNetwork, inputUsername, inputPassword);
                                            }
                                        }
                                    });
                                    dialogPassword.show();
                                }
                            }
                        });
                        dialogUsername.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        dialogNetwork.show();
    }
    void addNetwork(String networkName, String creatorUserName, String creatorPassword){
        //Generate Self Key
        KeyPair self = EncryptionUtility.generateAsymmetricKey();

        //Generate Network Key
        KeyPair net = EncryptionUtility.generateAsymmetricKey();

        //Put into Network Table
        NetworkList nList = new NetworkList();
        Network nt = new Network();
        nt.NetworkName = networkName;
        nt.Signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(networkName.getBytes(), net.getPrivate().getEncoded()));

        //Save Into File
        ArrayList<Network> nn = nList.getNetworkTable(getApplicationContext());

        if (!nn.contains(nt)) {
            nn.add(nt);
        }

        nList.addNetwork(getApplicationContext(),nn);

        User u = new User();
        u.username = creatorUserName;
        u.role = "creator";
        u.publicKey = DatatypeConverter.printBase64Binary(self.getPublic().getEncoded());
        try {
           u.privateKeyHash = EncryptionUtility.hash(self.getPrivate().getEncoded());
        }catch(NoSuchAlgorithmException e){
            Toast.makeText(getApplicationContext(), "This phone does not support specified algorithm", Toast.LENGTH_SHORT).show();
            return;
        }
        u.privateKey = DatatypeConverter.printBase64Binary(EncryptionUtility.encryptSymetric(self.getPrivate().getEncoded(), EncryptionUtility.createKeyFromPassword(creatorPassword,u.privateKeyHash).getEncoded()));

        ArrayList<User> userlist = new ArrayList();
        userlist.add(u);

        SignatureContainer sc = NetworkUserTable.createNetworkUserTable(networkName, creatorUserName, 1, self.getPrivate().getEncoded(), userlist);
        NetworkUserTable.replaceNetworkUserTable(getApplicationContext(), sc);
        refreshNetworkList();
        Dialog err = new Dialog(ManageNetworkActivity.this, "Success", "Network " + networkName + " Created");
        err.show();
        err.getButtonAccept().setText("OK");
        err.getButtonCancel().setVisibility(View.INVISIBLE);
    }
    void deleteNetwork(String networkName){
        ArrayList<ActiveLogin> alal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
        if (alal == null) {
            alal = new ArrayList();
        }
        ActiveLogin ul = null;
        for (int i = 0; i < alal.size(); i++) {
            ActiveLogin temp = alal.get(i);
            if (temp.role.equals("admin")) {
                ul = alal.get(i);
            }
        }
        if (ul == null) {
            Toast.makeText(getApplicationContext(), "This function only works for admin", Toast.LENGTH_SHORT).show();
            return;
        }
        NetworkList nList = new NetworkList();
        nList.selectAndRemoveNetworkByName(getApplicationContext(), networkName);
        refreshNetworkList();
        Dialog err = new Dialog(ManageNetworkActivity.this, "Success", "Network "+networkName+" deleted");
        err.show();
        err.getButtonAccept().setText("OK");
        err.getButtonCancel().setVisibility(View.INVISIBLE);
    }
    void refreshNetworkList(){
        ArrayList<Network> networkTable = new NetworkList().getNetworkTable(getApplicationContext());
        if(networkTable!=null) {
            networkList.clear();
            for (Network n : networkTable) {
                networkList.add(n.NetworkName);
            }
            networkAdapter.notifyDataSetChanged();
        }
    }

}
