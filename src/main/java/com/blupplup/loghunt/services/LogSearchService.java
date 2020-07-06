package com.blupplup.loghunt.services;

import java.io.IOException;

public interface LogSearchService{

    void getLogsBetween(String startDate, String endDate, String directoryPath) throws IOException;

}
