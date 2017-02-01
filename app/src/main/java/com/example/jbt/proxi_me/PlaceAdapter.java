package com.example.jbt.proxi_me;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Elena Fainleib on 12/11/2016.
 *
 * This is the adapter that handles the display of the results of the search history and the favorites saved by the user
 */

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceHolder> {

    private Context context;
    private String hostingViewName;
    private OnPlaceSelectedListener listener;
    private ArrayList<Place> places = new ArrayList<>();
    private boolean isInternet = true;

    // Constructor that gets the context and the name of the fragment that uses it
    public PlaceAdapter(Context context, String hostingViewName) {

        this.context = context;
        this.hostingViewName = hostingViewName;
        listener = (OnPlaceSelectedListener) context;
    }

    // Setter for the data that the adapter handles
    public void setPlaces(ArrayList<Place> places) {
        notifyDataSetChanged();
        clearPlaces();
        this.places.addAll(places);
    }

    // Clears the data
    public void clearPlaces() {
        this.places.clear();
    }

    @Override
    public PlaceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View currView = LayoutInflater.from(context).inflate(R.layout.place_item, parent, false);

        return new PlaceHolder(currView);
    }

    @Override

    // Receives the holder of the specific recycler view item and the position of the
    // element in the array that should be bound together
    public void onBindViewHolder(PlaceHolder holder, int position) {
        holder.bind(places.get(position));
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public class PlaceHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, DialogInterface.OnClickListener {

        /**
         * Created by Elena Fainleib
         *
         * This class handles individual items in the adapter. It is responsible for binding each element to its view,
         * handling the click events on a particular item and to get each item's place photos
         */

        private static final String PHOTO_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=100&photoreference=%s&key=AIzaSyBoGiTG9g3Asg2SJJb_pZPL0CEPJsHPaQo";

        private ImageView image;
        private TextView name, phone, address, distance;
        private SharedPreferences sp, defSP;
        private Place place;
        private AlertDialog addShareDialog, deleteShareDialog, noInternetDialog;

        public PlaceHolder(View itemView) {
            // Finds the views in the item view and sets listeners

            super(itemView);

            image = (ImageView) itemView.findViewById(R.id.placePic);
            name = (TextView) itemView.findViewById(R.id.placeName);
            phone = (TextView) itemView.findViewById(R.id.placePhone);
            address = (TextView) itemView.findViewById(R.id.placeAddress);
            distance = (TextView) itemView.findViewById(R.id.distance_to_place);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void bind(Place currPlace) {
            // Presents specific data in the item view
            this.place = currPlace;
            sp = context.getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
            defSP = PreferenceManager.getDefaultSharedPreferences(context);
            int unitsStringId;

            // Brings the place image from the web - if a photo is available - gets it
            // If not - gets a place category icon
            if (!place.getPhotoRef().equals("")) {
                GetPlaceImageTask getPlaceImageTask = new GetPlaceImageTask();
                getPlaceImageTask.execute(String.format(PHOTO_DETAILS_URL, place.getPhotoRef()));
            }  else {
                GetPlaceImageTask getPlaceImageTask = new GetPlaceImageTask();
                getPlaceImageTask.execute(place.getIconRef());
            }


            double longitude, latitude, tempDistance;

            name.setText(place.getName());
            phone.setText(place.getPhone());
            address.setText(place.getAddress());

            // Calculates the distance to this place from the user's current location in chosen units
            latitude = (double) sp.getFloat(Constants.LATITUDE_KEY, -1);
            longitude = (double) sp.getFloat(Constants.LONGITUDE_KEY, -1);

            tempDistance = distance(latitude, place.getLatitude(),longitude, place.getLongitude());
            tempDistance = tempDistance/1000; //converting to kilometers.

            if (defSP.getString("search_radius_units", "km").equals("mi")) {
                tempDistance = tempDistance/Constants.MILES_IN_KM;
                unitsStringId = R.string.mi;
            } else {
                unitsStringId = R.string.km;
            }

            distance.setText(new DecimalFormat("#.##").format(tempDistance) + context.getString(unitsStringId));
        }

        @Override
        public void onClick(View view) {
            // Handles a click on an item: if it's a tablet - pass the place to the main activity so it
            // can pass it to the map fragment. If it's a cellphone - call the maps activity and pass the place to it.
            // If this is a list item then color it and execute call map

            if (context.getResources().getBoolean(R.bool.is_tablet) == true) {
                listener.placeSelected(place);
            } else {
                Intent showPlace = new Intent(context, MapsActivity.class);
                showPlace.putExtra(Constants.CURR_PLACE, place);
                context.startActivity(showPlace);
            }

        }

        @Override
        public boolean onLongClick(View view) {
            // Handles the long click on the item
            // Checks what is  hosting this adapter: if search results - can be shared/added to favorites.
            // If favorites - shared/deleted
            // Present an appropriate choice dialog in each case
            switch (hostingViewName){
                case Constants.SEARCH_RESULTS_FRAGMENT:
                    // Shows a dialog asking if the user wants to save the place to favorites or
                    // wants to share it with others
                    addShareDialog = new AlertDialog.Builder(context)
                            .setTitle(R.string.add_share_place_dialog_title)
                            .setMessage(R.string.add_share_place_dialog_message)
                            .setPositiveButton(R.string.share_button, this)
                            .setNegativeButton(R.string.add_button, this)
                            .create();
                    addShareDialog.show();
                    break;
                case Constants.FAVORITES_FRAGMENT:
                    // Shows a dialog asking if the user wants to delete the place from favorites or
                    // wants to share it with others
                    deleteShareDialog = new AlertDialog.Builder(context)
                            .setTitle(R.string.delete_share_place_dialog_title)
                            .setMessage(R.string.delete_share_place_dialog_message)
                            .setPositiveButton(R.string.share_button, this)
                            .setNegativeButton(R.string.delete_button, this)
                            .create();
                    deleteShareDialog.show();
                    break;


            }


            return true;
        }

        // Method to calculate distance between two locations COURTESY OF DOMMER FROM STACK OVERFLOW
        public double distance(double lat1, double lat2, double lon1, double lon2) {

            final int R = 6371; // Radius of the earth

            Double latDistance = Math.toRadians(lat2 - lat1);
            Double lonDistance = Math.toRadians(lon2 - lon1);
            Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = R * c * 1000; // convert to meters


            distance = Math.pow(distance, 2);

            return Math.sqrt(distance);
        }


        @Override
        public void onClick(DialogInterface dialogInterface, int button) {
            // Handles the button clicks of dialogs
            // Check which dialog it came from:
            if (button == DialogInterface.BUTTON_POSITIVE) {
                if ((dialogInterface == addShareDialog) || (dialogInterface == deleteShareDialog)) {
                    // Start a share all intent
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.favorite_shared) + " " + place.getName() + " " + context.getString(R.string.at) + " " + place.getAddress());
                    sendIntent.setType("text/plain");
                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.choose_service)));
                }
            } else {
                if (dialogInterface == addShareDialog) {
                    // Save this place to the favorites table
                    ContentValues values = new ContentValues();

                    values.put(PlaceDBHelper.NAME_COL, place.getName());
                    values.put(PlaceDBHelper.ADDRESS_COL, place.getAddress());
                    values.put(PlaceDBHelper.PHONE_COL, place.getPhone());
                    values.put(PlaceDBHelper.ICON_REF_COL, place.getIconRef());
                    values.put(PlaceDBHelper.PHOTO_REF_COL, place.getPhotoRef());
                    values.put(PlaceDBHelper.LATITUDE_COL, place.getLatitude());
                    values.put(PlaceDBHelper.LONGITUDE_COL, place.getLongitude());

                    context.getContentResolver().insert(PlaceContentProvider.FAVORITES_TABLE_NAME, values);
                } else {
                    // Delete this place from favorites
                    context.getContentResolver().delete(PlaceContentProvider.FAVORITES_TABLE_NAME,PlaceDBHelper.ID_COL+"=?",new String[]{""+(place.getId())});
                    //int position  = places.indexOf(place);
                    places.remove(place);
                    notifyDataSetChanged();
                }
            }


            }


        public class GetPlaceImageTask extends AsyncTask<String, Void, Bitmap> {
            // This task downloads and returns an image from the url that was passed to it.

            @Override
            protected Bitmap doInBackground(String... params) {

                Bitmap currImage = null;
                HttpsURLConnection connection = null;

                try {
                    // Preparing to open an Internet connection
                    URL url = new URL(params[0]);
                    connection = (HttpsURLConnection) url.openConnection();

                    if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                        return null;
                    } else {
                        currImage = BitmapFactory.decodeStream(connection.getInputStream());
                    }

                } catch (UnknownHostException e) {
                    // Happens when the Internet is off
                    return null;
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                return currImage;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                // Checking if the poster download was successful.
                // if not - alert the user
                // if yes - put the image into its view
                // Putting into try/catch block since occasionally it seems to lose the context/activity
                try {
                    if (bitmap == null) {
                        // If the internet is off, alerting the user but just once - not for every item
                        // on the list
                        if (isInternet == true) {
                            noInternetDialog = new AlertDialog.Builder(context)
                                    .setTitle(R.string.no_internet_dialog_title)
                                    .setMessage(R.string.no_internet_dialog_message)
                                    .setPositiveButton(R.string.dialog_positive_button, PlaceHolder.this)
                                    .create();
                            noInternetDialog.show();
                            isInternet = false;
                        }

                    } else {
                        super.onPostExecute(bitmap);
                        image.setImageBitmap(bitmap);
                        isInternet = true;

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface OnPlaceSelectedListener {
        // Handles the passing of the place to the map fragment on the tablet
        void placeSelected(Place place);
    }
}

