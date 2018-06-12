package io.github.hazyair.gui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.net.ConnectivityManager;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.hazyair.R;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.data.StationsLoader;
import io.github.hazyair.data.HazyairProvider;
import io.github.hazyair.source.iface.StationsCallback;
import io.github.hazyair.source.Source;
import io.github.hazyair.source.Station;
import io.github.hazyair.sync.DatabaseService;
import io.github.hazyair.util.Network;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_SWIPE;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static io.github.hazyair.util.Location.PERMISSION_REQUEST_FINE_LOCATION;

public class StationsActivity extends AppCompatActivity implements LocationListener {

    @Override
    public void onLocationChanged(Location location) {
        mAdapter.setLocation(location);
        return;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        return;
    }

    @Override
    public void onProviderEnabled(String provider) {
        mAdapter.setDistance(true);
        return;
    }

    @Override
    public void onProviderDisabled(String provider) {
        mAdapter.setDistance(false);
        return;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public int viewType;

        @Nullable @BindView(R.id.place)
        TextView place;

        @Nullable @BindView(R.id.address)
        TextView address;

        @Nullable @BindView(R.id.station)
        TextView station;

        @Nullable @BindView(R.id.distance)
        TextView distance;

        public int _id;

        ViewHolder(View itemView, int viewType) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.viewType = viewType;
        }
    }

    public class Adapter extends RecyclerView.Adapter {

        public static final int VIEW_TYPE_SELECTED = 0;
        static final int VIEW_TYPE_DIVIDER = 1;
        static final int VIEW_TYPE_ALL = 2;

        private List<Station> mStations;
        private Cursor mCursor;

        private boolean mDistance;
        private Location mLocation;


        public Adapter(boolean distance) {
            super();
            mDistance = distance;
        }

        void setCursor(Cursor cursor) {
            //if (mCursor != null) mCursor.close();
            mCursor = cursor;
            if (mCursor != null) notifyDataSetChanged();
        }

        void setStations(List<Station> stations) {
            mStations = stations;
            notifyDataSetChanged();
        }

        void setDistance(boolean distance) {
            mDistance = distance;
            notifyDataSetChanged();
        }

        void setLocation(Location location) {
            mLocation = location;
            notifyDataSetChanged();
        }

        List<Station> getStations() {
            return mStations;
        }

        @Override
        public int getItemViewType(int position){
            int count = (mCursor == null ? 0 : mCursor.getCount());
            if(position < count){
                return VIEW_TYPE_SELECTED;
            }
            if(position == count) {
                return VIEW_TYPE_DIVIDER;
            }
            int size = (mStations == null ? 0 : mStations.size());
            if(position - count - 1 < size){
                return VIEW_TYPE_ALL;
            }
            return -1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int resource = R.layout.station;
            if (viewType == VIEW_TYPE_DIVIDER) resource = R.layout.divider;
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(resource, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ViewHolder viewHolder = (ViewHolder) holder;
            int viewType = viewHolder.viewType;
            Context context = viewHolder.itemView.getContext();
            switch (viewType) {
                case VIEW_TYPE_SELECTED: {
                    if (mCursor == null) break;
                    mCursor.moveToPosition(viewHolder.getAdapterPosition());
                    Bundle bundle = Station.loadBundleFromCursor(mCursor);
                    if (viewHolder.place == null) break;
                    viewHolder.place.setText(String.format("%s %s",
                            bundle.getString(StationsContract.COLUMN_COUNTRY),
                            bundle.getString(StationsContract.COLUMN_LOCALITY)));
                    if (viewHolder.address == null) break;
                    viewHolder.address.setText(bundle.getString(StationsContract.COLUMN_ADDRESS));
                    if (viewHolder.station == null) break;
                    viewHolder.station.setText(String.format("%s %s",
                            context.getString(R.string.text_station_by),
                            bundle.getString(StationsContract.COLUMN_SOURCE)));
                    if (viewHolder.distance == null) break;
                    viewHolder.distance.setVisibility(mDistance ? View.VISIBLE : View.GONE);
                    Location location = new Location(bundle.getString(StationsContract.COLUMN_SOURCE));
                    location.setLongitude(bundle.getDouble(StationsContract.COLUMN_LONGITUDE));
                    location.setLatitude(bundle.getDouble(StationsContract.COLUMN_LATITUDE));
                    if (mDistance && mLocation != null)
                        viewHolder.distance.setText(String.format("%s %s",
                                String.valueOf((int) (location.distanceTo(mLocation) / 1000)),
                                context.getString(R.string.text_km)));
                    viewHolder._id = bundle.getInt(StationsContract.COLUMN__ID);

                }
                break;
                case VIEW_TYPE_ALL: {
                    int adapterPosition = viewHolder.getAdapterPosition() -
                            (mCursor == null ? 0 : mCursor.getCount()) - 1;
                    Station station = mStations.get(adapterPosition);
                    if (viewHolder.place != null) viewHolder.place.setText(String.format("%s %s",
                            station.country, station.locality));
                    if (viewHolder.address != null) viewHolder.address.setText(station.address);
                    if (viewHolder.station != null)
                        viewHolder.station.setText(String.format("%s %s",
                                context.getString(R.string.text_station_by), station.source));
                    if (viewHolder.distance == null) break;
                    viewHolder.distance.setVisibility(mDistance ? View.VISIBLE : View.GONE);
                    Location location = new Location(station.source);
                    location.setLongitude(station.longitude);
                    location.setLatitude(station.latitude);
                    if (mDistance && mLocation != null)
                        viewHolder.distance.setText(String.format("%s %s",
                                String.valueOf((int) (location.distanceTo(mLocation) / 1000)),
                                context.getString(R.string.text_km)));
                    if (station._status) {
                        viewHolder.itemView.setBackgroundColor(context.getColor(R.color.accent));
                        viewHolder.place.setTextColor(context.getColor(R.color.textLighter));
                        viewHolder.address.setTextColor(context.getColor(R.color.textLight));
                        viewHolder.station.setTextColor(context.getColor(R.color.textLight));
                        viewHolder.distance.setTextColor(context.getColor(R.color.textLight));
                    } else {
                        if (HazyairProvider.Stations.selected(context, station)) {
                            viewHolder.itemView.setBackgroundColor(context.getColor(R.color.primaryLight));
                            viewHolder.place.setTextColor(context.getColor(R.color.textLighter));
                            viewHolder.address.setTextColor(context.getColor(R.color.textLight));
                            viewHolder.station.setTextColor(context.getColor(R.color.textLight));
                            viewHolder.distance.setTextColor(context.getColor(R.color.textLight));
                            //viewHolder.source.setTextColor(context.getColor(R.color.textLight));
                        } else {
                            viewHolder.itemView.setBackgroundColor(context.getColor(android.R.color.white));
                            viewHolder.place.setTextColor(context.getColor(R.color.textDarker));
                            viewHolder.address.setTextColor(context.getColor(R.color.textDark));
                            viewHolder.station.setTextColor(context.getColor(R.color.textDark));
                            viewHolder.distance.setTextColor(context.getColor(R.color.textDark));
                            //viewHolder.source.setTextColor(context.getColor(R.color.textDark));
                        }
                        viewHolder.itemView.setOnClickListener((v) -> {
                            mSwipeRefreshLayout.setRefreshing(true);
                            setEnabled(false);
                            station._status = true;
                            v.setBackgroundColor(context.getColor(R.color.accent));
                            viewHolder.place.setTextColor(context.getColor(R.color.textLighter));
                            viewHolder.address.setTextColor(context.getColor(R.color.textLight));
                            viewHolder.station.setTextColor(context.getColor(R.color.textLight));
                            viewHolder.distance.setTextColor(context.getColor(R.color.textLight));
                            startService(new Intent(context, DatabaseService.class)
                                    .setAction(DatabaseService.ACTION_INSERT_OR_DELETE)
                                    .putExtra(DatabaseService.PARAM_POSITION, adapterPosition)
                                    .putExtra(DatabaseService.PARAM_STATION, station));
                        });
                    }
                }
                break;
            }
        }

        @Override
        public int getItemCount() {
            return (mCursor == null ? 0 : mCursor.getCount()) + 1 +
                    (mStations == null ? 0 : mStations.size());
        }
    }


    public class SwipeController extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, LEFT | RIGHT);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // TODO: Move to AsyncTask or IntentService
            ViewHolder holder = (ViewHolder) viewHolder;
            Context context = holder.itemView.getContext();
            /*HazyairProvider.Stations.delete(context, holder._id);
            HazyairProvider.Sensors.delete(context, holder._id);*/
            //HazyairProvider.delete(context, holder._id);
            startService(new Intent(context, DatabaseService.class)
                    .setAction(DatabaseService.ACTION_DELETE)
                    .putExtra(DatabaseService.PARAM__ID, holder._id));

        }

        private boolean mSwipeBack;

        @Override
        public int convertToAbsoluteDirection(int flags, int layoutDirection) {
            if (mSwipeBack) {
                mSwipeBack = false;
                return 0;
            }
            return super.convertToAbsoluteDirection(flags, layoutDirection);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onChildDraw(Canvas c,
                                RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder,
                                float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
            if (actionState == ACTION_STATE_SWIPE) {
                ViewHolder holder = (ViewHolder) viewHolder;
                if (holder.viewType == Adapter.VIEW_TYPE_SELECTED) {
                    recyclerView.setOnTouchListener((v, event) -> {
                        mSwipeBack = false;
                        return false;
                    });
                } else {
                    recyclerView.setOnTouchListener((v, event) -> {
                        mSwipeBack = event.getAction() == MotionEvent.ACTION_CANCEL ||
                                event.getAction() == MotionEvent.ACTION_UP;
                        return false;
                    });
                }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    private Adapter mAdapter;

    private ActionBar mActionBar;

    private SearchView mSearchView;

    @BindView(R.id.swipe)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.stations)
    RecyclerView mStations;

    private LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations);
        ButterKnife.bind(this);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            // Show the Up button in the action bar.
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setTitle(R.string.title_add_station);
        }
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            Source.with(this).load(Source.Type.GIOS).into(new StationsCallback() {
                @Override
                public void onSuccess(List<Station> stations) {
                    mAdapter.setStations(stations);
                    mSwipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onError() {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        });

        mAdapter = new Adapter(io.github.hazyair.util.Location.checkPermission(this));
        mStations.setAdapter(mAdapter);
        SwipeController swipeController = new SwipeController();
        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(mStations);
        getSupportLoaderManager().initLoader(0, null,
                new LoaderManager.LoaderCallbacks<Cursor>() {
            @NonNull
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return StationsLoader.newInstanceForAllStations(StationsActivity.this);
            }


            @Override
            public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
                mAdapter.setCursor(data);
            }

            @Override
            public void onLoaderReset(@NonNull Loader<Cursor> loader) {
                mStations.setAdapter(null);
            }

        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //if (mSearchView != null && !mSearchView.isIconified()) {
                //    mSearchView.setIconified(true);
                //} else {
                    NavUtils.navigateUpFromSameTask(this);
                //}
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            mSearchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stations, menu);
        MenuItem search = menu.findItem(R.id.search);
        mSearchView = (SearchView) search.getActionView();

        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(dm);
            mSearchView.setMaxWidth(dm.widthPixels / 2);
        }
        mSearchView.setOnSearchClickListener((v) -> {
            //mActionBar.setDisplayHomeAsUpEnabled(false);
        });
        mSearchView.setOnCloseListener(() -> {
            //mActionBar.setDisplayHomeAsUpEnabled(true);
            return false;
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            List<Station> mStations;

            boolean contains(String string, String query) {
                return StringUtils.containsIgnoreCase(
                        Normalizer.normalize(string, Normalizer.Form.NFD)
                                .replaceAll("\\p{InCombiningDiacriticalMarks}+",
                                        ""), query);
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                //mActionBar.setDisplayHomeAsUpEnabled(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mStations == null) mStations = mAdapter.getStations();
                if (mStations == null || mStations.size() == 0) return false;
                // TODO: Create normalized keywords on async thread
                mAdapter.setStations(Stream.of(mStations)
                        .filter(p ->  contains(p.locality, newText) ||
                                contains(p.address, newText) ||
                                contains(p.country, newText))
                        .collect(Collectors.toList()));
                mAdapter.notifyDataSetChanged();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case DatabaseService.ACTION_DATA_UPDATED:
                    int position = intent.getIntExtra(DatabaseService.PARAM_POSITION, -1);
                    if (position == -1) return;
                    mAdapter.getStations().get(position)._status = false;
                    setEnabled(true);
                    mSwipeRefreshLayout.setRefreshing(false);
                    mAdapter.notifyDataSetChanged();
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    if (!Network.isAvailable(StationsActivity.this)) break;
                    mSwipeRefreshLayout.setRefreshing(true);
                    Source.with(StationsActivity.this).load(Source.Type.GIOS).into(new StationsCallback() {
                        @Override
                        public void onSuccess(List<Station> stations) {
                            mAdapter.setStations(stations);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }

                        @Override
                        public void onError() {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    break;
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DatabaseService.ACTION_DATA_UPDATED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void setEnabled(boolean enabled) {
        for (int i = 0; i < mStations.getChildCount(); i++) {
            View child = mStations.getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    io.github.hazyair.util.Location.requestUpdates(this,
                            mLocationManager, this);
                    // TODO Remove
                    mAdapter.setDistance(true);

                } else {

                    mAdapter.setDistance(false);

                }
                return;
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        io.github.hazyair.util.Location.requestUpdates(this, mLocationManager,
                this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        io.github.hazyair.util.Location.removeUpdates(this, mLocationManager,
                this);
    }

}
