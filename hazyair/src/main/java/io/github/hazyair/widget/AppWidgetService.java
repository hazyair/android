package io.github.hazyair.widget;

import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.TimeUnit;

import io.github.hazyair.R;
import io.github.hazyair.gui.MainActivity;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Info;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.util.Config;
import io.github.hazyair.util.Preference;
import io.github.hazyair.util.Quality;
import io.github.hazyair.util.Time;

import static android.graphics.Typeface.BOLD;

public class AppWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            Info mInfo;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                Thread thread = new Thread() {
                    public void run() {
                        mInfo = Config.getInfo(getApplicationContext());
                    }
                };
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    if (Preference.isCrashlyticsEnabled(getApplicationContext())) {
                        Crashlytics.logException(e);
                    }
                }
            }

            @Override
            public void onDestroy() {

            }

            @Override
            public int getCount() {
                if (mInfo == null || mInfo.sensors == null) return 0;
                return mInfo.sensors.size();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                RemoteViews remoteViews = new RemoteViews(getPackageName(),
                        R.layout.appwidget_sensor);
                if (mInfo == null || mInfo.sensors == null || mInfo.data == null ||
                        mInfo.station == null) return remoteViews;
                int sensorSize = mInfo.sensors.size();
                int dataSize = mInfo.data.size();
                if (position >= dataSize || sensorSize != dataSize)
                    return remoteViews;
                Sensor sensor = mInfo.sensors.get(position);
                Data data = mInfo.data.get(position);
                long timestamp = Time
                        .getTimestamp(data.timestamp);
                long hours = (System.currentTimeMillis() - timestamp) /
                        TimeUnit.HOURS.toMillis(1);
                long minutes = (System.currentTimeMillis() -
                        timestamp) %
                        TimeUnit.HOURS.toMillis(1) /
                        TimeUnit.MINUTES.toMillis(1);
                int percent = Quality.normalize(sensor.parameter, data.value);
                SpannableString text = new SpannableString(String.format("%s: %s %s (%s%%) %s %s h",
                        sensor.parameter, data.value, sensor.unit, String.valueOf(percent),
                        (hours < 1 ? "<" : (minutes > 0 ? (minutes > 30 ? "<" : ">")
                                : "")), (hours < 1 ? "1" : (minutes > 30 ?
                                String.valueOf(hours+1) : String.valueOf(hours)))));
                text.setSpan(new StyleSpan(BOLD), 0, sensor.parameter.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (percent > 100) {
                    text.setSpan(new ForegroundColorSpan(getColor(R.color.accent)),
                            0, text.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                remoteViews.setTextViewText(R.id.sensor, text);
                remoteViews.setOnClickFillInIntent(R.id.sensor, new Intent(getApplicationContext(),
                        MainActivity.class)
                        .putExtra(MainActivity.PARAM_STATION, mInfo.station.toBundle()));
                return remoteViews;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
