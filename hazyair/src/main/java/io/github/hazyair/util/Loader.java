/*package io.github.hazyair.util;

import android.content.Context;
import android.support.v4.app.LoaderManager;
import android.support.v4.util.SparseArrayCompat;

import com.crashlytics.android.Crashlytics;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Loader {
    public static void clean(Context context, LoaderManager loaderManager) {
        try {
            Field field = loaderManager.getClass().getDeclaredField("mLoaderViewModel");
            field.setAccessible(true);
            Object mLoaderViewModel = field.get(loaderManager);
            if (mLoaderViewModel == null) return;
            field = mLoaderViewModel.getClass().getDeclaredField("mLoaders");
            field.setAccessible(true);
            SparseArrayCompat mLoaders = (SparseArrayCompat) field.get(mLoaderViewModel);
            if (mLoaders == null) return;
            int size = mLoaders.size();
            for (int index = 0; index < size; index++) {
                Object info = mLoaders.valueAt(index);
                if (info == null) continue;
                Method destroy = info.getClass().getDeclaredMethod("destroy", boolean.class);
                destroy.setAccessible(true);
                destroy.invoke(info, true);
            }
            mLoaders.clear();
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            if (Preference.isCrashlyticsEnabled(context)) {
                Crashlytics.logException(e);
            }
        }
    }
}
*/