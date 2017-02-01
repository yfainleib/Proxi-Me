package com.example.jbt.proxi_me;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Elena Fainleib
 *
 * This fragment holds the user preferences and the reference to the icons website
 */

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
