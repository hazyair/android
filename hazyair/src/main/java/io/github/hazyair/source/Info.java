package io.github.hazyair.source;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Info implements Parcelable {
    public final Station station;
    public final List<Sensor> sensors;
    public final List<Data> data;

    public Info(Station station, List<Sensor> sensors, List<Data> data) {
        this.station = station;
        this.sensors = sensors;
        this.data = data;
    }

    private Info(Parcel in) {
        station = in.readParcelable(Station.class.getClassLoader());
        sensors = in.createTypedArrayList(Sensor.CREATOR);
        data = in.createTypedArrayList(Data.CREATOR);
    }

    public static final Creator<Info> CREATOR = new Creator<Info>() {
        @Override
        public Info createFromParcel(Parcel in) {
            return new Info(in);
        }

        @Override
        public Info[] newArray(int size) {
            return new Info[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(station, flags);
        dest.writeTypedList(sensors);
        dest.writeTypedList(data);
    }
}
