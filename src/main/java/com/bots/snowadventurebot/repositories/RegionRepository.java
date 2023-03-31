package com.bots.snowadventurebot.repositories;

import com.bots.snowadventurebot.utils.RegionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<RegionEntity, Integer> {
}
