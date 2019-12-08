package io.github.hazyair.gui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import io.fabric.sdk.android.Fabric;
import io.github.hazyair.R;
import io.github.hazyair.service.NotificationService;
import io.github.hazyair.service.DatabaseSyncService;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
            = (preference, value) -> {
                if (value instanceof String) {
                    String stringValue = value.toString();
                    if (preference instanceof ListPreference) {
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(stringValue);
                        preference.setSummary(
                                index >= 0
                                        ? listPreference.getEntries()[index]
                                        : null);
                        if (preference.hasKey() && preference.getKey().equals(
                                preference.getContext().getString(R.string.pref_key_sync_frequency))) {
                            DatabaseSyncService.schedule(preference.getContext(),
                                    Integer.valueOf(stringValue));
                        }
                        if (preference.hasKey() && preference.getKey().equals(
                                preference.getContext()
                                        .getString(R.string.pref_key_notifications_frequency))) {
                            NotificationService.schedule(preference.getContext(),
                                    Integer.valueOf(stringValue));
                        }

                    } else {
                        preference.setSummary(stringValue);

                    }
                } else  {

                    if (preference.hasKey() && preference.getKey()
                            .equals(preference.getContext()
                            .getString(R.string.pref_key_crashlytics))) {
                        Context context = preference.getContext();
                        if ((boolean) value) Fabric.with(context, new Crashlytics());
                        else new AlertDialog.Builder(context)
                                .setTitle(context.getString(R.string.title_crashlytics))
                                .setMessage(context.getString(R.string.message_crashlytics))
                                .setPositiveButton(
                                        context.getString(R.string.button_ok),
                                        null)
                                .create()
                                .show();
                    }
                }
                return true;
            };

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        Context context = preference.getContext();
        String key = preference.getKey();

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        if (!key.equals(context.getString(R.string.pref_key_crashlytics))) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    sharedPreferences.getString(preference.getKey(), ""));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_crashlytics)));
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            bindPreferenceSummaryToValue(
                    findPreference(getString(R.string.pref_key_notifications_frequency)));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_sync_frequency)));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
