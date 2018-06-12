package io.github.hazyair.sync;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.github.hazyair.data.HazyairProvider;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Source;
import io.github.hazyair.source.Station;
import io.github.hazyair.source.iface.DataCallback;
import io.github.hazyair.source.iface.SensorsCallback;

public class DatabaseService extends IntentService {

    public final static String ACTION_INSERT = "io.github.hazyair.ACTION_INSERT";
    public final static String ACTION_DELETE = "io.github.hazyair.ACTION_DELETE";
    public final static String ACTION_INSERT_OR_DELETE =
            "io.github.hazyair.ACTION_INSERT_OR_DELETE";
    public final static String ACTION_DATA_UPDATED =
            "io.github.hazyair.ACTION_DATA_UPDATED";

    public final static String PARAM__ID = "_id";
    public final static String PARAM_STATION = "station";
    public final static String PARAM_POSITION = "position";

    private static int count;


    public DatabaseService() {
        super(DatabaseService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        switch (intent.getAction()) {
            case ACTION_DELETE:
                int _id = intent.getIntExtra(PARAM__ID, 0);
                if (_id == 0) return;
                HazyairProvider.delete(this, _id);
                break;
            case ACTION_INSERT_OR_DELETE:
                Station station = intent.getParcelableExtra(PARAM_STATION);
                int position = intent.getIntExtra(PARAM_POSITION, -1);
                if (HazyairProvider.Stations.selected(this, station)) {
                    HazyairProvider.delete(this, station._id);
                    sendConfirmation(position);
                } else {
                    ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
                    HazyairProvider.Stations.bulkInsertAdd(station, cpo);
                    Source.with(DatabaseService.this).load(Source.Type.GIOS).from(station)
                            .into(new SensorsCallback() {
                                @Override
                                public void onSuccess(List<Sensor> sensors) {
                                    count = sensors.size();
                                    HazyairProvider.Sensors.bulkInsertAdd(0, sensors, cpo);
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
                                                    HazyairProvider.bulkInsertExecute(DatabaseService.this, cpo);
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
    }

    private void sendConfirmation(int position) {
        sendBroadcast(new Intent(ACTION_DATA_UPDATED)
                .putExtra(PARAM_POSITION, position));
    }
}
