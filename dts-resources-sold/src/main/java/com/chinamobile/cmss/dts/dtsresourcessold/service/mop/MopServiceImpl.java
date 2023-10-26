package com.chinamobile.cmss.dts.dtsresourcessold.service.mop;

import com.asiainfo.openplatform.secure.RequestUtils;
import com.asiainfo.openplatform.secure.SignUtilsMOP;
import com.asiainfo.openplatform.secure.utils.KeyEntity;
import com.asiainfo.openplatform.secure.utils.ParamEntity;
import com.asiainfo.openplatform.secure.utils.RSAUtil;
import com.chinamobile.bcop.api.sdk.http.HttpResult;

import jdk.nashorn.internal.runtime.regexp.joni.exception.InternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class MopServiceImpl implements MopService{
    @Value("${mop.url}")
    private String URL;

    @Value("${mop.publicKey}")
    private String PUBLIC_KEY;

    @Value("${mop.privateKey}")
    private String PRIVATE_KEY;

    @Value("${mop.appId}")
    private String APP_ID;

    @Value("${mop.status:1}")
    private String STATUS;

    @Value("${mop.order.enable}")
    private boolean mopOrderEnable;

    @Value("${mop.user.enable}")
    private boolean mopUserEnable;

    private static final String QRY_OFFER = "SYAN_UNHQ_qryOffer";


    @Override
    public String getQryOffer(String poolId, String productType) {
        Map<String, String> busiMap = new HashMap<>();
        busiMap.put("poolId", poolId);
        busiMap.put("productType", productType);
        String qryOffer = null;
        try {
            qryOffer = mopBaseRequest(QRY_OFFER, busiMap);
        }catch (InternalException e){
            log.error("调用Mop接口失败");
        }
        return qryOffer;
    }

    private String mopBaseRequest(String method, Map<String, String> busiMap){
        KeyEntity.setPrivateKey(PRIVATE_KEY);
        KeyEntity.setPublicKey(PUBLIC_KEY);
        ParamEntity paramEntity = new ParamEntity(APP_ID, method, STATUS, "json");
        String mopResult;
        try {
            log.info("MOP Request content：" + busiMap);
            String signStr = SignUtilsMOP.doSign(URL, KeyEntity.getPrivateKey(), paramEntity);
            log.info("MOP Signature verification results：" + SignUtilsMOP.verify(signStr, KeyEntity.getPublicKey()));
            HttpResult httpResult = RequestUtils.doPost(signStr, busiMap);
            log.info("MOP Response results：" + httpResult.getCode());
            mopResult = RSAUtil.decryptByPrivateKey(httpResult.getResult(), KeyEntity.getPrivateKey());
            log.info("MOP Response decryption：" + mopResult);
        } catch (Exception ex) {
            log.error("Request for mop interface failed, error message is : " + ex.getMessage());
            throw new InternalException("Request for mop interface failed, error message is :" + ex.getMessage());
        }
        return mopResult;
    }


}
