package io.github.hazyair.gui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.location.Location;
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
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.hazyair.R;
import io.github.hazyair.data.HazyairProvider;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.data.StationsLoader;
import io.github.hazyair.source.iface.StationsCallback;
import io.github.hazyair.source.Source;
import io.github.hazyair.source.Station;

import android.support.v4.app.DatabaseService;

import io.github.hazyair.util.License;
import io.github.hazyair.util.Network;
import io.github.hazyair.util.Preference;
import io.github.hazyair.util.Text;
import io.github.hazyair.widget.AppWidget;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_SWIPE;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static io.github.hazyair.util.Location.PERMISSION_REQUEST_FINE_LOCATION;

public class StationsActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    // Final definitions
    private final static String PARAM_ALL_STATIONS_POSITION =
            "io.github.hazyair.PARAM_ALL_STATIONS_POSITION";
    private final static String PARAM_STATION_LIST_POSITION =
            "io.github.hazyair.PARAM_STATION_LIST_POSITION";
    private final static String PARAM_QUERY_STRING = "io.github.hazyair.PARAM_QUERY_STRING";
    private final static String PARAM_ICONIFIED = "io.github.hazyair.PARAM_ICONIFIED";

    // Nested classes definitions
    class ViewHolder extends RecyclerView.ViewHolder {

        final int viewType;

        @Nullable
        @BindView(R.id.cardview)
        CardView card;

        @Nullable
        @BindView(R.id.place)
        TextView place;

        @Nullable
        @BindView(R.id.address)
        TextView address;

        @Nullable
        @BindView(R.id.station)
        TextView station;

        @Nullable
        @BindView(R.id.distance)
        TextView distance;

        int _id;

        ViewHolder(View itemView, int viewType) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.viewType = viewType;
        }
    }

    class StationListAdapter extends RecyclerView.Adapter<ViewHolder> {

        static final int VIEW_TYPE_SELECTED = 0;
        static final int VIEW_TYPE_DIVIDER = 1;
        static final int VIEW_TYPE_ALL = 2;

        private List<Station> mStations;
        private Cursor mCursor;

        private boolean mDistance;
        private Location mLocation;
        private final boolean mDivider;

        StationListAdapter(boolean distance, boolean divider) {
            super();
            mDistance = distance;
            mDivider = divider;
        }

        void setCursor(Cursor cursor) {
            mCursor = cursor;
            if (mCursor != null) notifyDataSetChanged();
        }

        Cursor getCursor() {
            return mCursor;
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
        public int getItemViewType(int position) {
            int count = (mCursor == null ? 0 : mCursor.getCount());
            if (position < count) {
                return VIEW_TYPE_SELECTED;
            }
            int size = (mStations == null ? 0 : mStations.size());
            if (mDivider && position == count) {
                return VIEW_TYPE_DIVIDER;
            }
            if (position - count - (mDivider ? 1 : 0) < size) {
                return VIEW_TYPE_ALL;
            }
            return -1;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int resource = R.layout.station;
            if (viewType == VIEW_TYPE_DIVIDER) resource = R.layout.divider;
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(resource, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int viewType = holder.viewType;
            switch (viewType) {
                case VIEW_TYPE_SELECTED: {
                    if (mCursor == null || !mCursor.moveToPosition(holder.getLayoutPosition()))
                        break;
                    Bundle station = Station.toBundleFromCursor(mCursor);
                    if (holder.place == null) return;
                    holder.place.setText(String.format("%s %s",
                            getString(station.getInt(StationsContract.COLUMN_COUNTRY)),
                            station.getString(StationsContract.COLUMN_LOCALITY)));
                    if (holder.address == null) return;
                    holder.address.setText(station.getString(StationsContract.COLUMN_ADDRESS));
                    if (holder.station == null) return;
                    holder.station.setText(String.format("%s %s",
                            getString(R.string.text_station_by),
                            getString(station.getInt(StationsContract.COLUMN_SOURCE))));
                    if (holder.distance == null) return;
                    holder.distance.setVisibility(mDistance ? View.VISIBLE : View.GONE);
                    Location location =
                            new Location(getString(station.getInt(StationsContract.COLUMN_SOURCE)));
                    location.setLongitude(station.getDouble(StationsContract.COLUMN_LONGITUDE));
                    location.setLatitude(station.getDouble(StationsContract.COLUMN_LATITUDE));
                    if (mDistance && mLocation != null)
                        holder.distance.setText(String.format("%s %s",
                                String.valueOf((int) (location.distanceTo(mLocation) / 1000)),
                                getString(R.string.text_km)));
                    holder._id = station.getInt(StationsContract.COLUMN__ID);
                    if (holder.card == null) return;
                    holder.card.setCardBackgroundColor(getColor(R.color.accent));
                    holder.place.setTextColor(getColor(R.color.textLighter));
                    holder.address.setTextColor(getColor(R.color.textLight));
                    holder.station.setTextColor(getColor(R.color.textLight));
                    holder.distance.setTextColor(getColor(R.color.textLight));
                    holder.itemView.setOnClickListener((v) -> {
                        Intent intent = new Intent(StationsActivity.this,
                                MainActivity.class);
                        intent.putExtra(MainActivity.PARAM_STATION, station);
                        NavUtils.navigateUpTo(StationsActivity.this, intent);
                    });

                }
                break;
                case VIEW_TYPE_DIVIDER: {
                    int count = (mCursor == null ? 0 : mCursor.getCount());
                    int size = (mStations == null ? 0 : mStations.size());
                    if (count > 0 && size > 0) {
                        holder.itemView.setVisibility(View.VISIBLE);
                    } else {
                        holder.itemView.setVisibility(View.GONE);
                    }
                }
                break;
                case VIEW_TYPE_ALL: {
                    if (holder.card == null) break;
                    int layoutPosition = holder.getLayoutPosition() -
                            (mCursor == null ? 0 : mCursor.getCount()) - (mDivider ? 1 : 0);
                    Station station = mStations.get(layoutPosition);
                    if (holder.place == null) break;
                    holder.place.setText(String.format("%s %s",
                            getString(station.country), station.locality));
                    if (holder.address == null) break;
                    holder.address.setText(station.address);
                    if (holder.station == null) break;
                    holder.station.setText(String.format("%s %s",
                            getString(R.string.text_station_by), getString(station.source)));
                    if (holder.distance == null) break;
                    holder.distance.setVisibility(mDistance ? View.VISIBLE : View.GONE);
                    Location location = new Location(getString(station.source));
                    location.setLongitude(station.longitude);
                    location.setLatitude(station.latitude);
                    if (mDistance && mLocation != null)
                        holder.distance.setText(String.format("%s %s",
                                String.valueOf((int) (location.distanceTo(mLocation) / 1000)),
                                getString(R.string.text_km)));
                    if (station._status) {
                        holder.card.setCardBackgroundColor(getColor(R.color.accent));
                        holder.place.setTextColor(getColor(R.color.textLighter));
                        holder.address.setTextColor(getColor(R.color.textLight));
                        holder.station.setTextColor(getColor(R.color.textLight));
                        holder.distance.setTextColor(getColor(R.color.textLight));
                    } else {
                        if (HazyairProvider.Stations.selected(StationsActivity.this,
                                station)) {
                            holder.card.setCardBackgroundColor(getColor(R.color.primaryLight));
                            holder.place.setTextColor(getColor(R.color.textLighter));
                            holder.address.setTextColor(getColor(R.color.textLight));
                            holder.station.setTextColor(getColor(R.color.textLight));
                            holder.distance.setTextColor(getColor(R.color.textLight));
                        } else {
                            holder.card.setCardBackgroundColor(getColor(android.R.color.white));
                            holder.place.setTextColor(getColor(R.color.textDarker));
                            holder.address.setTextColor(getColor(R.color.textDark));
                            holder.station.setTextColor(getColor(R.color.textDark));
                            holder.distance.setTextColor(getColor(R.color.textDark));
                        }
                        holder.itemView.setOnClickListener((v) -> {
                            if (Network.isAvailable(v.getContext())) {
                                mSwipeRefreshLayout.setRefreshing(true);
                                setEnabled(false);
                                station._status = true;
                                holder.card.setCardBackgroundColor(getColor(R.color.accent));
                                holder.place.setTextColor(getColor(R.color.textLighter));
                                holder.address.setTextColor(getColor(R.color.textLight));
                                holder.station.setTextColor(getColor(R.color.textLight));
                                holder.distance.setTextColor(getColor(R.color.textLight));
                                DatabaseService.insertOrDelete(StationsActivity.this,
                                        layoutPosition, station);
                            } else {
                                Network.showWarning(v.getContext());
                            }
                        });
                    }
                }
                break;
            }
        }

        @Override
        public int getItemCount() {
            return (mCursor == null ? 0 : mCursor.getCount()) + (mDivider ? 1 : 0) +
                    (mStations == null ? 0 : mStations.size());
        }
    }

    class SwipeController extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, LEFT | RIGHT);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            ViewHolder holder = (ViewHolder) viewHolder;
            Bundle station = DatabaseService.selectedStation(StationsActivity.this);
            if (station != null && station.getInt(StationsContract.COLUMN__ID) == holder._id)
                DatabaseService.selectStation(StationsActivity.this, null);
            DatabaseService.delete(StationsActivity.this, holder._id);
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
                if (holder.viewType == StationListAdapter.VIEW_TYPE_SELECTED) {
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

    private class StationListItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int itemCount = state.getItemCount();

            int itemPosition = parent.getChildLayoutPosition(view);

            if (itemPosition == RecyclerView.NO_POSITION) {
                return;
            }
            if (itemCount > 0) {
                int padding = getResources().getDimensionPixelSize(R.dimen.edge);
                if (((mStationListAdapter.getCursor() != null &&
                        mStationListAdapter.getStations() != null &&
                        itemPosition == mStationListAdapter.getCursor().getCount() - 1) ||
                        itemPosition == itemCount - 1)) {
                    outRect.set(0, 0, padding, padding);
                } else if (mStationListAdapter.getCursor() == null ||
                        itemPosition != mStationListAdapter.getCursor().getCount()) {
                    outRect.set(0, 0, padding, 0);
                }

            }

        }

    }

    private class AllStationsItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int itemCount = state.getItemCount();

            int itemPosition = parent.getChildLayoutPosition(view);

            if (itemPosition == RecyclerView.NO_POSITION) {
                return;
            }
            if (itemCount > 0) {
                int padding = getResources().getDimensionPixelSize(R.dimen.edge);
                if (mAdapter.getStations() != null) {
                    int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
                    if (mAdapter.getStations() != null && (itemPosition + 1) % spanCount == 0) {
                        outRect.set(0, 0, padding, 0);
                    }
                    if (mAdapter.getStations() != null &&
                            itemPosition + 1 > itemCount - spanCount) {
                        outRect.set(0, 0, 0, padding);
                    }
                }
            }

        }
    }

    // Class members
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case DatabaseService.ACTION_UPDATING:
                    mSwipeRefreshLayout.setRefreshing(true);
                    break;
                case DatabaseService.ACTION_UPDATED:
                    int position = intent.getIntExtra(DatabaseService.PARAM_POSITION,
                            -1);
                    if (position == -1) {
                        if (mTwoPane) {
                            mAdapter.notifyDataSetChanged();
                        } else {
                            mStationListAdapter.notifyDataSetChanged();
                        }
                    } else {
                        List<Station> stations;
                        if (mTwoPane) {
                            stations = mAdapter.getStations();
                            if (stations != null && stations.size() > 0)
                                stations.get(position)._status = false;
                            mAdapter.notifyDataSetChanged();
                            setEnabled(true);
                        } else {
                            stations = mStationListAdapter.getStations();
                            if (stations != null && stations.size() > 0)
                                stations.get(position)._status = false;
                            mStationListAdapter.notifyDataSetChanged();
                            setEnabled(true);
                        }
                    }
                    mSwipeRefreshLayout.setRefreshing(false);
                    String message = intent.getStringExtra(DatabaseService.PARAM_MESSAGE);
                    if (message != null) {
                        DatabaseService.showWarning(context, message);
                    }
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    if (Network.isAvailable(StationsActivity.this)) {
                        mSwipeRefreshLayout.setRefreshing(true);
                        getStations(true);
                    } else {
                        Network.showWarning(StationsActivity.this);
                    }
                    break;
                case DatabaseService.ACTION_SELECTED:
                    Preference.putInfo(StationsActivity.this,
                            intent.getParcelableExtra(DatabaseService.PARAM_INFO));
                    AppWidget.update(StationsActivity.this);
                    break;
            }
        }
    };

    private StationListAdapter mStationListAdapter;

    private StationListAdapter mAdapter;

    private SearchView mSearchView;

    private LocationCallback mLocationCallback;

    private LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private boolean mTwoPane;

    private int mAllStationsPosition;

    private int mStationListPosition;

    private String mQueryString;

    private boolean mIconified = true;

    private List<Station> mStations;

    // ButterKnife
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.stations)
    RecyclerView mStationList;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    @BindView(R.id.all_stations)
    RecyclerView mAllStations;

    // Activity lifecycle
    private void setEnabled(boolean enabled) {
        if (mTwoPane && mAllStations != null) {
            for (int i = 0; i < mAllStations.getChildCount(); i++) {
                View child = mAllStations.getChildAt(i);
                child.setEnabled(enabled);
            }

        }
        if (mStationList != null) {
            for (int i = 0; i < mStationList.getChildCount(); i++) {
                View child = mStationList.getChildAt(i);
                child.setEnabled(enabled);
            }
        }
    }

    private void getStations(boolean scroll) {
        setEnabled(false);
        Source.with(StationsActivity.this).load(Source.Type.GIOS).into(new StationsCallback() {
            @Override
            public void onSuccess(List<Station> stations) {
                if (mTwoPane) {
                    mAdapter.setStations(stations);
                    if (scroll && mAllStations != null) {
                        RecyclerView.LayoutManager layoutManager
                                = mAllStations.getLayoutManager();
                        if (layoutManager instanceof GridLayoutManager) {
                            ((GridLayoutManager) layoutManager)
                                    .scrollToPositionWithOffset(mAllStationsPosition,
                                            0);
                        }
                    }
                } else {
                    if (mStationListAdapter != null) mStationListAdapter.setStations(stations);
                }
                if (scroll && mStationList != null) {
                    RecyclerView.LayoutManager layoutManager
                            = mStationList.getLayoutManager();
                    if (layoutManager instanceof LinearLayoutManager) {
                        ((LinearLayoutManager) layoutManager)
                                .scrollToPositionWithOffset(mStationListPosition,
                                        0);
                    }
                }
                if (!mIconified) {
                    mStations = stations;
                    if (mTwoPane) query(mAdapter, mQueryString);
                    else query(mStationListAdapter, mQueryString);
                }
                if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(false);
                setEnabled(true);
            }

            @Override
            public void onError() {
                mSwipeRefreshLayout.setRefreshing(false);
                setEnabled(true);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations);
        ButterKnife.bind(this);

        mTwoPane = getResources().getBoolean(R.bool.two_pane);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (mTwoPane) {
                        mAdapter.setLocation(location);
                    }
                    mStationListAdapter.setLocation(location);
                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                if (mTwoPane) {
                    mAdapter.setDistance(locationAvailability.isLocationAvailable());
                }
                mStationListAdapter.setDistance(locationAvailability.isLocationAvailable());
            }
        };
        mLocationRequest = io.github.hazyair.util.Location.createLocationRequest();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_add_station);
        }


        if (savedInstanceState != null) {
            if (mTwoPane) mAllStationsPosition =
                    savedInstanceState.getInt(PARAM_ALL_STATIONS_POSITION, 0);
            mStationListPosition =
                    savedInstanceState.getInt(PARAM_STATION_LIST_POSITION, 0);
            mQueryString = savedInstanceState.getString(PARAM_QUERY_STRING);
            mIconified = savedInstanceState.getBoolean(PARAM_ICONIFIED);
        }

        if (mTwoPane) {
            mAdapter = new StationListAdapter(
                    io.github.hazyair.util.Location.checkPermission(this), false);
            if (mAllStations != null) {
                mAllStations.setAdapter(mAdapter);
                mAllStations.addItemDecoration(new AllStationsItemDecoration());
                ((GridLayoutManager) mAllStations.getLayoutManager())
                        .setSpanCount((int) ((float) getResources().getDisplayMetrics().widthPixels /
                                getResources().getDimensionPixelSize(R.dimen.panel) - 1));
            }

        }

        mStationListAdapter = new StationListAdapter(
                io.github.hazyair.util.Location.checkPermission(this), true);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (Network.isAvailable(StationsActivity.this)) {
                getStations(false);
            } else {
                mSwipeRefreshLayout.setRefreshing(false);
                Network.showWarning(StationsActivity.this);
            }
        });

        mStationList.setAdapter(mStationListAdapter);
        mStationList.addItemDecoration(new StationListItemDecoration());
        SwipeController swipeController = new SwipeController();
        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(mStationList);
        getSupportLoaderManager().initLoader(0, null,
                new LoaderManager.LoaderCallbacks<Cursor>() {
                    @NonNull
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                        return StationsLoader.newInstanceForAllStations(StationsActivity.this);
                    }


                    @Override
                    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
                        mStationListAdapter.setCursor(data);
                    }

                    @Override
                    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
                        mStationListAdapter.setCursor(null);
                    }

                });
    }

    @Override
    protected void onDestroy() {
        mSearchView = null;
        mLocationCallback = null;
        mLocationRequest = null;
        mFusedLocationProviderClient = null;
        mSwipeRefreshLayout = null;
        mStationListAdapter = null;
        mStationList.setAdapter(null);
        mStationList = null;
        if (mTwoPane) {
            if (mAllStations != null)
                mAllStations.setAdapter(null);
            mAllStations = null;
            mAdapter = null;
        }
        super.onDestroy();
    }

    private void query(StationListAdapter stationListAdapter, String newText) {
        if (mStations == null) mStations = stationListAdapter.getStations();
        if (mStations == null || mStations.size() == 0) return;
        stationListAdapter.setStations(Stream.of(mStations)
                .filter(p -> Text.contains(p.locality, newText) ||
                        Text.contains(p.address, newText) ||
                        Text.contains(getString(p.country), newText))
                .collect(Collectors.toList()));
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


        mSearchView.setIconified(mIconified);
        if (!mIconified) {
            mSearchView.setQuery(mQueryString, false);
        }

        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnSearchClickListener((view) -> mIconified = false);
        mSearchView.setOnCloseListener(() -> {
            mIconified = true;
            return false;
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_license:
                License.showLicense(this);
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
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DatabaseService.ACTION_UPDATING);
        intentFilter.addAction(DatabaseService.ACTION_UPDATED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(DatabaseService.ACTION_SELECTED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        io.github.hazyair.util.Location.requestUpdates(this,
                mFusedLocationProviderClient, mLocationRequest, mLocationCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        io.github.hazyair.util.Location.removeUpdates(this, mFusedLocationProviderClient,
                mLocationCallback);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTwoPane && mAllStations != null &&
                mAllStations.getLayoutManager() instanceof GridLayoutManager) {
            outState.putInt(PARAM_ALL_STATIONS_POSITION,
                    ((GridLayoutManager) mAllStations.getLayoutManager())
                            .findFirstCompletelyVisibleItemPosition());
        }
        if (mStationList != null && mStationList.getLayoutManager()
                instanceof LinearLayoutManager) {
            outState.putInt(PARAM_STATION_LIST_POSITION,
                    ((LinearLayoutManager) mStationList.getLayoutManager())
                            .findFirstCompletelyVisibleItemPosition());

        }
        outState.putString(PARAM_QUERY_STRING, mQueryString);
        outState.putBoolean(PARAM_ICONIFIED, mSearchView.isIconified());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    io.github.hazyair.util.Location.requestUpdates(this,
                            mFusedLocationProviderClient, mLocationRequest, mLocationCallback);

                }
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // TODO: Create normalized keywords on async thread
        if (mTwoPane) query(mAdapter, newText);
        else query(mStationListAdapter, newText);
        mQueryString = newText;
        return false;
    }

}
