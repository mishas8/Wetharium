package com.exsoft.weatharium.view;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.exsoft.weatharium.R;
import com.exsoft.weatharium.model.CityPreference;
import com.exsoft.weatharium.utils.RemoteFetch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by eXetrum on 02.12.2015.
 */


import android.graphics.Color;

/**
 * Created by M.A. on 02.12.2015.
 */

public class Day5ForecastActivity extends Fragment {

    private static final String LOGTAG = "Day5ForecastActivity";

    Typeface weatherFont;

    CityPreference cityPreference;

    Handler handler;

    public Day5ForecastActivity(){
        handler = new Handler();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        weatherFont = WeatherActivity.weatherFont;
        cityPreference = CityPreference.getInstance(this.getActivity());
        updateWeatherData(cityPreference.getCity().ID());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.day5_forecast, container, false);

        return rootView;
    }

    private void updateWeatherData(final Integer cityID) {
        new Thread(){
            public void run(){
                Log.d(LOGTAG, "UdateDay5ForecastData RemoteFetch, cityID=" + cityID);
                // ForecastActivity
                final JSONObject json = RemoteFetch.getJSON(getActivity(), RemoteFetch.DATA_TYPE.forecast_daily, cityID, cityPreference.getForecastCount());

                if(json == null){
                    handler.post(new Runnable(){
                        public void run(){
                            Log.d(LOGTAG, "UpdateDay5ForecastData RemoteFetch json==null");
                            // Загружаем данные из кеша
                            JSONObject cache = cityPreference.getCacheData(cityID, CityPreference.CACHE_DATA_TYPE.day5forecast);
                            if(cache == null) {
                                Log.d(LOGTAG, "UdateDay5ForecastData RemoteFetch result=place_not_found");
                                Log.d(LOGTAG, "cache json==null");
                                try {
                                    Toast.makeText(getActivity(),
                                            getActivity().getString(R.string.place_not_found),
                                            Toast.LENGTH_LONG).show();
                                } catch (Exception ex) {
                                    Log.d(LOGTAG, "UdateDay5ForecastData RemoteFetch error=" + ex.getMessage());
                                }
                            } else {
                                Log.d(LOGTAG, "RENDER CACHE DATA for city=" + cityID);
                                renderWeather(cache);
                            }
                        }
                    });
                } else {
                    handler.post(new Runnable(){
                        public void run(){
                            // Делаем отладочную запись
                            Log.d(LOGTAG, "UpdateDay5ForecastData, RemoteFetch result=OK");
                            // Запускаем отображение полученной информации
                            renderWeather(json);
                            // Запоминаем текущий прогноз (Используем для кеширования)
                            cityPreference.setCacheData(cityID, CityPreference.CACHE_DATA_TYPE.day5forecast, json);
                        }
                    });
                }
            }
        }.start();
    }

    private void renderWeather(JSONObject json){
        // Пишем сообщение в лог
        Log.d(LOGTAG, "renderWeather start, JSON=" + json.toString());
        try {
            // Получаем список объектов которые вернул сервер
            JSONArray list = json.getJSONArray("list");
            // Пишем в лог количество полученных прогнозов
            Log.d(LOGTAG, "forecast list len=" + list.length());
            // Находим идентификатор таблицы в которую будем выводить прогноз
            TableLayout table = (TableLayout)getActivity().findViewById(R.id.day5_table);
            // Очищаем содержимое (Если рендеринг вызывается повторно, вероятно таблица уже заполнена данными, поэтому и очищаем)
            table.removeAllViews();
            // Создаем настройки строки которые будем применять для всех дальнейших вставляемых строк
            TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(5, 5, 5, 5);
            rowParams.weight = 2.0f;
            // Создаем настройки для поля вывода иконки
            TableRow.LayoutParams paramsLeft = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
            paramsLeft.setMargins(10, 5, 0, 5);
            paramsLeft.weight = 1.5f;
            paramsLeft.gravity = Gravity.CENTER;
            // Создаем настройки для поля вывода данных о погоде
            TableRow.LayoutParams paramsRight = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            paramsRight.setMargins(0, 5, 5, 5);
            paramsRight.weight = 0.5f;
            paramsRight.gravity = Gravity.CENTER;
            int cnt = cityPreference.getForecastCount();
            Log.d(LOGTAG, "CNT=" + cnt);
            // Разбираем каждый объект прогноза и заносим в строку таблицы
            for(int i = 0; i < list.length() && i < cnt ; ++i) {
                // Отлавливаем ошибки разбора JSON объектов
                try
                {
                    // Начинаем разбор
                    // Сюда собираем строку - прогноз
                    StringBuilder day5forecastData = new StringBuilder();
                    // Получаем след. объект
                    JSONObject nextJSON = list.getJSONObject(i);
                    // Получаем временную метку прогноза (день на который получен прогноз)
                    Long dateLong = nextJSON.getLong("dt");
                    // Получаем объек описывающий температуру
                    JSONObject temp = nextJSON.getJSONObject("temp");
                    // Описание деталей погоды
                    JSONObject details = nextJSON.getJSONArray("weather").getJSONObject(0);
                    //String ico = details.getString("icon");
                    // Идентификатор иконки
                    int icoID = details.getInt("id");
                    // Преобразуем числовую метку в читабельный вид
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(dateLong * 1000);
                    // Добавляем данные о числе на которое получен прогноз
                    day5forecastData.append(df.format(date).toString());
                    day5forecastData.append("\n" + details.getString("description").toUpperCase(Locale.US));

                    double celsium_min = temp.getDouble("min") - 273.15;
                    double celsium_max = temp.getDouble("max") - 273.15;
                    if(cityPreference.getMetric()) {
                        day5forecastData.append("\n" + String.format("Температура: от %.2f до %.2f", celsium_min, celsium_max) + " ℃");
                    } else {
                        double fahrenheit_min = celsium_min * 9 / 5 + 32;
                        double fahrenheit_max = celsium_min * 9 / 5 + 32;
                        day5forecastData.append("\n" + String.format("Температура: от %.2f до %.2f", fahrenheit_min, fahrenheit_max) + " °F");
                    }

                    day5forecastData.append("\n" + "Влажность: " + nextJSON.getString("humidity") + "%");
                    day5forecastData.append("\n" + "Давление: " + nextJSON.getString("pressure") + " hPa");
                    // Ветер
                    try {
                        day5forecastData.append("\n" + "Ветер: ");
                        day5forecastData.append(nextJSON.getString("speed") + " м/с");
                        day5forecastData.append(", " + nextJSON.getString("deg") + " °");
                    } catch (Exception wex) {}
                    // Создаем строку
                    TableRow tableRow = new TableRow(this.getContext());
                    tableRow.setLayoutParams(rowParams);
                    // Поля вывода
                    TextView icoTextView = new TextView(this.getContext());
                    TextView detailTextView = new TextView(this.getContext());
                    // Выводим иконку
                    icoTextView.setTypeface(weatherFont);
                    icoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
                    icoTextView.setTextColor(Color.parseColor("#0c1b2e"));
                    icoTextView.setText(getWeatherIcon(icoID));
                    // Выводим данные прогноза
                    detailTextView.setText(day5forecastData.toString());
                    // Добавляем поля вывода данных в строку
                    tableRow.addView(icoTextView, paramsLeft);
                    tableRow.addView(detailTextView, paramsRight);
                    // Каждую четную строку пометим другим цветом
                    if(i % 2 == 0)
                        tableRow.setBackgroundResource(R.color.lightRowColor);
                    // Добавляем строку в таблицу
                    table.addView(tableRow);

                }catch (Exception ex) {
                    Log.e(LOGTAG, ex.getMessage());
                }
            }

        }catch(Exception e){
            Log.e(LOGTAG, "One or more fields not found in the JSON data");
        }
        Log.d(LOGTAG, "renderWeather complete");
    }

    private String getWeatherIcon(int actualId){
        //Log.d(LOGTAG, "setWeatherIcon");
        int id = actualId / 100;
        String icon = "";
        if(actualId == 800){
            icon = getActivity().getString(R.string.weather_sunny);
        } else {
            switch(id) {
                case 2 : icon = getActivity().getString(R.string.weather_thunder);
                    break;
                case 3 : icon = getActivity().getString(R.string.weather_drizzle);
                    break;
                case 7 : icon = getActivity().getString(R.string.weather_foggy);
                    break;
                case 8 : icon = getActivity().getString(R.string.weather_cloudy);
                    break;
                case 6 : icon = getActivity().getString(R.string.weather_snowy);
                    break;
                case 5 : icon = getActivity().getString(R.string.weather_rainy);
                    break;
            }
        }
        //weatherIcon.setText(icon);
        return icon;
    }


    public void changeCity(Integer cityID) {
        updateWeatherData(cityID);
    }

}
