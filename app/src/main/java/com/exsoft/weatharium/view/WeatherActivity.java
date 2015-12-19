package com.exsoft.weatharium.view;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.exsoft.weatharium.R;
import com.exsoft.weatharium.model.CityPreference;
import com.exsoft.weatharium.utils.RemoteFetch;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherActivity extends Fragment {

    private static final String LOGTAG = "WeatherActivity";

    public static Typeface weatherFont;

    TextView cityField;
    TextView updatedField;
    TextView detailsField;
    TextView currentTemperatureField;
    TextView weatherIcon;

    Handler handler;
    CityPreference cityPreference;

    public WeatherActivity(){
        Log.d(LOGTAG, "--new WeatherActivity");
        // Получаем ссылку на объект контроллера (кеш, настройки, список городов)
        this.cityPreference = CityPreference.getInstance(this.getActivity());
        handler = new Handler();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        // Читаем шрифт (используем для отображения значков погоды)
        weatherFont = Typeface.createFromAsset(getActivity().getAssets(), "fonts/weather.ttf");
        // Вызываем считывание данныех с сервера и последующее обновление интерфейса
        updateWeatherData(cityPreference.getCity().ID());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.weather, container, false);
        // Получаем ссылки на визуальные элементы
        cityField = (TextView)getActivity().findViewById(R.id.city_field);
        updatedField = (TextView)getActivity().findViewById(R.id.updated_field);
        detailsField = (TextView)rootView.findViewById(R.id.details_field);
        currentTemperatureField = (TextView) rootView.findViewById(R.id.current_temperature_field);
        weatherIcon = (TextView)rootView.findViewById(R.id.weather_icon);
        weatherIcon.setTypeface(weatherFont);

        return rootView;
    }

    private void updateWeatherData(final Integer cityID) {
        // Работает с сервером новом потоке
        new Thread(){
            public void run(){
                Log.d(LOGTAG, "UpdateWeatherData RemoteFetch start, cityID=" + cityID);
                // Получаем текущие данные погоды, для выбранного города
                final JSONObject json = RemoteFetch.getJSON(getActivity(), RemoteFetch.DATA_TYPE.weather, cityID, cityPreference.getForecastCount());
                // Если данные не получены по каким то причинам (нет связи с сервером/не доступна сеть/превышен лимит обращений к серверу и т.д.)
                if(json == null){
                    handler.post(new Runnable(){
                        public void run(){
                            Log.d(LOGTAG, "UpdateWeatherData RemoteFetch json==null");
                            // Пробуем загрузить данные из кеша
                            JSONObject cache = cityPreference.getCacheData(cityID, CityPreference.CACHE_DATA_TYPE.weather);
                            // Если и в кеше нет данных
                            if(cache == null) {
                                Log.d(LOGTAG, "UpdateWeatherData RemoteFetch result=place_not_found");
                                Log.d(LOGTAG, "cache json==null");
                                try {
                                    updatedField.setText("n/a");
                                    weatherIcon.setText("n/a");
                                    detailsField.setText("n/a");
                                    currentTemperatureField.setText("n/a");
                                    // Выводим сообщение что данных для отображения нет
                                    Toast.makeText(getActivity(),
                                            getActivity().getString(R.string.place_not_found),
                                            Toast.LENGTH_LONG).show();
                                } catch (Exception ex) {
                                    Log.d(LOGTAG, "UpdateWeatherData RemoteFetch error=" + ex.getMessage());
                                }
                            } else {
                                // Если данные считали из кеша
                                Log.d(LOGTAG, "RENDER CACHE DATA for city=" + cityID);

                                // Отрисовываем
                                renderWeather(cache);
                                // Меняем в настройках город по умолчанию
                                cityPreference.setCity(cityID);
                            }
                        }
                    });
                } else {
                    // Если данные получены
                    handler.post(new Runnable(){
                        public void run(){
                            Log.d(LOGTAG, "UpdateWeatherData RemoteFetch result=OK");
                            // Отображаем
                            renderWeather(json);
                            // Данные в кеш
                            cityPreference.setCacheData(cityID, CityPreference.CACHE_DATA_TYPE.weather, json);
                            cityPreference.setCity(cityID);
                        }
                    });
                }
                //progress.dismiss();
            }
        }.start();

    }

    private void renderWeather(JSONObject json){
        Log.d(LOGTAG, "renderWeather start, JSON=" + json.toString());
        try {
            cityField.setText(json.getString("name").toUpperCase(Locale.US) +
                    ", " +
                    json.getJSONObject("sys").getString("country"));
            // Сюда собираем данные о погоде
            StringBuilder weatherData = new StringBuilder();

            JSONObject main = json.getJSONObject("main");
            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            weatherData.append(details.getString("description").toUpperCase(Locale.US));
            weatherData.append("\n" + "Влажность: " + main.getString("humidity") + "%");
            weatherData.append("\n" + "Давление: " + main.getString("pressure") + " hPa");
            try {
                weatherData.append("\n" + "Ветер: ");
                weatherData.append(json.getJSONObject("wind").getString("speed") + " м/с");
                weatherData.append(", " + json.getJSONObject("wind").getString("deg") + " °");
                weatherData.append("\n" + "Облачность: " + json.getJSONObject("clouds").getString("all") + " %");
            } catch(Exception ex) {}
            detailsField.setText(weatherData.toString());

            try {
                double celsium = main.getDouble("temp") - 273.15;
                if (cityPreference.getMetric()) {
                    currentTemperatureField.setText(
                            String.format("%.2f", celsium) + " ℃");
                } else {
                    double fahrenheit = celsium * 9 / 5 + 32;
                    currentTemperatureField.setText(
                            String.format("%.2f", fahrenheit) + " °F");
                }
            }
            catch (Exception ex) {
                currentTemperatureField.setText("n/a");
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");
            String updatedOn = dateFormat.format(new Date(json.getLong("dt")*1000));
            updatedField.setText("Last update: " + updatedOn);

            setWeatherIcon(details.getInt("id"),
                    json.getJSONObject("sys").getLong("sunrise") * 1000,
                    json.getJSONObject("sys").getLong("sunset") * 1000);

        }catch(Exception e){
            Log.e(LOGTAG, "renderWeather exception: [One or more fields not found in the JSON data], error=" + e.getMessage());
        }
        Log.d(LOGTAG, "renderWeather complete");
    }

    private void setWeatherIcon(int actualId, long sunrise, long sunset){
        Log.d(LOGTAG, "setWeatherIcon");
        int id = actualId / 100;
        String icon = "";
        if(actualId == 800){
            long currentTime = new Date().getTime();
            if(currentTime>=sunrise && currentTime<sunset) {
                icon = getActivity().getString(R.string.weather_sunny);
            } else {
                icon = getActivity().getString(R.string.weather_clear_night);
            }
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
        weatherIcon.setText(icon);
    }

    public void changeCity(Integer cityID){
        updateWeatherData(cityID);
    }

}