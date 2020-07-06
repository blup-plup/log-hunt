package com.blupplup.loghunt.controllers.Impl;

import com.blupplup.loghunt.controllers.SearchController;
import com.blupplup.loghunt.services.LogIndexService;
import com.blupplup.loghunt.services.LogSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;


@Controller
@RequestMapping(path = "/search")
public class SearchControllerImpl implements SearchController {

    @Autowired
    LogIndexService logIndexService;

    @Autowired
    LogSearchService logSearchService;

    @Override
    @GetMapping("/date")
    public ResponseEntity searchLogs(@RequestParam String startDate,
                                     @RequestParam String endDate,
                                     @RequestParam String folderPath) {
        try{
            logIndexService.indexLogsByDirectory(folderPath);
            logSearchService.getLogsBetween(startDate,endDate,folderPath);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return ResponseEntity.ok("failure");
        }
        return ResponseEntity.ok("success");
    }
}
