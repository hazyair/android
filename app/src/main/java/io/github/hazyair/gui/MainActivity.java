package io.github.hazyair.gui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.stetho.Stetho;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.hazyair.R;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.data.StationsLoader;
import io.github.hazyair.source.Station;
import io.github.hazyair.sync.DatabaseService;

import static android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM;
import static io.github.hazyair.util.Location.PERMISSION_REQUEST_FINE_LOCATION;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, LocationListener {

    public class StationsPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor mCursor;
        private SparseArray<Fragment> fragments = new SparseArray<>();

        public StationsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setCursor(Cursor cursor) {
            mCursor = cursor;
            notifyDataSetChanged();
        }

        public Cursor getCursor() {
            return mCursor;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a StationFragment (defined as a static inner class below).
            mCursor.moveToPosition(position);
            Fragment fragment;
            if (fragments.indexOfKey(position) < 0) {
                fragment = StationFragment.newInstance(mCursor);
                fragments.put(position, fragment);
            } else {
                fragment = fragments.get(position);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        public void removePage(int position) {
            Fragment fragment = fragments.get(position);
            if (fragment == null) return;
            destroyItem(null, position, fragment);
            fragments.remove(position);
            mViewPager.setCurrentItem(position - 1, false);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

    }

    private StationsPagerAdapter mStationsPagerAdapter;

    private LocationManager mLocationManager;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.container)
    ViewPager mViewPager;

    @BindView(R.id.fab_add_station)
    FloatingActionButton mFloatingActionButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Stetho.initializeWithDefaults(this);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        setSupportActionBar(mToolbar);

        mStationsPagerAdapter = new StationsPagerAdapter(getSupportFragmentManager());

        getSupportLoaderManager().initLoader(0, null, this);

        mViewPager.setAdapter(mStationsPagerAdapter);
        //mViewPager.setOffscreenPageLimit(2);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mStationsPagerAdapter.mCursor.moveToPosition(position);
                Bundle bundle = Station.loadBundleFromCursor(mStationsPagerAdapter.mCursor);
                mToolbar.setTitle(String.format("%s - %s",
                        bundle.getString(StationsContract.COLUMN_LOCALITY),
                        bundle.getString(StationsContract.COLUMN_ADDRESS)));

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mFloatingActionButton.setOnClickListener((view) -> {
            startActivity(new Intent(MainActivity.this, StationsActivity.class));
        });

    }

    private Menu mMenu;

    private static final int ACTION_REMOVE_STATION = 0xDEADBEEF;
    private static final int ACTION_SHOW_LOCATION = 0xFEEDBEEF;

    private void addLocationButton() {
        mMenu.add(Menu.NONE, ACTION_SHOW_LOCATION, Menu.NONE,
                getString(R.string.title_show_location))
                .setIcon(R.drawable.ic_location_on_white_24dp)
                .setShowAsAction(SHOW_AS_ACTION_IF_ROOM);
    }


    private void addRemoveButton() {
        mMenu.add(Menu.NONE, ACTION_REMOVE_STATION, Menu.NONE,
                getString(R.string.title_remove_station))
                .setIcon(R.drawable.ic_remove_circle_outline_white_24dp)
                .setShowAsAction(SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (mStationsPagerAdapter == null) return true;
        Cursor cursor = mStationsPagerAdapter.getCursor();
        if (cursor == null) return true;
        if (mStationsPagerAdapter.getCursor().getCount() > 0) {
            //if (io.github.hazyair.util.Location.checkPermission(this)) {
            addLocationButton();
            //}
            addRemoveButton();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case ACTION_REMOVE_STATION:
                StationsPagerAdapter stationsPagerAdapter =
                        (StationsPagerAdapter) mViewPager.getAdapter();
                if (stationsPagerAdapter == null) return true;
                Cursor cursor = stationsPagerAdapter.getCursor();
                if (cursor == null || cursor.getCount() == 0) return true;
                int position = mViewPager.getCurrentItem();
                stationsPagerAdapter.removePage(position);
                cursor.moveToPosition(position);
                int id = Station.loadBundleFromCursor(cursor)
                        .getInt(StationsContract.COLUMN__ID);
                startService(new Intent(this, DatabaseService.class)
                        .setAction(DatabaseService.ACTION_DELETE)
                        .putExtra(DatabaseService.PARAM__ID, id));
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
                    //addLocationButton();

                }/* else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }*/
                return;
            }

        }
    }

    // LocationListener

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // LoaderManager.LoaderCallbacks<Cursor>

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return StationsLoader.newInstanceForAllStations(MainActivity.this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data == null) return;
        if (data.getCount() == 0) {
            mToolbar.setTitle(R.string.app_name);
            if (mMenu != null) {
                mMenu.removeItem(ACTION_REMOVE_STATION);
                mMenu.removeItem(ACTION_SHOW_LOCATION);
            }
        } else {
            data.moveToPosition(mViewPager.getCurrentItem());
            Bundle bundle = Station.loadBundleFromCursor(data);
            mToolbar.setTitle(String.format("%s - %s",
                    bundle.getString(StationsContract.COLUMN_LOCALITY),
                    bundle.getString(StationsContract.COLUMN_ADDRESS)));
        }
        mStationsPagerAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mViewPager.setAdapter(null);
    }

}
