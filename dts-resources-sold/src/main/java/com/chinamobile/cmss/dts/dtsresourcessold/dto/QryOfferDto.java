package com.chinamobile.cmss.dts.dtsresourcessold.dto;

import com.chinamobile.cmss.dts.dtsresourcessold.entity.Attr;

import com.chinamobile.cmss.dts.dtsresourcessold.entity.Zone;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QryOfferDto {
    private String chaId;
    private String chaName;
    private List<Zone> zoneList;
    private List<Attr> attrList;
}

