package com.example.jbt.proxi_me;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Elena Fainleib on 12/11/2016.
 *
 * This class represents a location with its properties
 */

public class Place implements Parcelable {

    private long id;
    private String name, address, phone, iconRef, photoRef;
    private double  latitude, longitude;

    // Contructors
    // This constructor is needed for the favorites table, whose records can be deleted by an id
    public Place(long id, String name, String address, String phone, String iconRef, String photoRef, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.iconRef = iconRef;
        this.photoRef = photoRef;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // This constructor is needed for the search history which is only required to be presented - no need for id
    public Place(String name, String address, String phone, String iconRef, String photoRef, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.iconRef = iconRef;
        this.photoRef = photoRef;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected Place(Parcel in) {
        name = in.readString();
        address = in.readString();
        phone = in.readString();
        iconRef = in.readString();
        photoRef = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<Place> CREATOR = new Creator<Place>() {
        @Override
        public Place createFromParcel(Parcel in) {
            return new Place(in);
        }

        @Override
        public Place[] newArray(int size) {
            return new Place[size];
        }
    };

    // Getters and Setters
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIconRef() {
        return iconRef;
    }

    public void setIconRef(String iconRef) {
        this.iconRef = iconRef;
    }

    public String getPhotoRef() {
        return photoRef;
    }

    public void setPhotoRef(String photoRef) {
        this.photoRef = photoRef;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(address);
        parcel.writeString(phone);
        parcel.writeString(iconRef);
        parcel.writeString(photoRef);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
    }
}
