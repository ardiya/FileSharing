package file;
import java.io.Serializable;

/**
 * Container for public key signature
 */
public class SignatureContainer implements Serializable{
   public String Type;
   public String Username;
   public String signature;
   public String object;
}
