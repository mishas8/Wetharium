package com.exsoft.weatharium.view;

/**
 * Created by M.A. on 05.12.2015.
 */

import com.exsoft.weatharium.R;
import com.exsoft.weatharium.model.City;
import com.exsoft.weatharium.model.CityPreference;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import java.util.ArrayList;

public class SettingsActivity extends PreferenceActivity {

    private static final String LOGTAG = "PreferenceActivity";

    CityPreference cityPref;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // загружаем предпочтения из ресурсов
        addPreferencesFromResource(R.xml.pref_settings);

        cityPref = CityPreference.getInstance(this);

        final ListPreference listPreference = (ListPreference) findPreference("key_pref_city");

        // THIS IS REQUIRED IF YOU DON'T HAVE 'entries' and 'entryValues' in your XML
        setListPreferenceData(listPreference);

        listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                setListPreferenceData(listPreference);
                return false;
            }
        });
    }

    protected void setListPreferenceData(ListPreference lp) {
        ArrayList<City> allCity = cityPref.getAllCity();
        int defaultCityID = cityPref.getCity().ID();

        Log.d(LOGTAG, "setListPreferenceData, defaultCityID=" + defaultCityID);

        int len = allCity.size();

        CharSequence[] entries = new CharSequence[len];
        CharSequence[] entryValues = new CharSequence[len];

        for(int i = 0; i < allCity.size(); ++i) {
            entries[i] = allCity.get(i).Name();
            entryValues[i] = allCity.get(i).ID().toString();
        }

        lp.setEntries(entries);
        lp.setEntryValues(entryValues);
        lp.setDefaultValue(defaultCityID);

    }
}