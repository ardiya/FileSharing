package util;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import file.ActiveLogin;
import file.FileLog;
import file.FileLogStatus;

public class SqliteUtil extends SQLiteOpenHelper {
	ConnectionSource connectionSource;
	Dao<FileLogStatus,String> fileLogStatusDao;
	Dao<ActiveLogin,String> activeLoginDao;
	public SqliteUtil(Context context){
		super(context, "filesharingdb", null, 1);
		connectionSource = new AndroidConnectionSource(this);
		try{
			TableUtils.createTable(connectionSource, FileLogStatus.class);
		}catch(Exception e){}
		try{
			TableUtils.createTable(connectionSource, ActiveLogin.class);
		}catch(Exception e){}
		try {
			fileLogStatusDao = BaseDaoImpl.createDao(connectionSource, FileLogStatus.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			activeLoginDao = BaseDaoImpl.createDao(connectionSource, ActiveLogin.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void insert(FileLogStatus fls) throws SQLException{
		fileLogStatusDao.create(fls);
	}
	public void insert(ActiveLogin al) throws SQLException{
		activeLoginDao.create(al);
	}
	public void update(FileLogStatus fls) throws SQLException{
		fileLogStatusDao.update(fls);
	}
	public void update(ActiveLogin o) throws SQLException{
		activeLoginDao.update(o);
	}
	public void delete(FileLogStatus fls) throws SQLException{
		fileLogStatusDao.delete(fls);
	}
	public void delete(ActiveLogin o) throws SQLException{
		activeLoginDao.delete(o);
	}
	public Dao query(Class c){
		if(FileLogStatus.class == c){
			return fileLogStatusDao;
		}
		if(ActiveLogin.class == c)
			return activeLoginDao;
		return null;
	}
	public void close(){
		try {
			connectionSource.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		try {
			TableUtils.dropTable(connectionSource, FileLogStatus.class, true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			TableUtils.dropTable(connectionSource, ActiveLogin.class, true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void clear(Class c){
		if(c==ActiveLogin.class){
			try {
				activeLoginDao.delete(activeLoginDao.deleteBuilder().prepare());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
