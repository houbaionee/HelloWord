package com.chinamobile.cmss.dts.dtsresourcessold.service.capacity;


import com.chinamobile.cmss.dts.dtsresourcessold.dto.QryOfferDto;

import java.util.List;


public interface CapacityService {
     List<QryOfferDto> parseJSONToQryOfferDto(String qryOffer);

     List<String> getKafkaData(String poolId, String poolName, String productCode, String productType, List<QryOfferDto> qryOfferDtoList);

     void sendDataToKafka();

     String getQryOfferFromFile(String file);

}
