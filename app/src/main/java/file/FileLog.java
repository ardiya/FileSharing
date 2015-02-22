package file;

import java.io.Serializable;

public class FileLog implements Serializable {

    String Filename;
    String Hash;
    String usernameFrom;
    String usernameTo;
    String LogDate;

    public FileLog() {
        this("", "", "", "", "");
    }

    /**
     *
     * Create New FileLog , Assign Variable
     *
     * @param Filename the log name
     * @param Hash the file hash
     * @param usernameFrom
     * @param usernameTo
     * @param LogDate date
     */
    public FileLog(String Filename, String Hash, String usernameFrom, String usernameTo, String LogDate) {
        this.Filename = Filename;
        this.Hash = Hash;
        this.usernameFrom = usernameFrom;
        this.usernameTo = usernameFrom;
        this.LogDate = LogDate;
    }

    /**
     *
     * Get the filename properties
     *
     * @return filename
     */
    public String getFilename() {
        return Filename;
    }

    /**
     *
     * set the filename properties
     *
     * @param Filename set filename
     */
    public void setFilename(String Filename) {
        this.Filename = Filename;
    }

    /**
     *
     * Get the file hash properties
     *
     * @return the file hash
     */
    public String getHash() {
        return Hash;
    }

    /**
     *
     * set the file hash properties
     *
     * @param Hash file hash
     */
    public void setHash(String Hash) {
        this.Hash = Hash;
    }

    /**
     *
     * Get the username properties
     *
     * @return username
     */
    public String getUsernameFrom() {
        return usernameFrom;
    }

    /**
     *
     * Get the username properties
     *
     * @return username
     */
    public String getUsernameTo() {
        return usernameTo;
    }

    /**
     *
     * set the username From properties
     *
     * @param usernameFrom
     */
    public void setUsernameFrom(String usernameFrom) {
        this.usernameFrom = usernameFrom;
    }

    /**
     *
     * set the username To properties
     *
     * @param usernameTo
     */
    public void setUsernameTo(String usernameTo) {
        this.usernameTo = usernameTo;
    }

    /**
     *
     * get the log date properties
     *
     * @return when the log created
     */
    public String getLogDate() {
        return LogDate;
    }

    /**
     *
     * set the log date properties
     *
     * @param LogDate the date the log created
     */
    public void setLogDate(String LogDate) {
        this.LogDate = LogDate;
    }

}