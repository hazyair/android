package io.github.hazyair.data;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.crashlytics.android.Crashlytics;

import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;
import net.simonvt.schematic.annotation.ContentProvider;

import java.util.ArrayList;
import java.util.List;

import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Station;
import io.github.hazyair.util.Preference;

@ContentProvider(
        authority = HazyairProvider.AUTHORITY,
        database = HazyairDatabase.class)
public final class HazyairProvider {

    @SuppressWarnings("WeakerAccess")
    static final String AUTHORITY = "io.github.hazyair.provider";

    @TableEndpoint(table = HazyairDatabase.CONFIG)
    public static class Config {
        public final static String PARAM_UPDATE = "io.github.hazyair.PARAM_UPDATE";

        @ContentUri(
                path = "config",
                type = "vnd.android.cursor.dir/stations")

        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/config");

        public static void set(Context context, String key, String value) {
            if (context == null || key == null || value == null) return;
            ContentValues contentValues = new ContentValues();
            contentValues.put(ConfigContract.COLUMN_KEY, key);
            contentValues.put(ConfigContract.COLUMN_KEY, value);
            context.getContentResolver().insert(CONTENT_URI, contentValues);
        }

        public static long get(Context context, String key) {
            if (context == null || key == null) return 0;
            long result = 0;
            Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                    new String[]{ConfigContract.COLUMN_VALUE},
                    ConfigContract.COLUMN_KEY + "=?", new String[]{key},
                    null);
            if (cursor != null) {
                if (cursor.moveToFirst()) result = cursor.getLong(0);
                cursor.close();
            }
            return result;
        }

    }

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
            if (context == null || station == null) return false;
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
            if (station == null || cpo == null) return;
            cpo.add(ContentProviderOperation.newInsert(Stations.CONTENT_URI)
                    .withValues(station.toContentValues()).build());
        }

        public static Cursor select(Context context, int _id) {
            if (context == null) return null;
            return context.getContentResolver().query(CONTENT_URI,
                    Station.keys(), "_id="+_id, null, null);
        }

        public static Cursor select(Context context) {
            if (context == null) return null;
            return context.getContentResolver().query(CONTENT_URI,
                    Station.keys(), null, null, null);
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
                                                      List<Sensor> sensors,
                                                      ArrayList<ContentProviderOperation> cpo) {
            if (sensors == null || cpo == null) return;
            for (io.github.hazyair.source.Sensor sensor : sensors) {
                cpo.add(ContentProviderOperation.newInsert(Sensors.CONTENT_URI)
                        .withValueBackReference(SensorsContract.COLUMN__STATION_ID, _station_id)
                        .withValues(sensor.toContentValues()).build());
            }
        }

        public static Cursor select(Context context) {
            if (context == null) return null;
            return context.getContentResolver().query(CONTENT_URI,
                    Sensor.keys(), null, null, null);
        }

        public static Cursor select(Context context, int _id) {
            if (context == null) return null;
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
            if (data == null || cpo == null) return;
            for (io.github.hazyair.source.Data entry : data) {
                cpo.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(DataContract.COLUMN__STATION_ID, _station_id)
                        .withValueBackReference(DataContract.COLUMN__SENSOR_ID, _sensor_id)
                        .withValues(entry.toContentValues()).build());
            }
        }
        public synchronized static void bulkInsertAdd(List<io.github.hazyair.source.Data> data,
                                                      long timestamp,
                                                      ArrayList<ContentProviderOperation> cpo) {
            if (data == null || cpo == null) return;
            for (io.github.hazyair.source.Data entry : data) {
                if (entry.timestamp >= timestamp) {
                    cpo.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValues(entry.toContentValues()).build());
                }
            }
        }

        public synchronized static void bulkDeleteAdd(int _id, long timestamp,
                                                      ArrayList<ContentProviderOperation> cpo) {
            if (cpo == null) return;
            cpo.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(
                    DataContract.COLUMN__SENSOR_ID + "=? AND " +
                            DataContract.COLUMN_TIMESTAMP + "<?",
                    new String[]{String.valueOf(_id), String.valueOf(timestamp)}).build());
        }

        public static Cursor selectLast(Context context, int _id) {
            if (context == null) return null;
            return context.getContentResolver().query(CONTENT_URI,
                    io.github.hazyair.source.Data.keys(), "_sensor_id="+_id,
                    null, HazyairProvider.Data.DEFAULT_SORT + " LIMIT 1");

        }

    }

    public synchronized static ContentProviderResult[] bulkExecute(Context context,
                                                      ArrayList<ContentProviderOperation> cpo) {
        if (context == null || cpo == null) return null;
        try {
            return context.getContentResolver().applyBatch(AUTHORITY, cpo);
        } catch (RemoteException | OperationApplicationException e) {
            if (Preference.isCrashlyticsEnabled(context)) {
                Crashlytics.logException(e);
            }
            return null;
        }
    }

    private synchronized static void bulkDeleteAdd(int _id,
                                      ArrayList<ContentProviderOperation> cpo) {
        if (cpo == null) return;
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
        if (context == null) return null;
        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
        bulkDeleteAdd(_id, cpo);
        return bulkExecute(context, cpo);
    }

}
