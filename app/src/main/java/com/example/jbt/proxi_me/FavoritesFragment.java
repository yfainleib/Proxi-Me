package com.example.jbt.proxi_me;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * Created by Elena Fainleib
 *
 * This Fragment displays favorite places saved by the user
 */
public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private PlaceAdapter adapter;
    private RecyclerView favoritesList;
    private Context context;
    private int count = 0;


    public FavoritesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, find its views and get the fragment's context
        View v = inflater.inflate(R.layout.fragment_favorites, container, false);

        this.context = getContext();

        favoritesList = (RecyclerView) v.findViewById(R.id.favorites_list);
        adapter = new PlaceAdapter(context, Constants.FAVORITES_FRAGMENT);
        favoritesList.setLayoutManager(new LinearLayoutManager(context));
        favoritesList.setAdapter(adapter);

        return v;
    }

    public void loadFavorites () {
        // Calls the query from the content provider.
        getLoaderManager().initLoader(count, null, FavoritesFragment.this);
    }

    // Sets the query count to certain number
    public void setCount(int number) {
        count = number;
    }

    // Gets the query count
    public int getCount() {
        return count;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Returns a new query from the favorites table
        return new CursorLoader(context,PlaceContentProvider.FAVORITES_TABLE_NAME, null, null, null, PlaceDBHelper.NAME_COL );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Brings results of the query into an array and connects it with the adapter to be displayed in the favorites list

        ArrayList<Place> favoritePlaces = new ArrayList<>();
        String name = "", address = "", phone = "", iconRef = "", photoRef = "";
        double longitude, latitude;
        long id;

        while(data.moveToNext()) {
            id = data.getLong(data.getColumnIndex(PlaceDBHelper.ID_COL));
            name = data.getString(data.getColumnIndex(PlaceDBHelper.NAME_COL));
            address = data.getString(data.getColumnIndex(PlaceDBHelper.ADDRESS_COL));
            phone = data.getString(data.getColumnIndex(PlaceDBHelper.PHONE_COL));
            iconRef = data.getString(data.getColumnIndex(PlaceDBHelper.ICON_REF_COL));
            photoRef = data.getString(data.getColumnIndex(PlaceDBHelper.PHOTO_REF_COL));
            latitude = data.getDouble(data.getColumnIndex(PlaceDBHelper.LATITUDE_COL));
            longitude = data.getDouble(data.getColumnIndex(PlaceDBHelper.LONGITUDE_COL));

            favoritePlaces.add(new Place(id, name, address, phone, iconRef, photoRef, latitude, longitude));
        }
        adapter.notifyDataSetChanged();
        adapter.setPlaces(favoritePlaces);
        count++;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
