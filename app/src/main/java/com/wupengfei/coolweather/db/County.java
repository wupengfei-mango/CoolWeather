package com.wupengfei.coolweather.db;

import org.litepal.crud.DataSupport;

/**
 * 县数据信息
 * <p>
 * Created by 邬鹏飞 on 2017/6/22.
 */

public class County extends DataSupport {

    private int id;//县ID
    private String countyName;//县名称
    private String weatherId;//县所对应天气ID
    private int cityId;//当前县所属市的id

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public String getWeatherId() {
        return weatherId;
    }

    public void setWeatherId(String weatherId) {
        this.weatherId = weatherId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }
}
