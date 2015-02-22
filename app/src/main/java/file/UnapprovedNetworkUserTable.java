package file;

import android.content.Context;
import android.util.Log;

import com.google.common.io.Files;

import cryptography.EncryptionUtility;
import util.DatatypeConverter;
import util.ObjectConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *Unapproved Network user table class
 *
 */
public class UnapprovedNetworkUserTable {

    /**
     * Get network user table from file
     *
     * @param networkName network name
     * @param username username , if set to null return all Network user table
     * for the given network , otherwise only return Network user table from
     * specified username
     * @param version this unapproved table version
     * @param privateKey private key
     * @param userlist userlist
     * @return signature container for unapproved network user table
     */
    public static SignatureContainer createUnapprovedNetworkUserTable(String networkName, String username, long version, byte[] privateKey, ArrayList<User> userlist) {
        SignatureContainer c = new SignatureContainer();
        NetworkUserDetail obj = new NetworkUserDetail();
        obj.VersionNumber = version;
        obj.networkName = networkName;
        obj.userlist = userlist;

        c.Type = "usertable";
        c.Username = username;
        c.object = ObjectConverter.ObjectToString(obj);

        c.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(ObjectConverter.ObjectToByte(c.object), privateKey));
        return c;
    }

    /**
     * Replace the unapproved network user list
     *
     * @param c SignatureContainer
     */
    public static void replaceUnapprovedNetworkUserTable(Context ct, SignatureContainer c) {
        try {
            NetworkUserDetail d = (NetworkUserDetail) ObjectConverter.StringToObject(c.object);
            String networkName = d.networkName;

            SignatureContainer sc = UnapprovedNetworkUserTable.getUnapprovedNetworkUserTable(ct, d.networkName, c.Username);
            if (sc != null) {
                NetworkUserDetail e = (NetworkUserDetail) ObjectConverter.StringToObject(sc.object);
                System.out.println(d.VersionNumber);
                if (e.VersionNumber >= d.VersionNumber) {
                    return;
                }
            }
            //Files.write(Paths.get(networkName + "-" + c.Username + ".uundat"), ObjectConverter.ObjectToByte(c));
            String filename = networkName + "-" + c.Username + ".uundat";
            FileOutputStream fs = ct.openFileOutput(filename, Context.MODE_PRIVATE);
            fs.write(ObjectConverter.ObjectToByte(c));
            fs.flush();
            fs.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Replace the unapproved network user list
     *
     * @param networkName network name 
     * @param username administrator username
     * @return  signature container for user  the user table
     */
    public static SignatureContainer getUnapprovedNetworkUserTable(Context ct, String networkName, String username) {
        byte[] data;
        SignatureContainer c = null;
        try {
            File f = new File(ct.getFilesDir().getAbsolutePath() + "/" + networkName + "-" + username + ".uundat");
            data = Files.toByteArray(f);
            c = (SignatureContainer) ObjectConverter.byteToObject(data);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return c;
    }
}
