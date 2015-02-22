package file;
import java.io.Serializable;
import java.util.ArrayList;

public class NetworkUserDetail
  implements Serializable
{
  public String networkName;
  public long VersionNumber;
  public ArrayList<User> userlist;
}

