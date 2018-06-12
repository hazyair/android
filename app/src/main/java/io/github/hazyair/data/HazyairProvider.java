package io.github.hazyair.data;

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

import io.github.hazyair.source.Station;
import timber.log.Timber;

@ContentProvider(
        authority = HazyairProvider.AUTHORITY,
        database = HazyairDatabase.class)
public final class HazyairProvider {

    static final String AUTHORITY = "io.github.hazyair.provider";

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

        /*
            public static void delete(Context context, Station station) {
                context.getContentResolver().delete(Stations.CONTENT_URI, StationsContract.selection,
                        StationsContract.selectionArgs(station));
            }
        */
        public static void delete(Context context, int _id) {
            context.getContentResolver().delete(CONTENT_URI, StationsContract.COLUMN__ID +
                    "=?", new String[]{String.valueOf(_id)});
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

        public static void bulkInsertAdd(io.github.hazyair.source.Station station,
                                         ArrayList<ContentProviderOperation> cpo) {
            cpo.add(ContentProviderOperation.newInsert(Stations.CONTENT_URI)
                    .withValues(station.toContentValues()).build());
        }

        public static void bulkInsertAdd(List<io.github.hazyair.source.Station> stations,
                                         ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Station station : stations) {
                bulkInsertAdd(station, cpo);
            }
        }
    }

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

        public static void delete(Context context, int _id) {
            context.getContentResolver().delete(Sensors.CONTENT_URI, SensorsContract.COLUMN__STATION_ID
                    + "=?", new String[]{String.valueOf(_id)});
        }

        public static void bulkInsertAdd(int _station_id, List<io.github.hazyair.source.Sensor> sensors,
                                         ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Sensor sensor : sensors) {
                cpo.add(ContentProviderOperation.newInsert(Sensors.CONTENT_URI)
                        .withValueBackReference(SensorsContract.COLUMN__STATION_ID, _station_id)
                        .withValues(sensor.toContentValues()).build());
            }
        }
    }

    @TableEndpoint(table = HazyairDatabase.DATA)
    public static class Data {
        static final String DEFAULT_SORT = DataContract.COLUMN_TIMESTAMP + " DESC";

        @ContentUri(
                path = "data",
                type = "vnd.android.cursor.dir/data",
                defaultSort = DEFAULT_SORT)
        static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/data");
        //static final Uri CONTENT_URI = Sensors.CONTENT_URI.buildUpon().appendPath("#").appendPath("data").build();

        @InexactContentUri(
                path = "data/#",
                name = "DATA_ID",
                type = "vnd.android.cursor.item/data",
                whereColumn = DataContract.COLUMN__ID,
                pathSegment = 1)

        public static Uri withId(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }

        public static void bulkInsertAdd(int _station_id, int _sensor_id,
                                         List<io.github.hazyair.source.Data> data,
                                         ArrayList<ContentProviderOperation> cpo) {
            for (io.github.hazyair.source.Data entry : data) {
                cpo.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(DataContract.COLUMN__STATION_ID, _station_id)
                        .withValueBackReference(DataContract.COLUMN__SENSOR_ID, _sensor_id)
                        .withValues(entry.toContentValues()).build());
            }
        }

        public static void delete(Context context, int _id) {
            context.getContentResolver().delete(Data.CONTENT_URI, DataContract.COLUMN__STATION_ID
                    + "=?", new String[]{String.valueOf(_id)});
        }


    }
    public static ContentProviderResult[] bulkInsertExecute(Context context,
                                                            ArrayList<ContentProviderOperation> cpo) {
        try {
            return context.getContentResolver().applyBatch(AUTHORITY, cpo);
        } catch (RemoteException | OperationApplicationException e) {
            Timber.e(e);
            return null;
        }
    }

    public static ContentProviderResult[] delete(Context context, int _id) {
        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
        cpo.add(ContentProviderOperation.newDelete(Stations.CONTENT_URI).withSelection(
                StationsContract.COLUMN__ID + "=?",
                new String[] { String.valueOf(_id) }).build());
        cpo.add(ContentProviderOperation.newDelete(Sensors.CONTENT_URI).withSelection(
                SensorsContract.COLUMN__STATION_ID + "=?",
                new String[] { String.valueOf(_id) }).build());
        cpo.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(
                DataContract.COLUMN__STATION_ID + "=?",
                new String[] { String.valueOf(_id) }).build());
        try {
            return context.getContentResolver().applyBatch(AUTHORITY, cpo);
        } catch (RemoteException | OperationApplicationException e) {
            Timber.e(e);
            return null;
        }
    }
}
