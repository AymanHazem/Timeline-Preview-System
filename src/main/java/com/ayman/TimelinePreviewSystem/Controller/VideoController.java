package com.ayman.TimelinePreviewSystem.Controller;

import com.ayman.TimelinePreviewSystem.Service.VideoProcessingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/videos/")
@CrossOrigin("*")
public class VideoController
{
    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    private final VideoProcessingService videoProcessingService;

    public VideoController(VideoProcessingService videoProcessingService)
    {
        this.videoProcessingService = videoProcessingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String,String>> uploadVideo(@RequestParam("file") MultipartFile videoFile)
    {
        if (videoFile.isEmpty()) {
            log.warn("Video upload rejected: uploaded file is empty");
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload video file"));
        }

        log.info("Received video upload request: filename='{}', size={} bytes",
                videoFile.getOriginalFilename(), videoFile.getSize());
        long start = System.currentTimeMillis();

        try
        {
            String videoID = videoProcessingService.processVideo(videoFile);
            long totalTime = System.currentTimeMillis() - start;
            log.info("Video upload & processing completed successfully for videoID [{}] in {}",
                    videoID, formatTime(totalTime));

            Map<String,String> response = new HashMap<>();
            response.put("videoID", videoID);
            response.put("spriteURL" , "/api/videos/" + videoID + "/sprite");
            response.put("vttURL" , "/api/videos/" + videoID + "/vtt");
            response.put("message" , "Good");
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            long totalTime = System.currentTimeMillis() - start;
            log.error("Video upload & processing failed after {} for file '{}': {}",
                    formatTime(totalTime), videoFile.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error processing video"));
        }
    }

    @GetMapping("/{videoID}/sprite")
    public ResponseEntity<Resource> getSprite(@PathVariable String videoID)
    {
        Path spritePath = videoProcessingService.getSpritePath(videoID);
        if (!Files.exists(spritePath)) {
            log.warn("Sprite requested but not found for videoID [{}] at [{}]", videoID, spritePath);
            return ResponseEntity.notFound().build();
        }
        log.debug("Serving sprite for videoID [{}]", videoID);
        Resource resource = new FileSystemResource(spritePath);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @GetMapping("/{videoID}/vtt")
    public ResponseEntity<String> getVTT(@PathVariable String videoID) throws IOException
    {
        Path vttPath = videoProcessingService.getVTTPath(videoID);
        if (!Files.exists(vttPath)) {
            log.warn("VTT requested but not found for videoID [{}] at [{}]", videoID, vttPath);
            return ResponseEntity.notFound().build();
        }
        log.debug("Serving VTT for videoID [{}]", videoID);
        String vttContent = Files.readString(vttPath);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .contentType(MediaType.parseMediaType("text/vtt"))
                .body(vttContent);
    }

    private String formatTime(long elapsed) {
        long min = TimeUnit.MILLISECONDS.toMinutes(elapsed);
        long sec = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60;
        long ms = elapsed % 1000;
        return min + " min " + sec + " sec " + ms + " ms";
    }
}
