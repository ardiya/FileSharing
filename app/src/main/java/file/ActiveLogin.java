package file;

import java.io.Serializable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "activelogin")
public class ActiveLogin implements Serializable {
	@DatabaseField
	public String ActiveNetwork;
	@DatabaseField
	public String username;
	@DatabaseField
	public byte[] privKey;
	@DatabaseField
	public byte[] pubKey;
	@DatabaseField
	public String role;
	@DatabaseField
	public String homeDirectory;
}
