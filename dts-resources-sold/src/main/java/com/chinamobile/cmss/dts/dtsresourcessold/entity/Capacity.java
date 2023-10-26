package com.chinamobile.cmss.dts.dtsresourcessold.entity;


import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Capacity {

    //资源池名称
    private String region;

    //资源池Id
    @JSONField(name = "region_code")
    private String regionCode;

    //可用区名称
    private String az;

    //可用区编码
    @JSONField(name = "az_code")
    private String azCode;

    //商品名称
    private String product;

    //商品编码
    @JSONField(name = "product_code")
    private String productCode;

    //规格名称
    private String flavor;

    //规格编码
    @JSONField(name="flavor_code")
    private String flavorCode;

    //超分比例
    @JSONField(name = "allocation_rate")
    private String allocationRate;

    @JSONField(name = "create_time")
    private String createTime;

    @JSONField(name = "group_type")
    private int groupType;

    @JSONField(name = "unit")
    private String unit;

    private Value value;

    @Data
    public static class Value {
        @JSONField(name = "sold_available")
        private int soldAvailable;

    }





}

