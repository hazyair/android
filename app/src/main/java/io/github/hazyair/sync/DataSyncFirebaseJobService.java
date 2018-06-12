package io.github.hazyair.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import io.github.hazyair.source.Source;
import io.github.hazyair.source.gios.Station;

public class DataSyncFirebaseJobService extends JobService {

    //private RequestQueue mRequestQueue;

    @Override
    public boolean onStartJob(final JobParameters params) {
        //mRequestQueue = Volley.newRequestQueue(this);
        /*mRequestQueue.add(new StringRequest(Request.Method.GET, Source.GIOS.URL, (response) -> {
            List<Station> stationsUrl = new Gson().fromJson(response, new TypeToken<List<Station>>() {}.getType());

        }, (onError -> {})));*/
        /*mAsyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                //mRequestQueue.add()
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                jobFinished(params, false);
            }
        };
        mAsyncTask.execute();*/
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        /*if (mRequestQueue != null) {
            mRequestQueue.cancelAll(request -> true);
            mRequestQueue.stop();
            mRequestQueue = null;
        }*/
        return true;
    }
}
