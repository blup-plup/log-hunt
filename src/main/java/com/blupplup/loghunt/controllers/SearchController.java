package com.blupplup.loghunt.controllers;

import org.springframework.http.ResponseEntity;


public interface SearchController {

    ResponseEntity searchLogs(String startDate, String endDate, String folderPath);

}
