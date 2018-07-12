package io.github.hazyair.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;;
import android.widget.Toast;

import com.facebook.stetho.Stetho;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.hazyair.R;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.data.StationsLoader;
import io.github.hazyair.source.Station;
import android.support.v4.app.DatabaseService;
import io.github.hazyair.sync.DatabaseSyncService;

import static android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String PARAM_STATION = "io.github.hazyair.PARAM_STATION";

    public class StationsPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor mCursor;
        private final SparseArray<Fragment> mFragments = new SparseArray<>();
        private final FragmentManager mFragmentManager;

        StationsPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
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
            //if (mFragments.size() <= position) {
            fragment = StationFragment.newInstance(mCursor);
            mFragments.put(position, fragment);
            //mFragments.add(fragment);
            //} else {
            //fragment = mFragments.get(position);
            //}
            return fragment;
        }

        @Override
        public int getCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        void removePage(int position) {
            //Fragment fragment = mFragments.get(position);
            Fragment fragment = mFragments.get(position);
            if (fragment == null) return;
            destroyItem(null, position, fragment);
            mFragments.remove(position);
        }

        @Override
        public Parcelable saveState() {
            Bundle state = (Bundle) super.saveState();
            for (int i=0; i<mFragments.size(); i++) {
                Fragment fragment = mFragments.get(i);
                if (fragment != null && fragment.isAdded()) {
                    if (state == null) {
                        state = new Bundle();
                    }
                    String key = StationFragment.class.getName() + i;
                    mFragmentManager.putFragment(state, key, fragment);
                }
            }
            return state;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            super.restoreState(state, loader);
            if (state == null) return;
            Bundle bundle = (Bundle)state;
            Iterable<String> keys = bundle.keySet();
            if (keys == null) return;
            for (String key: keys) {
                if (key.startsWith(StationFragment.class.getName())) {
                    int index = Integer.parseInt(key.substring(
                            StationFragment.class.getName().length()));
                    Fragment fragment = mFragmentManager.getFragment(bundle, key);
                    if (fragment != null) {
                        //while (mFragments.size() <= index) {
                        //    mFragments.add(null);
                        //}
                        fragment.setMenuVisibility(false);
                        //mFragments.set(index, fragment);
                        mFragments.put(index, fragment);
                    }
                }
            }
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

    }

    private StationsPagerAdapter mStationsPagerAdapter;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Nullable
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

        setSupportActionBar(mToolbar);

        mSelectedStation = getIntent().getBundleExtra(PARAM_STATION);
        if (mSelectedStation == null) {
            mSelectedStation = DatabaseService.selectedStation(this);
        } else {
            DatabaseService.selectStation(this, mSelectedStation);
        }

        getSupportLoaderManager().initLoader(0, mSelectedStation, this);

        if (mViewPager != null) {

            mStationsPagerAdapter = new StationsPagerAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(mStationsPagerAdapter);
            mViewPager.setOffscreenPageLimit(4);
            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    mStationsPagerAdapter.mCursor.moveToPosition(position);
                    mSelectedStation = Station.toBundleFromCursor(mStationsPagerAdapter.mCursor);
                    DatabaseService.selectStation(MainActivity.this, mSelectedStation);


                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });

        } else {
            // TODO
        }
        mFloatingActionButton.setOnClickListener((view) ->
            startActivity(new Intent(MainActivity.this, StationsActivity.class))
        );
        DatabaseSyncService.schedule(this);

    }


    private Menu mMenu;

    private static final int ACTION_REMOVE_STATION = 0xDEADBEEF;

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
        if (mStationsPagerAdapter != null && mStationsPagerAdapter.getCount() > 0) {
            addRemoveStationButton();
        }
        return super.onCreateOptionsMenu(menu);
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
                int id = Station.toBundleFromCursor(cursor)
                        .getInt(StationsContract.COLUMN__ID);
                DatabaseService.delete(this, id);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Bundle mSelectedStation;

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return StationsLoader.newInstanceForAllStations(MainActivity.this);
    }


    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data == null) return;

        if (mStationsPagerAdapter != null) {
            mStationsPagerAdapter.setCursor(data);
        } else {
            //TODO
        }

        int count = data.getCount();
        if (count == 0) {
            removeRemoveStationButton();
            DatabaseService.selectStation(this, null);
        } else {
            addRemoveStationButton();
            if (mSelectedStation == null) {
                data.moveToFirst();
                DatabaseService.selectStation(this, Station.toBundleFromCursor(data));
            } else {
                for (int i = 0; i < count; i++) {
                    data.moveToPosition(i);
                    if (Station.equals(Station.toBundleFromCursor(data), mSelectedStation)) {
                        if (mViewPager != null) {
                            mViewPager.setCurrentItem(i, false);
                        } else {
                            //TODO
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mViewPager.setAdapter(null);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case DatabaseService.ACTION_UPDATED:
                    // TODO Add refresh layout
                    Toast.makeText(MainActivity.this, "Updated",
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DatabaseService.ACTION_UPDATED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
    }

}
