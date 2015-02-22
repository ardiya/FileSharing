package com.ay.filesharing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.ListIterator;

import util.StateManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonRectangle;

import butterknife.ButterKnife;
import butterknife.InjectView;
import file.ActiveLogin;

public class PreferencesActivity extends ActionBarActivity {

	@InjectView(R.id.etDirectory)
	EditText editTextDirectory;
	@InjectView(R.id.img)
	ImageView imageViewProfilePicture;
    @InjectView(R.id.btBrowseImage)
    ButtonRectangle buttonImage;
    @InjectView(R.id.btBrowseDirectory)
    ButtonRectangle buttonDirectory;

	public static final String TAG_ACTIVE_LOGIN="login";
	ActiveLogin activeLogin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preferences);
		setTitle("Set Preferences");
        ActionBar supportActionBar = getSupportActionBar();

        Bundle b = getIntent().getExtras();
        ButterKnife.inject(this);
		try{
			activeLogin = (ActiveLogin) b.get(TAG_ACTIVE_LOGIN);

            if(activeLogin.homeDirectory!=null) {
                editTextDirectory.setText(activeLogin.homeDirectory);
                buttonDirectory.setText("Change");
                supportActionBar.setDisplayHomeAsUpEnabled(true);
                supportActionBar.setHomeButtonEnabled(true);
            }
            try{
                String filename = activeLogin.username + "_" + activeLogin.ActiveNetwork + ".uimg";
                FileInputStream fis = getApplicationContext().openFileInput(filename);
                Bitmap bitmap = BitmapFactory.decodeStream(fis);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                imageViewProfilePicture.setImageBitmap(bitmap);
                buttonImage.setText("Change");
            }catch(Exception e){
                e.printStackTrace();
            }
		}catch(Exception e){
            e.printStackTrace();
        }
	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if(activeLogin.homeDirectory!=null)
            super.onBackPressed();
    }

    public void chooseDirectory(View v) {
		Intent intent = new Intent(this, FolderChooserActivity.class);
		startActivityForResult(intent, 101);
	}

	public void chooseImage(View v) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		try {
			startActivityForResult(
					Intent.createChooser(intent, "Select Profile Picture"),
					102);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(this, "Please install a File Manager.",
					Toast.LENGTH_SHORT).show();
		}
	}

	public void save(View v) {
		String dir = editTextDirectory.getText().toString();
		File x = new File(dir);
		if (!x.exists()) {
			Toast.makeText(this, "Directory do not exists", Toast.LENGTH_SHORT).show();
		}else if(!x.isDirectory()){
			Toast.makeText(this, "Directory do not exists", Toast.LENGTH_SHORT).show();
		}else {
			try{
				File tempFile = File.createTempFile("cache", "tmp", x);
				tempFile.delete();
			}catch(Exception e){
				Toast.makeText(this, "KITKAT/Not enough access problem: Please choose accessible folder", Toast.LENGTH_SHORT).show();
				return;
			}
			activeLogin.homeDirectory = dir;
			ArrayList<ActiveLogin> alal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
			for(ListIterator<ActiveLogin> i = alal.listIterator(); i.hasNext();){
				ActiveLogin temp = i.next();
				if(temp.ActiveNetwork.equals(activeLogin.ActiveNetwork) && temp.username.equals(activeLogin.username))
					i.set(activeLogin);
			}
			StateManager.setItem("ActiveLogin", alal);
            StateManager.setItem("currentActiveLogin", activeLogin);

            setResult(ActionBarActivity.RESULT_OK);
            Toast.makeText(getApplicationContext(), "Preferences Saved", Toast.LENGTH_SHORT).show();
            SharedPreferences setting = getSharedPreferences("filesharing", 0);
            SharedPreferences.Editor edit = setting.edit();
            edit.putString(activeLogin.username+"@"+activeLogin.ActiveNetwork, dir);
            edit.commit();
            finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 101:
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				String path = (String) extras
						.get(FolderChooserActivity.TAG_PATH);
				if (path != null) {
					editTextDirectory.setText(path);
				}
			}
			break;
		case 102:
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				if (uri != null) {
					Toast.makeText(this, uri.toString(), Toast.LENGTH_SHORT)
							.show();
					Log.d("URI", uri.toString());
                    imageViewProfilePicture.setImageURI(uri);
                    saveProfilePicture();
				} else
					Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	void saveProfilePicture() {
		imageViewProfilePicture.setDrawingCacheEnabled(true);
		Bitmap b = imageViewProfilePicture.getDrawingCache();
		String destinationFilename = activeLogin.username + "_" + activeLogin.ActiveNetwork + ".uimg";
		try {
			FileOutputStream openFileOutput = getApplicationContext().openFileOutput(destinationFilename, Context.MODE_PRIVATE);
			b.compress(CompressFormat.PNG, 100, openFileOutput);
			openFileOutput.close(); 
		} catch (FileNotFoundException e1) {
			Toast.makeText(this, e1.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			e1.printStackTrace();
		} catch (Exception e) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

}
