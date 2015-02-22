package com.ay.filesharing;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.widgets.Dialog;

import java.io.File;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import cryptography.EncryptionUtility;
import file.Network;
import file.NetworkList;
import file.NetworkUserDetail;
import file.NetworkUserTable;
import file.SignatureContainer;
import file.User;
import protocol.DistributedFileSharingProtocol;
import util.DatatypeConverter;
import util.ObjectConverter;


public class AdminCreatorActivity extends ActionBarActivity implements ActionBar.TabListener {

    SectionsPagerAdapter sectionsPagerAdapter;

    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_creator);


        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(sectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }



    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    return new CreateNetworkFragment();
                default:
                    return new AssignAdminFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Create Network";
                case 1:
                    return "Assign Admin";
            }
            return null;
        }
    }



    class CreateNetworkFragment extends Fragment implements View.OnClickListener {

        ButtonRectangle buttonCreate;
        EditText editTextUsername;
        EditText editTextPassword;
        EditText editTextNetwork;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_create_network, container, false);

            editTextNetwork = (EditText) rootView.findViewById(R.id.editText1);
            editTextUsername = (EditText) rootView.findViewById(R.id.editText2);
            editTextPassword = (EditText) rootView.findViewById(R.id.editText3);
            buttonCreate = (ButtonRectangle) rootView.findViewById(R.id.button1);
            buttonCreate.setOnClickListener(this);

            return rootView;
        }
        @Override
        public void onClick(View v) {
            String network = editTextNetwork.getText().toString();
            String username = editTextUsername.getText().toString();
            String password = editTextPassword.getText().toString();

            Log.d("CreateNetworkFragment", "Creating network " + network);

            //Generate Self Key
            KeyPair self = EncryptionUtility.generateAsymmetricKey();

            //Generate Network Key
            KeyPair net = EncryptionUtility.generateAsymmetricKey();

            //Put into Network Table
            NetworkList nList = new NetworkList();
            Network nt = new Network();
            nt.NetworkName = network;
            nt.Signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(network.getBytes(), net.getPrivate().getEncoded()));

            //Save Into File
            ArrayList<Network> nn = nList.getNetworkTable(getApplicationContext());

            if (!nn.contains(nt)) {
                nn.add(nt);
            }

            nList.addNetwork(getApplicationContext(), nn);
            User u = new User();
            u.username = username;
            u.role = "admin";
            u.publicKey = DatatypeConverter.printBase64Binary(self.getPublic().getEncoded());
            try {
                u.privateKeyHash = EncryptionUtility.hash(self.getPrivate().getEncoded());
            }catch(NoSuchAlgorithmException e){
                Toast.makeText(getApplicationContext(), "This phone does not support specified algorithm", Toast.LENGTH_SHORT).show();
                return;
            }
            u.privateKey = DatatypeConverter.printBase64Binary(EncryptionUtility.encryptSymetric(self.getPrivate().getEncoded(), EncryptionUtility.createKeyFromPassword(password,u.privateKeyHash).getEncoded()));

            ArrayList<User> userlist = new ArrayList();
            userlist.add(u);

            SignatureContainer sc = NetworkUserTable.createNetworkUserTable(network, username, 1, self.getPrivate().getEncoded(), userlist);
            NetworkUserTable.replaceNetworkUserTable(getApplicationContext(), sc);

            Dialog err = new Dialog(AdminCreatorActivity.this, "Success", "Network "+network+" created");
            err.show();
            err.getButtonAccept().setText("OK");
            err.getButtonCancel().setVisibility(View.INVISIBLE);
            editTextUsername.setText("");
            editTextPassword.setText("");
            editTextNetwork.setText("");
        }
    }
    class AssignAdminFragment extends Fragment implements View.OnClickListener {

        ButtonRectangle buttonCreate;
        EditText editTextUsername;
        EditText editTextPassword;
        Spinner spinnerNetworkLists;

        ArrayList<String> networkLists;
        ArrayAdapter<String> networkAdapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_assign_admin, container, false);
            spinnerNetworkLists = (Spinner) rootView.findViewById(R.id.spinner1);

            networkLists=new ArrayList<>();
            networkAdapter = new ArrayAdapter<>(getApplicationContext(),
                    R.layout.simple_spinner_item_grey, networkLists);

            networkAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            spinnerNetworkLists.setAdapter(networkAdapter);

            refreshNetwork();

            rootView.findViewById(R.id.imageButton1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refreshNetwork();
                }
            });

            editTextUsername = (EditText) rootView.findViewById(R.id.editText2);
            editTextPassword = (EditText) rootView.findViewById(R.id.editText3);
            buttonCreate = (ButtonRectangle) rootView.findViewById(R.id.button1);
            buttonCreate.setOnClickListener(this);
            return rootView;
        }

        public void refreshNetwork() {
            DistributedFileSharingProtocol.getInstance().requestNetworkName();
            //Load From File
            Log.d("Refresh Network List", "Loading From File");
            NetworkList n = new NetworkList();
            ArrayList<Network> networkFromFile = n
                    .getNetworkTable(getApplicationContext());
            if(networkFromFile!=null && !networkFromFile.isEmpty()){
                networkLists.clear();
                for(Network net:networkFromFile)
                    networkLists.add(net.NetworkName);
                networkAdapter.notifyDataSetChanged();
                Log.d("Refresh Network List", networkLists.size()+" Network From File");
            }else{
                Log.d("Refresh Network List", "No Data From File");
            }
        }

        @Override
        public void onClick(View v) {
            String network = spinnerNetworkLists.getSelectedItem().toString();
            String username = editTextUsername.getText().toString();
            String password = editTextPassword.getText().toString();
            Log.d("AssignAdminFragment", "Creating user " + username+" with password "+password);

            KeyPair self = EncryptionUtility.generateAsymmetricKey();

            //Create the user
            User u = new User();
            u.username = username;
            u.role = "admin";
            u.publicKey = DatatypeConverter.printBase64Binary(self.getPublic().getEncoded());
            try {
                u.privateKeyHash = EncryptionUtility.hash(self.getPrivate().getEncoded());
            }catch(NoSuchAlgorithmException e){
                Log.e("ManageUserActivity", "NoSuchAlgorithmException");
                return;
            }
            u.privateKey = DatatypeConverter.printBase64Binary(EncryptionUtility.encryptSymetric(self.getPrivate().getEncoded(), EncryptionUtility.createKeyFromPassword(password, u.privateKeyHash).getEncoded()));

            byte[] enprivKey = DatatypeConverter
                    .parseBase64Binary(u.privateKey);
            byte[] passwordKey = EncryptionUtility.createKeyFromPassword(
                    password, u.privateKeyHash).getEncoded();
            byte[] privKey = EncryptionUtility.decryptSymetric(
                    enprivKey, passwordKey);

            NetworkUserDetail obj = new NetworkUserDetail();
            ArrayList<SignatureContainer> scao = NetworkUserTable.getNetworkUserTable(getApplicationContext(), network, null);
            if (scao == null) {
                Log.e("ManageUserActivity", "scao null");
            }else{
                if (scao.size() > 0) {
                    SignatureContainer myNT = scao.get(0);
                    if (myNT == null) {
                        return;
                    }
                    for(File f:getApplicationContext().getFilesDir().listFiles())
                        Log.d("AdminCreatorActivity", f.getAbsolutePath());

                    NetworkUserDetail myNud = (NetworkUserDetail) ObjectConverter.StringToObject(myNT.object);

                    ArrayList<User> ual = myNud.userlist;

                    ual.add(u);
                    obj.userlist = ual;
                    obj.VersionNumber = myNud.VersionNumber + 1;
                    obj.networkName = myNud.networkName;

                    //put into new Signature Container
                    SignatureContainer c = new SignatureContainer();
                    c.Type = "usertable";
                    c.Username = username;
                    c.object = ObjectConverter.ObjectToString(obj);
                    c.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(DatatypeConverter.parseBase64Binary(c.object), privKey));

                    NetworkUserTable.replaceNetworkUserTable(getApplicationContext(), c);
                } else {
                    ArrayList<User> userlist = new ArrayList();
                    userlist.add(u);
                    SignatureContainer sc = NetworkUserTable.createNetworkUserTable(network, username, 1, privKey, userlist);
                    NetworkUserTable.replaceNetworkUserTable(getApplicationContext(), sc);
                }

            }
            Dialog err = new Dialog(AdminCreatorActivity.this, "Success", "User "+username+" created in network "+network);
            err.show();
            err.getButtonAccept().setText("OK");
            err.getButtonCancel().setVisibility(View.INVISIBLE);
            editTextUsername.setText("");
            editTextPassword.setText("");
        }
    }

}
