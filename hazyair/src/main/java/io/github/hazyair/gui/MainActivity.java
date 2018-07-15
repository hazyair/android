package io.github.hazyair.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
            notifyDataSetChanged();
        }

        Cursor getCursor() {
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

    class ViewHolder extends RecyclerView.ViewHolder {

        @Nullable @BindView(R.id.cardview)
        CardView card;

        @Nullable @BindView(R.id.place)
        TextView place;

        @Nullable @BindView(R.id.address)
        TextView address;

        @Nullable @BindView(R.id.station)
        TextView station;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class StationListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private Cursor mCursor;
        private int mCurrentItem = 0;
        private ViewHolder mCurrentViewHolder;

        void setCursor(Cursor cursor) {
            mCursor = cursor;
            notifyDataSetChanged();
        }

        void setCurrentItem(int currentItem) {
            mCurrentItem = currentItem;
            notifyDataSetChanged();
        }

        int getCurrentItem() {
            return mCurrentItem;
        }

        Cursor getCursor() {
            return mCursor;
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
                    station.getString(StationsContract.COLUMN_COUNTRY),
                    station.getString(StationsContract.COLUMN_LOCALITY)));
            if (holder.address == null) return;
            holder.address.setText(station.getString(StationsContract.COLUMN_ADDRESS));
            if (holder.station == null) return;
            holder.station.setText(String.format("%s %s",
                    getString(R.string.text_station_by),
                    station.getString(StationsContract.COLUMN_SOURCE)));
            if (holder.card == null) return;
            if (layoutPosition == mCurrentItem) {
                selectStation(holder, layoutPosition);
                mCurrentViewHolder = holder;
            } else {
                deselectStation(holder);
            }
            holder.itemView.setOnClickListener((v) -> {
                if (layoutPosition == mCurrentItem) return;
                deselectCurrentStation();
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

        private void selectStation(ViewHolder holder, int position) {
            if (holder.card == null) return;
            holder.card.setCardBackgroundColor(getColor(R.color.primaryLight));
            if (holder.place == null) return;
            holder.place.setTextColor(getColor(R.color.textLighter));
            if (holder.address == null) return;
            holder.address.setTextColor(getColor(R.color.textLight));
            if (holder.station == null) return;
            holder.station.setTextColor(getColor(R.color.textLight));
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

        }

        void deselectCurrentStation() {
            deselectStation(mCurrentViewHolder);
        }

    }

    private StationPagerAdapter mStationPagerAdapter;

    private StationListAdapter mStationListAdapter;

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

    private boolean mTwoPane;
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

        if (savedInstanceState != null) {
            mStationFragment =
                    (StationFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                    StationFragment.class.getName());
        }

        getSupportLoaderManager().initLoader(0, mSelectedStation, this);

        mTwoPane = getResources().getBoolean(R.bool.two_pane);
        if (mTwoPane) {
            if (mRecyclerView != null) {
                mStationListAdapter = new StationListAdapter();
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
            }
        } else {
            if (mViewPager != null) {

                mStationPagerAdapter = new StationPagerAdapter(getSupportFragmentManager());
                mViewPager.setAdapter(mStationPagerAdapter);
                mViewPager.setOffscreenPageLimit(4);
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
            }
        }

        DatabaseSyncService.schedule(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mFloatingActionButton.setOnClickListener((view) -> {
            mFloatingActionButton.setOnClickListener(null);
            startActivity(new Intent(MainActivity.this, StationsActivity.class));
        });
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
                    if (position + 1 < mStationListAdapter.getItemCount()) {
                        cursor.moveToPosition(position + 1);
                    } else if (position > 0) {
                        cursor.moveToPosition(position - 1);
                    } else cursor.moveToFirst();
                    mSelectedStation = Station.toBundleFromCursor(cursor);
                } else {
                    if (mViewPager == null || mStationPagerAdapter == null) return true;
                    cursor = mStationPagerAdapter.getCursor();
                    if (cursor == null || cursor.getCount() == 0) return true;
                    position = mViewPager.getCurrentItem();
                    mStationPagerAdapter.removePage(position);
                    cursor.moveToPosition(position);
                    DatabaseService.delete(this, Station.toBundleFromCursor(cursor)
                            .getInt(StationsContract.COLUMN__ID));
                }
                cursor.moveToPosition(position);
                DatabaseService.delete(this, Station.toBundleFromCursor(cursor)
                        .getInt(StationsContract.COLUMN__ID));
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

        if (mTwoPane) {
            if (data.getCount() == 0) {
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
                        if (mTwoPane) {
                            if (mStationListAdapter != null) mStationListAdapter.setCurrentItem(i);
                            if (mRecyclerView != null) mRecyclerView.scrollToPosition(i);
                        } else {
                            if (mViewPager != null) mViewPager.setCurrentItem(i, false);
                        }
                        break;
                    }
                }
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

    private StationFragment mStationFragment;
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTwoPane && mStationFragment != null && mStationFragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, StationFragment.class.getName(),
                    mStationFragment);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStationFragment = null;
    }

    //TODO Add location
}
