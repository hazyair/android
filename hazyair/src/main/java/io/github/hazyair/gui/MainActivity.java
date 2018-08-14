package io.github.hazyair.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;
import io.github.hazyair.R;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.data.StationsLoader;
import io.github.hazyair.service.NotificationService;
import io.github.hazyair.source.Station;
import android.support.v4.app.DatabaseService;

import io.github.hazyair.service.DatabaseSyncService;
import io.github.hazyair.util.License;
import io.github.hazyair.util.Network;
import io.github.hazyair.util.Preference;
import io.github.hazyair.widget.AppWidget;

import static android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM;
import static io.github.hazyair.util.Location.PERMISSION_REQUEST_FINE_LOCATION;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, LocationListener {

    // Final definitions
    public static final String PARAM_STATION = "io.github.hazyair.PARAM_STATION";
    public static final String PARAM_EXIT = "io.github.hazyair.PARAM_EXIT";
    private static final String PARAM_REFRESHING = "io.github.hazyair.PARAM_REFRESHING";
    private static final int ACTION_REMOVE_STATION = 0xDEADBEEF;

    // Nested classes definitions
    class StationPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor mCursor;
        private final SparseArray<Fragment> mFragments = new SparseArray<>();
        private final FragmentManager mFragmentManager;

        StationPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
        }

        void setCursor(Cursor cursor) {
            mCursor = cursor;
            if (mCursor != null) notifyDataSetChanged();
        }

        Cursor getCursor() {
            return mCursor;
        }

        void removePage(int position) {
            Fragment fragment = mFragments.get(position);
            if (fragment == null) return;
            destroyItem(null, position, fragment);
            mFragments.remove(position);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            Fragment newFragment = StationFragment.newInstance(mCursor);
            StationFragment oldFragment = (StationFragment) mFragments.get(position);
            if (oldFragment != null) oldFragment.clear();
            mFragments.put(position, newFragment);
            return newFragment;
        }

        @Override
        public int getCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public Parcelable saveState() {
            Bundle state = (Bundle) super.saveState();
            for (int i = 0; i < mFragments.size(); i++) {
                Fragment fragment = mFragments.get(i);
                if (fragment != null && fragment.isAdded()) {
                    if (state == null) {
                        state = new Bundle();
                    }
                    String key = StationFragment.class.getName() + i;
                    mFragmentManager.putFragment(state, key, fragment);
                }
            }
            mFragments.clear();
            //clear();
            return state;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            synchronized (mFragments) {
                super.restoreState(state, loader);
                if (state == null) return;
                Bundle bundle = (Bundle) state;
                Iterable<String> keys = bundle.keySet();
                if (keys == null) return;
                for (String key : keys) {
                    if (key.startsWith(StationFragment.class.getName())) {
                        int index = Integer.parseInt(key.substring(
                                StationFragment.class.getName().length()));
                        Fragment fragment = mFragmentManager.getFragment(bundle, key);
                        if (fragment != null) {
                            fragment.setMenuVisibility(false);
                            mFragments.put(index, fragment);
                        }
                    }
                }
            }
        }

        /*synchronized void clear() {
            for (int i = 0; i < mFragments.size(); i++)
                ((StationFragment) mFragments.get(i)).clear();
            mFragments.clear();
        }*/
    }

    class ViewHolder extends RecyclerView.ViewHolder {

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

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class StationListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private Cursor mCursor;
        private int mCurrentItem = 0;
        private ViewHolder mCurrentViewHolder;
        private Location mLocation;
        private boolean mDistance;

        StationListAdapter(boolean distance) {
            mDistance = distance;
        }

        void setCursor(Cursor cursor) {
            mCursor = cursor;
            if (mCursor != null) notifyDataSetChanged();
        }

        Cursor getCursor() {
            return mCursor;
        }

        void setLocation(Location location) {
            mLocation = location;
            notifyDataSetChanged();
        }

        void setDistance(boolean distance) {
            mDistance = distance;
            notifyDataSetChanged();
        }

        void setCurrentItem(int currentItem) {
            mCurrentItem = currentItem;
            notifyDataSetChanged();
        }

        int getCurrentItem() {
            return mCurrentItem;
        }

        private void selectStation(ViewHolder holder, int position) {
            if (holder.card == null) return;
            holder.card.setCardBackgroundColor(getColor(R.color.primaryLight));
            if (holder.place == null) return;
            holder.place.setTextColor(getColor(R.color.textLighter));
            if (holder.address == null) return;
            holder.address.setTextColor(getColor(R.color.textLight));
            if (holder.station == null) return;
            holder.station.setTextColor(getColor(R.color.textLight));
            if (holder.distance == null) return;
            holder.distance.setTextColor(getColor(R.color.textLight));
            if (mContainer == null) return;
            ConstraintLayout.LayoutParams layoutParams =
                    (ConstraintLayout.LayoutParams) mContainer.getLayoutParams();
            int width = mContainer.getMeasuredWidth();
            if (width > 0) {
                layoutParams.width = width;
                mContainer.setLayoutParams(layoutParams);
                mCursor.moveToPosition(position);
                if (mStationFragment == null)
                    mStationFragment = StationFragment.newInstance(mCursor);
                getSupportFragmentManager().beginTransaction().replace(R.id.container,
                        mStationFragment).commit();
                DatabaseService.selectStation(holder.itemView.getContext(),
                        Station.toBundleFromCursor(mCursor));
            } else {
                ViewTreeObserver vto = mContainer.getViewTreeObserver();
                vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        ConstraintLayout.LayoutParams layoutParams =
                                (ConstraintLayout.LayoutParams) mContainer.getLayoutParams();
                        layoutParams.width = mContainer.getMeasuredWidth();
                        mContainer.setLayoutParams(layoutParams);
                        mCursor.moveToPosition(position);
                        if (mStationFragment == null)
                            mStationFragment = StationFragment.newInstance(mCursor);
                        getSupportFragmentManager().beginTransaction().replace(R.id.container,
                                mStationFragment).commit();
                        DatabaseService.selectStation(holder.itemView.getContext(),
                                Station.toBundleFromCursor(mCursor));
                    }
                });
            }
        }

        void deselectStation(ViewHolder viewHolder) {
            if (viewHolder == null) return;
            if (viewHolder.card == null) return;
            viewHolder.card.setCardBackgroundColor(getColor(android.R.color.white));
            if (viewHolder.place == null) return;
            viewHolder.place.setTextColor(getColor(R.color.textDarker));
            if (viewHolder.address == null) return;
            viewHolder.address.setTextColor(getColor(R.color.textDark));
            if (viewHolder.station == null) return;
            viewHolder.station.setTextColor(getColor(R.color.textDark));
            if (viewHolder.distance == null) return;
            viewHolder.distance.setTextColor(getColor(R.color.textDark));
        }

        void deselectCurrentStation() {
            deselectStation(mCurrentViewHolder);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.station, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int layoutPosition = holder.getLayoutPosition();
            if (mCursor == null || !mCursor.moveToPosition(layoutPosition)) return;
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
            if (holder.distance != null) {
                holder.distance.setVisibility(mDistance ? View.VISIBLE : View.GONE);
                if (mDistance && mLocation != null) {
                    Location location = new Location(
                            station.getString(StationsContract.COLUMN_SOURCE));
                    location.setLongitude(
                            station.getDouble(StationsContract.COLUMN_LONGITUDE));
                    location.setLatitude(
                            station.getDouble(StationsContract.COLUMN_LATITUDE));
                    holder.distance.setText(String.format("%s %s",
                            String.valueOf((int) (location.distanceTo(mLocation) / 1000)),
                            getString(R.string.text_km)));
                }
            }
            if (layoutPosition == mCurrentItem) {
                selectStation(holder, layoutPosition);
                mCurrentViewHolder = holder;
            } else {
                deselectStation(holder);
            }
            holder.itemView.setOnClickListener((v) -> {
                if (layoutPosition == mCurrentItem) return;
                deselectCurrentStation();
                if (mStationFragment != null) mStationFragment.clear();
                mStationFragment = null;
                selectStation(holder, layoutPosition);
                mCurrentViewHolder = holder;
                mCurrentItem = layoutPosition;
            });
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
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
                    mRefreshing = true;
                    mSwipeRefreshLayout.setRefreshing(true);
                    break;
                case DatabaseService.ACTION_UPDATED:
                    mRefreshing = false;
                    mSwipeRefreshLayout.setRefreshing(false);
                    break;
                case DatabaseService.ACTION_SELECTED:
                    Preference.putInfo(MainActivity.this,
                            intent.getParcelableExtra(DatabaseService.PARAM_INFO));
                    AppWidget.update(MainActivity.this);
                    break;
            }
        }
    };

    private StationPagerAdapter mStationPagerAdapter;

    private StationListAdapter mStationListAdapter;

    private boolean mTwoPane;

    private Menu mMenu;

    private Bundle mSelectedStation;

    private StationFragment mStationFragment;

    private LocationCallback mLocationCallback;

    private LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private boolean mRefreshing;

    // ButterKnife
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    @BindView(R.id.view_pager)
    ViewPager mViewPager;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.fab_add_station)
    FloatingActionButton mFloatingActionButton;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    @BindView(R.id.stations)
    RecyclerView mRecyclerView;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    @BindView(R.id.container)
    FrameLayout mContainer;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    @BindView(R.id.divider)
    View mDivider;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.tabDots)
    TabLayout mTabLayout;


    // Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preference.initialize(this);
        if (Preference.isCrashlyticsEnabled(this)) {
            Fabric.with(this, new Crashlytics());
        }
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (!Preference.getLicense(this)) {
            License.showLicense(this);
        }

        if (getIntent().getBooleanExtra(PARAM_EXIT, false)) {
            finish();
        }

        setSupportActionBar(mToolbar);

        mSelectedStation = getIntent().getBundleExtra(PARAM_STATION);
        if (mSelectedStation == null)
            mSelectedStation = DatabaseService.selectedStation(this);
        else DatabaseService.selectStation(this, mSelectedStation);


        if (savedInstanceState != null) {
            mStationFragment =
                    (StationFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                    StationFragment.class.getName());
            mRefreshing = savedInstanceState.getBoolean(PARAM_REFRESHING);
            mSwipeRefreshLayout.setRefreshing(mRefreshing);
        }

        getSupportLoaderManager().initLoader(0, mSelectedStation, this);

        mTwoPane = getResources().getBoolean(R.bool.two_pane);
        if (mTwoPane) {
            if (mRecyclerView != null) {
                mStationListAdapter = new StationListAdapter(
                        io.github.hazyair.util.Location.checkPermission(this));
                mRecyclerView.setAdapter(mStationListAdapter);
                mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                               RecyclerView.State state) {
                        super.getItemOffsets(outRect, view, parent, state);
                        int itemCount = state.getItemCount();

                        final int itemPosition = parent.getChildLayoutPosition(view);

                        if (itemPosition == RecyclerView.NO_POSITION) {
                            return;
                        }

                        if (itemCount > 0) {
                            if (itemPosition == itemCount - 1) {
                                outRect.set(0, 0,
                                        getResources().getDimensionPixelSize(R.dimen.edge),
                                        getResources().getDimensionPixelSize(R.dimen.edge));
                            } else {
                                outRect.set(0, 0,
                                        getResources().getDimensionPixelSize(R.dimen.edge),
                                        0);
                            }
                        }
                    }
                });
                mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(
                                this);

                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        if (locationResult == null) {
                            return;
                        }
                        for (Location location : locationResult.getLocations()) {
                            mStationListAdapter.setLocation(location);
                        }
                    }

                    @Override
                    public void onLocationAvailability(LocationAvailability locationAvailability) {
                        super.onLocationAvailability(locationAvailability);
                        mStationListAdapter.setDistance(locationAvailability.isLocationAvailable());
                    }
                };
                mLocationRequest = io.github.hazyair.util.Location.createLocationRequest();
            }
        } else {
            if (mViewPager != null) {

                mStationPagerAdapter = new StationPagerAdapter(getSupportFragmentManager());
                mViewPager.setAdapter(mStationPagerAdapter);
                mViewPager.setOffscreenPageLimit(8);
                mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset,
                                               int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {
                        Cursor cursor = mStationPagerAdapter.getCursor();
                        cursor.moveToPosition(position);
                        mSelectedStation = Station.toBundleFromCursor(cursor);
                        DatabaseService.selectStation(MainActivity.this, mSelectedStation);
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });
                mTabLayout.setupWithViewPager(mViewPager, true);
                mTabLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mStationFragment != null) mStationFragment.clear();
        mStationFragment = null;
        if (mTwoPane) {
            mStationListAdapter = null;
        } else {
            //mStationPagerAdapter.clear();
            if (mViewPager != null) mViewPager.setAdapter(null);
            mStationPagerAdapter = null;
            mViewPager = null;
        }
        mSelectedStation = null;
        mTabLayout = null;
        mSwipeRefreshLayout = null;
        mRecyclerView = null;
        mDivider = null;
        mMenu = null;
        mToolbar = null;
        mContainer = null;
        mFloatingActionButton = null;
        mLocationCallback = null;
        mLocationRequest = null;
        mFusedLocationProviderClient = null;
        super.onDestroy();
    }

    private void addRemoveStationButton() {
        if (mMenu == null || mMenu.findItem(ACTION_REMOVE_STATION) != null) return;
        mMenu.add(Menu.NONE, ACTION_REMOVE_STATION, Menu.NONE,
                getString(R.string.title_remove_station))
                .setIcon(R.drawable.ic_remove_circle_outline_white_24dp)
                .setShowAsAction(SHOW_AS_ACTION_IF_ROOM);
    }

    private void removeRemoveStationButton() {
        if (mMenu == null) return;
        mMenu.removeItem(ACTION_REMOVE_STATION);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (mTwoPane) {
            if (mStationListAdapter != null && mStationListAdapter.getItemCount() > 0) {
                addRemoveStationButton();
            }
        } else {
            if (mStationPagerAdapter != null && mStationPagerAdapter.getCount() > 0) {
                addRemoveStationButton();
            }
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_license:
                License.showLicense(this);
                return true;
            case ACTION_REMOVE_STATION:
                int position;
                Cursor cursor;
                if (mTwoPane) {
                    if (mStationListAdapter == null) return true;
                    cursor = mStationListAdapter.getCursor();
                    if (cursor == null || cursor.getCount() == 0) return true;
                    removeRemoveStationButton();
                    mStationListAdapter.deselectCurrentStation();
                    position = mStationListAdapter.getCurrentItem();
                    int count = mStationListAdapter.getItemCount();
                    if (count > 0) {
                        if (position + 1 < count) {
                            cursor.moveToPosition(position + 1);
                        } else if (position > 0) {
                            cursor.moveToPosition(position - 1);
                        } else cursor.moveToFirst();
                        mSelectedStation = Station.toBundleFromCursor(cursor);
                    } else {
                        mSelectedStation = null;
                    }
                    if (mStationFragment != null) mStationFragment.clear();
                    mStationFragment = null;
                } else {
                    if (mViewPager == null || mStationPagerAdapter == null) return true;
                    cursor = mStationPagerAdapter.getCursor();
                    if (cursor == null || cursor.getCount() == 0) return true;
                    position = mViewPager.getCurrentItem();
                    int count = mStationPagerAdapter.getCount();
                    if (count > 0) {
                        if (position + 1 < count) {
                            cursor.moveToPosition(position + 1);
                        } else if (position > 0) {
                            cursor.moveToPosition(position - 1);
                        } else cursor.moveToFirst();
                        mSelectedStation = Station.toBundleFromCursor(cursor);
                    } else {
                        mSelectedStation = null;
                    }
                    mStationPagerAdapter.removePage(position);
                }
                DatabaseService.selectStation(MainActivity.this, mSelectedStation);
                cursor.moveToPosition(position);
                DatabaseService.delete(this, Station.toBundleFromCursor(cursor)
                        .getInt(StationsContract.COLUMN__ID));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DatabaseService.ACTION_UPDATED);
        intentFilter.addAction(DatabaseService.ACTION_UPDATING);
        intentFilter.addAction(DatabaseService.ACTION_SELECTED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFloatingActionButton.setOnClickListener((view) -> {
            mFloatingActionButton.setOnClickListener(null);
            startActivity(new Intent(MainActivity.this, StationsActivity.class));
        });
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (Network.isAvailable(MainActivity.this)) {
                if (!DatabaseService.update(this)) {
                    mRefreshing = false;
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            } else {
                mRefreshing = false;
                mSwipeRefreshLayout.setRefreshing(false);
                Network.showWarning(MainActivity.this);
            }
        });
        DatabaseSyncService.schedule(this);
        NotificationService.schedule(this);
        if (mTwoPane) requestUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTwoPane) {
            io.github.hazyair.util.Location.removeUpdates(this,
                    mFusedLocationProviderClient, mLocationCallback);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTwoPane && mStationFragment != null && mStationFragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, StationFragment.class.getName(),
                    mStationFragment);
        }
        outState.putBoolean(PARAM_REFRESHING, mRefreshing);
        mRefreshing = false;
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void requestUpdates() {
        io.github.hazyair.util.Location.requestUpdates(this,
                mFusedLocationProviderClient, mLocationRequest, mLocationCallback);
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

                    if (mTwoPane) {
                        requestUpdates();
                    } else {
                        for (Fragment fragment: getSupportFragmentManager().getFragments()) {
                            ((StationFragment) fragment).requestUpdates();
                        }
                    }

                }
            }

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getBooleanExtra(PARAM_EXIT, false)) {
            finish();
        }
        mSelectedStation = intent.getBundleExtra(PARAM_STATION);
        if (mSelectedStation == null)
            mSelectedStation = DatabaseService.selectedStation(this);
        else DatabaseService.selectStation(this, mSelectedStation);
        getSupportLoaderManager().destroyLoader(0);
        if (mTwoPane) mStationListAdapter.notifyDataSetChanged();
        else mStationPagerAdapter.notifyDataSetChanged();
        getSupportLoaderManager().initLoader(0, mSelectedStation, this);
        super.onNewIntent(intent);
    }

    // Loader handlers
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return StationsLoader.newInstanceForAllStations(MainActivity.this);
    }

    private void selectStation(int position) {
        if (mTwoPane) {
            if (mStationListAdapter != null) mStationListAdapter.setCurrentItem(position);
            if (mRecyclerView != null) mRecyclerView.scrollToPosition(position);
        } else {
            if (mViewPager != null) mViewPager.setCurrentItem(position, false);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data == null) return;
        int count = data.getCount();
        if (mTwoPane) {
            if (count == 0) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container,
                        new Fragment()).commit();
                if (mDivider != null) mDivider.setVisibility(View.GONE);
            } else {
                if (mDivider != null) mDivider.setVisibility(View.VISIBLE);
            }
            mStationListAdapter.setCursor(data);
        } else {
            mStationPagerAdapter.setCursor(data);
        }
        if (!mTwoPane && count > 0)
            io.github.hazyair.util.Location.checkPermission(this);
        if (count == 0) {
            removeRemoveStationButton();
            DatabaseService.selectStation(this, null);
        } else {
            addRemoveStationButton();
            if (mSelectedStation != null) {
                int i;
                for (i = 0; i < count; i++) {
                    data.moveToPosition(i);
                    if (Station.equals(Station.toBundleFromCursor(data), mSelectedStation)) {
                        selectStation(i);
                        break;
                    }
                }
                if (i == count) mSelectedStation = null;
            }
            if (mSelectedStation == null) {
                data.moveToFirst();
                DatabaseService.selectStation(this, Station.toBundleFromCursor(data));
                selectStation(0);
            }
        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (mTwoPane) {
            mStationListAdapter.setCursor(null);
        } else {
            mStationPagerAdapter.setCursor(null);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mTwoPane && mStationListAdapter != null) mStationListAdapter.setLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        if (mTwoPane && mStationListAdapter != null) mStationListAdapter.setDistance(true);
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (mTwoPane && mStationListAdapter != null) mStationListAdapter.setDistance(false);
    }

}
