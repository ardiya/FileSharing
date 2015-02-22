package file;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class DeletedNetworkList implements Serializable {
	/**
	 * Add Deleted Network to list.
	 * 
	 * @param network
	 *            network name that want to be added to deleted network list
	 */
	public void addDeletedNetwork(Context ct, String network) {

		ArrayList<String> delNetworkList = getDeletedNetworkTable(ct);
		if (delNetworkList == null) {
			delNetworkList = new ArrayList();
		}

		for (int i = 0; i < delNetworkList.size(); i++) {
			if (delNetworkList.get(i).contentEquals(network)) {
				return;
			}
		}

		delNetworkList.add(network);
		writeDeletedNetworkFile(ct,delNetworkList);
	}
	/**
     * Write Deleted Network List to file.
     *
     * @param delNetworkList array with deleted network list
     * @return true or false
     */
    private boolean writeDeletedNetworkFile(Context ct, ArrayList<String> delNetworkList) {
        FileOutputStream fos = null;
        try {
            //fos = new FileOutputStream("deletedNetwork.dat");
            fos = ct.openFileOutput("deletedNetwork.dat", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(delNetworkList);

            return true;
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
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
     * Get Deleted Network List from file.
     *
     * @return Network list
     */
    public ArrayList<String> getDeletedNetworkTable(Context ct) {
        try {
            String filename = ct.getFilesDir().getAbsolutePath() +"/deletedNetwork.dat";
            FileInputStream fis;
            ObjectInputStream in;

            fis = new FileInputStream(filename);
            in = new ObjectInputStream(fis);
            ArrayList<String> delNetworkList = (ArrayList<String>) in.readObject();
            in.close();
            return delNetworkList;
        } catch (IOException | ClassNotFoundException ex) {
            return new ArrayList<>();
        }
    }
}