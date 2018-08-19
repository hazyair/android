package io.github.hazyair.gui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.hazyair.R;
import io.github.hazyair.data.DataContract;
import io.github.hazyair.data.DataLoader;
import io.github.hazyair.data.SensorsContract;
import io.github.hazyair.data.SensorsLoader;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.source.Base;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Station;
import io.github.hazyair.util.LocationCallbackReference;
import io.github.hazyair.util.Quality;
import io.github.hazyair.util.Text;
import io.github.hazyair.util.Time;

public class StationFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // Final definitions
    private static final String PARAM_SENSOR_SELECTED = "io.github.hazyair.PARAM_SENSOR_SELECTED";
    private static final String PARAM_STATION_SELECTED = "io.github.hazyair.PARAM_STATION_SELECTED";

    // Nested class definitions
    class ViewHolder extends RecyclerView.ViewHolder {

        @Nullable @BindView(R.id.cardview)
        CardView cardView;

        @Nullable @BindView(R.id.map)
        FrameLayout frameLayout;

        @Nullable @BindView(R.id.expand_collapse)
        Button expandCollapse;

        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    class MapViewHolder extends ViewHolder implements OnMapReadyCallback {

        @Nullable @BindView(R.id.place)
        TextView place;

        @Nullable @BindView(R.id.distance)
        TextView distance;

        @Nullable @BindView(R.id.map)
        FrameLayout frameLayout;

        private final Bundle mStation;
        private SupportMapFragment mSupportMapFragment;

        MapViewHolder(View itemView, Bundle station) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mStation = station;
            if (place != null) {
                place.setText(Text.truncateSting(String.format("%s %s %s",
                        getString(mStation.getInt(StationsContract.COLUMN_COUNTRY)),
                        mStation.getString(StationsContract.COLUMN_LOCALITY),
                        mStation.getString(StationsContract.COLUMN_ADDRESS)), 32));
            }

        }

        void getMap() {
            if (mSupportMapFragment == null)
                mSupportMapFragment = StationMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map, mSupportMapFragment).commitAllowingStateLoss();
            mSupportMapFragment.getMapAsync(this);
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            LatLng latLng = new LatLng(mStation.getDouble(StationsContract.COLUMN_LATITUDE),
                    mStation.getDouble(StationsContract.COLUMN_LONGITUDE));
            googleMap.addMarker(new MarkerOptions().position(latLng)
                    .title(String.format("%s %s",
                            getString(R.string.text_station_by),
                            getString(mStation.getInt(StationsContract.COLUMN_SOURCE)))));
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    class SensorViewHolder extends ViewHolder {

        @Nullable @BindView(R.id.placeholder)
        ConstraintLayout constraintLayout;

        @Nullable @BindView(R.id.parameter)
        TextView parameter;

        @Nullable @BindView(R.id.result)
        TextView result;

        @Nullable @BindView(R.id.updated)
        TextView updated;

        @Nullable @BindView(R.id.expand_collapse)
        Button expandCollapse;

        @Nullable @BindView(R.id.chart)
        LineChart chart;

        SensorViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class SensorsAdapter extends RecyclerView.Adapter {

        static final int VIEW_TYPE_MAP = 0;
        static final int VIEW_TYPE_SENSOR = 1;

        private Cursor mCursor;
        private final SparseArray<Bundle> mData = new SparseArray<>();
        private final SparseArray<LongSparseArray<Double>> mChart = new SparseArray<>();
        private Location mLocation;
        private boolean mDistance;

        private final Bundle mStation;

        private Bundle mSelectedItem;
        private Bundle mSelectedItemCache;

        SensorsAdapter(Bundle station, Bundle selectedItem, boolean distance) {
            super();
            mStation = station;
            mSelectedItem = selectedItem;
            mDistance = distance;
        }

        @SuppressWarnings("deprecation")
        void setCursor(Cursor cursor) {
            if (cursor == null) {
                mCursor = null;
                return;
            }
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                Bundle bundle = Sensor.toBundleFromCursor(cursor);
                getLoaderManager().initLoader(bundle.getInt(SensorsContract.COLUMN__ID), bundle,
                        StationFragment.this);
            }
            if (mSelectedItem != null) {
                getLoaderManager().initLoader(-mSelectedItem.getInt(SensorsContract.COLUMN__ID),
                        mSelectedItem, StationFragment.this);
            }
            mCursor = cursor;
            notifyItemRangeChanged(1, getItemCount()-1);
        }

        void setData(Cursor cursor) {
            if (cursor == null || cursor.getCount() == 0) return;
            cursor.moveToFirst();
            Bundle bundle = Data.toBundleFromCursor(cursor);
            mData.put(bundle.getInt(DataContract.COLUMN__SENSOR_ID), bundle);
            notifyItemRangeChanged(1, getItemCount()-1);
        }

        void setChart(Cursor cursor) {
            if (cursor == null || cursor.getCount() == 0) return;
            LongSparseArray<Double> data = new LongSparseArray<>();
            Bundle bundle = null;
            for (int position = 0; position < cursor.getCount(); position++) {
                cursor.moveToPosition(position);
                bundle = Data.toBundleFromCursor(cursor);
                data.put(Time.getTimestamp(bundle.getLong(DataContract.COLUMN_TIMESTAMP)),
                        bundle.getDouble(DataContract.COLUMN_VALUE));
            }
            if (bundle != null) mChart.put(bundle.getInt(DataContract.COLUMN__SENSOR_ID), data);
            notifyItemRangeChanged(1, getItemCount()-1);
        }

        void setLocation(Location location) {
            mLocation = location;
            notifyItemChanged(0);
        }

        void setDistance(boolean distance) {
            mDistance = distance;
            notifyItemChanged(0);
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
                    return new MapViewHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.location, parent, false), mStation);
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
                    if (Base.equals(mSelectedItem, mStation))
                        expand(context, mapViewHolder, mStation);
                    else collapse(context, mapViewHolder);
                    if (mapViewHolder.cardView != null)
                        mapViewHolder.cardView.setOnClickListener((v) ->
                                OnClickListener(context, mapViewHolder, mStation));
                    if (mapViewHolder.expandCollapse != null)
                        mapViewHolder.expandCollapse.setOnClickListener((v) ->
                                OnClickListener(context, mapViewHolder, mStation));
                    if (mapViewHolder.distance != null) {
                        mapViewHolder.distance.setVisibility(mDistance ? View.VISIBLE : View.GONE);
                        if (mDistance && mLocation != null) {
                            Location location = new Location(
                                    getString(mStation.getInt(StationsContract.COLUMN_SOURCE)));
                            location.setLongitude(
                                    mStation.getDouble(StationsContract.COLUMN_LONGITUDE));
                            location.setLatitude(
                                    mStation.getDouble(StationsContract.COLUMN_LATITUDE));
                            mapViewHolder.distance.setText(String.format("%s %s",
                                    String.valueOf((int) (location.distanceTo(mLocation) / 1000)),
                                    getString(R.string.text_km)));
                        }
                    }
                    break;
                }
                case VIEW_TYPE_SENSOR:
                    position--;
                    SensorViewHolder sensorViewHolder = (SensorViewHolder) holder;
                    mCursor.moveToPosition(position);
                    Context context = getContext();
                    if (context == null) return;
                    Bundle sensor = Sensor.toBundleFromCursor(mCursor);
                    if (sensorViewHolder.parameter != null)
                        sensorViewHolder.parameter.setText(
                                sensor.getString(SensorsContract.COLUMN_PARAMETER));
                    Bundle data = mData.get(sensor.getInt(SensorsContract.COLUMN__ID));
                    if (data == null || data.size() == 0) {
                        if (mSelectedItem != null) mSelectedItemCache = mSelectedItem;
                        if (Sensor.equals(sensor, mSelectedItem)) mSelectedItem = null;
                        if (sensorViewHolder.cardView != null)
                            sensorViewHolder.cardView
                                    .setCardBackgroundColor(context.getColor(R.color.textLight));
                        collapse(context, sensorViewHolder);
                    } else {
                        if (mSelectedItem == null) {
                            mSelectedItem = mSelectedItemCache;
                        }
                        mSelectedItemCache = null;
                        if (sensorViewHolder.cardView != null)
                            sensorViewHolder.cardView
                                    .setCardBackgroundColor(context.getColor(android.R.color.white));
                        if (sensorViewHolder.result != null) {
                            int percent = Quality.normalize(
                                    sensor.getString(SensorsContract.COLUMN_PARAMETER),
                                    data.getDouble(DataContract.COLUMN_VALUE));
                            sensorViewHolder.result.setText(String.format(": %s %s (%s%%)",
                                    String.valueOf(data.getDouble(DataContract.COLUMN_VALUE)),
                                    sensor.getString(SensorsContract.COLUMN_UNIT),
                                    String.valueOf(percent)));
                            if (sensorViewHolder.updated != null) {
                                long timestamp = Time
                                        .getTimestamp(data.getLong(DataContract.COLUMN_TIMESTAMP));
                                long hours = (System.currentTimeMillis() - timestamp) /
                                        TimeUnit.HOURS.toMillis(1);
                                long minutes = (System.currentTimeMillis() -
                                        timestamp) %
                                        TimeUnit.HOURS.toMillis(1) /
                                        TimeUnit.MINUTES.toMillis(1);
                                sensorViewHolder.updated.setText(String.format("%s %s %s",
                                        (hours < 1 ? "<" : (minutes > 0 ? (minutes > 30 ? "<" : ">")
                                                : "")), (hours < 1 ? "1" : (minutes > 30 ?
                                                String.valueOf(hours+1) : String.valueOf(hours))),
                                        "h"));
                            }
                            if (percent > 100) {
                                sensorViewHolder.parameter.setTextColor(
                                        context.getColor(R.color.accent));
                                sensorViewHolder.result.setTextColor(
                                        context.getColor(R.color.accent));
                            } else {
                                sensorViewHolder.parameter.setTextColor(
                                        context.getColor(R.color.textDark));
                                sensorViewHolder.result.setTextColor(
                                        context.getColor(R.color.textDark));
                            }
                        }
                    }
                    LongSparseArray<Double> chart =
                            mChart.get(sensor.getInt(SensorsContract.COLUMN__ID));
                    if (chart != null && sensorViewHolder.chart != null) {
                        ArrayList<Entry> values = new ArrayList<>();
                        for (int i = 0; i < chart.size(); i++) {
                            values.add(new Entry(chart.keyAt(i), chart.valueAt(i).floatValue()));
                        }
                        XAxis xAxis = sensorViewHolder.chart.getXAxis();
                        xAxis.setLabelCount(3, true);
                        xAxis.setValueFormatter((value, axis) ->
                                new SimpleDateFormat("E HH:mm",
                                        Locale.getDefault()).format(new Date((long) value))
                        );
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        LineDataSet lineDataSet = new LineDataSet(values,
                                sensor.getString(SensorsContract.COLUMN_PARAMETER));
                        lineDataSet.setDrawCircles(false);
                        lineDataSet.setDrawValues(false);
                        lineDataSet.setColor(context.getColor(R.color.accent));
                        Description description = new Description();
                        description.setText(sensor.getString(SensorsContract.COLUMN_UNIT));
                        sensorViewHolder.chart.setDescription(description);
                        sensorViewHolder.chart.setData(new LineData(lineDataSet));
                    }
                    if (Base.equals(mSelectedItem, sensor))
                        expand(context, sensorViewHolder, sensor);
                    else collapse(context, sensorViewHolder);
                    if (sensorViewHolder.expandCollapse != null) {
                        if (data != null)
                            sensorViewHolder.expandCollapse.setOnClickListener((v) ->
                                    OnClickListener(context, sensorViewHolder, sensor));
                        else
                            sensorViewHolder.expandCollapse.setOnClickListener(null);
                    }
                    if (sensorViewHolder.cardView != null) {
                        if (data != null)
                            sensorViewHolder.cardView.setOnClickListener((v) ->
                                    OnClickListener(context, sensorViewHolder, sensor));
                        else
                            sensorViewHolder.cardView.setOnClickListener(null);
                    }
            }
        }

        private void collapse(Context context, ViewHolder vh) {
            if (vh instanceof MapViewHolder && vh.frameLayout != null) {
                vh.frameLayout.setVisibility(View.GONE);
            }
            if (vh instanceof SensorViewHolder) {
                SensorViewHolder svh = (SensorViewHolder) vh;
                if (svh.chart != null)
                    svh.chart.setVisibility(View.GONE);
            }
            if (vh != null && vh.expandCollapse != null)
                vh.expandCollapse.setBackground(context
                        .getDrawable(R.drawable.ic_keyboard_arrow_down_black_24dp));
        }

        private void expand(Context context, ViewHolder viewHolder, Bundle bundle) {

            if (viewHolder.expandCollapse != null)
                viewHolder.expandCollapse.setBackground(
                        context.getDrawable(R.drawable.ic_keyboard_arrow_up_black_24dp));
            if (viewHolder instanceof MapViewHolder && viewHolder.frameLayout != null) {
                ((MapViewHolder) viewHolder).getMap();
                viewHolder.frameLayout.setVisibility(View.VISIBLE);
                mSelectedItem = bundle;
            }
            if (viewHolder instanceof  SensorViewHolder) {
                SensorViewHolder sensorViewHolder = (SensorViewHolder) viewHolder;
                if (sensorViewHolder.chart != null) {
                    sensorViewHolder.chart.setNoDataText(
                            getString(R.string.chart_no_data_available));
                    sensorViewHolder.chart.setVisibility(View.VISIBLE);
                    sensorViewHolder.chart.invalidate();
                    mSelectedItem = bundle;
                }
            }

        }

        @SuppressWarnings("deprecation")
        private void OnClickListener(Context context, ViewHolder viewHolder, Bundle bundle) {
            boolean visibility = false;
            if (viewHolder instanceof MapViewHolder && viewHolder.frameLayout != null) {
                visibility = viewHolder.frameLayout.getVisibility() == View.VISIBLE;
            }
            if (viewHolder instanceof SensorViewHolder) {
                SensorViewHolder sensorViewHolder = (SensorViewHolder) viewHolder;
                if (sensorViewHolder.chart == null) return;
                visibility = sensorViewHolder.chart.getVisibility() == View.VISIBLE;
            }
            if (visibility) {
                if (viewHolder.expandCollapse != null)
                    viewHolder.expandCollapse.setBackground(
                            context.getDrawable(R.drawable.ic_keyboard_arrow_down_black_24dp));
                if (viewHolder instanceof MapViewHolder)
                    viewHolder.frameLayout.setVisibility(View.GONE);
                if (viewHolder instanceof SensorViewHolder) {
                    SensorViewHolder sensorViewHolder = (SensorViewHolder) viewHolder;
                    if (sensorViewHolder.chart != null)
                        sensorViewHolder.chart.setVisibility(View.GONE);
                }
                mSelectedItem = null;
            } else {
                for (int i = 0; i < getItemCount(); i++) {
                    collapse(context,
                            (ViewHolder) mRecyclerView.findViewHolderForLayoutPosition(i));
                }
                if (viewHolder instanceof SensorViewHolder)
                    getLoaderManager().initLoader(-bundle.getInt(SensorsContract.COLUMN__ID),
                            bundle, StationFragment.this);
                expand(context, viewHolder, bundle);
            }
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 1 : mCursor.getCount() + 1;
        }

        Bundle getSelectedItem() {
            return mSelectedItem;
        }
    }

    private SensorsAdapter mSensorsAdapter;

    private LocationCallback mLocationCallback;

    private LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    // ButterKnife
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.sensors)
    RecyclerView mRecyclerView;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case Intent.ACTION_TIME_CHANGED:
                case Intent.ACTION_TIME_TICK:
                case Intent.ACTION_TIMEZONE_CHANGED:
                    mSensorsAdapter.notifyItemRangeChanged(1,
                            mSensorsAdapter.getItemCount()-1);
                    break;
            }
        }
    };

    // Fragment initialization
    public StationFragment() {
    }

    public static StationFragment newInstance(Cursor cursor, StationFragment oldFragment) {
        StationFragment newFragment = new StationFragment();
        Bundle bundle = new Bundle();
        if (cursor != null)
            bundle.putBundle(PARAM_STATION_SELECTED, Station.toBundleFromCursor(cursor));
        if (oldFragment != null && oldFragment.mSensorsAdapter != null)
            bundle.putBundle(PARAM_SENSOR_SELECTED, oldFragment.mSensorsAdapter.getSelectedItem());
        newFragment.setArguments(bundle);
        return newFragment;
    }

    // Fragment lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        if (context != null) {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

            mLocationCallback = new LocationCallbackReference(new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        mSensorsAdapter.setLocation(location);
                    }
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    super.onLocationAvailability(locationAvailability);
                    mSensorsAdapter.setDistance(locationAvailability.isLocationAvailable());
                }
            });
            mLocationRequest = io.github.hazyair.util.Location.createLocationRequest();
        }
    }

    @Override
    public void onDestroy() {
        mRecyclerView = null;
        mLocationCallback = null;
        mLocationRequest = null;
        mFusedLocationProviderClient = null;
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, rootView);
        Bundle bundle = getArguments();
        Bundle station = null;
        Bundle sensor = null;
        if (bundle != null) {
            station = bundle.getBundle(PARAM_STATION_SELECTED);
            sensor = bundle.getBundle(PARAM_SENSOR_SELECTED);
        }
        if (savedInstanceState != null) sensor = savedInstanceState.getBundle(PARAM_SENSOR_SELECTED);
        mSensorsAdapter = new SensorsAdapter(station, sensor,
                io.github.hazyair.util.Location.checkPermission(getActivity(), false));
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mSensorsAdapter);
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                int itemCount = state.getItemCount();

                final int itemPosition = parent.getChildLayoutPosition(view);

                if (itemPosition == RecyclerView.NO_POSITION) {
                    return;
                }

                int fab = 0;
                Activity activity = getActivity();
                if (activity != null)
                    fab = activity.findViewById(R.id.fab_add_station).getMeasuredHeight();

                Resources resources = getResources();
                if (itemCount > 1 && itemPosition == itemCount - 1) {
                    outRect.set(0, 0, 0,
                            resources.getDimensionPixelSize(R.dimen.edge) +
                                    resources.getDimensionPixelSize(R.dimen.fab_margin) + fab);
                }
            }
        });

        getLoaderManager().initLoader(0, station, this);

        return rootView;
    }

    public void requestUpdates() {
        io.github.hazyair.util.Location.requestUpdates(getContext(), mFusedLocationProviderClient,
                mLocationRequest,mLocationCallback);
    }

    public void removeUpdates() {
        io.github.hazyair.util.Location.removeUpdates(getContext(), mFusedLocationProviderClient,
                mLocationCallback);
    }

    @Override
    public void onStart() {
        super.onStart();
        Context context = getContext();
        if (context == null) return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        context.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        Context context = getContext();
        if (context == null) return;
        context.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        requestUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        removeUpdates();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSensorsAdapter != null)
            outState.putBundle(PARAM_SENSOR_SELECTED, mSensorsAdapter.getSelectedItem());
    }

    // Loader handlers
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
        mSensorsAdapter.setCursor(null);
    }

}
