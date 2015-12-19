package com.exsoft.weatharium.model;

/**
 * Created by M.A. on 02.12.2015.
 */

/*
Несколько ссылок для сверки данных

Санкт-Петербург
http://openweathermap.org/city/519690

Минск
http://openweathermap.org/city/625144

Париж
http://openweathermap.org/city/2988507


 */

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONObject;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;

public class CityPreference {
    //SharedPreferences prefs;
    public static final int DEFAULT_CITY_ID = 536203;
    public static final int DEFAULT_FORECAST_COUNT = 5;
    public static final boolean DEFAULT_TEMP_METRIC = true;

    final String LOG_TAG = "CityPreference";
    HashMap<Integer, City> cityList;
    int defaultCityID;
    int forecastCount;
    boolean celsiumMetric;
    DBHelper dbHelper;
    //JSONObject cacheData;
    static CityPreference controllerInstance;

    public static CityPreference getInstance(Activity activity) {
        if(CityPreference.controllerInstance == null) {
            controllerInstance = new CityPreference(activity);
        }
        return controllerInstance;
    }

    public CityPreference(Activity activity){

        defaultCityID = DEFAULT_CITY_ID;
        forecastCount = DEFAULT_FORECAST_COUNT;
        celsiumMetric = DEFAULT_TEMP_METRIC ;

        cityList = new HashMap<>();
        dbHelper = new DBHelper(activity);
        // подключаемся к БД
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Log.d(LOG_TAG, "CityPreference ctor, --- Rows in city_table: ---");
        // делаем запрос всех данных из таблицы city_table, получаем Cursor
        Cursor c = db.query("city_table", null, null, null, null, null, null);
        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        if (c.moveToFirst()) {
            // определяем номера столбцов по имени в выборке
            int idColIndex = c.getColumnIndex("id");
            int nameColIndex = c.getColumnIndex("name");
            int countryColIndex = c.getColumnIndex("country");
            int lonColIndex = c.getColumnIndex("lon");
            int latColIndex = c.getColumnIndex("lat");

            do {
                // получаем значения по номерам столбцов и пишем все в лог
                Log.d(LOG_TAG,
                        "ID = " + c.getInt(idColIndex) +
                                ", name = " + c.getString(nameColIndex) +
                                ", country = " + c.getString(countryColIndex) +
                                ", longitude = " + c.getString(lonColIndex) +
                                ", latitude = " + c.getString(latColIndex));

                cityList.put(c.getInt(idColIndex), new City(
                        c.getInt(idColIndex),
                        c.getString(nameColIndex),
                        c.getString(countryColIndex),
                        c.getString(lonColIndex),
                        c.getString(latColIndex)
                ));

                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (c.moveToNext());
        } else
            Log.d(LOG_TAG, "ERROR ! 0 rows readed !");
        c.close();

        Log.d(LOG_TAG, "city list count=" + cityList.size());

        Log.d(LOG_TAG, "--- Rows in cache_table: ---");
        // делаем запрос всех данных из таблицы cache_table, получаем Cursor
        Cursor ccc = db.query("cache_table", null, null, null, null, null, null);
        if (ccc.moveToFirst()) {
            int cacheIDCol = ccc.getColumnIndex("id");
            int cacheWeatherCol = ccc.getColumnIndex("weather");
            int cacheForecastCol = ccc.getColumnIndex("forecast");
            int cacheDay5ForecastCol = ccc.getColumnIndex("day5forecast");
            do {
                Integer cacheCityID = ccc.getInt(cacheIDCol);
                String weatherCache = ccc.getString(cacheWeatherCol);
                String forecastCache = ccc.getString(cacheForecastCol);
                String day5forecastCache = ccc.getString(cacheDay5ForecastCol);
                Log.d(LOG_TAG, "cacheCityID= " + cacheCityID);
                Log.d(LOG_TAG, "weatherCache = " + weatherCache);
                Log.d(LOG_TAG, "forecastCache = " + forecastCache);
                Log.d(LOG_TAG, "day5forecastCache = " + day5forecastCache);
                //cacheData[cacheCityID] = jsonData;

            } while (ccc.moveToNext()) ;
        }
        ccc.close();

        db.close();
    }

    public enum CACHE_DATA_TYPE {
        weather,
        forecast,
        day5forecast
    }
    // Инициализация данными из настроек
    public void InitDefaults(Integer cityID, Integer forecastCount, boolean metric) {
        Log.d(LOG_TAG, "InitDefaults, " +
                String.format("city=%s, forecastCount=%s, metric=%s", cityID, forecastCount, metric)
        );
        setCity(cityID);
        setForecastCount(forecastCount);
        setMetric(metric);
    }

    public int getForecastCount() {
        return forecastCount;
    }

    public void setForecastCount(int count) {
        forecastCount = count;
    }

    public boolean getMetric () { return celsiumMetric; }

    public void setMetric(boolean newMetric) {
        celsiumMetric = newMetric;
    }

    public City getCity(){
        Log.d(LOG_TAG, "--- getCity ---, cityID=" + defaultCityID);
        return cityList.get(defaultCityID);
    }

    public void setCity(int cityID) {
        defaultCityID = cityID;
    }

    public int getCityByName(String cityName) {
        List<City> allCity = getAllCity(false);
        int cityID = -1;
        for (int i = 0; i < allCity.size() && cityID == -1; ++i)
            if (allCity.get(i).Name().compareTo(cityName) == 0) {
                cityID = allCity.get(i).ID();
                break;
            }
        return cityID;
    }

    public void setCityByName(String cityName) {
        int cityID = getCityByName(cityName);
        if(cityID == -1) cityID = DEFAULT_CITY_ID;
        defaultCityID = cityID;
    }

    public boolean containCity(int cityID) {
        return cityList.containsKey(cityID);
    }

    public ArrayList<City> getAllCity() {
        return getAllCity(true);
    }

    public ArrayList<City> getAllCity(boolean sorted) {
        ArrayList<City> res = new ArrayList<>();
        for (City c : cityList.values()) {
            res.add(c);
        }
        if (sorted) {
            Collections.sort(res);
        }
        return res;
    }

    /////////////////////////////////////// КЕШ //////////////////////////////////////////////////////
    public JSONObject getCacheData(Integer cityID, CACHE_DATA_TYPE cacheDataType) {
        Log.d(LOG_TAG, "getCacheData, cityID=" + cityID + ", CACHE_DATA_TYPE=" + cacheDataType);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sqlQuery = "SELECT * FROM cache_table WHERE id=?";
        JSONObject cacheData = new JSONObject();
        Log.d(LOG_TAG, "--- Rows in cache_table: ---");
        // делаем запрос всех данных из таблицы cache_table, получаем Cursor
        Cursor c = db.rawQuery(sqlQuery, new String[]{String.valueOf(cityID)});
        if (c.moveToFirst()) {
            int cacheIDCol = c.getColumnIndex("id");
            int cacheDataCol = c.getColumnIndex(cacheDataType.toString());
            do {
                Integer cacheCityID = c.getInt(cacheIDCol);
                String cacheDataString = c.getString(cacheDataCol);
                try {
                    cacheData = new JSONObject(cacheDataString);
                } catch (Exception ex) {
                }
                Log.d(LOG_TAG, "cacheCityID= " + cacheCityID);
                Log.d(LOG_TAG, "cacheDataString = " + cacheDataString);
                //cacheData[cacheCityID] = jsonData;
            } while (c.moveToNext()) ;
        }
        c.close();
        db.close();
        return cacheData;
    }
    ////////////////////
    public void setCacheData(Integer cityID, CACHE_DATA_TYPE cacheDataType, JSONObject cacheData) {
        Log.d(LOG_TAG, "setCacheData, cityID=" + cityID + ", CACHE_DATA_TYPE=" + cacheDataType + ", cacheData=" + cacheData);
        SQLiteDatabase dbReader = dbHelper.getReadableDatabase();
        String sqlQuery = "SELECT * FROM cache_table WHERE id=?";
        Cursor cursor = dbReader.rawQuery(sqlQuery, new String[]{String.valueOf(cityID)});
        ContentValues cv = new ContentValues();
        long rowID = -1;
        SQLiteDatabase dbWriteer = dbHelper.getWritableDatabase();
        Log.d(LOG_TAG, "CURSOR=" + cursor.getCount());
        if(cursor.getCount() > 0) {
            Log.d(LOG_TAG, "setCacheData, --- UPDATE in cache_table: ---");
            cv.put(cacheDataType.toString(), cacheData.toString());
            rowID = dbWriteer.update("cache_table", cv, "id = '" + cityID + "'", null);
        } else {
            Log.d(LOG_TAG, "setCacheData, --- INSERT in cache_table: ---");
            cv.put("id", cityID);
            cv.put(cacheDataType.toString(), cacheData.toString());
            rowID = dbWriteer.insert("cache_table", null, cv);
        }
        Log.d(LOG_TAG, "row updated, ID = " + rowID);
        dbWriteer.close();
        cursor.close();
        dbReader.close();
    }

}
