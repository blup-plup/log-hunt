package com.blupplup.loghunt.services.Impl;

import com.blupplup.loghunt.common.Pair;
import com.blupplup.loghunt.services.LogIndexService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

@Service
public class LogIndexServiceImpl implements LogIndexService {

    @Value("${default.logs.folder}")
    private String LOG_FOLDER_PATH;

    @Value("${default.logs.index.file}")
    private String LOG_INDEX_FILE_PATH;

    private static final Logger logger = LoggerFactory.getLogger(LogIndexServiceImpl.class);


    @Override
    public void indexLogsOnStartup() throws IOException {
        logger.info("searching for log files in {}",LOG_FOLDER_PATH);

        File rootFolder = FileUtils.getFile(LOG_FOLDER_PATH);


        if(!rootFolder.exists()){
            throw new IOException("directory {} " + LOG_FOLDER_PATH+" not found");
        }

        Map<String, Pair> fileTimeRangeMap = getLogFileList(rootFolder);

        logger.info("got a list of {} log files",fileTimeRangeMap.size());

        File logIndexFile = new File(LOG_INDEX_FILE_PATH);

        long lastModifiedTimestamp = logIndexFile.lastModified();
        long timeStampNow = System.currentTimeMillis()/1000l;

        if(timeStampNow - lastModifiedTimestamp > 86400){
            logger.info("log index already exists");
            return;
        }

        FileUtils.deleteQuietly(logIndexFile);

        logger.info("writing log file index to file {}",LOG_INDEX_FILE_PATH);

        setLogFileList(fileTimeRangeMap,LOG_INDEX_FILE_PATH);

        logger.info("Done indexing log files");
    }

    @Override
    @Scheduled(cron = "0 2 * * *")
    public void indexLogsScheduled() throws IOException {
        logger.info("running scheduled indexing of log files in {}",LOG_FOLDER_PATH);

        File rootFolder = FileUtils.getFile(LOG_FOLDER_PATH);


        if(!rootFolder.exists()){
            throw new IOException("directory {} " + LOG_FOLDER_PATH +" not found");
        }

        Map<String,Pair> fileTimeRangeMap = getLogFileList(rootFolder);

        FileUtils.deleteQuietly(new File(LOG_INDEX_FILE_PATH));

        logger.info("got a list of {} log files",fileTimeRangeMap.size());

        logger.info("writing log file index to file {}",LOG_INDEX_FILE_PATH);
        setLogFileList(fileTimeRangeMap,LOG_INDEX_FILE_PATH);

        logger.info("Done scheduled indexing of log files");
    }


    private Map<String,Pair> getLogFileList(File root) throws IOException {
        Iterator<File> filesList = FileUtils.iterateFiles(root, TrueFileFilter.INSTANCE,null);

        Map<String, Pair> fileTimeRangeMap = new LinkedHashMap<>();

        while(filesList.hasNext()){

            File logFile = filesList.next();

            if(!logFile.getName().startsWith("LogFile")){
                continue;
            }

            //reading last line
            ReversedLinesFileReader reverseReader = new ReversedLinesFileReader(logFile, Charset.defaultCharset());
            String lastLine = reverseReader.readLine();
            String[] lastLineSplit = lastLine.split(",");

            String endTime = lastLineSplit[0];

            //reading first line
            LineIterator reader = FileUtils.lineIterator(logFile, "UTF-8");
            String firstLine = reader.nextLine();
            String[] firstLineSplit = firstLine.split(",");

            String startTime = firstLineSplit[0];

            fileTimeRangeMap.put(logFile.getName(),new Pair(startTime,endTime));
        }

        return fileTimeRangeMap;
    }


    private void setLogFileList(Map<String,Pair> fileTimeRangeMap, String logIndexFilePath) throws IOException {
        File logFileTimeIndex = new File(logIndexFilePath);

        for(String filename: fileTimeRangeMap.keySet()){
            Pair timeRange = fileTimeRangeMap.get(filename);
            String lineToWrite = filename+" "+timeRange.getKey()+" "+timeRange.getValue();

            FileUtils.writeLines(logFileTimeIndex, Collections.singleton(lineToWrite),true);
        }
    }


    @Override
    public void indexLogsByDirectory(String directoryPath) throws IOException {
        logger.info("searching for log files in {}",directoryPath);

        File rootFolder = FileUtils.getFile(directoryPath);

        if(!rootFolder.exists()){
            throw new IOException("directory {} " + directoryPath+" not found");
        }

        String logIndexFilePath = directoryPath+"/"+"logIndex.txt";

        Map<String,Pair> fileTimeRangeMap = getLogFileList(rootFolder);

        logger.info("got a list of {} log files",fileTimeRangeMap.size());


        File logIndexFile = new File(logIndexFilePath);

        long lastModifiedTimestamp = logIndexFile.lastModified()/1000l;
        long timeStampNow = System.currentTimeMillis()/1000l;

        if(timeStampNow - lastModifiedTimestamp < 86400){
                logger.info("log index already exists");
                return;
        }
        FileUtils.deleteQuietly(new File(logIndexFilePath));
        logger.info("writing log file index to file {}",logIndexFilePath);

        setLogFileList(fileTimeRangeMap,logIndexFilePath);

        logger.info("Done indexing log files");
    }
}
