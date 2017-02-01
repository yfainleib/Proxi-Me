package com.example.jbt.proxi_me;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


/**
 * Created by Elena Fainleib
 *
 * This is a map fragment that is used if the application runs on a tablet.
 * It shows the place that the user selected on the map.
 */
public class TabletMapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private SharedPreferences sp;
    private MarkerOptions currMarkedPlace;
    private LatLng currLocation;


    public TabletMapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_tablet_map, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        sp = getActivity().getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return v;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // if we have permission add My Location button
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION }, 1);
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    public void zoomOnMe() {
        // Gets the user's current coordinates and zooms on the place
        LatLng myLocation = new LatLng(((double) sp.getFloat(Constants.LATITUDE_KEY, -1)), ((double)sp.getFloat(Constants.LONGITUDE_KEY, -1)));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));
    }

    public void showPlaceLocation (Place place) {
        // This method puts a marker on the map according to the details of a specific place that was passed to it
        // from the main activity

        // Hide the previously displayed markers
        mMap.clear();
        currLocation = new LatLng(place.getLatitude(), place.getLongitude());
        currMarkedPlace = new MarkerOptions().position(currLocation).title(place.getName()).snippet(place.getAddress());
        mMap.addMarker(currMarkedPlace);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currLocation, 15));
    }
}
