package com.wupengfei.coolweather.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wupengfei.coolweather.MainActivity;
import com.wupengfei.coolweather.R;
import com.wupengfei.coolweather.WeatherActivity;
import com.wupengfei.coolweather.db.City;
import com.wupengfei.coolweather.db.County;
import com.wupengfei.coolweather.db.Province;
import com.wupengfei.coolweather.util.HttpUtil;
import com.wupengfei.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by 邬鹏飞 on 2017/6/22.
 */

public class ChooseAreaFragment extends Fragment {

    private int levelProvince = 0;
    private int levelCity = 1;
    private int levelCounty = 2;
    private int currentLevel;//当前选中的级别
    private Province selectedProvince;//选中的省份
    private City selectedCity;//选中的城市
    private List<Province> provinceList;//省份列表
    private List<City> cityList;//城市列表
    private List<County> countyList;//县/区列表
    private TextView tv_title;
    private Button btn_back;
    private ListView lv_listView;
    private ProgressDialog progressDialog;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        tv_title = (TextView) view.findViewById(R.id.tv_title);
        btn_back = (Button) view.findViewById(R.id.btn_back);
        lv_listView = (ListView) view.findViewById(R.id.lv_listView);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        lv_listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        lv_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == levelProvince) {
                    selectedProvince = provinceList.get(position);
                    queryCitys();
                } else if (currentLevel == levelCity) {
                    selectedCity = cityList.get(position);
                    queryCountys();
                } else if (currentLevel == levelCounty) {
                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                        weatherActivity.dl_weather.closeDrawers();
                        weatherActivity.srl_weather.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
                    }
                }


            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == levelCounty) {
                    queryCitys();
                } else if (currentLevel == levelCity) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有则去服务器查询
     */
    private void queryProvinces() {
        tv_title.setText("中国");
        btn_back.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            lv_listView.setSelection(0);
            currentLevel = levelProvince;
        } else {
            queryFromServer("http://guolin.tech/api/china", "province");
        }
    }

    /**
     * 根据选中的省份查询所有的城市，优先从数据库查询，如果没有则去服务器查询
     */
    private void queryCitys() {
        tv_title.setText(selectedProvince.getProvinceName());
        btn_back.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            lv_listView.setSelection(0);
            currentLevel = levelCity;
        } else {
            queryFromServer("http://guolin.tech/api/china/" + selectedProvince.getProvinceCode(), "city");
        }
    }

    /**
     * 根据选中的城市查询所有的县/区，优先从数据库查询，如果没有则去服务器查询
     */
    private void queryCountys() {
        tv_title.setText(selectedCity.getCityName());
        btn_back.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            lv_listView.setSelection(0);
            currentLevel = levelCounty;
        } else {
            queryFromServer("http://guolin.tech/api/china/" + selectedProvince.getProvinceCode() + "/" + selectedCity.getCityCode(), "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省、市、县/区数据
     *
     * @param url
     * @param type
     */
    private void queryFromServer(String url, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(), "数据加载失败，请稍后再试！", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCitys();
                            } else if ("county".equals(type)) {
                                queryCountys();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("数据加载中，请稍后...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
}
