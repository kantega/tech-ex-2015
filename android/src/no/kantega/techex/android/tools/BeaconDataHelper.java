package no.kantega.techex.android.tools;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by zsuhor on 27.02.2015.
 */
public class BeaconDataHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;

    private static final String DATABASE_NAME = "beacons";

    private static final String SQL_CREATE_TABLE = "CREATE TABLE last_data (major INTEGER NOT NULL, minor INTEGER NOT NULL, distance INTEGER, PRIMARY KEY (major,minor) );";

    /**
     * Save beacons distance
     */
    public static final String SQL_SAVE_DISTANCE = "INSERT OR REPLACE INTO last_data VALUES (?,?,?);";

    /**
     * Query last saved beacon distance
     */
    public static final String SQL_QUERY_DISTANCE = "SELECT distance FROM last_data WHERE major=? AND minor=?";


    BeaconDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}