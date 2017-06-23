package com.wupengfei.coolweather;

import android.content.SharedPreferences;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.annotations.Until;
import com.wupengfei.coolweather.gson.Weather;
import com.wupengfei.coolweather.util.HttpUtil;
import com.wupengfei.coolweather.util.Utility;

import org.json.JSONArray;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ImageView iv_bing_pic;
    private ScrollView sv_weather;
    private TextView tv_titleName;
    private TextView tv_updateTime;
    private TextView tv_degree;
    private TextView tv_weatherInfo;
    private LinearLayout forecast_layout;
    private TextView tv_aqi;
    private TextView tv_pm;
    private TextView tv_comfort;
    private TextView tv_carWash;
    private TextView tv_sport;
    public SwipeRefreshLayout srl_weather;
    public DrawerLayout dl_weather;
    private Button btn_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        iv_bing_pic = (ImageView) findViewById(R.id.iv_bing_pic);
        sv_weather = (ScrollView) findViewById(R.id.sv_weather);
        tv_titleName = (TextView) findViewById(R.id.tv_titleName);
        tv_updateTime = (TextView) findViewById(R.id.tv_updateTime);
        tv_degree = (TextView) findViewById(R.id.tv_degree);
        tv_weatherInfo = (TextView) findViewById(R.id.tv_weatherInfo);
        forecast_layout = (LinearLayout) findViewById(R.id.forecast_layout);
        tv_aqi = (TextView) findViewById(R.id.tv_aqi);
        tv_pm = (TextView) findViewById(R.id.tv_pm);
        tv_comfort = (TextView) findViewById(R.id.tv_comfort);
        tv_carWash = (TextView) findViewById(R.id.tv_carWash);
        tv_sport = (TextView) findViewById(R.id.tv_sport);
        srl_weather = (SwipeRefreshLayout) findViewById(R.id.srl_weather);
        dl_weather = (DrawerLayout) findViewById(R.id.dl_weather);
        btn_home = (Button) findViewById(R.id.btn_home);

        srl_weather.setColorSchemeResources(R.color.colorPrimary);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String bingPic = sharedPreferences.getString("bingPic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(iv_bing_pic);
        } else {
            loadBingPic();
        }
        final String weatherId;
        String weatherString = sharedPreferences.getString("weather", null);
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather.HeWeatherBean weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.getBasic().getId();
            showWeatherInfo(weather);
        } else {
            //获取服务器最新数据
            weatherId = getIntent().getStringExtra("weather_id");
            sv_weather.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }

        srl_weather.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });

        btn_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dl_weather.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String url = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putString("bingPic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Glide.with(getApplicationContext()).load(bingPic).into(iv_bing_pic);
                    }
                });
            }
        });
    }

    /**
     * 根据天气ID请求城市天气信息
     *
     * @param weatherId
     */
    public void requestWeather(final String weatherId) {
//        String weatherUrl = "https://free-api.heweather.com/v5/forecast?cityid=" + weatherId + "&key=1f48091bc58843fcbc63b2855f545226";
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=1f48091bc58843fcbc63b2855f545226";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "天气数据获取失败，请稍后再试！", Toast.LENGTH_SHORT).show();
                        srl_weather.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather.HeWeatherBean weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.getStatus())) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(getApplicationContext(), "天气数据获取失败，请稍后再试！", Toast.LENGTH_SHORT).show();
                        }
                        srl_weather.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    /**
     * 显示天气数据
     *
     * @param weather
     */
    private void showWeatherInfo(Weather.HeWeatherBean weather) {
        tv_titleName.setText(weather.getBasic().getCity());
        tv_updateTime.setText(weather.getBasic().getUpdate().getLoc().split(" ")[0]);
        tv_degree.setText(weather.getNow().getTmp() + " °C");
        tv_weatherInfo.setText(weather.getNow().getCond().getTxt());

        forecast_layout.removeAllViews();
        for (int i = 0; i < weather.getDaily_forecast().size(); i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecast_layout, false);
            TextView tv_date = (TextView) view.findViewById(R.id.tv_date);
            TextView tv_info = (TextView) view.findViewById(R.id.tv_info);
            TextView tv_max = (TextView) view.findViewById(R.id.tv_max);
            TextView tv_min = (TextView) view.findViewById(R.id.tv_min);
            tv_date.setText(weather.getDaily_forecast().get(i).getDate());
            tv_info.setText(weather.getDaily_forecast().get(i).getCond().getTxt_d());
            tv_min.setText("↓：" + weather.getDaily_forecast().get(i).getTmp().getMin());
            tv_max.setText("↑：" + weather.getDaily_forecast().get(i).getTmp().getMax());
            forecast_layout.addView(view);
        }
        if (weather.getAqi() != null) {
            tv_aqi.setText(weather.getAqi().getCity().getAqi());
            tv_pm.setText(weather.getAqi().getCity().getPm25());
        }
        tv_comfort.setText("舒适度：" + weather.getSuggestion().getComf().getBrf() + "\n\n" + weather.getSuggestion().getComf().getTxt());
        tv_carWash.setText("洗车指数：" + weather.getSuggestion().getCw().getBrf() + "\n\n" + weather.getSuggestion().getCw().getTxt());
        tv_sport.setText("运动建议：" + weather.getSuggestion().getSport().getBrf() + "\n\n" + weather.getSuggestion().getSport().getTxt());
        sv_weather.setVisibility(View.VISIBLE);
    }
}
