package io.github.hazyair.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.concurrent.TimeUnit;

import io.github.hazyair.R;
import io.github.hazyair.gui.MainActivity;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Info;
import io.github.hazyair.util.Config;
import io.github.hazyair.util.Preference;
import io.github.hazyair.util.Quality;
import io.github.hazyair.util.Time;

import static android.app.job.JobScheduler.RESULT_SUCCESS;

public class NotificationService extends JobService {
    private static int mInterval = -1;
    private static final int JOB_ID = 0xCAFEFEED;
    private static final String CHANNEL_ID = "io.github.hazyair";
    private static final int NOTIFICATION_ID = 0xDEAD10CC;

    public static void schedule(Context context, int interval) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        if (interval != mInterval && jobScheduler != null) {
            if (interval == 0) {
                jobScheduler.cancel(JOB_ID);
            } else if (jobScheduler.schedule(new JobInfo.Builder(JOB_ID,
                    new ComponentName(context, NotificationService.class))
                    .setPeriodic(TimeUnit.MINUTES.toMillis(interval))
                    .setPersisted(true)
                    .build()) == RESULT_SUCCESS) {
                mInterval = interval;
            }
        }
    }

    public static void schedule(Context context) {
        schedule(context, Preference.getNotificationsFrequency(context));
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Info info = Config.getInfo(this);
        if (info == null) return false;
        StringBuilder stringBuilder = new StringBuilder();
        boolean notify = false;
        Long timestamp = System.currentTimeMillis();
        for (int i = 0; i < info.sensors.size(); i ++) {
            String parameter = info.sensors.get(i).parameter;
            Data data = info.data.get(i);
            int percent = Quality.normalize(parameter, data.value);
            if (timestamp - Time.getTimestamp(data.timestamp) < TimeUnit.HOURS.toMillis(1)
                    && percent > 100) {
                stringBuilder.append(parameter).append(": ").append(percent).append("%")
                        .append(", ");
                notify = true;
            }
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (notify) {
            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
            createNotificationChannel();
            notificationManager.notify(NOTIFICATION_ID,
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_cloud_white_18dp)
                            .setContentTitle(String.format("%s %s %s",
                                    getString(info.station.country), info.station.locality,
                                    info.station.address))
                            .setContentText(stringBuilder)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(PendingIntent.getActivity(this,
                                    info.station._id, new Intent(this,
                                            MainActivity.class).putExtra(MainActivity.PARAM_STATION,
                                                    info.station.toBundle()), 0)).build());
        } else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
