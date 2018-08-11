package io.github.hazyair.widget;

import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import io.github.hazyair.R;
import io.github.hazyair.gui.MainActivity;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Info;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.util.Preference;
import io.github.hazyair.util.Quality;

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
                mInfo = Preference.getInfo(getApplicationContext());
            }

            @Override
            public void onDestroy() {

            }

            @Override
            public int getCount() {
                if (mInfo == null) return 0;
                return mInfo.sensors.size();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                RemoteViews remoteViews = new RemoteViews(getPackageName(),
                        R.layout.appwidget_sensor);
                if (mInfo == null) return remoteViews;
                int sensorSize = mInfo.sensors.size();
                int dataSize = mInfo.data.size();
                if (position >= sensorSize || position >= dataSize || sensorSize != dataSize)
                    return remoteViews;
                Sensor sensor = mInfo.sensors.get(position);
                Data data = mInfo.data.get(position);
                int percent = Quality.normalize(sensor.parameter, data.value);
                SpannableString text = new SpannableString(String.format("%s: %s %s (%s%%)",
                        sensor.parameter, data.value, sensor.unit, String.valueOf(percent)));
                text.setSpan(new StyleSpan(BOLD), 0, sensor.parameter.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (percent > 100) {
                    text.setSpan(new ForegroundColorSpan(getColor(android.R.color.holo_red_light)),
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
