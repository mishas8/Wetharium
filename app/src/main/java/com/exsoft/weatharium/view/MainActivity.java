package com.exsoft.weatharium.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TextView;

import com.exsoft.weatharium.R;
import com.exsoft.weatharium.model.CityPreference;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    // Метка для отслеживания отладочных сообщений
    private static final String LOGTAG = "MainActivity";
    // Ссылка на объект для работы с кешем, списком городов и настройками
    private static CityPreference cityPref;
    // Ссылки на визуальные элементы
    private TabHost mTabHost;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private TextView cityField;
    private TextView updatedField;
    // Ссылка на будущий делегат обработчик события изменения настроек
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // Получаем ссылку на объект настроек
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        // Получаем экземпляр объекта хранящего список всех городов и кеш погоды
        cityPref = CityPreference.getInstance(this);
        // Достаем интересующие нас настройки
        // Идентификатор текущего города
        String city_id = sp.getString("key_pref_city", String.valueOf(CityPreference.DEFAULT_CITY_ID));
        // Количество прогнозов погоды
        int forecast_count = sp.getInt("key_weather_cnt", CityPreference.DEFAULT_FORECAST_COUNT);
        // Предпочитаемые единицы измерения температуры (Цельсий/Фаренгейт)
        boolean metric = sp.getBoolean("key_metric_units", CityPreference.DEFAULT_TEMP_METRIC);
        // Заносим данные о считанных настройках в объект
        cityPref.InitDefaults(Integer.parseInt(city_id), forecast_count, metric);
        // Создаем обработчик срабатывающий при изменении настроек
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.d(LOGTAG, "KEY CHANGED=" + key);
                // Если произошли изменения настроек города или количества отображаемых прогнозов - обновляем полностью данные
                // Получаем текущий идентификатор города
                String cityName = sp.getString("key_pref_city", String.valueOf(CityPreference.DEFAULT_CITY_ID));//"Санкт-Петербург");
                // Получаем количество прогнозов
                int cnt = sp.getInt("key_weather_cnt", CityPreference.DEFAULT_FORECAST_COUNT);
                boolean units = sp.getBoolean("key_metric_units", true);
                Log.d(LOGTAG, "------------------- onResume, city=" + cityName);
                Log.d(LOGTAG, "------------------- onResume, cnt=" + cnt);
                Log.d(LOGTAG, "------------------- onResume, units=" + units);
                cityPref.setForecastCount(cnt);
                cityPref.setMetric(units);
                changeCity(Integer.parseInt(cityName));
            }
        };
        // Прикрепляем обработчик
        sp.registerOnSharedPreferenceChangeListener(prefListener);
        // Получаем элементы находящиеся в тулбаре (Надпись города и последнего времени обновления данных на сервере)
        cityField = (TextView)findViewById(R.id.city_field);
        updatedField = (TextView)findViewById(R.id.updated_field);
        // Ищем элемент в который сложим вкладки
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
        // Добавляем вкладки
        mTabsAdapter.addTab(mTabHost.newTabSpec("simple1").setIndicator(getString(R.string.weather_label)), WeatherActivity.class, null);
        mTabsAdapter.addTab(mTabHost.newTabSpec("simple2").setIndicator(getString(R.string.forecast_label)), ForecastActivity.class, null);
        mTabsAdapter.addTab(mTabHost.newTabSpec("simple3").setIndicator(getString(R.string.day5_forecast_label)), Day5ForecastActivity.class, null);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.weather_content, new WeatherActivity())
                    .commit();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.forecast_content, new ForecastActivity())
                    .commit();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.day5_forecast_content, new Day5ForecastActivity())
                    .commit();
        }
    }
    // Метод смены города
    public void changeCity(int cityID){
        // Получаем ссылки на фрагменты активити
        WeatherActivity weather = (WeatherActivity)getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.pager + ":0");

        ForecastActivity forecast = (ForecastActivity)getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.pager + ":1");

        Day5ForecastActivity day5Forecast = (Day5ForecastActivity)getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.pager + ":2");

        // Проверим полученные ссылки
        if(weather == null) {
            // Пишем в лог
            Log.d(LOGTAG, "changeCity, weather==null");
            // Прерываем выполнение
            return;
        }
        if(forecast == null) {
            // Пишем в лог
            Log.d(LOGTAG, "changeCity, forecast==null");
            // Прерываем выполнение
            return;
        }
        if(day5Forecast == null) {
            // Пишем в лог
            Log.d(LOGTAG, "changeCity, day5Forecast==null");
            // Прерываем выполнение
            return;
        }
        // Обновляем всю информацию.
        // Обращаемся к каждому Fragment`у и вызываем смену города.
        // Внутри каждого из фрагментов произойдет вызов обновления информации
        weather.changeCity(cityID);
        forecast.changeCity(cityID);
        day5Forecast.changeCity(cityID);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if(id == R.id.action_update) {
            changeCity(cityPref.getCity().ID());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_update) {
            changeCity(cityPref.getCity().ID());
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}