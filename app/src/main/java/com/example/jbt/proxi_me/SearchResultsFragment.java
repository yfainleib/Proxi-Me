package com.example.jbt.proxi_me;


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.jbt.proxi_me.Constants;
import com.example.jbt.proxi_me.GetPlacesService;
import com.example.jbt.proxi_me.MainActivity;
import com.example.jbt.proxi_me.Place;
import com.example.jbt.proxi_me.PlaceAdapter;
import com.example.jbt.proxi_me.PlaceContentProvider;
import com.example.jbt.proxi_me.PlaceDBHelper;
import com.example.jbt.proxi_me.R;

import java.util.ArrayList;

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Elena Fainleib
 *
 * This fragment displays the last search performed by the user
 */
public class SearchResultsFragment extends Fragment implements View.OnClickListener, DialogInterface.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private EditText searchText;
    private ImageButton searchButton, findNearButton;
    private RecyclerView searchResults;
    private PlaceAdapter placeAdapter;
    private SharedPreferences sp, defSp;
    private ProgressBar progressBar;
    private Context context;
    private Activity activity;
    private PlacesDataReceiver placesReceiver;
    private int count;


    public SearchResultsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = activity;
        this.context = getContext();

        // Registering the receiver for when the search results are saved in the database
        placesReceiver = new PlacesDataReceiver();
        IntentFilter filter = new IntentFilter(Constants.ACTION_PLACES_DATA_RECEIVED);
        LocalBroadcastManager.getInstance(activity).registerReceiver(placesReceiver, filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        // Inflate the layout for this fragment and find its views
        View v = inflater.inflate(R.layout.fragment_search_results, container, false);
        progressBar = (ProgressBar) v.findViewById(R.id.search_results_progress_bar);
        searchText = (EditText) v.findViewById(R.id.search_text);
        searchButton = (ImageButton) v.findViewById(R.id.search_btn);
        findNearButton = (ImageButton) v.findViewById(R.id.getNear_btn);
        searchResults = (RecyclerView) v.findViewById(R.id.searchResults);

        searchButton.setOnClickListener(this);
        findNearButton.setOnClickListener(this);

        placeAdapter = new PlaceAdapter(this.context, Constants.SEARCH_RESULTS_FRAGMENT);
        searchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResults.setAdapter(placeAdapter);

        return v;
    }

    @Override
    public void onClick(View view) {
        // Handles the clicks on the search buttons
        AlertDialog textEmptyDialog;

        Intent getPlacesIntent = new Intent(getContext(), GetPlacesService.class);
        switch (view.getId()) {
            case R.id.search_btn:
                // Open service intent and get it the search string from the text field
                // if not empty
                if (!searchText.getText().toString().isEmpty()) {
                    getPlacesIntent.putExtra(Constants.SEARCH_TYPE, Constants.KEYWORD_SEARCH);
                    String keyword = searchText.getText().toString().replace(" ", "%20");
                    getPlacesIntent.putExtra(Constants.KEYWORD_KEY, keyword);

                } else {
                    // Alert the user if the search field is empty
                    textEmptyDialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.text_empty_dialog_title)
                            .setMessage(R.string.text_empty_dialog_message)
                            .setPositiveButton(R.string.dialog_positive_button, SearchResultsFragment.this)
                            .create();
                    textEmptyDialog.show();
                    return;
                }
                break;
            case R.id.getNear_btn:
                // Open service intent and get all near places
                getPlacesIntent.putExtra(Constants.SEARCH_TYPE, Constants.GET_NEAR_PLACES);
                break;
        }

        // Preparing the search parameters to pass it to the service that will return the search results
        sp = getActivity().getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
        defSp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        getPlacesIntent.putExtra(Constants.LATITUDE_KEY, (double) sp.getFloat(Constants.LATITUDE_KEY, -1));
        getPlacesIntent.putExtra(Constants.LONGITUDE_KEY, (double) sp.getFloat(Constants.LONGITUDE_KEY, -1));
        getPlacesIntent.putExtra(Constants.RADIUS_KEY, Integer.parseInt(defSp.getString("search_radius", "1")));
        getPlacesIntent.putExtra(Constants.UNITS_KEY, defSp.getString("search_radius_units", "km"));

        this.context.startService(getPlacesIntent);

        progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    // Sets the query count to certain number
    public void setCount(int number) {
        count = number;
    }

    // Gets the query count
    public int getCount() {
        return count;
    }

    public void loadSearchHistory() {
        // Calls the query from the content provider.
        getLoaderManager().initLoader(count, null, SearchResultsFragment.this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Returns a new query from the search_history table
        return new CursorLoader(context,PlaceContentProvider.SEARCH_HISTORY_TABLE_NAME, null, null, null, PlaceDBHelper.NAME_COL );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Brings results of the query into an array and connects it with the adapter to be displayed in the search_results list
        ArrayList<Place> searchHistory = new ArrayList<>();

        String name = "", address = "", phone = "", iconRef = "", photoRef = "";
        double longitude, latitude;

        while(data.moveToNext()){
            name = data.getString(data.getColumnIndex(PlaceDBHelper.NAME_COL));
            address = data.getString(data.getColumnIndex(PlaceDBHelper.ADDRESS_COL));
            phone = data.getString(data.getColumnIndex(PlaceDBHelper.PHONE_COL));
            iconRef = data.getString(data.getColumnIndex(PlaceDBHelper.ICON_REF_COL));
            photoRef = data.getString(data.getColumnIndex(PlaceDBHelper.PHOTO_REF_COL));
            latitude = data.getDouble(data.getColumnIndex(PlaceDBHelper.LATITUDE_COL));
            longitude = data.getDouble(data.getColumnIndex(PlaceDBHelper.LONGITUDE_COL));

            searchHistory.add(new Place(name, address, phone, iconRef, photoRef, latitude, longitude));
        }

        placeAdapter.notifyDataSetChanged();
        placeAdapter.setPlaces(searchHistory);
        count++;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    public class PlacesDataReceiver extends BroadcastReceiver {
        // This class presents the last search results in the list

        @Override
        public void onReceive(Context context, Intent intent) {
            AlertDialog noInternetDialog, noResultsDialog;

            progressBar.setVisibility(ProgressBar.GONE);

            // We received a feedback from trying to bring the data on the search places.
            // The codes can be search sucessful/zero results/some other problem
            // We were notified that the data on places search was put into the database.
            // We can query it and present it in the list
            // Otherwise - notify the user
            // Putting into try/catch block since occasionally it seems to lose the context/activity
            try {
                switch (intent.getIntExtra(Constants.GOT_DATA_RESULT, Constants.GOT_DATA_BROADCAST)) {
                    case Constants.GOT_DATA_BROADCAST:
                        //Bring the search results from the database into a list
                        loadSearchHistory();
                        break;
                    case Constants.ZERO_RESULTS_BROADCAST:
                        // Present a message to the user
                        noResultsDialog = new AlertDialog.Builder(activity)
                                .setTitle(R.string.no_results_dialog_title)
                                .setMessage(R.string.no_results_dialog_message)
                                .setPositiveButton(R.string.dialog_positive_button, SearchResultsFragment.this)
                                .create();
                        noResultsDialog.show();
                        break;
                    case Constants.INTERNET_PROBLEMS_BROADCAST:
                        // Present a message to the user
                        noInternetDialog = new AlertDialog.Builder(activity)
                                .setTitle(R.string.no_internet_dialog_title)
                                .setMessage(R.string.no_internet_dialog_message)
                                .setPositiveButton(R.string.dialog_positive_button, SearchResultsFragment.this)
                                .create();
                        noInternetDialog.show();
                        break;

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        // Handles the clicks on the OK button of the dialogs - does nothing
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregisters the receiver for finding places
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(placesReceiver);
    }
}
