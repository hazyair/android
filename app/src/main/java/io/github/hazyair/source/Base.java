package io.github.hazyair.source;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Base {

    private boolean skip(String name, boolean _id) {
        return name.equals("serialVersionUID") || name.equals("CREATOR") || name.equals("_status")
                || (!_id && name.equals("_id"));
    }

    private HashMap<String, Object> toHashMap(boolean _id) {
        HashMap<String, Object> map = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isSynthetic()) continue;
            String name = field.getName();
            if (skip(name, _id)) continue;
            field.setAccessible(true);
            try {
                map.put(name, field.get(this));
            } catch (IllegalAccessException e) {
                Timber.e(e);
            }
        }
        return map;
    }

    public ContentValues toContentValues() {

        HashMap<String, Object> map = toHashMap(false);
        ContentValues contentValues = new ContentValues();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object object = entry.getValue();
            if (object instanceof Integer) {
                contentValues.put(entry.getKey(), (Integer) entry.getValue());
            } else if (object instanceof Double) {
                contentValues.put(entry.getKey(), (Double) entry.getValue());
            } else if (object instanceof Long) {
                contentValues.put(entry.getKey(), (Long) entry.getValue());
            } else {
                contentValues.put(entry.getKey(),
                        ((entry.getValue() == null) ? "" : (String) entry.getValue()));
            }
        }
        return contentValues;

    }

    private int index(String key) {

        int result = 0;
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isSynthetic()) continue;
            String name = field.getName();
            if (skip(name, true)) continue;
            if (name.equals(key)) break;
            result ++;
        }
        return result;

    }

    public String[] keys() {

        ArrayList<String> result = new ArrayList<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isSynthetic()) continue;
            String name = field.getName();
            if (skip(name, true)) continue;
            result.add(name);
        }
        return result.toArray(new String[0]);

    }

    protected Bundle _loadBundleFromCursor(Cursor cursor) {

        HashMap<String, Object> map = toHashMap(true);//new HashMap<>();
        Bundle bundle = new Bundle();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object object = entry.getValue();
            if (object instanceof Integer) {
                bundle.putInt(entry.getKey(), cursor.getInt(this.index(entry.getKey())));
            } else if (object instanceof Double) {
                bundle.putDouble(entry.getKey(), cursor.getDouble(this.index(entry.getKey())));
            } else if (object instanceof Long) {
                bundle.putLong(entry.getKey(), cursor.getLong(this.index(entry.getKey())));
            } else {
                bundle.putString(entry.getKey(),
                        ((cursor.getString(this.index(entry.getKey())) == null) ? "" :
                                cursor.getString(this.index(entry.getKey()))));
            }
        }
        return bundle;

    }
}
