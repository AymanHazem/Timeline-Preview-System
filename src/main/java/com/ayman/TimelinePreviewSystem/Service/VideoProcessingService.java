package com.ayman.TimelinePreviewSystem.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessingService
{
    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;
    @Value("${app.output-dir:outputs}")
    private String outputDir;
    @Value("${app.frame-interval:5}")
    private int frameInterval;
    @Value("${app.sprite.thumbnail-width:160}")
    private int THUMBNAIL_WIDTH = 160;

    @Value("${app.sprite.thumbnail-height:90}")
    private int THUMBNAIL_HEIGHT = 90;

    @Value("${app.sprite.columns:10}")
    private int COLUMNS = 10;
    private final FfmpegService ffmpegService;
    private final VTTService vttService;

    public VideoProcessingService(FfmpegService ffmpegService, VTTService vttService)
    {
        this.ffmpegService = ffmpegService;
        this.vttService = vttService;
    }

    public String processVideo(MultipartFile file) throws IOException
    {
        long totalStart = System.currentTimeMillis();
        String videoID = UUID.randomUUID().toString();
        Path uploadPath = Paths.get(uploadDir);
        Path videoOutputPath = Paths.get(outputDir, videoID);

        Files.createDirectories(uploadPath);
        Files.createDirectories(videoOutputPath);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "video.mp4";
        Path videoPath = uploadPath.resolve(videoID + "_" + originalFilename);

        log.info("Starting upload for video [{}] (filename: '{}', size: {} bytes)",
                videoID, originalFilename, file.getSize());

        long uploadStart = System.currentTimeMillis();
        file.transferTo(videoPath);
        long uploadDuration = System.currentTimeMillis() - uploadStart;
        log.info("Video [{}] upload complete: saved to [{}] in {}",
                videoID, videoPath, formatTime(uploadDuration));

        try
        {
            Path spritePath = videoOutputPath.resolve("sprite.jpg");
            log.info("Generating sprite sheet for video [{}]...", videoID);
            long spriteStart = System.currentTimeMillis();
            ffmpegService.generateSprite(
                    videoPath, spritePath, frameInterval, COLUMNS);
            long spriteDuration = System.currentTimeMillis() - spriteStart;
            log.info("Sprite sheet generated for video [{}] in {} -> [{}]",
                    videoID, formatTime(spriteDuration), spritePath);

            long vttStart = System.currentTimeMillis();
            int frameCount = ffmpegService.getFrameCount(videoPath, frameInterval);
            String spriteURL = "/api/videos/" + videoID + "/sprite";
            String vttContent = vttService.generateVTT(
                    frameCount, frameInterval,
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT,
                    COLUMNS, spriteURL);
            Files.writeString(videoOutputPath.resolve("preview.vtt"), vttContent);
            long vttDuration = System.currentTimeMillis() - vttStart;
            log.info("VTT cue sheet generated for video [{}] ({} frames) in {}",
                    videoID, frameCount, formatTime(vttDuration));

            long totalDuration = System.currentTimeMillis() - totalStart;
            log.info("Total processing for video [{}] completed in {} (upload: {}, sprite: {}, vtt: {})",
                    videoID,
                    formatTime(totalDuration),
                    formatTime(uploadDuration),
                    formatTime(spriteDuration),
                    formatTime(vttDuration));

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - totalStart;
            log.error("Failed to process video [{}] after {}: {}",
                    videoID, formatTime(totalDuration), e.getMessage(), e);
            throw new RuntimeException("Failed to process video: " + e.getMessage(), e);
        }

        return videoID;
    }

    public Path getSpritePath(String videoID) {
        return Paths.get(outputDir, videoID, "sprite.jpg");
    }

    public Path getVTTPath(String videoID) {
        return Paths.get(outputDir, videoID, "preview.vtt");
    }

    private String formatTime(long elapsed) {
        long min = TimeUnit.MILLISECONDS.toMinutes(elapsed);
        long sec = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60;
        long ms = elapsed % 1000;
        return min + " min " + sec + " sec " + ms + " ms";
    }
}
