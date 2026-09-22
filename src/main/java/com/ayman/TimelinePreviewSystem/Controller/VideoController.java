package com.ayman.TimelinePreviewSystem.Controller;
import com.ayman.TimelinePreviewSystem.Service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos/")
@CrossOrigin("*")
public class VideoController
{
    private final VideoProcessingService videoProcessingService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String,String>> uploadVideo(@RequestParam("file") MultipartFile videoFile)
    {
        if (videoFile.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload video file"));
        try
        {
            String videoID = videoProcessingService.processVideo(videoFile);
            Map<String,String> response = new HashMap<>();
            response.put("videoID", videoID);
            response.put("spriteURL" , "/api/videos/" + videoID + "/sprite");
            response.put("vttURL" , "/api/videos/" + videoID + "/vtt");
            response.put("message" , "Good");
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error processing video"));
        }

    }

    @GetMapping("/{videoID}/sprite")
    public ResponseEntity<Resource> getSprite(@PathVariable String videoID)
    {
        Path spritePath = videoProcessingService.getSpritePath(videoID);
        if (!Files.exists(spritePath))
            return ResponseEntity.notFound().build();
        Resource resource = new FileSystemResource(spritePath);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);

    }

    @GetMapping("/{videoID}/vtt")
    public ResponseEntity<String> getVTT(@PathVariable String videoID) throws IOException
    {
        Path vttPath = videoProcessingService.getVTTPath(videoID);
        if (!Files.exists(vttPath))
            return ResponseEntity.notFound().build();
        String vttContent = Files.readString(vttPath);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/vtt")).body(vttContent);
    }
}
