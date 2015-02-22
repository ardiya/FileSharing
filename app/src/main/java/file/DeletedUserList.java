package file;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class DeletedUserList {
    /**
     * Add Deleted User
     *
     * @param network network name that want to be added to deleted network list
     * @param username
     */
    public static void addDeletedUser(Context ct, String network , String username) {
        
        ArrayList<DeletedUser> delUserList = getDeletedUserList(ct);
        if (delUserList == null) {
            delUserList = new ArrayList();
        }

        for (int i = 0; i < delUserList.size(); i++) {
            if (delUserList.get(i).username.contentEquals(username) && delUserList.get(i).networkName.contentEquals(network)) {
                Log.d("DeletedUserList", "failed to delete, user already deleted");
                return;
            }
        }
        
        DeletedUser d = new DeletedUser();
        d.networkName = network;
        d.username = username;
        
        delUserList.add(d);
        writeDeletedUserFile(ct, delUserList);
    }


    /**
     * Write Deleted Network List to file.
     *
     * @param delUserList array with deleted network list
     * @return true or false
     */
    private static boolean writeDeletedUserFile(Context context, ArrayList<DeletedUser> delUserList) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput("deletedUser.dat", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(delUserList);
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
    public static ArrayList<DeletedUser> getDeletedUserList(Context context) {
        try {
            String filename = context.getFilesDir().getAbsolutePath() + "/deletedUser.dat";
            FileInputStream fis;
            ObjectInputStream in;

            fis = new FileInputStream(filename);
            in = new ObjectInputStream(fis);
            ArrayList<DeletedUser> delUserList = (ArrayList<DeletedUser>) in.readObject();
            in.close();
            return delUserList;
        } catch (IOException | ClassNotFoundException ex) {
            return new ArrayList<>();
        }
    }

}
