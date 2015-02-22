package file;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import it.sauronsoftware.ftp4j.FTPClient;
@DatabaseTable(tableName = "filelogstatus")
public class FileLogStatus {
	@DatabaseField(id = true)
	private Integer id;
	@DatabaseField
    private String filename;
	@DatabaseField
    private String to;
	@DatabaseField
    private String from;
	@DatabaseField
    private String networkName;
	@DatabaseField
    private String path;
	@DatabaseField
    private long size;
	@DatabaseField
    private Double progress;
	@DatabaseField
    private String status;
    @DatabaseField
    private String type;
    private FTPClient ftpclient;

	public FileLogStatus(){
		
	}
	
	public Integer getId() {
		return this.id;
	}

	public String getFileName() {
		return this.filename;
	}

	public String getFrom() {
		return this.from;
	}

	public String getTo() {
		return this.to;
	}

	public String getPath() {
		return this.path;
	}

	public String getNetworkName() {
		return this.networkName;
	}

    public String getType(){
        return this.type;
    }

    public Double getProgress(){
        return this.progress;
    }


	public String getSize() {
		return this.size + " bytes";
	}

	public String getStatus() {
		return this.status;
	}

    public void setId(Integer id) {
        this.id = id;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFtpclient(FTPClient ftpclient) {
        this.ftpclient = ftpclient;
    }
}