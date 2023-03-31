/*package com.bots.snowadventurebot.utils;

import com.bots.snowadventurebot.service.ResortService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InitiateUtils implements CommandLineRunner {
    private final ResortService resortService;

    public InitiateUtils(ResortService resortService) {
        this.resortService = resortService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Run");

        List<ResortEntity> allList = resortService.getAll();

        for (ResortEntity entity : allList){
            System.out.println(entity.getResortDescription());
        }
    }
}*/
