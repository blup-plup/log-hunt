package com.blupplup.loghunt.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@Order(1)
public class StartUpService implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    LogIndexService logIndexService;

    private static final Logger logger = LoggerFactory.getLogger(StartUpService.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.info("loading and indexing log files...");
        int retryCount = 3;
        while(true){
            try{
                logIndexService.indexLogsOnStartup();
                break;
            } catch(IOException ioException){
                logger.info("Retry {} loading log files...",retryCount);
                if(retryCount-- <= 1){
                    break;
                }
            }
        }
    }
}
