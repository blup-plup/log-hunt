package com.blupplup.loghunt.services.Impl;

import com.blupplup.loghunt.services.LogSearchService;
import com.github.sisyphsu.dateparser.DateParserUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
public class LogSearchServiceImpl implements LogSearchService {

    private static final Logger logger = LoggerFactory.getLogger(LogSearchServiceImpl.class);

    private static final long FILE_SEGMENT_SIZE = 1024l*1024l;

    @Override
    public void getLogsBetween(String startDateString, String endDateString, String directoryPath) throws IOException{

        LocalDateTime startDate = DateParserUtils.parseDateTime(startDateString);
        LocalDateTime endDate = DateParserUtils.parseDateTime(endDateString);

        List<String> logFileList = getFileListByRange(startDate,endDate,directoryPath);
        logFileList.sort(Comparator.naturalOrder());
        for(String logFile: logFileList){
            searchFile(startDate,endDate,directoryPath+"/"+logFile);
        }
    }

    private List<String> getFileListByRange(LocalDateTime startQueryDate, LocalDateTime endQueryDate, String directoryPath) throws IOException {
        File logIndexFile = FileUtils.getFile(directoryPath+"/logIndex.txt");
        if(!logIndexFile.exists()){
            throw new IOException("logIndex file not found");
        }
        LineIterator lineIterator = FileUtils.lineIterator(logIndexFile);

        List<String> logFileList = new ArrayList<>();

        while(lineIterator.hasNext()){
            String line = lineIterator.nextLine();
            String[] logLineArr = line.split(" ");

            String endTimeString = logLineArr[2];
            String startTimeString = logLineArr[1];
            String fileName = logLineArr[0];

            LocalDateTime endTime = DateParserUtils.parseDateTime(endTimeString);
            LocalDateTime startTime = DateParserUtils.parseDateTime(startTimeString);

            if((startQueryDate.isAfter(startTime) || startQueryDate.isEqual(startTime)) && (startQueryDate.isBefore(endTime))){
                logFileList.add(fileName);
            }
            else if((endQueryDate.isBefore(endTime) || endQueryDate.isEqual(endTime)) && (endQueryDate.isAfter(startTime))){
                logFileList.add(fileName);
            }
            else if((startQueryDate.isEqual(startTime) || startQueryDate.isBefore(startTime)) && (endQueryDate.isEqual(endTime) || endQueryDate.isAfter(endTime))){
                logFileList.add(fileName);
            }
        }

        return logFileList;
    }

    private long searchFileHelper(RandomAccessFile file,LocalDateTime dateTimeToFind, long first, long last) throws IOException {

        while(first < last){

            long mid = first+((last-first)/2);
            long midLineStartingPoint = getToLineStart(file,mid);

            file.seek(midLineStartingPoint);
            LocalDateTime dateTimeMid = getDateFromLine(file.readLine());

            if(dateTimeMid.isAfter(dateTimeToFind) || dateTimeMid.isEqual(dateTimeToFind)){
                last = midLineStartingPoint;
            } else{
                first = file.getFilePointer();
            }
        }

        return first;
    }



    private void searchFile(LocalDateTime startTime, LocalDateTime endTime , String filePath) throws IOException {
        RandomAccessFile file = new RandomAccessFile(filePath,"r");

        long pos = searchFileHelper(file,startTime,0,file.length());
        if(pos == -1){
            return;
        }
        file.seek(pos);

        while(true){
            if(pos >= file.length()){
                break;
            }
            String line = file.readLine();
            LocalDateTime dateTime = getDateFromLine(line);
            if(dateTime.isAfter(endTime)){
                break;
            }
            logger.info("{}",line);
        }
    }

    private LocalDateTime getDateFromLine(String line){
        if(line == null || line.isEmpty()){
            return LocalDateTime.now();
        }
        String[] lineArr = line.split(",");
        return DateParserUtils.parseDateTime(lineArr[0]);
    }

    private long getToLineStart(RandomAccessFile file,long pointer) throws IOException {
        long segmentStart = pointer-FILE_SEGMENT_SIZE > 0l?pointer-FILE_SEGMENT_SIZE:0;
        long startingPointLine = segmentStart;

        file.seek(segmentStart);

        while(true){
            file.readLine();
            if(file.getFilePointer() >= pointer){
                return startingPointLine;
            }
            startingPointLine = file.getFilePointer();
        }
    }
}
