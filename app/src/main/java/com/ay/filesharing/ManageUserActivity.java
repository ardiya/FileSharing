package com.ay.filesharing;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.ay.filesharing.view.InputDialog;
import com.ay.filesharing.view.UserStatusAdapter;
import com.gc.materialdesign.widgets.Dialog;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.InjectView;
import cryptography.EncryptionUtility;
import file.ActiveLogin;
import file.DeletedUser;
import file.DeletedUserList;
import file.Network;
import file.NetworkList;
import file.NetworkUserDetail;
import file.NetworkUserTable;
import file.SignatureContainer;
import file.UnapprovedNetworkUserTable;
import file.User;
import file.UserStatus;
import protocol.DistributedFileSharingProtocol;
import util.DatatypeConverter;
import util.ObjectConverter;
import util.StateManager;

import static file.NetworkUserTable.getNetworkUserTable;

public class ManageUserActivity extends ActionBarActivity {


    ArrayList<UserStatus> userList;
    @InjectView(R.id.listView1)
    ListView listViewUser;
    @InjectView(R.id.spinner)
    Spinner spinnerNetworkList;
    UserStatusAdapter userAdapter;
    ArrayList<String> networkLists;
    ArrayAdapter<String> networkAdapter;
    public static final String TAG_SELECTED_NETWORK = "selectednetwork";
    public static final String TAG_SEARCH_USER = "searchuser";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user);
        userList=new ArrayList<>();
        ButterKnife.inject(this);

        networkLists=new ArrayList<>();
        networkAdapter = new ArrayAdapter<>(getApplicationContext(),
                R.layout.simple_spinner_item, networkLists);

        networkAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinnerNetworkList.setAdapter(networkAdapter);
        spinnerNetworkList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ManageUserActivity.this, "Getting user list for network "+spinnerNetworkList.
                        getSelectedItem().toString(), Toast.LENGTH_SHORT).show();
                Log.d("refreshUserList","onItemSelected on network"+spinnerNetworkList.getSelectedItem().toString());
                try{
                    refreshUserList(getIntent().getStringExtra(TAG_SEARCH_USER));
                }catch(Exception e) {
                    refreshUserList("");
                }
                DistributedFileSharingProtocol.getInstance().requestDeletedUserList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerNetworkList.setSelection(0);
            }
        });
        refreshNetwork(null);
        try {
            spinnerNetworkList.setSelection(
                    networkLists.indexOf(getIntent().getExtras().getString(TAG_SELECTED_NETWORK))
            );
        }catch(Exception e){}

        userAdapter = new UserStatusAdapter(getApplicationContext(), R.layout.view_user_status, userList);
        listViewUser.setAdapter(userAdapter);
        listViewUser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserStatus userStatus = userList.get(position);
                if (!userStatus.getStatus().equals("Active")) {
                    activateUser(spinnerNetworkList.getSelectedItem().toString(), userStatus.getUsername());
                } else {
                    Toast.makeText(getApplicationContext(), "User already activated", Toast.LENGTH_SHORT).show();
                }
            }
        });
        listViewUser.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final UserStatus user = userList.get(position);
                Dialog dialog = new Dialog(ManageUserActivity.this, "Confirm",
                        String.format("Are you sure to delete %s user %s", user.getStatus(), user.getUsername()));
                dialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String networkName = spinnerNetworkList.getSelectedItem().toString();
                        if (user.getStatus().equals("Active")) {
                            deleteUser(networkName, user);
                        } else {
                            deletePendingUser(networkName, user);
                        }
                    }
                });
                dialog.show();
                dialog.getButtonAccept().setText("Delete");
                return true;
            }
        });
        handleSearchIntent(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getComponentName()));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.search)
            onSearchRequested();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startActivity(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            intent.putExtra(TAG_SELECTED_NETWORK, spinnerNetworkList.getSelectedItem().toString());
            intent.putExtra(TAG_SEARCH_USER, intent.getStringExtra(SearchManager.QUERY));
        }
        super.startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleSearchIntent(intent);
    }
    private void handleSearchIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d("Handle Query", query);
            refreshUserList(query);
        }
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
    void refreshUserList(String query){
        Log.d("ManageUserActivity[refreshUserList]", query);
        userList.clear();
        String networkName = spinnerNetworkList.getSelectedItem().toString();

        //get Active User
        ArrayList<SignatureContainer> signatureContainer = getNetworkUserTable(getApplicationContext(), networkName, null);
        ArrayList<DeletedUser> delUser = DeletedUserList.getDeletedUserList(getApplicationContext());
        String del = "";
        for (int i = 0; i < delUser.size(); i++) {
            del = String.format("%s-%s, %s",delUser.get(i).username, delUser.get(i).networkName, del);
        }
        Log.d("ManageUserActivity", String.format("Deleted User [%s]", del));
        for (int i = 0; i < signatureContainer.size(); i++) {
            NetworkUserDetail nude = (NetworkUserDetail) ObjectConverter.StringToObject(signatureContainer.get(i).object);
            ArrayList<User> ulist = nude.userlist;
            for (ListIterator<User> j = ulist.listIterator(); j.hasNext();) {
                User u = j.next();
                boolean show = true;
                for (int k = 0; k < delUser.size(); k++) {
                    if (u.username.contentEquals(delUser.get(k).username) && delUser.get(k).networkName.equals(networkName)) {
                        show = false;
                        break;
                    }
                    if(!u.username.contains(query)){
                        show = false;
                        break;
                    }
                }
                if (show) {
                    userList.add(new UserStatus(u.username, u.role, "Active", signatureContainer.get(i).Username));
                }
            }
        }

        //get Pending User
        ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
        if (aal == null) {
            Toast.makeText(this, "No Privilege to administer network " + networkName, Toast.LENGTH_SHORT).show();
            userAdapter.notifyDataSetChanged();
            return;
        }
        ActiveLogin activeLogin = null;
        for (int i = 0; i < aal.size(); i++) {
            if (aal.get(i).ActiveNetwork.equals(networkName)) {
                activeLogin = aal.get(i);
            }
        }
        if (activeLogin == null) {
            userAdapter.notifyDataSetChanged();
            return;
        }

        SignatureContainer c = UnapprovedNetworkUserTable.getUnapprovedNetworkUserTable(getApplicationContext(), networkName, activeLogin.username);
        if (c == null) {
            userAdapter.notifyDataSetChanged();
            return;
        }

        NetworkUserDetail nude = (NetworkUserDetail) ObjectConverter.StringToObject(c.object);

        ArrayList<User> unapprovedUser = nude.userlist;

        for (int j = 0; j < unapprovedUser.size(); j++) {
            if(unapprovedUser.get(j).username.contains(query))
                userList.add(new UserStatus(unapprovedUser.get(j).username, unapprovedUser.get(j).role, "Pending", activeLogin.username));
        }

        userAdapter.notifyDataSetChanged();
    }
    public void createUser(View v){

        final InputDialog dialogUsername = new InputDialog(ManageUserActivity.this, "Create User", "Input username, use email format eg mail@mail.mail :");
        dialogUsername.setOnAcceptButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String networkName = spinnerNetworkList.getSelectedItem().toString();
                final String username = dialogUsername.getEditText().getText().toString();
                ArrayList<DeletedUser> deletedUserList = DeletedUserList.getDeletedUserList(getApplicationContext());
                boolean userAlreadyExist = false;
                for(DeletedUser du: deletedUserList)
                    if (du.networkName.equals(networkName) && du.username.equals(username))
                        userAlreadyExist = true;
                Pattern emailpattern = Pattern
                        .compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
                if(!emailpattern.matcher(username).matches()){
                    Dialog err = new Dialog(ManageUserActivity.this, "Username Not Valid", "Acceptable username format is XX@XXX.X");
                    err.show();
                    err.getButtonAccept().setText("OK");
                    err.getButtonCancel().setVisibility(View.INVISIBLE);
                }else if(userAlreadyExist){
                    Dialog err = new Dialog(ManageUserActivity.this, "Username Already Used", "Username already deleted, username cannot be used");
                    err.show();
                    err.getButtonAccept().setText("OK");
                    err.getButtonCancel().setVisibility(View.INVISIBLE);
                } else{
                    final InputDialog dialogPassword = new InputDialog(ManageUserActivity.this, "Create User", "Input password");
                    dialogPassword.setOnAcceptButtonClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String password = dialogPassword.getEditText().getText().toString();
                            if(password==null||password.equals("")){
                                Dialog err = new Dialog(ManageUserActivity.this, "Password Not Valid", "Password cannot be empty");
                                err.show();
                                err.getButtonAccept().setText("OK");
                                err.getButtonCancel().setVisibility(View.INVISIBLE);
                            }else{
                                final String [] items = new String[]{"clientadmin", "user"};
                                new AlertDialog.Builder(ManageUserActivity.this)
                                    .setTitle("Select Role (permanent)")
                                    .setSingleChoiceItems(items, 0, null)
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();
                                            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                                            createUser(networkName, username, password, items[selectedPosition]);
                                        }
                                    })
                                    .show();
                            }
                        }
                    });
                    dialogPassword.show();
                }
            }
        });
        dialogUsername.show();
        dialogUsername.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    }
    void createUser(String networkName, String username, String password, String role){
        Log.d("ManageUserActivity", String.format("Create User %s %s %s %s",networkName, username, password, role));
        ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
        if (aal == null) {
            aal = new ArrayList();
        }
        ActiveLogin ul = null;
        for (int i = 0; i < aal.size(); i++) {
            ActiveLogin temp = aal.get(i);
            if (temp.ActiveNetwork.equals(networkName)) {
                ul = aal.get(i);
            }
        }
        if(ul==null|| ul.role.equals("client")){
            Toast.makeText(this, "No Privilege to administer network"+networkName, Toast.LENGTH_SHORT).show();
        }
        else{
            KeyPair self = EncryptionUtility.generateAsymmetricKey();

            //Create the user
            User u = new User();
            u.username = username;
            u.role = role;
            u.publicKey = DatatypeConverter.printBase64Binary(self.getPublic().getEncoded());
            try {
                u.privateKeyHash = EncryptionUtility.hash(self.getPrivate().getEncoded());
            }catch(NoSuchAlgorithmException e){
                Log.e("ManageUserActivity", "NoSuchAlgorithmException");
                return;
            }
            u.privateKey = DatatypeConverter.printBase64Binary(EncryptionUtility.encryptSymetric(self.getPrivate().getEncoded(), EncryptionUtility.createKeyFromPassword(password, u.privateKeyHash).getEncoded()));

            NetworkUserDetail obj = new NetworkUserDetail();
            ArrayList<SignatureContainer> scao = NetworkUserTable.getNetworkUserTable(getApplicationContext(), networkName, ul.username);
            if (scao == null) {
                Log.e("ManageUserActivity", "scao null");
            }else{
                if (scao.size() > 0) {
                    SignatureContainer myNT = scao.get(0);
                    if (myNT == null) {
                        return;
                    }
                    NetworkUserDetail myNud = (NetworkUserDetail) ObjectConverter.StringToObject(myNT.object);

                    ArrayList<User> ual = myNud.userlist;

                    ual.add(u);
                    obj.userlist = ual;
                    obj.VersionNumber = myNud.VersionNumber + 1;
                    obj.networkName = myNud.networkName;

                    //put into new Signature Container
                    SignatureContainer c = new SignatureContainer();
                    c.Type = "usertable";
                    c.Username = ul.username;
                    c.object = ObjectConverter.ObjectToString(obj);
                    c.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(DatatypeConverter.parseBase64Binary(c.object), ul.privKey));

                    NetworkUserTable.replaceNetworkUserTable(getApplicationContext(), c);
                } else {
                    ArrayList<User> userlist = new ArrayList();
                    userlist.add(u);
                    SignatureContainer sc = NetworkUserTable.createNetworkUserTable(networkName, ul.username, 1, ul.privKey, userlist);
                    NetworkUserTable.replaceNetworkUserTable(getApplicationContext(), sc);
                }
                Dialog err = new Dialog(ManageUserActivity.this, "Success", "User created");
                err.show();
                err.getButtonAccept().setText("OK");
                err.getButtonCancel().setVisibility(View.INVISIBLE);
                refreshUserList("");
            }
        }
    }
    public void activateUser(String networkName, final String username){
        ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
        if (aal == null) {
            Toast.makeText(this, "No Privilege for administer network"+networkName, Toast.LENGTH_SHORT).show();
            return;
        }
        ActiveLogin activeLogin = null;
        for (int i = 0; i < aal.size(); i++) {
            if (aal.get(i).ActiveNetwork.equals(networkName)) {
                activeLogin = aal.get(i);
            }
        }

        if (activeLogin == null) {
            Toast.makeText(this, "No Privilege to administer network"+networkName, Toast.LENGTH_SHORT).show();
            return;
        }
        final ActiveLogin u = activeLogin;
        final SignatureContainer c = UnapprovedNetworkUserTable.getUnapprovedNetworkUserTable(getApplicationContext(),networkName, u.username);
        final NetworkUserDetail nude = (NetworkUserDetail) ObjectConverter.StringToObject(c.object);
        final ArrayList<User> uulist = nude.userlist;
        User targetUser = null;
        boolean changed = false;
        for (int i = 0; i < uulist.size(); i++) {
            if (uulist.get(i).username.contentEquals(username)) {
                targetUser = uulist.get(i);
                uulist.remove(i);
                changed = true;
                break;
            }
        }
        if (changed) {
            final User toBeApproved = targetUser;
            final String [] items = new String[]{"clientadmin", "user"};
            new AlertDialog.Builder(ManageUserActivity.this)
                    .setTitle("Select Role (permanent)")
                    .setSingleChoiceItems(items, 0, null)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            String networkName = spinnerNetworkList.getSelectedItem().toString();
                            String role = items[selectedPosition];
                            //Change Unapproved Table
                            nude.userlist = uulist;
                            nude.VersionNumber += 1;
                            c.object = ObjectConverter.ObjectToString(nude);
                            c.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(DatatypeConverter.parseBase64Binary(c.object), u.privKey));

                            UnapprovedNetworkUserTable.replaceUnapprovedNetworkUserTable(getApplicationContext(), c);

                            //Change User Table
                            NetworkUserDetail obj = new NetworkUserDetail();
                            ArrayList<SignatureContainer> scao = NetworkUserTable.getNetworkUserTable(getApplicationContext(), u.ActiveNetwork, u.username);
                            if (scao == null) {
                                return;
                            }
                            SignatureContainer myNT;
                            if (scao.isEmpty()) {
                                myNT = NetworkUserTable.createNetworkUserTable(networkName, u.username, 1, u.privKey, new ArrayList<User>());
                            } else {
                                myNT = scao.get(0);
                            }

                            if (myNT == null) {
                                return;
                            }
                            NetworkUserDetail myNud = (NetworkUserDetail) ObjectConverter.StringToObject(myNT.object);

                            ArrayList<User> ual = myNud.userlist;
                            toBeApproved.role = role;
                            ual.add(toBeApproved);
                            obj.userlist = ual;
                            obj.VersionNumber = myNud.VersionNumber + 1;
                            obj.networkName = myNud.networkName;

                            //put into new Signature Container
                            SignatureContainer cc = new SignatureContainer();
                            cc.Type = "usertable";
                            cc.Username = u.username;
                            cc.object = ObjectConverter.ObjectToString(obj);
                            cc.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(DatatypeConverter.parseBase64Binary(cc.object), u.privKey));
                            NetworkUserTable.replaceNetworkUserTable(getApplicationContext(), cc);

                            DistributedFileSharingProtocol.getInstance().requestReplaceNetworkUserTable(u.ActiveNetwork, u.username, u.privKey);
                            Dialog err = new Dialog(ManageUserActivity.this, "Success", "User " + username + " activated");
                            err.show();
                            err.getButtonAccept().setText("OK");
                            err.getButtonCancel().setVisibility(View.INVISIBLE);
                            refreshUserList("");
                        }
                    })
                    .show();

        }else{
            Toast.makeText(this, "User already activated", Toast.LENGTH_SHORT).show();
        }
    }
    void deleteUser(String networkName, UserStatus user){
        ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
        if (aal == null) {
            aal = new ArrayList();
        }
        ActiveLogin u = null;
        for (int i = 0; i < aal.size(); i++) {
            ActiveLogin temp = aal.get(i);
            if (temp.ActiveNetwork.equals(networkName)) {
                u = aal.get(i);
            }
        }

        if (u == null || u.role.equals("client")) {
            Toast.makeText(this, "No Privilege to administer network"+networkName, Toast.LENGTH_SHORT).show();
        }else if(user.getRole().equals("admin")){
            Toast.makeText(this, "No Privilege to delete user "+user.getUsername(), Toast.LENGTH_SHORT).show();
        }else if(user.getRole().equals("creator") && !u.role.equals("admin")){
            Toast.makeText(this, "No Privilege to delete user "+user.getUsername(), Toast.LENGTH_SHORT).show();
        }else{
            DeletedUserList.addDeletedUser(getApplicationContext(), networkName, user.getUsername());
            DeletedUser usr = new DeletedUser();
            usr.networkName = networkName;
            usr.username = user.getUsername();
            DistributedFileSharingProtocol.getInstance().requestDeleteUser(usr, u.username, u.privKey, networkName);
            Dialog err = new Dialog(ManageUserActivity.this, "Success", "User " + user.getUsername() + " deleted");
            err.show();
            err.getButtonAccept().setText("OK");
            err.getButtonCancel().setVisibility(View.INVISIBLE);
            refreshUserList("");
        }
    }
    void deletePendingUser(String networkName, UserStatus user){
        ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
        if (aal == null) aal = new ArrayList<>();
        ActiveLogin u = null;
        for (int i = 0; i < aal.size(); i++) {
            if (aal.get(i).ActiveNetwork.equals(networkName)) {
                u = aal.get(i);
            }
        }

        if (u == null) {
            Toast.makeText(this, "No Privilege to administer network"+networkName, Toast.LENGTH_SHORT).show();
        }else{
            SignatureContainer c = UnapprovedNetworkUserTable.getUnapprovedNetworkUserTable(getApplicationContext(), networkName, u.username);
            NetworkUserDetail nude = (NetworkUserDetail) ObjectConverter.StringToObject(c.object);
            ArrayList<User> uulist;
            uulist = nude.userlist;
            boolean changed = false;

            for (int i = 0; i < uulist.size(); i++) {
                if (uulist.get(i).username.contentEquals(user.getUsername())) {
                    uulist.remove(i);
                    changed = true;
                    break;
                }
            }
            if (changed) {
                nude.userlist = uulist;
                nude.VersionNumber += 1;
                c.object = ObjectConverter.ObjectToString(nude);
                c.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(DatatypeConverter.parseBase64Binary(c.object), u.privKey));
                UnapprovedNetworkUserTable.replaceUnapprovedNetworkUserTable(getApplicationContext(), c);
                Dialog err = new Dialog(ManageUserActivity.this, "Success", "User "+user.getUsername()+" deleted");
                err.show();
                err.getButtonAccept().setText("OK");
                err.getButtonCancel().setVisibility(View.INVISIBLE);
                refreshUserList("");
            } else {
                Dialog err = new Dialog(ManageUserActivity.this, "Success", "Failed to delete user "+user.getUsername());
                err.show();
                err.getButtonAccept().setText("OK");
                err.getButtonCancel().setVisibility(View.INVISIBLE);
            }
        }
    }
}
