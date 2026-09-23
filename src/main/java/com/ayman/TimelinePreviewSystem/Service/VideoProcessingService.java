package com.ayman.TimelinePreviewSystem.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;
@Service
public class VideoProcessingService
{
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;
    @Value("${app.output-dir:outputs}")
    private String outputDir;
    @Value("${app.frame-interval:5}")
    private int frameInterval;
    private final FfmpegService ffmpegService;
    private final SpriteService spriteService;
    private final VTTService vttService;

    public VideoProcessingService(FfmpegService ffmpegService, SpriteService spriteService, VTTService vttService)
    {
        this.ffmpegService = ffmpegService;
        this.spriteService = spriteService;
        this.vttService = vttService;
    }

    public String processVideo(MultipartFile file) throws IOException
    {
        String videoID = UUID.randomUUID().toString();
        Path uploadPath = Paths.get(uploadDir);
        Path videoOutputPath = Paths.get(outputDir, videoID);
        Path framesPath = videoOutputPath.resolve("frames");

        Files.createDirectories(uploadPath);
        Files.createDirectories(framesPath);

        String originalFilename = file.getOriginalFilename()!=null ? file.getOriginalFilename(): "video.mp4";
        Path videoPath = uploadPath.resolve(videoID + "_" + originalFilename);

        file.transferTo(videoPath);

        try
        {
            List<Path>frames = ffmpegService.extractFrames(videoPath,framesPath,frameInterval);
            if (null == frames)throw new RemoteException("NO FRAME FOUND");
            Path spritePath = videoOutputPath.resolve("sprite.jpg");
            spriteService.generateSprite(frames , spritePath);
            String spriteURL = "/api/videos/"+videoID+"/sprite";

            String vttContent = vttService.generateVTT(frames.size(), frameInterval , spriteService.getTHUMBNAIL_WIDTH() ,
                    spriteService.getTHUMBNAIL_HEIGHT(), spriteService.getCOLUMNS(), spriteURL);

            Path  vttPath = videoOutputPath.resolve("preview.vtt");
            Files.writeString(vttPath, vttContent);

            for (Path frame : frames)
                Files.deleteIfExists(frame);

            Files.deleteIfExists(framesPath);

        } catch (Exception e){e.printStackTrace();}

        return videoID;
    }
    public Path getSpritePath(String videoID) {
        return Paths.get(outputDir, videoID, "sprite.jpg");
    }
    public Path getVTTPath(String videoID) {
        return Paths.get(outputDir, videoID, "preview.vtt");
    }
}
