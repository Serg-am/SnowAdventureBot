package com.bots.snowadventurebot.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "resort")
public class ResortEntity {
    @Id
    @Column(name = "resort_id")
    @GenericGenerator(name = "generator", strategy = "increment")
    @GeneratedValue(generator = "generator")
    private int resortId;

    @Column(name = "region_id")
    private int regionId;
    @Column(name = "resort_name")
    private String resortName;

    @Column(name = "resort_telephone")
    private String resortTelephone;

    @Column(name = "resort_web_site")
    private String resortWebSite;

    @Column(name = "resort_description")
    private String resortDescription;

    @Column(name = "weather_region")
    private String weatherRegion;
}
