package io.github.hazyair.source;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("WeakerAccess")
public class Data extends Base implements Parcelable {
    public int _id;
    public int _station_id;
    public int _sensor_id;
    public long timestamp;
    public double value;

    protected Data(Parcel in) {
        _id = in.readInt();
        _station_id = in.readInt();
        _sensor_id = in.readInt();
        timestamp = in.readLong();
        value = in.readDouble();
    }


    public static final Creator<Data> CREATOR = new Creator<Data>() {
        @Override
        public Data createFromParcel(Parcel in) {
            return new Data(in);
        }

        @Override
        public Data[] newArray(int size) {
            return new Data[size];
        }
    };

    public static Bundle toBundleFromCursor(Cursor cursor) {
        return new Data()._toBundleFromCursor(cursor);
    }

    public static String[] keys() {
        return new Data()._keys();
    }

    public Data() {
        this(0, 0.0);
    }

    public Data(long timestamp, Double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    @SuppressWarnings("unused")
    public Data(Bundle bundle) {
        super(bundle);
    }

    public Data(Cursor cursor) {
        super(cursor);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_id);
        dest.writeInt(_station_id);
        dest.writeInt(_sensor_id);
        dest.writeLong(timestamp);
        dest.writeDouble(value);
    }
}
