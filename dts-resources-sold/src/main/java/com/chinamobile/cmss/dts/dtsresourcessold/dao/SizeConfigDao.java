package com.chinamobile.cmss.dts.dtsresourcessold.dao;

import com.chinamobile.cmss.dts.dtsresourcessold.entity.SizeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SizeConfigDao extends JpaRepository<SizeConfig, Integer> {

    @Query(value = "select * from size_config where size = :size",nativeQuery = true)
    public SizeConfig getSizeConfigBySize(String size);
}
