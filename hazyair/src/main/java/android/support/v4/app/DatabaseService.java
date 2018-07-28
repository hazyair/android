package android.support.v4.app;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import io.github.hazyair.R;
import io.github.hazyair.data.HazyairProvider;
import io.github.hazyair.data.SensorsContract;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Info;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Source;
import io.github.hazyair.source.Station;
import io.github.hazyair.source.iface.DataCallback;
import io.github.hazyair.source.iface.SensorsCallback;
import io.github.hazyair.util.Preference;
import io.github.hazyair.widget.AppWidget;

public class DatabaseService extends JobIntentService {

    private static final int JOB_ID = 0xABADCAFE;

    private final static String ACTION_UPDATE = "io.github.hazyair.ACTION_UPDATE";
    private final static String ACTION_UPDATE_STATION = "io.github.hazyair.ACTION_UPDATE_STATION";
    private final static String ACTION_DELETE = "io.github.hazyair.ACTION_DELETE";
    private final static String ACTION_INSERT_OR_DELETE =
            "io.github.hazyair.ACTION_INSERT_OR_DELETE";
    private final static String ACTION_SELECT = "io.github.hazyair.ACTION_SELECT";
    public final static String ACTION_UPDATING =
            "io.github.hazyair.ACTION_UPDATING";
    public final static String ACTION_UPDATED =
            "io.github.hazyair.ACTION_UPDATED";


    private final static String PARAM__ID = "io.github.hazyair.PARAM__ID";
    private final static String PARAM_STATION = "io.github.hazyair.PARAM_STATION";
    public final static String PARAM_POSITION = "io.github.hazyair.PARAM_POSITION";
    public final static String PARAM_RESCHEDULE = "io.github.hazyair.PARAM_RESCHEDULE";
    public final static String PARAM_MESSAGE = "io.github.hazyair.PARAM_MESSAGE";

    private static int count;

    private static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, DatabaseService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@Nullable Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        switch (intent.getAction()) {
            case ACTION_DELETE: {
                sendConfirmation();
                int _id = intent.getIntExtra(PARAM__ID, 0);
                if (_id == 0) return;
                HazyairProvider.delete(this, _id);
                sendConfirmation(-1);
                break;
            }
            case ACTION_INSERT_OR_DELETE: {
                sendConfirmation();
                Station station = intent.getParcelableExtra(PARAM_STATION);
                int position = intent.getIntExtra(PARAM_POSITION, -1);
                if (HazyairProvider.Stations.selected(this, station)) {
                    HazyairProvider.delete(this, station._id);
                    Info info = Preference.getInfo(this);
                    if (info != null && info.station._id == station._id) {
                        Preference.putInfo(this, null);
                        AppWidget.update(this);
                    }
                    sendConfirmation(position);
                } else {
                    ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
                    Cursor cursor = HazyairProvider.Stations.select(this);
                    if (cursor.getCount() >= 8) {
                        sendConfirmation(position, getString(R.string.message_maximum));
                        break;
                    }
                    cursor.close();
                    HazyairProvider.Stations.bulkInsertAdd(station, cpo);
                    Source.with(DatabaseService.this).load(Source.Type.GIOS).from(station)
                            .into(new SensorsCallback() {
                                @Override
                                public void onSuccess(List<Sensor> sensors) {
                                    count = sensors.size();
                                    HazyairProvider.Sensors.bulkInsertAdd(0, sensors,
                                            cpo);
                                    int index = 1;
                                    for (Sensor sensor : sensors) {
                                        sensor._id = index;
                                        index++;
                                        Source.with(DatabaseService.this).load(Source.Type.GIOS)
                                                .from(sensor).into(new DataCallback() {
                                            @Override
                                            public void onSuccess(List<Data> data) {
                                                HazyairProvider.Data.bulkInsertAdd(0,
                                                        sensor._id, data, cpo);
                                                count--;
                                                if (count == 0) {
                                                    HazyairProvider.bulkExecute(
                                                            DatabaseService.this, cpo);
                                                    sendConfirmation(position);
                                                }
                                            }

                                            @Override
                                            public void onError() {
                                                sendConfirmation(position);
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onError() {
                                    sendConfirmation(position);
                                }
                            });
                }
                break;
            }
            case ACTION_UPDATE: {
                ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
                Cursor cursor = HazyairProvider.Sensors.selectAll(this);
                count = cursor.getCount();
                if (cursor.getCount() > 0) {
                    sendConfirmation();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        cursor.moveToPosition(i);
                        Bundle sensor = Sensor.toBundleFromCursor(cursor);
                        int _sensor_id = sensor.getInt(SensorsContract.COLUMN__ID);
                        int _station_id = sensor.getInt(SensorsContract.COLUMN__STATION_ID);
                        HazyairProvider.Data.bulkDeleteAdd(
                                sensor.getInt(SensorsContract.COLUMN__ID), cpo);
                        Source.with(DatabaseService.this).load(Source.Type.GIOS)
                                .from(new Sensor(sensor.getString(SensorsContract.COLUMN_ID),
                                        sensor.getString(SensorsContract.COLUMN_STATION_ID),
                                        sensor.getString(SensorsContract.COLUMN_PARAMETER),
                                        sensor.getString(SensorsContract.COLUMN_UNIT)
                                )).into(new DataCallback() {

                            @Override
                            public void onSuccess(List<Data> data) {
                                for (Data entry : data) {
                                    entry._sensor_id = _sensor_id;
                                    entry._station_id = _station_id;
                                }
                                HazyairProvider.Data.bulkInsertAdd(data, cpo);
                                count--;
                                if (count == 0) {
                                    HazyairProvider.bulkExecute(DatabaseService.this, cpo);
                                    Info info = Preference.getInfo(DatabaseService.this);
                                    if (info != null) select(info.station._id);
                                    sendConfirmation(false);
                                }

                            }

                            @Override
                            public void onError() {
                                sendConfirmation(true);
                            }
                        });
                    }
                }
                break;
            }
            case ACTION_UPDATE_STATION: {
                Station station = intent.getParcelableExtra(PARAM_STATION);
                ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
                Cursor cursor = HazyairProvider.Sensors.select(this, station._id);
                count = cursor.getCount();
                if (cursor.getCount() > 0) {
                    sendConfirmation();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        cursor.moveToPosition(i);
                        Bundle sensor = Sensor.toBundleFromCursor(cursor);
                        int _sensor_id = sensor.getInt(SensorsContract.COLUMN__ID);
                        int _station_id = sensor.getInt(SensorsContract.COLUMN__STATION_ID);
                        HazyairProvider.Data.bulkDeleteAdd(
                                sensor.getInt(SensorsContract.COLUMN__ID), cpo);
                        Source.with(DatabaseService.this).load(Source.Type.GIOS)
                                .from(new Sensor(sensor.getString(SensorsContract.COLUMN_ID),
                                        sensor.getString(SensorsContract.COLUMN_STATION_ID),
                                        sensor.getString(SensorsContract.COLUMN_PARAMETER),
                                        sensor.getString(SensorsContract.COLUMN_UNIT)
                                )).into(new DataCallback() {

                            @Override
                            public void onSuccess(List<Data> data) {
                                for (Data entry : data) {
                                    entry._sensor_id = _sensor_id;
                                    entry._station_id = _station_id;
                                }
                                HazyairProvider.Data.bulkInsertAdd(data, cpo);
                                count--;
                                if (count == 0) {
                                    HazyairProvider.bulkExecute(DatabaseService.this, cpo);
                                    Info info = Preference.getInfo(DatabaseService.this);
                                    if (info != null) select(info.station._id);
                                    sendConfirmation(false);
                                }

                            }

                            @Override
                            public void onError() {
                                sendConfirmation(true);
                            }
                        });
                    }
                }
                break;
            }
            case ACTION_SELECT: {
                int _id = intent.getIntExtra(PARAM__ID, 0);
                if (_id == 0) return;
                select(_id);
                break;
            }
        }
    }

    private void select(int _id) {
        Cursor stationCursor = HazyairProvider.Stations.select(this, _id);
        if (stationCursor == null || stationCursor.getCount() <= 0 ||
                !stationCursor.moveToFirst()) return;
        Cursor cursor = HazyairProvider.Sensors.select(this, _id);
        if (cursor == null || cursor.getCount() <= 0) return;
        List<Sensor> sensors = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            if (!cursor.moveToPosition(i)) continue;
            sensors.add(new Sensor(cursor));
        }
        List<Data> data = new ArrayList<>();
        List<Sensor> sensorList = new ArrayList<>();
        for (Sensor sensor: sensors) {
            cursor = HazyairProvider.Data.selectLast(this, sensor._id);
            if (cursor == null || cursor.getCount() <= 0 || !cursor.moveToFirst()) continue;
            sensorList.add(sensor);
            data.add(new Data(cursor));
        }
        Preference.putInfo(this, new Info(new Station(stationCursor), sensorList,
                data));
        AppWidget.update(this);
    }

    private void sendConfirmation(int position) {
        sendBroadcast(new Intent(ACTION_UPDATED).putExtra(PARAM_POSITION, position));
    }

    private void sendConfirmation() {
        sendBroadcast(new Intent(ACTION_UPDATING));
    }

    private void sendConfirmation(boolean reschedule) {
        sendBroadcast(new Intent(ACTION_UPDATED).putExtra(PARAM_RESCHEDULE, reschedule));
    }

    private void sendConfirmation(int position, String message) {
        sendBroadcast(new Intent(ACTION_UPDATED).putExtra(PARAM_POSITION, position)
                .putExtra(PARAM_MESSAGE, message));
    }

    @Override
    GenericWorkItem dequeueWork() {

        try {
            return super.dequeueWork();
        } catch (SecurityException ignored) { }

        return null;
    }

    public static void selectStation(Context context, Bundle station) {
        if (station == null) {
            Preference.putInfo(context, null);
            AppWidget.update(context);
        } else {
            DatabaseService.enqueueWork(context,
                    new Intent(context, DatabaseService.class)
                            .setAction(DatabaseService.ACTION_SELECT)
                            .putExtra(DatabaseService.PARAM__ID,
                                    station.getInt(StationsContract.COLUMN__ID)));
        }
    }

    public static Bundle selectedStation(Context context) {
        Info info = Preference.getInfo(context);
        if (info != null) return info.station.toBundle();
        return null;
    }

    public static void delete(Context context, int _id) {
        DatabaseService.enqueueWork(context,
                new Intent(context, DatabaseService.class)
                        .setAction(DatabaseService.ACTION_DELETE)
                        .putExtra(DatabaseService.PARAM__ID, _id));
    }

    public static void insertOrDelete(Context context, int position, Station station) {
        DatabaseService.enqueueWork(context,
                new Intent(context, DatabaseService.class)
                        .setAction(DatabaseService.ACTION_INSERT_OR_DELETE)
                        .putExtra(DatabaseService.PARAM_POSITION, position)
                        .putExtra(DatabaseService.PARAM_STATION, station));
    }

    public static void update(Context context) {
        DatabaseService.enqueueWork(context,
                new Intent(context, DatabaseService.class)
                        .setAction(DatabaseService.ACTION_UPDATE));

    }

    /*public static void update(Context context, Station station) {
        DatabaseService.enqueueWork(context, new Intent(context, DatabaseService.class)
                        .setAction(DatabaseService.ACTION_UPDATE_STATION)
                        .putExtra(DatabaseService.PARAM_STATION, station));
    }*/

    public static void showWarning(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_add_station))
                .setMessage(message)
                .setPositiveButton(
                        context.getString(R.string.button_ok),
                        null)
                .create()
                .show();
    }

}
