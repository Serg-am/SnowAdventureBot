package com.bots.snowadventurebot.repositories;

import com.bots.snowadventurebot.model.RegionEntity;
import com.bots.snowadventurebot.model.ResortEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionRepository extends JpaRepository<RegionEntity, Integer> {
    @Query(value = "SELECT * FROM region WHERE region_id = :regionId",
            nativeQuery = true)
    RegionEntity getByResortId(int regionId);
}
