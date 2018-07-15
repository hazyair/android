package io.github.hazyair.sync;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.DatabaseService;

import java.util.concurrent.TimeUnit;

import static android.app.job.JobScheduler.RESULT_SUCCESS;

public class DatabaseSyncService extends JobService {
    private static boolean mInitialized = false;
    private static final int INTERVAL = 15;
    private static final int JOB_ID = 0xDEADCAFE;
    private JobParameters mJobParams;

    public static void schedule(Context context) {
        if (mInitialized) return;
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        if (jobScheduler != null && jobScheduler.schedule(new JobInfo.Builder(JOB_ID,
                new ComponentName(context, DatabaseSyncService.class))
                .setPeriodic(TimeUnit.MINUTES.toMillis(INTERVAL))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()) == RESULT_SUCCESS) mInitialized = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DatabaseService.ACTION_UPDATED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        mJobParams = params;
        /*DatabaseService.enqueueWork(this,
                new Intent(DatabaseSyncService.this, DatabaseSyncService.class)
                        .setAction(DatabaseService.ACTION_UPDATE));*/
        DatabaseService.update(this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean reschedule = intent.getBooleanExtra(DatabaseService.PARAM_RESCHEDULE,
                    false);
            if (action == null) return;
            switch (action) {
                case DatabaseService.ACTION_UPDATED:
                    jobFinished(mJobParams, reschedule);
                    break;
            }
        }
    };

}
