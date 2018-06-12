package io.github.hazyair.gui;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.maps.SupportMapFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.hazyair.R;
import io.github.hazyair.data.DataContract;
import io.github.hazyair.data.DataLoader;
import io.github.hazyair.data.SensorsContract;
import io.github.hazyair.data.SensorsLoader;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Station;
import timber.log.Timber;

public class StationFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == 0) {
            return SensorsLoader.newInstanceForAllSensorsFromStation(getContext(),
                    args == null ? 0 : args.getInt(StationsContract.COLUMN__ID));
        } else if (id > 0) {
            return DataLoader.newInstanceForLastDataFromSensor(getContext(),
                    args == null ? 0 : args.getInt(SensorsContract.COLUMN__ID));
        } else {
            return DataLoader.newInstanceForAllDataFromSensor(getContext(),
                    args == null ? 0 : args.getInt(SensorsContract.COLUMN__ID));
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        int id = loader.getId();
        if (id == 0) {
            mSensorsAdapter.setCursor(cursor);
        } else if (id > 0) {
            mSensorsAdapter.setData(cursor);
        } else {
            mSensorsAdapter.setChart(cursor);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    public class MapViewHolder extends RecyclerView.ViewHolder {

        @Nullable @BindView(R.id.location)
        CardView cardView;

        @Nullable @BindView(R.id.expand_collapse)
        Button expandCollapse;

        public SupportMapFragment supportMapFragment;

        public MapViewHolder(Fragment fragment, View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            supportMapFragment = (SupportMapFragment) fragment
                    .getFragmentManager().findFragmentById(R.id.map);
        }
    }

    public class SensorViewHolder extends RecyclerView.ViewHolder {

        @Nullable @BindView(R.id.sensor)
        CardView cardView;

        @Nullable @BindView(R.id.placeholder)
        ConstraintLayout constraintLayout;

        @Nullable @BindView(R.id.parameter)
        TextView parameter;

        @Nullable @BindView(R.id.result)
        TextView result;

        @Nullable @BindView(R.id.expand_collapse)
        Button expandCollapse;

        @Nullable @BindView(R.id.chart)
        LineChart chart;

        SensorViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private static View mView;

    private class SensorsAdapter extends RecyclerView.Adapter {

        static final int VIEW_TYPE_MAP = 0;
        static final int VIEW_TYPE_SENSOR = 1;

        private Cursor mCursor;
        private SparseArray<Bundle> mData = new SparseArray<>();
        private SparseArray<LongSparseArray<Double>> mChart = new SparseArray<>();

        private Fragment mFragment;

        SensorsAdapter(Fragment fragment) {
            mFragment = fragment;
        }

        public void setCursor(Cursor cursor) {
            if (cursor == null || cursor.getCount() == 0) return;
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                Bundle bundle = Sensor.loadBundleFromCursor(cursor);
                getLoaderManager().initLoader(bundle.getInt(SensorsContract.COLUMN__ID), bundle,
                        StationFragment.this);
                getLoaderManager().initLoader(-bundle.getInt(SensorsContract.COLUMN__ID), bundle,
                        StationFragment.this);
            }
            mCursor = cursor;
            notifyDataSetChanged();
        }

        public void setData(Cursor cursor) {
            if (cursor == null || cursor.getCount() == 0) return;
            cursor.moveToFirst();
            Bundle bundle = Data.loadBundleFromCursor(cursor);
            mData.put(bundle.getInt(DataContract.COLUMN__SENSOR_ID), bundle);
            notifyDataSetChanged();
        }

        public void setChart(Cursor cursor) {
            if (cursor == null || cursor.getCount() == 0) return;
            LongSparseArray<Double> data = new LongSparseArray<>();
            Bundle bundle = null;
            for (int position = 0; position < cursor.getCount(); position++) {
                cursor.moveToPosition(position);
                bundle = Data.loadBundleFromCursor(cursor);
                data.put(bundle.getLong(DataContract.COLUMN_TIMESTAMP),
                        bundle.getDouble(DataContract.COLUMN_VALUE));
            }
            if (bundle != null) mChart.put(bundle.getInt(DataContract.COLUMN__SENSOR_ID), data);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return VIEW_TYPE_MAP;
            } else {
                return VIEW_TYPE_SENSOR;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_MAP:
                    if (mView != null) {
                        ViewGroup p = (ViewGroup) mView.getParent();
                        if (p != null) p.removeView(mView);
                    }
                    try {
                        mView = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.location, parent, false);
                    } catch (InflateException e) {
                        Timber.e(e);
                    }
                    return new MapViewHolder(mFragment, mView);
                case VIEW_TYPE_SENSOR:
                default:
                    return new SensorViewHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.sensor, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MAP: {
                    Context context = getContext();
                    if (context == null) return;
                    MapViewHolder mapViewHolder = (MapViewHolder) holder;
                    if (mapViewHolder.cardView != null)
                        mapViewHolder.cardView.setOnClickListener((v) ->
                                OnClickListener(context, mapViewHolder));
                    if (mapViewHolder.expandCollapse != null)
                        mapViewHolder.expandCollapse.setOnClickListener((v) ->
                                OnClickListener(context, mapViewHolder));
                    break;
                }
                case VIEW_TYPE_SENSOR:
                    position--;
                    SensorViewHolder sensorViewHolder = (SensorViewHolder) holder;
                    mCursor.moveToPosition(position);
                    Context context = getContext();
                    if (context == null) return;
                    Bundle bundle = Sensor.loadBundleFromCursor(mCursor);
                    if (sensorViewHolder.parameter != null)
                        sensorViewHolder.parameter.setText(bundle.getString(SensorsContract.COLUMN_PARAMETER));
                    Bundle data = mData.get(bundle.getInt(SensorsContract.COLUMN__ID));
                    if (data != null) {
                        if (sensorViewHolder.result != null)
                            sensorViewHolder.result.setText(String.format("%s %s",
                                    String.valueOf(data.getDouble(DataContract.COLUMN_VALUE)),
                                    bundle.getString(SensorsContract.COLUMN_UNIT)));
                    }
                    LongSparseArray<Double> chart = mChart.get(bundle.getInt(SensorsContract.COLUMN__ID));
                    if (chart != null && sensorViewHolder.chart != null) {
                        ArrayList<Entry> values = new ArrayList<>();
                        for (int i = 0; i < chart.size(); i++) {
                            values.add(new Entry(chart.keyAt(i), chart.valueAt(i).floatValue()));
                        }
                        XAxis xAxis = sensorViewHolder.chart.getXAxis();
                        //xAxis.setGranularity(1);
                        xAxis.setLabelCount(3, true);
                        xAxis.setValueFormatter((value, axis) ->
                                new SimpleDateFormat("E HH:mm",
                                        Locale.getDefault()).format(new Date((long) value))
                        );
                        //xAxis.setDrawGridLines(false);
                        //xAxis.setLabelRotationAngle(15);
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        //LimitLine limitLine = new LimitLine();
                        //xAxis.addLimitLine();
                        //YAxis yAxis = sensorViewHolder.chart.getAxisRight();
                        //yAxis.setDrawLabels(false);
                        LineDataSet lineDataSet = new LineDataSet(values,
                                bundle.getString(SensorsContract.COLUMN_PARAMETER));
                        lineDataSet.setDrawCircles(false);
                        lineDataSet.setDrawValues(false);
                        lineDataSet.setColor(context.getColor(R.color.accent));
                        Description description = new Description();
                        description.setText(bundle.getString(SensorsContract.COLUMN_UNIT));
                        sensorViewHolder.chart.setDescription(description);
                        sensorViewHolder.chart.setData(new LineData(lineDataSet));
                        //sensorViewHolder.chart.setEnabled(false);
                        //sensorViewHolder.chart.notifyDataSetChanged();
                        //this.


                    }
                    if (sensorViewHolder.expandCollapse != null)
                        sensorViewHolder.expandCollapse.setOnClickListener((v) ->
                                OnClickListener(context, sensorViewHolder));
                    if (sensorViewHolder.cardView != null)
                        sensorViewHolder.cardView.setOnClickListener((v) ->
                                OnClickListener(context, sensorViewHolder));
            }
        }

        private void OnClickListener(Context context, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof MapViewHolder) {
                MapViewHolder mapViewHolder = (MapViewHolder) viewHolder;
                mapViewHolder.supportMapFragment.getView().setVisibility(View.GONE);
            }
            if (viewHolder instanceof SensorViewHolder) {
                SensorViewHolder sensorViewHolder = (SensorViewHolder) viewHolder;
                if (sensorViewHolder.chart == null) return;
                int visibility = sensorViewHolder.chart.getVisibility();
                if (visibility == View.GONE) {
                    for (int i = 1; i < getItemCount(); i++) {
                        SensorViewHolder svh = (SensorViewHolder) mRecyclerView
                                .findViewHolderForAdapterPosition(i);
                        if (svh == null) continue;
                        if (svh.chart != null)
                            svh.chart.setVisibility(View.GONE);
                        if (svh.expandCollapse != null)
                            svh.expandCollapse.setBackground(context
                                    .getDrawable(R.drawable.ic_keyboard_arrow_down_black_24dp));
                    }
                    if (sensorViewHolder.expandCollapse != null)
                        sensorViewHolder.expandCollapse.setBackground(
                                context.getDrawable(R.drawable.ic_keyboard_arrow_up_black_24dp));
                    sensorViewHolder.chart.setVisibility(View.VISIBLE);
                } else {
                    sensorViewHolder.chart.setVisibility(View.GONE);
                    if (sensorViewHolder.expandCollapse != null)
                        sensorViewHolder.expandCollapse.setBackground(
                                context.getDrawable(R.drawable.ic_keyboard_arrow_down_black_24dp));
                }
            }
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }
    }

    @BindView(R.id.sensors)
    RecyclerView mRecyclerView;

    private SensorsAdapter mSensorsAdapter;

    public StationFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static StationFragment newInstance(Cursor cursor) {
        StationFragment fragment = new StationFragment();
        fragment.setArguments(Station.loadBundleFromCursor(cursor));
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, rootView);
        mSensorsAdapter = new SensorsAdapter(this);
        mRecyclerView.setAdapter(mSensorsAdapter);

        getLoaderManager().initLoader(0, getArguments(), this);

        return rootView;
    }
}
