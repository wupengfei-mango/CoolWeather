package com.wupengfei.coolweather.db;

import org.litepal.crud.DataSupport;

/**
 * 省数据信息
 * <p>
 * Created by 邬鹏飞 on 2017/6/22.
 */

public class Province extends DataSupport {

    private int id;//省份ID
    private String provinceName;//省份名称
    private int provinceCode;//省份代号

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}
