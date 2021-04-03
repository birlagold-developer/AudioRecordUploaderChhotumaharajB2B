package com.kss.AudioRecordUploader.utils;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Recording implements Comparable<Recording> {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    private Date date;
    private final String fileName;
    private String userName;
    private final String phoneNumber;

    public Recording(String fileName) {
        this.fileName = fileName;
        String dateStr = fileName.substring(0, 14);

        try {
            this.date = formatter.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        phoneNumber = fileName.substring(15, fileName.indexOf('.'));
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFileName() {
        return fileName;
    }
    public String getPhoneNumber() { return phoneNumber; }
    public Date getDate() { return date; }

    public int compareTo(@NonNull Recording other) {
        return date.compareTo(other.date);
    }

}