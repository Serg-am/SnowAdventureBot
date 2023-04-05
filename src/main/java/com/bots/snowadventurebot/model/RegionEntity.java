package com.bots.snowadventurebot.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "region")
public class RegionEntity {
    @Id
    @Column(name = "region_id")
    @GenericGenerator(name = "generator", strategy = "increment")
    @GeneratedValue(generator = "generator")
    private int regionId;

    @Column(name = "region_name")
    private String regionName;
}
