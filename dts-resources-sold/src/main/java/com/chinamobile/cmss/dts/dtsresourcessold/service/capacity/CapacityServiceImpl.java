package com.chinamobile.cmss.dts.dtsresourcessold.service.capacity;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chinamobile.cmss.dts.dtsresourcessold.dao.SizeConfigDao;
import com.chinamobile.cmss.dts.dtsresourcessold.dto.QryOfferDto;
import com.chinamobile.cmss.dts.dtsresourcessold.entity.Attr;
import com.chinamobile.cmss.dts.dtsresourcessold.entity.Capacity;
import com.chinamobile.cmss.dts.dtsresourcessold.entity.SizeConfig;
import com.chinamobile.cmss.dts.dtsresourcessold.entity.Zone;
import com.chinamobile.cmss.dts.dtsresourcessold.service.kubenetes.K8sService;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class CapacityServiceImpl implements CapacityService {
    @Value("${capacity.regionCode}")
    private String regionCode;

    @Value("${capacity.region}")
    private String region;

    @Value("${capacity.productCode}")
    private String productCode;

    @Value("${capacity.productType}")
    private String productType;

    @Autowired
    private SizeConfigDao sizeConfigDao;

    @Autowired
    private K8sService k8sService;

    @Autowired
    private KafkaProducerServiceImpl kafkaProducerService;

    public static final String UNIT = "个";

    public static final int GROUPTYPE = 2;


    @Scheduled(fixedRate = 60000)
    public void sendDataToKafka() {
//        String qryOffer = mopService.getQryOffer(poolId, productType);
        //从文件获取局数据模拟mop返回的json
        String file = "./conf/qryOffer.json";
        String qryOffer = getQryOfferFromFile(file);
        //解析局数据然后封装到QryOfferDto
        List<QryOfferDto> qryOfferDtoList = parseJSONToQryOfferDto(qryOffer);
        //根据qryOfferDtoList得到发送到kafka的数据
        List<String> kafkaData = getKafkaData(regionCode, region, productCode, productType, qryOfferDtoList);
        //发送数据到kafka，每个一分钟发送一次
        for (String str : kafkaData) {
            kafkaProducerService.sendMessage(str);
        }

    }

    @Override
    public List<QryOfferDto> parseJSONToQryOfferDto(String qryOffer) {
        List<QryOfferDto> qryOfferDtoList = new ArrayList<>();
        if (qryOffer == null) {
            return qryOfferDtoList;
        }
        //需要考虑qryOffer body中为空的情况
        JSONObject jsonObject = JSON.parseObject(qryOffer);
        JSONObject body = jsonObject.getJSONObject("result")
                .getJSONArray("body")
                .getJSONObject(0);
        if (body == null) {
            log.warn("返回的Body数据为空");
            return qryOfferDtoList;
        }

        JSONArray productChasArray = body.getJSONArray("productChas");
        for (int i = 0; i < productChasArray.size(); ++i) {
            QryOfferDto qryOfferDto = new QryOfferDto();
            //设置chaId
            qryOfferDto.setChaId(productChasArray.getJSONObject(i).getString("chaId"));
            //设置chaName
            qryOfferDto.setChaName(productChasArray.getJSONObject(i).getString("chaName"));
            //设置zoneList
            JSONArray poolsArray = productChasArray.getJSONObject(i).getJSONArray("pools")
                    .getJSONObject(0)
                    .getJSONArray("zoneList");
            List<Zone> zoneList = new ArrayList<>();
            for (int j = 0; j < poolsArray.size(); ++j) {
                Zone zone = new Zone();
                zone.setZoneId(poolsArray.getJSONObject(j).getString("zoneId"));
                zone.setZoneCode(poolsArray.getJSONObject(j).getString("zoneCode"));
                zone.setZoneName(poolsArray.getJSONObject(j).getString("zoneName"));
                zoneList.add(zone);
            }
            qryOfferDto.setZoneList(zoneList);

            //设置attrList
            JSONArray attrsArray = productChasArray.getJSONObject(i).getJSONArray("attrs");
            List<Attr> attrsList = new ArrayList<>();
            for (int k = 0; k < attrsArray.size(); ++k) {
                Attr attr = new Attr();
                attr.setAttrId(attrsArray.getJSONObject(k).getString("attrId"));
                attr.setAttrCode(attrsArray.getJSONObject(k).getString("attrCode"));
                attr.setAttrVal(attrsArray.getJSONObject(k).getString("attrVal"));
                attr.setAttrName(attrsArray.getJSONObject(k).getString("attrName"));
                attrsList.add(attr);
            }
            qryOfferDto.setAttrList(attrsList);
            qryOfferDtoList.add(qryOfferDto);

        }
        return qryOfferDtoList;
    }

    @Override
    public List<String> getKafkaData(String poolId, String poolName, String productCode, String productType, List<QryOfferDto> qryOfferDtoList) {
        List<String> kafkaData = new ArrayList<>();
        for (QryOfferDto qryOfferDto : qryOfferDtoList) {
            //查询每个规格对应的CPU和Memory
            String size = null;
            String arch = null;
            for (Attr attr : qryOfferDto.getAttrList()) {
                if (attr.getAttrCode().equals("size")) {
                    size = attr.getAttrVal();
                }
                if (attr.getAttrCode().equals("arch")){
                    arch = attr.getAttrVal();
                }
            }
            //查询该规格占用的cpu和memory
            SizeConfig sizeConfig = sizeConfigDao.getSizeConfigBySize(size);
            if (sizeConfig == null) {
                throw new RuntimeException("在数据库中没有找到对应的DTS规格大小");
            }
            double cpu = sizeConfig.getCpu();
            double memory = sizeConfig.getMemory();
            //双向传输占用资源为单向传输的2倍
            if ("bidirectional".equals(arch)){
                cpu *= 2;
                memory *= 2;
            }


//            //现网操作查询每个规格对应的可用区列表
//            List<Zone> zoneList = qryOfferDto.getZoneList();
//            //传入poolId zone
//            KubernetesClient client = k8sService.getClient();
//            for (Zone zone:zoneList){
//                int soldAvailable = k8sService.getResidualInstanceForEveryZone(client, poolId, zone.getZoneId(), cpu, memory);
//                Capacity.Value value = new Capacity.Value();
//                value.setSoldAvailable(soldAvailable);
//                Capacity capacity = new Capacity(poolName,poolId,zone.getZoneName(),null,productType,productCode,
//                        qryOfferDto.getChaName(),qryOfferDto.getChaId(),null,
//                        DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"),
//                        GROUPTYPE, UNIT, value);
//                kafkaData.add(JSON.toJSONString(capacity));
//            }
            //现网机器进行测试
            KubernetesClient client = k8sService.getClient();
            int soldAvailable = k8sService.getResidualInstanceForEveryZone(client, cpu, memory);
            Capacity.Value value = new Capacity.Value();
            value.setSoldAvailable(soldAvailable);
            Capacity capacity = new Capacity(poolName, poolId, "可用区一", null, productType, productCode,
                    qryOfferDto.getChaName(), qryOfferDto.getChaId(), null,
                    DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"),
                    GROUPTYPE, UNIT, value);
            kafkaData.add(JSON.toJSONString(capacity));
            k8sService.closeClient(client);

        }
        return kafkaData;
    }

    public String getQryOfferFromFile(String file) {
        StringBuffer buffer = null;
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            buffer = new StringBuffer();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            log.error("读取json文件失败", e);
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    log.error("关闭资源失败", e);
                }
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.error("关闭资源失败", e);
                }
            }

        }
        return buffer == null ? null : buffer.toString();
    }
}
