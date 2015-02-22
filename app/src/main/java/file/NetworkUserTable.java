package file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import util.ObjectConverter;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Base64;
import android.util.Log;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import cryptography.EncryptionUtility;

/**
 * @author Ardiya
 *
 */
public class NetworkUserTable {

	/**
	 * Create new network user table
	 * 
	 * @param networkName
	 *            the new user table
	 * @param username
	 *            network administrator username
	 * @param version
	 *            network table version
	 * @param privateKey
	 *            network administrator private key
	 * @param userlist
	 *            the active user list
	 * @return signature container
	 */
	public static SignatureContainer createNetworkUserTable(String networkName,
			String username, long version, byte[] privateKey,
			ArrayList<User> userlist) {
		SignatureContainer c = new SignatureContainer();
		NetworkUserDetail obj = new NetworkUserDetail();
		obj.VersionNumber = version;
		obj.networkName = networkName;
		obj.userlist = userlist;

		c.Type = "usertable";
		c.Username = username;
		c.object = ObjectConverter.ObjectToString(obj);

		c.signature = Base64.encodeToString(EncryptionUtility
				.generateSignature(Base64.decode(c.object, 0), privateKey), 0);

		return c;
	}

	/**
	 * Create new network user table
	 * 
	 * @param c
	 *            signature Container we want to replace with
	 */
	public static void replaceNetworkUserTable(Context ct, SignatureContainer c) {
		try {
			NetworkUserDetail d = (NetworkUserDetail) ObjectConverter
					.StringToObject(c.object);
			String networkName = d.networkName;
			ArrayList<SignatureContainer> sco = NetworkUserTable
					.getNetworkUserTable(ct, d.networkName, c.Username);
			if (sco == null) {
				return;
			}

			if (sco.size() > 0) {
				SignatureContainer sc = sco.get(0);
				NetworkUserDetail e = (NetworkUserDetail) ObjectConverter
						.StringToObject(sc.object);
				if (e.VersionNumber >= d.VersionNumber) {
					return;
				}
			}
			String filename = networkName+"_"+c.Username+".undat";
			FileOutputStream fs = ct.openFileOutput(filename, Context.MODE_PRIVATE);
			fs.write(ObjectConverter.ObjectToByte(c));
			fs.flush();
			fs.close();
			Log.d("NetworkUserTable", "wrote to "+ct.getFileStreamPath(filename));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Get network user table from file
	 * 
	 * @param networkName
	 *            network name
	 * @param username
	 *            username , if set to null return all Network user table for
	 *            the given network , otherwise only return Network user table
	 *            from specified username
	 * @return signature container for network user table
	 */
	public static ArrayList<User> getUserList(Context context, String networkName,
			String username) {
		ArrayList<SignatureContainer> signatureContainer = getNetworkUserTable(
				context, networkName, username);
		DeletedUserList delUserList = new DeletedUserList();
		ArrayList<DeletedUser> delUser = delUserList.getDeletedUserList(context);
		ArrayList<User> aggregateUserList = new ArrayList<>();
		for (int i = 0; i < signatureContainer.size(); i++) {
			NetworkUserDetail nude = (NetworkUserDetail) ObjectConverter
					.StringToObject(signatureContainer.get(i).object);
			ArrayList<User> ulist = nude.userlist;
			for (ListIterator<User> j = ulist.listIterator(); j.hasNext();) {
				User u = j.next();
				for (int k = 0; k < delUser.size(); k++) {
					if (u.username.contentEquals(delUser.get(k).username)
							&& delUser.get(k).networkName.equals(networkName)) {
						j.remove();
						break;
					}
				}
				aggregateUserList.addAll(ulist);
			}
		}
		return aggregateUserList;
	}

	/**
	 * Get network user table from file
	 * 
	 * @param networkName
	 *            network name
	 * @param username
	 *            username , if set to null return all Network user table for
	 *            the given network , otherwise only return Network user table
	 *            from specified username
	 * @return signature container for network user table
	 */
	public static ArrayList<SignatureContainer> getNetworkUserTable(Context ct,
			String networkName, String username) {

		List<String> fileNames = new ArrayList<>();

		if (username == null) {
			try {
				File dir = ct.getFilesDir();
				File[] dirs = dir.listFiles();
				for (File path : dirs) {
					if (path.getName().toString()
							.matches(networkName + "_.*\\.undat")) {
						fileNames.add(path.getName().toString());
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			fileNames.add(networkName + "_" + username + ".undat");
		}

		ArrayList<SignatureContainer> asc = new ArrayList<SignatureContainer>();

		for (int i = 0; i < fileNames.size(); i++) {
			byte[] data;
			SignatureContainer c = null;
			try {
				
				File f = new File(ct.getFilesDir().getAbsolutePath() + "/" + fileNames.get(i));
				data = Files.toByteArray(f);
				c = (SignatureContainer) ObjectConverter.byteToObject(data);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
            if(username==null) Log.d("NetworkUserTable[fileNames.get(i)]", fileNames.get(i));
			if (c != null) {
				asc.add(c);
			}else{
                Log.e("NetworkUserTable", "failed to create Signature from file"+fileNames.get(i));
            }

		}
		return asc;
	}
}
