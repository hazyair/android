package io.github.hazyair.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import java.lang.IllegalArgumentException;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;

import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Station;

public class HazyairProvider extends ContentProvider {
    public static final String AUTHORITY = "io.github.hazyair.provider";

    private static final int STATIONS_CONTENT_URI = 0;

    private static final int STATIONS_STATION_ID = 1;

    private static final int SENSORS_CONTENT_URI = 2;

    private static final int SENSORS_SENSOR_ID = 3;

    private static final int DATA_CONTENT_URI = 4;

    private static final int DATA_DATA_ID = 5;

    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(AUTHORITY, "stations", STATIONS_CONTENT_URI);
        MATCHER.addURI(AUTHORITY, "stations/#", STATIONS_STATION_ID);
        MATCHER.addURI(AUTHORITY, "sensors", SENSORS_CONTENT_URI);
        MATCHER.addURI(AUTHORITY, "sensors/#", SENSORS_SENSOR_ID);
        MATCHER.addURI(AUTHORITY, "data", DATA_CONTENT_URI);
        MATCHER.addURI(AUTHORITY, "data/#", DATA_DATA_ID);
    }

    public static class Stations {
        static final String DEFAULT_SORT = StationsContract.COLUMN_LOCALITY + " ASC";

        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/stations");

        public static boolean selected(Context context, Station station) {
            Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                    new String[]{StationsContract.COLUMN__ID}, StationsContract.selection,
                    StationsContract.selectionArgs(station), null);
            boolean result = false;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    station._id = cursor.getInt(0);
                    result = true;
                }
                cursor.close();
            }
            return result;
        }

        public synchronized static void bulkInsertAdd(io.github.hazyair.source.Station station,
                                                      ArrayList<ContentProviderOperation> cpo) {
            cpo.add(ContentProviderOperation.newInsert(Stations.CONTENT_URI)
                    .withValues(station.toContentValues()).build());
        }

        public static Cursor select(Context context, int _id) {
            return context.getContentResolver().query(CONTENT_URI,
                    Station.keys(), "_id=" + _id, null, null);
        }
    }

    public static class Sensors {
        static final String DEFAULT_SORT = StationsContract.COLUMN_ID + " ASC";

        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sensors");

        public synchronized static void bulkInsertAdd(int _station_id,
                                                      List<Sensor> sensors,
                                                      ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Sensor sensor : sensors) {
                cpo.add(ContentProviderOperation.newInsert(Sensors.CONTENT_URI)
                        .withValueBackReference(SensorsContract.COLUMN__STATION_ID, _station_id)
                        .withValues(sensor.toContentValues()).build());
            }
        }

        public static Cursor selectAll(Context context) {
            return context.getContentResolver().query(CONTENT_URI,
                    Sensor.keys(), null, null, null);
        }

        public static Cursor select(Context context, int _id) {
            return context.getContentResolver().query(CONTENT_URI,
                    Sensor.keys(), "_station_id="+_id, null, null);
        }
    }

    public static class Data {
        static final String DEFAULT_SORT = DataContract.COLUMN_TIMESTAMP + " DESC";

        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/data");

        public synchronized static void bulkInsertAdd(int _station_id, int _sensor_id,
                                                      List<io.github.hazyair.source.Data> data,
                                                      ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Data entry : data) {
                cpo.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(DataContract.COLUMN__STATION_ID, _station_id)
                        .withValueBackReference(DataContract.COLUMN__SENSOR_ID, _sensor_id)
                        .withValues(entry.toContentValues()).build());
            }
        }
        public synchronized static void bulkInsertAdd(List<io.github.hazyair.source.Data> data,
                                                      ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Data entry : data) {
                cpo.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValues(entry.toContentValues()).build());
            }
        }

        public synchronized static void bulkDeleteAdd(int _id, ArrayList<ContentProviderOperation> cpo) {
            cpo.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(
                    DataContract.COLUMN__SENSOR_ID + "=?",
                    new String[]{String.valueOf(_id)}).build());
        }

        public static Cursor selectLast(Context context, int _id) {
            return context.getContentResolver().query(CONTENT_URI,
                    io.github.hazyair.source.Data.keys(), "_sensor_id="+_id,
                    null, HazyairProvider.Data.DEFAULT_SORT + " LIMIT 1");

        }

    }

    public synchronized static ContentProviderResult[] bulkExecute(Context context,
                                                                   ArrayList<ContentProviderOperation> cpo) {
        try {
            return context.getContentResolver().applyBatch(AUTHORITY, cpo);
        } catch (RemoteException | OperationApplicationException e) {
            return null;
        }
    }

    private synchronized static void bulkDeleteAdd(int _id,
                                                   ArrayList<ContentProviderOperation> cpo) {
        cpo.add(ContentProviderOperation.newDelete(Stations.CONTENT_URI).withSelection(
                StationsContract.COLUMN__ID + "=?",
                new String[] { String.valueOf(_id) }).build());
        cpo.add(ContentProviderOperation.newDelete(Sensors.CONTENT_URI).withSelection(
                SensorsContract.COLUMN__STATION_ID + "=?",
                new String[] { String.valueOf(_id) }).build());
        cpo.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(
                DataContract.COLUMN__STATION_ID + "=?",
                new String[] { String.valueOf(_id) }).build());
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized static ContentProviderResult[] delete(Context context, int _id) {
        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
        bulkDeleteAdd(_id, cpo);
        return bulkExecute(context, cpo);
    }

    private SQLiteOpenHelper database;

    @Override
    public boolean onCreate() {
        database = HazyairDatabase.getInstance(getContext());
        return true;
    }

    private void insertValues(SQLiteDatabase db, String table, ContentValues[] values) {
        for (ContentValues cv : values) {
            db.insertOrThrow(table, null, cv);
        }
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            switch(MATCHER.match(uri)) {
                case STATIONS_CONTENT_URI: {
                    insertValues(db, "stations", values);
                    break;
                }
                case STATIONS_STATION_ID: {
                    insertValues(db, "stations", values);
                    break;
                }
                case SENSORS_CONTENT_URI: {
                    insertValues(db, "sensors", values);
                    break;
                }
                case SENSORS_SENSOR_ID: {
                    insertValues(db, "sensors", values);
                    break;
                }
                case DATA_CONTENT_URI: {
                    insertValues(db, "data", values);
                    break;
                }
                case DATA_DATA_ID: {
                    insertValues(db, "data", values);
                    break;
                }
            }
            if (getContext() != null)
                getContext().getContentResolver().notifyChange(uri, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return values.length;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> ops) throws
            OperationApplicationException {
        ContentProviderResult[] results;
        final SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            results = super.applyBatch(ops);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return results;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch(MATCHER.match(uri)) {
            case STATIONS_CONTENT_URI: {
                return "vnd.android.cursor.dir/stations";
            }
            case STATIONS_STATION_ID: {
                return "vnd.android.cursor.item/stations";
            }
            case SENSORS_CONTENT_URI: {
                return "vnd.android.cursor.dir/sensors";
            }
            case SENSORS_SENSOR_ID: {
                return "vnd.android.cursor.item/sensors";
            }
            case DATA_CONTENT_URI: {
                return "vnd.android.cursor.dir/data";
            }
            case DATA_DATA_ID: {
                return "vnd.android.cursor.item/data";
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor;
        switch(MATCHER.match(uri)) {
            case STATIONS_CONTENT_URI: {
                if (sortOrder == null) {
                    sortOrder = "locality ASC";
                }
                final String groupBy = null;
                final String having = null;
                final String limit = null;
                cursor = db.query(HazyairDatabase.STATIONS_TABLE, projection, selection,
                        selectionArgs, groupBy, having, sortOrder, limit);
                break;
            }
            case STATIONS_STATION_ID: {
                final String groupBy = null;
                final String having = null;
                final String limit = null;
                cursor = db.query(HazyairDatabase.STATIONS_TABLE, projection, "_id=?",
                        new String[]{uri.getPathSegments().get(1)}, groupBy, having, sortOrder,
                        limit);
                break;
            }
            case SENSORS_CONTENT_URI: {
                if (sortOrder == null) {
                    sortOrder = "id ASC";
                }
                final String groupBy = null;
                final String having = null;
                final String limit = null;
                cursor = db.query(HazyairDatabase.SENSORS_TABLE, projection, selection,
                        selectionArgs, groupBy, having, sortOrder, limit);
                break;
            }
            case SENSORS_SENSOR_ID: {
                final String groupBy = null;
                final String having = null;
                final String limit = null;
                cursor = db.query(HazyairDatabase.SENSORS_TABLE, projection, "_id=?",
                        new String[]{uri.getPathSegments().get(1)}, groupBy, having, sortOrder,
                        limit);
                break;
            }
            case DATA_CONTENT_URI: {
                if (sortOrder == null) {
                    sortOrder = "timestamp DESC";
                }
                final String groupBy = null;
                final String having = null;
                final String limit = null;
                cursor = db.query(HazyairDatabase.DATA_TABLE, projection, selection,
                        selectionArgs, groupBy, having, sortOrder, limit);
                break;
            }
            case DATA_DATA_ID: {
                final String groupBy = null;
                final String having = null;
                final String limit = null;
                cursor = db.query(HazyairDatabase.DATA_TABLE, projection, "_id=?",
                        new String[]{uri.getPathSegments().get(1)}, groupBy, having, sortOrder,
                        limit);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
        if (getContext() != null)
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase db = database.getWritableDatabase();
        final long id;
        switch(MATCHER.match(uri)) {
            case STATIONS_CONTENT_URI: {
                id = db.insertOrThrow("stations", null, values);
                break;
            }
            case STATIONS_STATION_ID: {
                id = db.insertOrThrow("stations", null, values);
                break;
            }
            case SENSORS_CONTENT_URI: {
                id = db.insertOrThrow("sensors", null, values);
                break;
            }
            case SENSORS_SENSOR_ID: {
                id = db.insertOrThrow("sensors", null, values);
                break;
            }
            case DATA_CONTENT_URI: {
                id = db.insertOrThrow("data", null, values);
                break;
            }
            case DATA_DATA_ID: {
                id = db.insertOrThrow("data", null, values);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
        if (getContext() != null)
            getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        final SQLiteDatabase db = database.getWritableDatabase();
        final int count;
        switch(MATCHER.match(uri)) {
            case STATIONS_CONTENT_URI: {
                count = db.update(HazyairDatabase.STATIONS_TABLE, values, where, whereArgs);
                break;
            }
            case STATIONS_STATION_ID: {
                count = db.update(HazyairDatabase.STATIONS_TABLE, values, "_id=?",
                        new String[] {uri.getPathSegments().get(1)});
                break;
            }
            case SENSORS_CONTENT_URI: {
                count = db.update(HazyairDatabase.SENSORS_TABLE, values, where, whereArgs);
                break;
            }
            case SENSORS_SENSOR_ID: {
                count = db.update(HazyairDatabase.SENSORS_TABLE, values, "_id=?",
                        new String[] {uri.getPathSegments().get(1)});
                break;
            }
            case DATA_CONTENT_URI: {
                count = db.update(HazyairDatabase.DATA_TABLE, values, where, whereArgs);
                break;
            }
            case DATA_DATA_ID: {
                count = db.update(HazyairDatabase.DATA_TABLE, values, "_id=?",
                        new String[] {uri.getPathSegments().get(1)});
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        final SQLiteDatabase db = database.getWritableDatabase();
        final int count;
        switch(MATCHER.match(uri)) {
            case STATIONS_CONTENT_URI: {
                count = db.delete(HazyairDatabase.STATIONS_TABLE, where, whereArgs);
                break;
            }
            case STATIONS_STATION_ID: {
                count = db.delete(HazyairDatabase.STATIONS_TABLE, "_id=?",
                        new String[]{uri.getPathSegments().get(1)});
                break;
            }
            case SENSORS_CONTENT_URI: {
                count = db.delete(HazyairDatabase.SENSORS_TABLE, where, whereArgs);
                break;
            }
            case SENSORS_SENSOR_ID: {
                count = db.delete(HazyairDatabase.SENSORS_TABLE, "_id=?",
                        new String[]{uri.getPathSegments().get(1)});
                break;
            }
            case DATA_CONTENT_URI: {
                count = db.delete(HazyairDatabase.DATA_TABLE, where, whereArgs);
                break;
            }
            case DATA_DATA_ID: {
                count = db.delete(HazyairDatabase.DATA_TABLE, "_id=?",
                        new String[]{uri.getPathSegments().get(1)});
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
        if (getContext() != null)
            getContext().getContentResolver().notifyChange(uri, null);
        return count;    }
}

/*package io.github.hazyair.data;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;
import net.simonvt.schematic.annotation.ContentProvider;

import java.util.ArrayList;
import java.util.List;

import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Station;

@ContentProvider(
        authority = HazyairProvider.AUTHORITY,
        database = HazyairDatabase.class)
public final class HazyairProvider {

    @SuppressWarnings("WeakerAccess")
    static final String AUTHORITY = "io.github.hazyair.provider";

    @SuppressWarnings("unused")
    @TableEndpoint(table = HazyairDatabase.STATIONS)
    public static class Stations {
        static final String DEFAULT_SORT = StationsContract.COLUMN_LOCALITY + " ASC";

        @ContentUri(
                path = "stations",
                type = "vnd.android.cursor.dir/stations",
                defaultSort = DEFAULT_SORT)
        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/stations");

        @InexactContentUri(
                path = "stations/#",
                name = "STATION_ID",
                type = "vnd.android.cursor.item/stations",
                whereColumn = StationsContract.COLUMN__ID,
                pathSegment = 1)

        public static Uri withId(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }

        public static boolean selected(Context context, Station station) {
            Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                    new String[]{StationsContract.COLUMN__ID}, StationsContract.selection,
                    StationsContract.selectionArgs(station), null);
            boolean result = false;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    station._id = cursor.getInt(0);
                    result = true;
                }
                cursor.close();
            }
            return result;
        }

        public synchronized static void bulkInsertAdd(io.github.hazyair.source.Station station,
                                         ArrayList<ContentProviderOperation> cpo) {
            cpo.add(ContentProviderOperation.newInsert(Stations.CONTENT_URI)
                    .withValues(station.toContentValues()).build());
        }

        public static Cursor select(Context context, int _id) {
            return context.getContentResolver().query(CONTENT_URI,
                    Station.keys(), "_id="+_id, null, null);
        }
    }

    @SuppressWarnings("unused")
    @TableEndpoint(table = HazyairDatabase.SENSORS)
    public static class Sensors {
        static final String DEFAULT_SORT = StationsContract.COLUMN_ID + " ASC";

        @ContentUri(
                path = "sensors",
                type = "vnd.android.cursor.dir/sensors",
                defaultSort = DEFAULT_SORT)
        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sensors");

        @InexactContentUri(
                path = "sensors/#",
                name = "SENSOR_ID",
                type = "vnd.android.cursor.item/sensors",
                whereColumn = SensorsContract.COLUMN__ID,
                pathSegment = 1)

        public static Uri withId(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }

        public synchronized static void bulkInsertAdd(int _station_id,
                                                      List<io.github.hazyair.source.Sensor> sensors,
                                                      ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Sensor sensor : sensors) {
                cpo.add(ContentProviderOperation.newInsert(Sensors.CONTENT_URI)
                        .withValueBackReference(SensorsContract.COLUMN__STATION_ID, _station_id)
                        .withValues(sensor.toContentValues()).build());
            }
        }

        public static Cursor selectAll(Context context) {
            return context.getContentResolver().query(CONTENT_URI,
                    Sensor.keys(), null, null, null);
        }

        public static Cursor select(Context context, int _id) {
            return context.getContentResolver().query(CONTENT_URI,
                    Sensor.keys(), "_station_id="+_id, null, null);
        }
    }

    @SuppressWarnings("unused")
    @TableEndpoint(table = HazyairDatabase.DATA)
    public static class Data {
        static final String DEFAULT_SORT = DataContract.COLUMN_TIMESTAMP + " DESC";

        @ContentUri(
                path = "data",
                type = "vnd.android.cursor.dir/data",
                defaultSort = DEFAULT_SORT)
        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/data");

        @InexactContentUri(
                path = "data/#",
                name = "DATA_ID",
                type = "vnd.android.cursor.item/data",
                whereColumn = DataContract.COLUMN__ID,
                pathSegment = 1)

        public static Uri withId(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }

        public synchronized static void bulkInsertAdd(int _station_id, int _sensor_id,
                                         List<io.github.hazyair.source.Data> data,
                                         ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Data entry : data) {
                cpo.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(DataContract.COLUMN__STATION_ID, _station_id)
                        .withValueBackReference(DataContract.COLUMN__SENSOR_ID, _sensor_id)
                        .withValues(entry.toContentValues()).build());
            }
        }
        public synchronized static void bulkInsertAdd(List<io.github.hazyair.source.Data> data,
                                         ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Data entry : data) {
                cpo.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValues(entry.toContentValues()).build());
            }
        }

        public synchronized static void bulkDeleteAdd(int _id, ArrayList<ContentProviderOperation> cpo) {
            cpo.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(
                    DataContract.COLUMN__SENSOR_ID + "=?",
                    new String[]{String.valueOf(_id)}).build());
        }

        public static Cursor selectLast(Context context, int _id) {
            return context.getContentResolver().query(CONTENT_URI,
                    io.github.hazyair.source.Data.keys(), "_sensor_id="+_id,
                    null, HazyairProvider.Data.DEFAULT_SORT + " LIMIT 1");

        }

    }

    public synchronized static ContentProviderResult[] bulkExecute(Context context,
                                                      ArrayList<ContentProviderOperation> cpo) {
        try {
            return context.getContentResolver().applyBatch(AUTHORITY, cpo);
        } catch (RemoteException | OperationApplicationException e) {
            return null;
        }
    }

    private synchronized static void bulkDeleteAdd(int _id,
                                      ArrayList<ContentProviderOperation> cpo) {
        cpo.add(ContentProviderOperation.newDelete(Stations.CONTENT_URI).withSelection(
                StationsContract.COLUMN__ID + "=?",
                new String[] { String.valueOf(_id) }).build());
        cpo.add(ContentProviderOperation.newDelete(Sensors.CONTENT_URI).withSelection(
                SensorsContract.COLUMN__STATION_ID + "=?",
                new String[] { String.valueOf(_id) }).build());
        cpo.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(
                DataContract.COLUMN__STATION_ID + "=?",
                new String[] { String.valueOf(_id) }).build());
    }

    @SuppressWarnings("UnusedReturnValue")
    public synchronized static ContentProviderResult[] delete(Context context, int _id) {
        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
        bulkDeleteAdd(_id, cpo);
        return bulkExecute(context, cpo);
    }

}
*/