package com.chinamobile.cmss.dts.dtsresourcessold.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "size_config")
@Data
public class SizeConfig {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "size")
    private String size;

    @Column(name = "cpu")
    private double cpu;

    @Column(name = "memory")
    private double memory;

}
