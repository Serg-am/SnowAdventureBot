package com.bots.snowadventurebot.service;

import com.bots.snowadventurebot.repositories.RegionRepository;
import com.bots.snowadventurebot.utils.RegionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionRepository regionRepository;

    public List<RegionEntity> getAll() {
        return regionRepository.findAll();
    }

}
