package com.example.jbt.proxi_me;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Elena Fainleib on 12/12/2016.
 *
 * This is the main activity of the application that manages the loading of the fragments, location services,
 * power plug services and the activities of the menu.
 */

public class MainActivity extends AppCompatActivity implements LocationListener, PlaceAdapter.OnPlaceSelectedListener, DialogInterface.OnClickListener{

    public static final String APP_FIRST_RUN = "firstRun";
    public static final String TO_SHOW_MAIN = "showMain";
    public static final String GOT_THE_PLACE = "gotMyPlace";
    public static final String SEARCH_QUERY_COUNT = "search_query_count";
    public static final String FAVORITES_QUERY_COUNT = "favorites_query_count";
    public static final int LOCATION_PERMISSION_GRANTED = 11;

    private LocationManager locationManager;
    private SharedPreferences sp, defSp;
    private SearchResultsFragment searchResultsFragment;
    private FavoritesFragment favoritesFragment;
    private TabletMapFragment tabletMapFragment;
    private PowerPlugReceiver powerPlugReceiver;
    private String providerName;
    private Timer timer;
    private boolean gotLocation = false;
    private boolean gotMyPlace;
    private boolean firstRun, showMain;
    private AlertDialog noPermissionsDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Immediately checking if the app runs on a tablet so it can be locked in the landscape mode
        // I decided that on smaller tables portrait mode with the map side by side of search and
        // favorites will look bad.
        // Loading everything else before loading the fragment itself
        if ((getResources().getBoolean(R.bool.is_tablet) == true)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        // Initializing global variables and checking if this is the first time application is running
        sp = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
        defSp = PreferenceManager.getDefaultSharedPreferences(this);

        firstRun = sp.getBoolean(APP_FIRST_RUN, true);

        searchResultsFragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.search_results_fragment);
        favoritesFragment = (FavoritesFragment) getSupportFragmentManager().findFragmentById(R.id.favorites_fragment);

        // Checking if the application was recreated after the rotation of the device, in which case the app needs to
        // show the fragment (main search or favorites) that the user was viewing before the rotation and to refresh
        // their queries
        // If not, the application presents the main search fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().hide(favoritesFragment).show(searchResultsFragment).commit();
            showMain = true;
            gotMyPlace = false;
        } else {
            showMain = savedInstanceState.getBoolean(TO_SHOW_MAIN, true);
            gotMyPlace = savedInstanceState.getBoolean(GOT_THE_PLACE);

            if (showMain == true) {
                getSupportFragmentManager().beginTransaction().hide(favoritesFragment).show(searchResultsFragment).commit();
            } else {
                getSupportFragmentManager().beginTransaction().hide(searchResultsFragment).show(favoritesFragment).commit();
            }

            searchResultsFragment.setCount((savedInstanceState.getInt(SEARCH_QUERY_COUNT)) + 1);
            favoritesFragment.setCount((savedInstanceState.getInt(FAVORITES_QUERY_COUNT)) + 1);
        }

        // Checking if the application runs on a tablet. If yes, load map beside the search/favorites fragment
        if(getResources().getBoolean(R.bool.is_tablet) == true){
            tabletMapFragment = (TabletMapFragment) getSupportFragmentManager().findFragmentById(R.id.tablet_map_fragment);
        }

        // Initializing a receiver for when the phone is plugged or unplugged into a charger
        powerPlugReceiver = new PowerPlugReceiver();
        IntentFilter batteryStatusIntent = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatusIntent.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryStatusIntent.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerPlugReceiver, batteryStatusIntent);

        // Connecting to the location services. Trying the GPS first since it's more accurate
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        providerName = LocationManager.GPS_PROVIDER;
        getLocationUpdates();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Checking if this is the first time the application is used. If yes - initialize default settings
        // for the search radius and search units. If not - load the search history and the favorites.
        if (firstRun == true) {
            sp.edit().putBoolean(APP_FIRST_RUN, false).apply();
            firstRun = false;
            defSp.edit().putString("search_radius", "1").apply();
            defSp.edit().putString("search_radius_units", "km").apply();
        } else {
            searchResultsFragment.loadSearchHistory();
            favoritesFragment.loadFavorites();
        }
    }

    private void getLocationUpdates() {
        // This method manages the location updates from the provider, switching them if necessary

        // Asking for runtime permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_GRANTED);
            }
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 100, this);

        // create timer object to switch the location provider from GPS to NETWORK if GPS is unavailable -
        // no response from it after a certain time passes
        timer = new Timer("provider");

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(gotLocation == false){
                    try{
                        locationManager.removeUpdates(MainActivity.this);
                        providerName = LocationManager.NETWORK_PROVIDER;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    locationManager.requestLocationUpdates(providerName, 1000, 100, MainActivity.this);
                                }
                                catch (SecurityException e){

                                }
                            }
                        });
                    }catch(SecurityException e){
                        Log.e("Location", e.getMessage());
                    }
                }
            }
        };
        // schedule the timer to run the task after 5 seconds from now
        timer.schedule(task, new Date(System.currentTimeMillis() + 5000));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Handles the response from the user to grant permissions. If yes - get their location.
        // If not - show a dialog
        switch (requestCode) {
            case LOCATION_PERMISSION_GRANTED: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocationUpdates();


                } else {
                    noPermissionsDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.no_permissions_dialog_title)
                            .setMessage(R.string.no_permissions_dialog_message)
                            .setPositiveButton(R.string.dialog_positive_button, this)
                            .create();
                    noPermissionsDialog.show();
                }
                return;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // This method handles the changes in location and updates them in the shared preferences
        gotLocation = true;

        // cancel the timer
        timer.cancel();

        // A small gesture to the user that his coordinates were determined when the app is activated.
        // Will not toast again on location update
        if (gotMyPlace == false) {
            gotMyPlace = true;
            Toast.makeText(MainActivity.this, getString(R.string.coordinates) + " " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        }

        sp.edit().putFloat(Constants.LATITUDE_KEY, (float) location.getLatitude()).apply();
        sp.edit().putFloat(Constants.LONGITUDE_KEY, (float) location.getLongitude()).apply();

        // If we are in the table mode, zoom on the place we are in
        if ((getResources().getBoolean(R.bool.is_tablet) == true)) {
            tabletMapFragment.zoomOnMe();
        }

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // creating the options menu in the Activity
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the menu items choice

        switch (item.getItemId()){
            case R.id.settings_item:
                // Calls the settings activity
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;

            case R.id.delete_all:
                // Calling a content provider to delete all favorites and reloading the favorites fragment
                getContentResolver().delete(PlaceContentProvider.FAVORITES_TABLE_NAME, null, null);
                favoritesFragment.loadFavorites();
                break;

            case R.id.favorites_item:
                // Displaying the favorites: (re)loading them, showing the favorites fragment and
                // saving that this is the fragment that is being displayed now case the device will be
                // rotated
                favoritesFragment.loadFavorites();
                getSupportFragmentManager().beginTransaction().hide(searchResultsFragment).show(favoritesFragment).commit();
                showMain = false;
                sp.edit().putBoolean(TO_SHOW_MAIN, false).apply();
                break;

            case R.id.main_screen_item:
                // Displaying the search/search history: (re)loading them, showing the search fragment and
                // saving that this is the fragment that is being displayed now case the device will be
                // rotated
                getSupportFragmentManager().beginTransaction().hide(favoritesFragment).show(searchResultsFragment).commit();
                showMain = true;
                sp.edit().putBoolean(TO_SHOW_MAIN, true).apply();
                break;


        }
        return super.onOptionsItemSelected(item);
    }

    public class PowerPlugReceiver extends BroadcastReceiver {
        // Handles the broadcasting of the phone being connected and disconnected from power
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == Intent.ACTION_POWER_CONNECTED) {
                Toast.makeText(context, getString(R.string.phone_plugged), Toast.LENGTH_SHORT).show();
            } else if (intent.getAction() == Intent.ACTION_POWER_DISCONNECTED) {
                Toast.makeText(context, getString(R.string.phone_unplugged), Toast.LENGTH_SHORT).show();
            }


        }
    }

    @Override
    public void placeSelected(Place place) {
        // Handling the selection of location in a tablet mode - passing it to the TabletMap fragment
        tabletMapFragment.showPlaceLocation(place);
    }

    protected void onSaveInstanceState(Bundle outState) {
        // Handling the rotation of the device - saving if it's the search fragment or the favorites
        // fragment displayed now, and also the last fragments query count
        outState.putBoolean(TO_SHOW_MAIN, showMain);
        outState.putBoolean(GOT_THE_PLACE, gotMyPlace);
        outState.putInt(SEARCH_QUERY_COUNT, favoritesFragment.getCount());
        outState.putInt(FAVORITES_QUERY_COUNT, favoritesFragment.getCount());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        // Unregisters the receiver for plugging the phone
        super.onDestroy();
        unregisterReceiver(powerPlugReceiver);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        // OK button click of the noPermissionsDialog. Does nothing
    }


}
