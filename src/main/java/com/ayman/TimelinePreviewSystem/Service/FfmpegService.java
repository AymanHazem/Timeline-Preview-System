package com.ayman.TimelinePreviewSystem.Service;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Service
public class FfmpegService
{
    public List<Path> extractFrames(Path videoPath , Path outputDir , int intervalSeconds) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        String fps = "1/" + intervalSeconds;
        String outPutPattern = outputDir.resolve("frames_04%d.jpg").toString();
        ProcessBuilder ffmpegCommand = new ProcessBuilder("ffmpeg","-i",videoPath.toString(),
                "-vf" , "fps="+fps,
                "-q:v" , "2",
                outPutPattern);
        ffmpegCommand.redirectErrorStream(true);
        Process ffmpegProcess = ffmpegCommand.start();
        String ffmpegOutput = new String(ffmpegProcess.getInputStream().readAllBytes());
        int exitCode = ffmpegProcess.waitFor();
        if (exitCode != 0)
            throw new RuntimeException("FFmpeg failed with exit code: " + exitCode + ", output: " + ffmpegOutput);
        try (Stream<Path> stream = Files.list(outputDir))
        {
            return stream.filter(p->
                    p.getFileName().toString().endsWith(".jpg")).sorted().collect(Collectors.toList());
        }
    }
}