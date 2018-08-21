package android.support.v4.app;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.crashlytics.android.Crashlytics;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import io.github.hazyair.source.iface.Worker;
import io.github.hazyair.util.Preference;
import io.github.hazyair.widget.AppWidget;

public class DatabaseService extends JobIntentService {

    private static final int JOB_ID = 0xABADCAFE;

    private final static String ACTION_UPDATE = "io.github.hazyair.ACTION_UPDATE";
    private final static String ACTION_DELETE = "io.github.hazyair.ACTION_DELETE";
    private final static String ACTION_INSERT_OR_DELETE =
            "io.github.hazyair.ACTION_INSERT_OR_DELETE";
    private final static String ACTION_SELECT = "io.github.hazyair.ACTION_SELECT";
    public final static String ACTION_UPDATING =
            "io.github.hazyair.ACTION_UPDATING";
    public final static String ACTION_UPDATED =
            "io.github.hazyair.ACTION_UPDATED";
    public final static String ACTION_SELECTED =
            "io.github.hazyair.ACTION_SELECTED";


    private final static String PARAM__ID = "io.github.hazyair.PARAM__ID";
    private final static String PARAM_STATION = "io.github.hazyair.PARAM_STATION";
    public final static String PARAM_POSITION = "io.github.hazyair.PARAM_POSITION";
    public final static String PARAM_RESCHEDULE = "io.github.hazyair.PARAM_RESCHEDULE";
    public final static String PARAM_MESSAGE = "io.github.hazyair.PARAM_MESSAGE";
    public final static String PARAM_INFO = "io.github.hazyair.PARAM_INFO";

    private final static int LIMIT = 25;

    private static boolean mError;

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
                    Cursor cursor = HazyairProvider.Stations.select(this);
                    if (cursor == null) break;
                    int stations = cursor.getCount();
                    cursor.close();
                    if (stations >= 8) {
                        sendConfirmation(position, getString(R.string.message_maximum));
                        break;
                    }
                    ArrayList<Worker> workers = new ArrayList<>();
                    mError = false;
                    ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
                    SensorsCallback sensorsCallback = new SensorsCallback() {
                        boolean mIsAlive = true;

                        @Override
                        public boolean isAlive() {
                            return mIsAlive;
                        }

                        @Override
                        public void onSuccess(List<Sensor> sensors) {
                            HazyairProvider.Sensors.bulkInsertAdd(0, sensors,
                                    cpo);
                            int index = 1;
                            for (Sensor sensor : sensors) {
                                sensor._id = index;
                                index++;
                                DataCallback dataCallback = new DataCallback() {
                                    boolean mIsAlive = true;
                                    @Override
                                    public boolean isAlive() {
                                        return mIsAlive;
                                    }

                                    @Override
                                    public void onSuccess(List<Data> data) {
                                        int size = data.size();
                                        data = data.subList(0, (size > LIMIT ? LIMIT : size));
                                        HazyairProvider.Data.bulkInsertAdd(0,
                                                sensor._id, data, cpo);
                                        synchronized (this) {
                                            mIsAlive = false;
                                            this.notifyAll();
                                        }
                                    }

                                    @Override
                                    public void onError() {
                                        mError = true;
                                        synchronized (this) {
                                            mIsAlive = false;
                                            this.notifyAll();
                                        }
                                    }
                                };
                                workers.add(dataCallback);
                                Source.with(DatabaseService.this).load(Source.Type.GIOS)
                                        .from(sensor).into(dataCallback);
                            }
                            synchronized (this) {
                                mIsAlive = false;
                                this.notifyAll();
                            }
                        }

                        @Override
                        public void onError() {
                            mError = true;
                            synchronized (this) {
                                mIsAlive = false;
                                this.notifyAll();
                            }
                        }
                    };
                    HazyairProvider.Stations.bulkInsertAdd(station, cpo);
                    workers.add(sensorsCallback);
                    Source.with(DatabaseService.this).load(Source.Type.GIOS).from(station)
                            .into(sensorsCallback);
                    {
                        Worker worker = workers.get(0);
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (worker) {
                            while (worker.isAlive()) {
                                try {
                                    worker.wait();
                                } catch (InterruptedException e) {
                                    if (Preference.isCrashlyticsEnabled(this)) {
                                        Crashlytics.logException(e);
                                    }
                                }
                            }
                        }
                    }
                    if (workers.size() > 1) {
                        for (Worker worker : workers.subList(1, workers.size())) {
                            //noinspection SynchronizationOnLocalVariableOrMethodParameter
                            synchronized (worker) {
                                while (worker.isAlive()) {
                                    try {
                                        worker.wait();
                                    } catch (InterruptedException e) {
                                        if (Preference.isCrashlyticsEnabled(this)) {
                                            Crashlytics.logException(e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!mError) HazyairProvider.bulkExecute(
                            DatabaseService.this, cpo);
                    sendConfirmation(position);
                }
                break;
            }
            case ACTION_UPDATE: {
                Cursor cursor = HazyairProvider.Sensors.select(this);
                if (cursor == null) break;
                int count = cursor.getCount();
                if (count <= 0) {
                    cursor.close();
                    break;
                }
                sendConfirmation();
                ArrayList<Bundle> sensors = new ArrayList<>();
                for (int i=0; i < count; i ++) {
                    cursor.moveToPosition(i);
                    sensors.add(Sensor.toBundleFromCursor(cursor));
                }
                cursor.close();
                ArrayList<Worker> workers = new ArrayList<>();
                mError = false;
                ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
                long timestamp = new DateTime(DateTime.now().getMillis() -
                        TimeUnit.HOURS.toMillis(LIMIT),
                        DateTimeZone.getDefault()).withZone(DateTimeZone.forID("UTC"))
                        .getMillis();
                for (int i = 0; i < count; i++) {
                    Bundle sensor = sensors.get(i);
                    int _sensor_id = sensor.getInt(SensorsContract.COLUMN__ID);
                    int _station_id = sensor.getInt(
                            SensorsContract.COLUMN__STATION_ID);
                    HazyairProvider.Data.bulkDeleteAdd(
                            sensor.getInt(SensorsContract.COLUMN__ID),
                            timestamp, cpo);
                    DataCallback dataCallback = new DataCallback() {
                        boolean mIsAlive = true;

                        @Override
                        public boolean isAlive() {
                            return mIsAlive;
                        }

                        @Override
                        public void onSuccess(List<Data> data) {
                            int size = data.size();
                            data = data.subList(0, (size > LIMIT ? LIMIT : size));
                            for (Data entry : data) {
                                entry._sensor_id = _sensor_id;
                                entry._station_id = _station_id;
                            }
                            HazyairProvider.Data.bulkInsertAdd(data, timestamp, cpo);
                            synchronized (this) {
                                mIsAlive = false;
                                this.notifyAll();
                            }
                        }

                        @Override
                        public void onError() {
                            mError = true;
                            synchronized (this) {
                                mIsAlive = false;
                                this.notifyAll();
                            }
                        }
                    };
                    workers.add(dataCallback);
                    Source.with(DatabaseService.this).load(Source.Type.GIOS).from(
                            new Sensor(sensor.getString(SensorsContract.COLUMN_ID),
                                    sensor.getString(
                                            SensorsContract.COLUMN_STATION_ID),
                                    sensor.getString(
                                            SensorsContract.COLUMN_PARAMETER),
                                    sensor.getString(
                                            SensorsContract.COLUMN_UNIT)
                            )).into(dataCallback);
                }
                for (Worker worker: workers) {
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (worker) {
                        while (worker.isAlive()) {
                            try {
                                worker.wait();
                            } catch (InterruptedException e) {
                                if (Preference.isCrashlyticsEnabled(this)) {
                                    Crashlytics.logException(e);
                                }
                            }
                        }
                    }
                }
                if (!mError && cpo.size() > count) {
                    HazyairProvider.bulkExecute(DatabaseService.this, cpo);
                    HazyairProvider.Config.set(DatabaseService.this,
                            HazyairProvider.Config.PARAM_UPDATE,
                            String.valueOf(new DateTime(DateTime.now(),
                                    DateTimeZone.getDefault()).withZone(DateTimeZone.UTC)
                                    .getMillis()));
                    Info info = Preference.getInfo(DatabaseService.this);
                    if (info != null) select(info.station._id);
                }
                sendConfirmation(mError);
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
        if (stationCursor == null) return;
        if (stationCursor.getCount() <= 0 || !stationCursor.moveToFirst()) {
            stationCursor.close();
            return;
        }
        Cursor cursor = HazyairProvider.Sensors.select(this, _id);
        if (cursor == null) return;
        if (cursor.getCount() <= 0) {
            stationCursor.close();
            cursor.close();
            return;
        }
        List<Sensor> sensors = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            if (!cursor.moveToPosition(i)) continue;
            sensors.add(new Sensor(cursor));
        }
        cursor.close();
        List<Data> data = new ArrayList<>();
        List<Sensor> sensorList = new ArrayList<>();
        for (Sensor sensor: sensors) {
            cursor = HazyairProvider.Data.selectLast(this, sensor._id);
            if (cursor == null || cursor.getCount() <= 0 || !cursor.moveToFirst()) continue;
            sensorList.add(sensor);
            data.add(new Data(cursor));
            cursor.close();
        }
        Info info = new Info(new Station(stationCursor), sensorList, data);
        stationCursor.close();
        sendBroadcast(new Intent(ACTION_SELECTED).putExtra(PARAM_INFO, info));
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean update(Context context, long interval) {
        if (System.currentTimeMillis() - HazyairProvider.Config.get(context,
                HazyairProvider.Config.PARAM_UPDATE) > interval) {
            DatabaseService.enqueueWork(context,
                    new Intent(context, DatabaseService.class)
                            .setAction(DatabaseService.ACTION_UPDATE));
            return true;
        }
        return false;
    }

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
