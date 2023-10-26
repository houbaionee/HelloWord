package com.chinamobile.cmss.dts.dtsresourcessold.service.mop;

public interface MopService {

    /**
     * 根据资源池ID和产品类型获取局数据
     *
     * @param poolId      资源池Id
     * @param productType 产品类型
     * @return
     */
    String getQryOffer(String poolId, String productType);
}
