package com.bots.snowadventurebot.service;

import com.bots.snowadventurebot.repositories.ResortRepository;
import com.bots.snowadventurebot.utils.ResortEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResortService {
    private final ResortRepository resortRepository;

    public List<ResortEntity> getAll() {
        return resortRepository.findAll();
    }

    public List<ResortEntity> getAll(int regionId){
        return resortRepository.getRegionID(regionId);
    }

    public ResortEntity getByResortId(int resortId){
        return resortRepository.getByResortId(resortId);
    }

}

