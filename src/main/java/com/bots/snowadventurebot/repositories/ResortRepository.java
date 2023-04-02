package com.bots.snowadventurebot.repositories;

import com.bots.snowadventurebot.utils.ResortEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResortRepository extends JpaRepository<ResortEntity, Integer> {

    @Query(value = "SELECT * FROM resort WHERE region_id = :regionId",
            nativeQuery = true)
    List<ResortEntity> getRegionID(int regionId);

    @Query(value = "SELECT * FROM resort WHERE resort_id = :resortId",
    nativeQuery = true)
    ResortEntity getByResortId(int resortId);
}
