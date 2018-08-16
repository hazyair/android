package io.github.hazyair.source;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Base {

    Base() {}

    Base(Bundle bundle) {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isSynthetic()) continue;
            String name = field.getName();
            if (skip(name, true)) continue;
            field.setAccessible(true);
            try {
                field.set(this, bundle.get(name));
            } catch (IllegalAccessException ignore) { }
        }
    }

    Base(Cursor cursor) {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isSynthetic()) continue;
            String name = field.getName();
            if (skip(name, true)) continue;
            field.setAccessible(true);
            try {
                Object value;
                Object object = field.get(this);
                if (object instanceof Integer) {
                    value = cursor.getInt(this.index(name));
                } else if (object instanceof Double) {
                    value = cursor.getDouble(this.index(name));
                } else if (object instanceof Long) {
                    value = cursor.getLong(this.index(name));
                } else {
                    value = ((cursor.getString(this.index(name)) == null) ? "" :
                                    cursor.getString(this.index(name)));
                }
                field.set(this, value);
            } catch (IllegalAccessException ignore) { }
        }
    }

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
                Crashlytics.logException(e);
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

    public Bundle toBundle() {

        HashMap<String, Object> map = toHashMap(true);
        Bundle bundle = new Bundle();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object object = entry.getValue();
            if (object instanceof Integer) {
                bundle.putInt(entry.getKey(), (Integer) entry.getValue());
            } else if (object instanceof Double) {
                bundle.putDouble(entry.getKey(), (Double) entry.getValue());
            } else if (object instanceof Long) {
                bundle.putLong(entry.getKey(), (Long) entry.getValue());
            } else {
                bundle.putString(entry.getKey(),
                        ((entry.getValue() == null) ? "" : (String) entry.getValue()));
            }
        }
        return bundle;

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

    String[] _keys() {

        ArrayList<String> result = new ArrayList<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isSynthetic()) continue;
            String name = field.getName();
            if (skip(name, true)) continue;
            result.add(name);
        }
        return result.toArray(new String[0]);

    }

    Bundle _toBundleFromCursor(Cursor cursor) {

        HashMap<String, Object> map = toHashMap(true);
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

    public static boolean equals(Bundle one, Bundle two) {
        if (one == null && two == null) return true;
        if (one == null || two == null) return false;
        if (one.size() != two.size()) return false;

        Set<String> setOne = new HashSet<>(one.keySet());
        setOne.addAll(two.keySet());
        Object valueOne;
        Object valueTwo;

        for (String key : setOne) {
            if (!one.containsKey(key) || !two.containsKey(key)) return false;

            valueOne = one.get(key);
            valueTwo = two.get(key);
            if (valueOne instanceof Bundle && valueTwo instanceof Bundle &&
                    !Base.equals((Bundle) valueOne, (Bundle) valueTwo)) {
                return false;
            } else if (valueOne == null) {
                if (valueTwo != null) return false;
            } else if (!valueOne.equals(valueTwo)) return false;
        }

        return true;
    }

}
