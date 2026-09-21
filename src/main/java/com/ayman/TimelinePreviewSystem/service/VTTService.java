package com.ayman.TimelinePreviewSystem.service;
import org.springframework.stereotype.Service;
@Service
public class VTTService
{
    public String generateVTT (int frameCount , int intervalSeconds , int thumbnailWidth ,
                               int  thumbnailHeight , int columnns , String spriteURL)
    {
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");

        for (int i = 0; i < frameCount; i++)
        {
            int startSecond = i* intervalSeconds;
            int endSecond = (i+1) * intervalSeconds;
            int col = i%columnns; int row = i/columnns;
            int x = col*thumbnailWidth; int y = row*thumbnailHeight;
            vtt.append(formatTime(startSecond))
                    .append(" --> ")
                    .append(formatTime(endSecond))
                    .append("\n");

            vtt.append(spriteURL)
                    .append("#xywh=")
                    .append(x).append(",")
                    .append(y).append(",")
                    .append(thumbnailWidth).append(",")
                    .append(thumbnailHeight).append(",")
                    .append("\n\n");
        }

        return vtt.toString();
    }

    private String formatTime (int totalSeconds)
    {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d.000", hours, minutes, seconds);
    }
}
