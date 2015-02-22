package file;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import protocol.DistributedFileSharingProtocol;
import util.ObjectConverter;
import util.StateManager;

public class NetworkList implements Serializable {
	
	/**
	 * Add network to list.
	 * @param context application context
	 * @param networkList
	 */
	public void addNetwork(Context context, ArrayList<Network> networkList) {
		writeNetworkTable(context, networkList);
	}

	/**
	 * Remove Network from network list.
	 * @param context application context
	 * @param NetworkName
	 * @return
	 */
	public String selectAndRemoveNetworkByName(Context context, String NetworkName) {
		ArrayList<ActiveLogin> alal = (ArrayList) StateManager
				.getItem("ActiveLogin");
		if (alal == null) {
			alal = new ArrayList();
		}
		ActiveLogin ul = null;
		for (int i = 0; i < alal.size(); i++) {
			ActiveLogin temp = (ActiveLogin) alal.get(i);
			if (temp.role.equals("admin")) {
				ul = (ActiveLogin) alal.get(i);
			}
		}
		if (ul == null) {
			return null;
		}
		ArrayList<Network> networkList = getNetworkTable(context);
		Network currNet = null;
		for (int i = 0; i < networkList.size(); i++) {
			Network u = (Network) networkList.get(i);
			if (u.NetworkName.equals(NetworkName)) {
				currNet = (Network) networkList.get(i);
				networkList.remove(i);
				break;
			}
		}
		writeNetworkTable(context, networkList);
		if (currNet != null) {
			DeletedNetworkList dnl = new DeletedNetworkList();
			dnl.addDeletedNetwork(context, NetworkName);

			DistributedFileSharingProtocol protocol = DistributedFileSharingProtocol
					.getInstance();
			protocol.requestDeleteNetwork(currNet, ul.username, ul.privKey,
					ul.ActiveNetwork);
		}
		return NetworkName;
	}

	/**
	 * Write Network list to file.
	 * @param ct
	 * @param networkList array with network list
	 * @return success or failed
	 */
	public boolean writeNetworkTable(Context ct, ArrayList<Network> networkList) {
		FileOutputStream fos = null;
		try {
			fos = ct.openFileOutput("network.dat", Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(networkList);
			Log.d("NetworkList", "Successfully write network.dat");
			return true;
		} catch (FileNotFoundException ex) {
			Log.e("NetworkList",
					"File Not Found in writing network.dat:" + ex.getMessage());
			return false;
		} catch (IOException ex) {
			Log.e("NetworkList",
					"IOException write network.dat:" + ex.getMessage());
			return false;
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException ex) {
				return false;
			}
		}
	}

	/**
	 * Write Network list to file.
	 * @param context application context
	 * @return list of network table
	 */
	public ArrayList<Network> getNetworkTable(Context context) {
		try {
			String filename = "network.dat";

			FileInputStream fis = context.openFileInput(filename);
			ObjectInputStream in = new ObjectInputStream(fis);
			ArrayList<Network> networkList = (ArrayList<Network>) in.readObject();
			in.close();

			return networkList;
		} catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return new ArrayList<Network>();
	}
}