package com.blupplup.loghunt.services;

import java.io.IOException;

public interface LogIndexService {

    void indexLogsOnStartup() throws IOException;

    void indexLogsScheduled() throws IOException;

    void indexLogsByDirectory(String directoryPath) throws IOException;
}
