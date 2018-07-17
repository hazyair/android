package io.github.hazyair.source;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("WeakerAccess")
public class Sensor extends Base implements Parcelable {
    public int _id;
    public int _station_id;
    public String id;
    public String station_id;
    public String parameter;
    public String unit;

    protected Sensor(Parcel in) {
        _id = in.readInt();
        _station_id = in.readInt();
        id = in.readString();
        station_id = in.readString();
        parameter = in.readString();
        unit = in.readString();
    }

    public static final Creator<Sensor> CREATOR = new Creator<Sensor>() {
        @Override
        public Sensor createFromParcel(Parcel in) {
            return new Sensor(in);
        }

        @Override
        public Sensor[] newArray(int size) {
            return new Sensor[size];
        }
    };

    public static Bundle toBundleFromCursor(Cursor cursor) {
        return new Sensor()._toBundleFromCursor(cursor);
    }

    public static String[] keys() {
        return new Sensor()._keys();
    }

    public Sensor() {
        this(null, null, null, null);
    }

    public Sensor(String id, String station_id, String parameter, String unit) {
        this.id = (id == null ? "" : id);
        this.station_id = (station_id == null ? "" : station_id);
        this.parameter = (parameter == null ? "" : parameter);
        this.unit = (unit == null ? "" : unit);
    }

    @SuppressWarnings("unused")
    public Sensor(Bundle bundle) {
        super(bundle);
    }

    public Sensor(Cursor cursor) {
        super(cursor);
    }

    @SuppressWarnings({"unused", "SameReturnValue"})
    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("unused")
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_id);
        dest.writeInt(_station_id);
        dest.writeString(id);
        dest.writeString(station_id);
        dest.writeString(parameter);
        dest.writeString(unit);
    }
}
