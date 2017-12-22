package com.thomas_dieuzeide.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * Created by Thomas_Dieuzeide on 12/25/2015.
 *
 * Class that wraps the most common database operations. This example assumes you want a single table and data entity
 * with two properties: a title and a priority as an integer. Modify in all relevant locations if you need other/more
 * properties for your data and/or additional tables.
 */

public class DatabaseHelper {
    private SQLiteOpenHelper _openHelper;

    /**
     * Construct a new database helper object
     * @param context The current context for the application or activity
     */
    public DatabaseHelper(Context context) {
        _openHelper = new SimpleSQLiteOpenHelper(context);
    }

    /**
     * This is an internal class that handles the creation of all database tables
     */
    class SimpleSQLiteOpenHelper extends SQLiteOpenHelper {
        SimpleSQLiteOpenHelper(Context context) {
            super(context, "main.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS status(user VARCHAR primary key,bon_number INTEGER,year DATETIME DEFAULT CURRENT_TIMESTAMP);");
            db.execSQL("CREATE TABLE IF NOT EXISTS bons(id VARCHAR primary key, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, code VARCHAR);");
            db.execSQL("CREATE TABLE IF NOT EXISTS sessions(id VARCHAR, num INTEGER, created_at DATETIME, end_at DATETIME,PRIMARY KEY(id,num));");
            db.execSQL("CREATE TABLE IF NOT EXISTS clients(code_client VARCHAR, name_client, id VARCHAR primary key, lieu VARCHAR, new BOOLEAN NOT NULL CHECK (new IN (0,1)),contact VARCHAR);");
            db.execSQL("CREATE TABLE IF NOT EXISTS works(id VARCHAR,demand VARCHAR, execute VARCHAR,observations VARCHAR, number INTEGER, devis VARCHAR, facture VARCHAR,session INTEGER,PRIMARY KEY(id,session));");
            db.execSQL("CREATE TABLE IF NOT EXISTS addworkers(id VARCHAR, name VARCHAR, start VARCHAR, finish VARCHAR, session INTEGER, quantity INTEGER, PRIMARY KEY(id,name));");
            db.execSQL("CREATE TABLE IF NOT EXISTS pieces(id VARCHAR, type VARCHAR, description VARCHAR, quantity INTEGER, session INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public String getStatus() {
        String usr = "";
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        Cursor c = db.rawQuery("select * from status",null);
        while (c.moveToNext()) {
            usr = usr + c.getString(0);
            usr = usr + "-"+ c.getString(2).substring(0,4);
            usr = usr + "-"+ c.getString(1);
        }
        c.close();
        db.close();
        return usr;
    }

    public int setUsername(String name,int n) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.execSQL("INSERT INTO status VALUES ('" + name + "'," + n + ",datetime());");
        db.close();
        return 1;
    }

    public String getUsername() {
        String usr = "";
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        Cursor c = db.rawQuery("select * from status",null);
        while (c.moveToNext()) {
            usr = usr + c.getString(0);
        }
        db.close();
        c.close();
        return usr;
    }
    public void updateUsername(String old_name,String name) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        ContentValues cv = new ContentValues();
        cv.put("user",name); //These Fields should be your String values of actual column names
        cv.put("bon_number",1);
        cv.put("year",dateFormat.format(date));
        db.update("status", cv, "user='" + old_name + "'", null);
        db.close();
    }

    public void updateStatus(String name,int n,String time) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("user",name); //These Fields should be your String values of actual column names
        cv.put("bon_number",n);
        cv.put("year",time);
        db.update("status", cv, "user='" + name + "'", null);
        db.close();
    }
    /**
     * Return a cursor object with all rows in the table.
     * @return A cursor suitable for use in a SimpleCursorAdapter
     */
    public Cursor getAllBon() {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }

        return db.rawQuery("select * from bons order by created_at", null);
    }

    /**
     * Return values for a single row with the specified id
     * @param id The unique id for the row o fetch
     * @return All column values are stored as properties in the ContentValues object
     */
    public ContentValues getBon(String id) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        ContentValues row = new ContentValues();
        Cursor cur = db.rawQuery("select * from bons where id = ?", new String[]{id});
        if (cur.moveToNext()) {
            row.put("id", cur.getString(0));
            row.put("created_at", cur.getString(1));
            row.put("code", cur.getString(2));
        }
        cur.close();
        db.close();
        return row;
    }

    /**
     * Add a new row to the database table
     * @param id The title value for the new row
     * @return The unique id of the newly added row
     */
    public int addBon(String id) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.execSQL("INSERT INTO bons VALUES ('" + id + "', datetime(), 'None')");
        db.close();
        return 1;
    }

    /**
     * Add a new row to the database table
     * @param id The title value for the new row
     * @return The unique id of the newly added row
     */
    public int addBonCode(String id, String code) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.execSQL("INSERT INTO bons VALUES ('" + id + "', datetime(), '"+code+"')");
        db.close();
        return 1;
    }

    /**
     * Delete the specified row from the database table. For simplicity reasons, nothing happens if
     * this operation fails.
     * @param id The unique id for the row to delete
     */
    public void deleteBon(String id) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        db.delete("bons", "id = '" + id + "'", null);
        db.close();
    }

    /**
     * Updates a row in the database table with new column values, without changing the unique id of the row.
     * For simplicity reasons, nothing happens if this operation fails.
     * @param id The unique id of the row to update
     */
    public void updateBon(String id) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        db.close();
    }

    //==========================================================================================

    /**
     * Return a cursor object with all rows in the table.
     * @return A cursor suitable for use in a SimpleCursorAdapter
     */
    public Cursor getAllClients() {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        return db.rawQuery("select * from clients order by id", null);
    }

    /**
     * Return values for a single row with the specified id
     * @param id The unique id for the row o fetch
     * @return All column values are stored as properties in the ContentValues object
     */
    public ContentValues getClient(String id) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        ContentValues row = new ContentValues();
        Cursor cur = db.rawQuery("select * from clients where id = ?", new String[]{id});
        if (cur.moveToNext()) {
            row.put("code_client", cur.getString(0));
            row.put("name_client",cur.getString(1));
            row.put("id", cur.getString(2));
            row.put("lieu", cur.getString(3));
            row.put("new",cur.getString(4));
            row.put("contact",cur.getString(5));
        }
        cur.close();
        db.close();
        return row;
    }

    /**
     * Add a new row to the database table
     * @param id The title value for the new row
     * @return The unique id of the newly added row
     */
    public int addClient(String code,String name,String id,String lieu,boolean n,String contact) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return 0;
        }
        if(n) {
            db.execSQL("INSERT INTO clients VALUES ('" + code + "','" + name + "','" + id + "','" + lieu + "',1,'"+contact+"'); ");
        } else {
            db.execSQL("INSERT INTO clients VALUES ('" + code + "','" + name + "','" + id + "','" + lieu + "',0,'"+contact+"'); ");
        }

        db.close();
        return 1;
    }


    /**
     * Updates a row in the database table with new column values, without changing the unique id of the row.
     * For simplicity reasons, nothing happens if this operation fails.
     * @param id The unique id of the row to update
     */
    public void updateClient(String code, String name,String id,String lieu,boolean n,String contact) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("code_client",code); //These Fields should be your String values of actual column names
        cv.put("name_client",name);
        cv.put("id",id);
        cv.put("lieu",lieu);
        if(n) {
            cv.put("new", 1);
        } else {
            cv.put("new",0);
        }
        cv.put("contact",contact);
        db.update("clients", cv, "id='" + id + "'", null);
        db.close();
    }

    //==========================================================================================

    /**
     * Return a cursor object with all rows in the table.
     * @return A cursor suitable for use in a SimpleCursorAdapter
     */
    public Cursor getAllWorks() {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        return db.rawQuery("select * from works order by id", null);
    }

    /**
     * Return values for a single row with the specified id
     * @param id The unique id for the row o fetch
     * @return All column values are stored as properties in the ContentValues object
     */
    public ContentValues getWork(String id,int session) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        ContentValues row = new ContentValues();
        Cursor cur = db.rawQuery("select * from works where id = ? and session=?", new String[]{id,String.valueOf(session)});

        if (cur.moveToNext()) {
            row.put("id", cur.getString(0));
            row.put("demand", cur.getString(1));
            row.put("execute", cur.getString(2));
            row.put("observations", cur.getString(3));
            row.put("number",cur.getString(4));
            row.put("devis",cur.getString(5));
            row.put("facture",cur.getString(6));
            row.put("session",cur.getString(7));
        }

        cur.close();
        db.close();
        return row;
    }

    /**
     * Add a new row to the database table
     * @param id The title value for the new row
     * @return The unique id of the newly added row
     */
    public int addWork(String id,String demand,String execute,String observations, int num,String devis,String facture,int session) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.execSQL("INSERT INTO works VALUES ('" + id + "','" + demand + "','" + execute + "','" + observations + "',"+num+",'" + devis + "','"+facture+"',"+session+");");
        db.close();
        return 1;
    }




    /**
     * Updates a row in the database table with new column values, without changing the unique id of the row.
     * For simplicity reasons, nothing happens if this operation fails.
     * @param id The unique id of the row to update
     */
    public void updateWork(String id,String demand,String execute,String observations,int num,String devis,String facture,int session) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        ContentValues cv = new ContentValues();

        cv.put("id",id);
        cv.put("demand",demand); //These Fields should be your String values of actual column names
        cv.put("execute",execute);
        cv.put("observations",observations); //These Fields should be your String values of actual column names
        cv.put("number",num);
        cv.put("devis",devis);
        cv.put("facture",facture);
        cv.put("session",session);
        db.update("works", cv, "id='" + id + "' and session ="+session, null);
        db.close();
    }

    //=====================================================================================
    public Cursor getWorker(String id,String name) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        return db.rawQuery("select * from addworkers where id=? and name=?", new String[]{id,name});
    }

    public int addWorker(String id,String name,String start,String finish, int session,int quantity) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.execSQL("INSERT INTO addworkers VALUES ('" + id + "','" + name + "','" + start + "','" + finish + "',"+session+","+quantity+");");
        db.close();
        return 1;
    }

    public void updateWorker(String id, String old_name, String new_name,String start,String finish,int session,int quantity) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        ContentValues cv = new ContentValues();

        cv.put("id",id);
        cv.put("name",new_name); //These Fields should be your String values of actual column names
        cv.put("start",start);
        cv.put("finish",finish); //These Fields should be your String values of actual column names
        cv.put("quantity",quantity);
        db.update("addworkers", cv, "id='" + id + "' and name='" + old_name + "' and session="+session , null);
        db.close();
    }

    public Cursor getAllWorkers(String id, int session) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        return db.rawQuery("select * from addworkers where id=? and session=?", new String[]{id,String.valueOf(session)});
    }

    //=====================================================================================
    public Cursor getPiece(String id,String type,String description) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        return db.rawQuery("select * from pieces where id=? and name=? and description=?", new String[]{id,type,description});
    }

    public int addPiece(String id,String type,String description,int quantity, int session) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.execSQL("INSERT INTO pieces VALUES ('" + id + "','" + type + "','" + description+ "',"+quantity+","+session+");");
        db.close();
        return 1;
    }

    public void updatePiece(String id, String old_type, String new_type,String old_description,String new_description,int old_quantity,int quantity,int session) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        ContentValues cv = new ContentValues();

        cv.put("id",id);
        cv.put("type",new_type); //These Fields should be your String values of actual column names
        cv.put("description",new_description);
        cv.put("quantity",quantity);
        cv.put("session",session);
        db.update("pieces", cv, "id='" + id + "' and type='" + old_type + "' and description='"+old_description+"' and quantity="+old_quantity+" and session="+session, null);
        db.close();
    }

    public Cursor getAllPieces(String id, int session) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        return db.rawQuery("select * from pieces where id=? and session=?", new String[]{id,String.valueOf(session)});
    }

    //=====================================================================================
    public Cursor getSession(String id,int number) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        return db.rawQuery("select * from sessions where id=? and num=?", new String[]{id,String.valueOf(number)});
    }

    public int addSession(String id,int num, String start) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return 0;
        }
        db.execSQL("INSERT INTO sessions VALUES ('" + id + "'," + num + ", '"+start+"',"+null+");");
        db.close();
        return 1;
    }

    public void updateSession(String id, int num, String start, String end) {
        SQLiteDatabase db = _openHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        ContentValues cv = new ContentValues();

        cv.put("id",id);
        cv.put("num",num); //These Fields should be your String values of actual column names
        cv.put("created_at",start);
        cv.put("end_at", end);
        String[] whereArgs = {id,String.valueOf(num)};
        db.update("sessions", cv, "id=? and num=?", whereArgs );
        db.close();
    }

    public Cursor getAllSessions(String id) {
        SQLiteDatabase db = _openHelper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        return db.rawQuery("select * from sessions where id=?", new String[]{id});
    }
}
