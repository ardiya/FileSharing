package file;
import java.io.Serializable;

public class User
  implements Serializable
{
  public String username;
  public String role;
  public String privateKey;
  public String privateKeyHash;
  public String publicKey;
}
