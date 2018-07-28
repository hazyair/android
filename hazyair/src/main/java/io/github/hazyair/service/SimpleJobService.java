package io.github.hazyair.service;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;

public abstract class SimpleJobService extends JobService {
    private final SimpleArrayMap<JobParameters, AsyncJobTask> runningJobs = new SimpleArrayMap<>();

    @CallSuper
    @Override
    public boolean onStartJob(@NonNull JobParameters job) {
        AsyncJobTask async = new AsyncJobTask(this, job);

        synchronized (runningJobs) {
            runningJobs.put(job, async);
        }

        async.execute();

        return true; // more work to do
    }

    @CallSuper
    @Override
    public boolean onStopJob(@NonNull JobParameters job) {
        synchronized (runningJobs) {
            AsyncJobTask async = runningJobs.remove(job);
            if (async != null) {
                async.cancel(true);
                return true;
            }
        }

        return false;
    }

    private void onJobFinished(JobParameters jobParameters, boolean b) {
        synchronized (runningJobs) {
            runningJobs.remove(jobParameters);
        }

        jobFinished(jobParameters, b);
    }

    @SuppressWarnings("SameReturnValue")
    protected abstract boolean onRunJob(@SuppressWarnings("unused") @NonNull JobParameters job);

    private static class AsyncJobTask extends AsyncTask<Void, Void, Boolean> {
        @SuppressLint("StaticFieldLeak")
        private final SimpleJobService jobService;
        private final JobParameters jobParameters;

        private AsyncJobTask(SimpleJobService jobService, JobParameters jobParameters) {
            this.jobService = jobService;
            this.jobParameters = jobParameters;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return jobService.onRunJob(jobParameters);
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            jobService.onJobFinished(jobParameters, bool);
        }
    }
}

