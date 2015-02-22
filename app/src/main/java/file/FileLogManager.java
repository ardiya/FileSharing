package file;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class FileLogManager implements Serializable {

    Context context;
    public FileLogManager(Context context){
        this.context = context;
    }
    /**
     *
     * Add new File Log
     *
     * @param networkName network name
     * @param filo File Log
     * @param username username that add the log
     */
    public void addFileLog(String networkName, FileLog filo, String username) {
        ArrayList<FileLog> fiLoList = getFileLogList(networkName,username);
        if (fiLoList == null) {
            fiLoList = new ArrayList();
        }
        fiLoList.add(filo);
        writeFileLogs(networkName, fiLoList, username);
    }

    /**
     *
     * write File Log
     *
     * @author Wilyanto Lim
     * @param networkName the network name
     * @param  filolist the log list
     * @param username the username
     * @return true or false , read succeed or failed
     */
    public boolean writeFileLogs(String networkName, ArrayList<FileLog> filolist,String username) {
        FileOutputStream fos = null;
        try {
            String filename;
            if(username == null)
                filename = networkName + "_log.dat";
            else 
                filename = username + "_"+  networkName+ "_log.dat";
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(filolist);
            return true;
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
     *
     * Get File Log list
     *
     * @author Wilyanto Lim
     * @param networkName the network name
     * @param username the username
     * @return  arraylist of log
     */
    public ArrayList<FileLog> getFileLogList(String networkName,String username) {
        try {
            String filename;
            if(username == null)
                filename = networkName + "_log.dat";
            else 
                filename = username +"_"+networkName+"_log.dat";
            
            FileInputStream filins;
            ObjectInputStream obins;
            
            filins = context.openFileInput(filename);
            obins = new ObjectInputStream(filins);
            ArrayList<FileLog> fiLoList = (ArrayList<FileLog>) obins.readObject();
            obins.close();
            return fiLoList;
        } catch (IOException | ClassNotFoundException ex) {
            return null;
        }
    }
    
     /**
     *
     * Get File Log list from File
     *
     * @param filename the file name
     * @return  the array list of log
     */
    public ArrayList<FileLog> getFileLogListFromFile(String filename) {
        try {
            FileInputStream filins;
            ObjectInputStream obins;

            filins = new FileInputStream(filename);
            obins = new ObjectInputStream(filins);
            ArrayList<FileLog> fiLoList = (ArrayList<FileLog>) obins.readObject();
            obins.close();
            return fiLoList;
        } catch (IOException | ClassNotFoundException ex) {
            return null;
        }
    }
    
}
