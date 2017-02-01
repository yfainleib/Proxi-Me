package com.example.jbt.proxi_me;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Elena Fainleib
 *
 * This service gets the search data from the Google API, using the user coordinates, a certain radius and keywords
 *
 */

public class GetPlacesService extends IntentService {

    private static final String GOOGLE_MAPS_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%s,%s&radius=%s&key=AIzaSyBoGiTG9g3Asg2SJJb_pZPL0CEPJsHPaQo";
    private static final String PLACE_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?placeid=%s&key=AIzaSyBoGiTG9g3Asg2SJJb_pZPL0CEPJsHPaQo";
    private static final String JSON_STATUS = "status";
    private static final String JSON_STATUS_OK = "OK";
    private static final String JSON_STATUS_ZERO = "ZERO_RESULTS";
    private static final String JSON_RESULT = "result";
    private static final String JSON_RESULTS = "results";
    private static final String JSON_PHOTOS = "photos";
    private static final String JSON_FORMATTED_ADDRESS = "formatted_address";
    private static final String JSON_FORMATTED_PHONE = "formatted_phone_number";
    private static final String JSON_GEOMETRY = "geometry";
    private static final String JSON_LAT = "lat";
    private static final String JSON_LNG = "lng";
    private static final String JSON_ICON_REF = "icon";
    private static final String JSON_PHOTO_REF = "photo_reference";
    private static final String JSON_LOCATION = "location";
    private static final String JSON_PLACE_ID = "place_id";
    private static final String JSON_NAME = "name";

    private int gotDataCode = Constants.GOT_DATA_BROADCAST;

    public GetPlacesService() {
        super("GetPlacesService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int radius;
        double meters, latitude, longitude;
        String units, keyword, finalURL;

        radius = intent.getIntExtra(Constants.RADIUS_KEY, 1);
        units = intent.getStringExtra(Constants.UNITS_KEY);
        latitude = intent.getDoubleExtra(Constants.LATITUDE_KEY, -1);
        longitude = intent.getDoubleExtra(Constants.LONGITUDE_KEY, -1);

        meters = radius*1000;

        if (units.equals("mi")) {
            meters = radius * 1000*Constants.MILES_IN_KM;
        }

        finalURL = String.format(GOOGLE_MAPS_URL, latitude, longitude, meters);

        // If the intent contains a search keyword - use it in the search
        // Otherwise, bring the data from the near places. If successful,
        // erase the old search results from the database and put in new ones.
        // Successful - data was received.
        // Return an indication if there was data/the search produced no results/some other problem
        if (intent.getStringExtra(Constants.SEARCH_TYPE).equals(Constants.KEYWORD_SEARCH)) {
            keyword = "&keyword=" + (intent.getStringExtra(Constants.KEYWORD_KEY));
            finalURL = finalURL + keyword;
        }

        // Preparing to open a connection and to get all the places from it
        HttpsURLConnection mainConnection = null, detailsConnection = null;
        StringBuilder mainBuilder = new StringBuilder();
        BufferedReader mainResponse = null, detailsResponse = null;
        JSONObject fullPlaceResponse = null,  currPlace = null;
        JSONObject detailsPlaceResponse = null;
        JSONArray results = null;
        String placeDetailsFinalURL;


        try {
            URL searchUrl = new URL(finalURL);
            mainConnection = (HttpsURLConnection) searchUrl.openConnection();

            // Checking the connection - if problems note it and return
            if (mainConnection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                gotDataCode = Constants.INTERNET_PROBLEMS_BROADCAST;
                return;
            }

            // Preparing to read the stream from the connection
            mainResponse = new BufferedReader(new InputStreamReader(mainConnection.getInputStream()));
            String line;
            while ((line = mainResponse.readLine()) != null) {
                mainBuilder.append(line);
            }

            fullPlaceResponse = new JSONObject(mainBuilder.toString());

            // Getting the result of the query - the option that the search produced zero results or
            // there were some other problems
            if (fullPlaceResponse.has(JSON_STATUS)) {
                String status = fullPlaceResponse.getString(JSON_STATUS);
                if (status.equals(JSON_STATUS_ZERO)) {
                    gotDataCode = Constants.ZERO_RESULTS_BROADCAST;
                    return;
                } else if (!status.equals(JSON_STATUS_OK)) {
                    gotDataCode = Constants.INTERNET_PROBLEMS_BROADCAST;
                    return;
                }
            }

            // If we are here - the query has produced results
            results = fullPlaceResponse.getJSONArray(JSON_RESULTS);


            // Looping over the results: for each result getting its specific address and phone number
            // by its id. Set each value on a local variable and when got of them write them as a
            // record into the database.
            for (int i = 0; i < results.length(); i++) {
                String id = "", name = "", address = "",  iconRef = "", photoRef = "";
                String phone = "";

                currPlace = results.getJSONObject(i);
                id = currPlace.getString(JSON_PLACE_ID);
                name = currPlace.getString(JSON_NAME);
                iconRef = currPlace.getString(JSON_ICON_REF);


                if (currPlace.has(JSON_PHOTOS)) {
                    if (currPlace.getJSONArray(JSON_PHOTOS).getJSONObject(0).has(JSON_PHOTO_REF)) {
                        photoRef = currPlace.getJSONArray(JSON_PHOTOS).getJSONObject(0).getString(JSON_PHOTO_REF);
                    }
                }

                latitude = currPlace.getJSONObject(JSON_GEOMETRY).getJSONObject(JSON_LOCATION).getDouble(JSON_LAT);
                longitude = currPlace.getJSONObject(JSON_GEOMETRY).getJSONObject(JSON_LOCATION).getDouble(JSON_LNG);

                // Getting the place full address and phone from another API
                placeDetailsFinalURL = String.format(PLACE_DETAILS_URL, id);

                URL detailsUrl = new URL(placeDetailsFinalURL);
                detailsConnection = (HttpsURLConnection) detailsUrl.openConnection();

                // Checking for internet problems on the way
                if (detailsConnection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    gotDataCode = Constants.INTERNET_PROBLEMS_BROADCAST;
                    return;
                }

                // Building the response from the stream
                detailsResponse = new BufferedReader(new InputStreamReader(detailsConnection.getInputStream()));
                String line2;
                StringBuilder detailsBuilder = new StringBuilder();
                while ((line2 = detailsResponse.readLine()) != null) {
                    detailsBuilder.append(line2);
                }

                detailsPlaceResponse = new JSONObject(detailsBuilder.toString());


                if (detailsPlaceResponse.getJSONObject(JSON_RESULT).has(JSON_FORMATTED_ADDRESS)) {
                    address = detailsPlaceResponse.getJSONObject(JSON_RESULT).getString(JSON_FORMATTED_ADDRESS);
                }

                // how to check if the phone number exists
                if (detailsPlaceResponse.getJSONObject(JSON_RESULT).has(JSON_FORMATTED_PHONE)) {
                    phone = detailsPlaceResponse.getJSONObject(JSON_RESULT).getString(JSON_FORMATTED_PHONE);
                }

                // Don't need this connection anymore so closing it right away
                detailsConnection.disconnect();

                //If we got a first result, clearing the old search from the table
                if ((i == 0) && (gotDataCode == Constants.GOT_DATA_BROADCAST)) {
                    getContentResolver().delete(PlaceContentProvider.SEARCH_HISTORY_TABLE_NAME, null, null);
                }

                //Saving the place data as a record in the database
                ContentValues values = new ContentValues();

                values.put(PlaceDBHelper.NAME_COL, name);
                values.put(PlaceDBHelper.ADDRESS_COL, address);
                values.put(PlaceDBHelper.PHONE_COL, phone);
                values.put(PlaceDBHelper.ICON_REF_COL, iconRef);
                values.put(PlaceDBHelper.PHOTO_REF_COL, photoRef);
                values.put(PlaceDBHelper.LATITUDE_COL, latitude);
                values.put(PlaceDBHelper.LONGITUDE_COL, longitude);

                getContentResolver().insert(PlaceContentProvider.SEARCH_HISTORY_TABLE_NAME, values);

            }


        } catch (UnknownHostException e) {
            // Thrown in case of no internet - signal it and return
            gotDataCode = Constants.INTERNET_PROBLEMS_BROADCAST;
            return;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Broadcasting the result of the getting places action
            Intent gotData = new Intent(Constants.ACTION_PLACES_DATA_RECEIVED);
            gotData.putExtra(Constants.GOT_DATA_RESULT, gotDataCode);
            LocalBroadcastManager.getInstance(this).sendBroadcast(gotData);

            // Closing the connections if open
            if (mainConnection != null) {
                mainConnection.disconnect();
            }

            if (detailsConnection != null) {
                detailsConnection.disconnect();
            }
        }


    }
}
