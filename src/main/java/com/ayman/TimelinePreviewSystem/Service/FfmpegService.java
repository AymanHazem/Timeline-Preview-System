package com.ayman.TimelinePreviewSystem.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class FfmpegService
{
    private static final Logger log = LoggerFactory.getLogger(FfmpegService.class);

    /**
     * Generates a sprite sheet JPEG directly via the FFmpeg tile filter.
     * No intermediate frame files are written to disk.
     *
     * @param videoPath       absolute path to the uploaded video
     * @param outputPath      absolute path where sprite.jpg should be written
     * @param intervalSeconds one frame every N seconds
     * @param columns         number of tile columns
     */
    public void generateSprite(Path videoPath, Path outputPath,
                               int intervalSeconds, int columns)
            throws IOException, InterruptedException
    {
        Files.createDirectories(outputPath.getParent());
        int frameCount = getFrameCount(videoPath, intervalSeconds);
        int rows = Math.max(1, (int) Math.ceil((double) frameCount / columns));
        String vf = String.format(
                "fps=1/%d,scale=160:90,tile=%dx%d", intervalSeconds, columns, rows);

        log.info("Executing FFmpeg tile command: video='{}', grid={}x{} ({} frames), interval={}s",
                videoPath.getFileName(), columns, rows, frameCount, intervalSeconds);

        ProcessBuilder cmd = new ProcessBuilder(
                "ffmpeg", "-nostdin",
                "-i", videoPath.toString(),
                "-vf", vf,
                "-frames:v", "1",
                "-q:v", "2",
                "-y",
                outputPath.toString());
        cmd.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        Process proc = cmd.start();
        String output = new String(proc.getInputStream().readAllBytes());
        int exitCode = proc.waitFor();
        long duration = System.currentTimeMillis() - startTime;

        if (exitCode != 0) {
            log.error("FFmpeg tile generation failed with exit code {} after {}: {}",
                    exitCode, formatTime(duration), output);
            throw new RuntimeException(
                    "FFmpeg tile generation failed (exit " + exitCode + "): " + output);
        }

        log.info("FFmpeg tile generation completed successfully in {}: output='{}'",
                formatTime(duration), outputPath.getFileName());
    }

    /**
     * Uses ffprobe to read the video duration, then computes how many thumbnail
     * frames would be extracted at the given interval.
     *
     * @param videoPath       absolute path to the uploaded video
     * @param intervalSeconds one frame every N seconds
     * @return number of frames (= number of VTT cues)
     */
    public int getFrameCount(Path videoPath, int intervalSeconds)
            throws IOException, InterruptedException
    {
        long probeStart = System.currentTimeMillis();

        ProcessBuilder cmd = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=duration",
                "-of", "csv=p=0",
                videoPath.toString());
        cmd.redirectErrorStream(true);
        Process proc = cmd.start();
        String raw = new String(proc.getInputStream().readAllBytes()).trim();
        proc.waitFor();

        // Fallback: try format-level duration if stream duration is absent
        if (raw.isEmpty() || raw.equals("N/A")) {
            log.debug("Stream duration missing for [{}], attempting format-level duration probe", videoPath.getFileName());
            cmd = new ProcessBuilder(
                    "ffprobe", "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "csv=p=0",
                    videoPath.toString());
            cmd.redirectErrorStream(true);
            proc = cmd.start();
            raw = new String(proc.getInputStream().readAllBytes()).trim();
            proc.waitFor();
        }

        long probeDuration = System.currentTimeMillis() - probeStart;

        if (raw.isEmpty() || raw.equals("N/A")) {
            log.error("Failed to determine video duration for [{}] after {}",
                    videoPath, formatTime(probeDuration));
            throw new RuntimeException("Could not determine video duration for: " + videoPath);
        }

        double durationSeconds = Double.parseDouble(raw);
        int frameCount = (int) Math.ceil(durationSeconds / intervalSeconds);
        log.info("ffprobe duration check for [{}]: duration={}s -> {} frames [took {}]",
                videoPath.getFileName(), durationSeconds, frameCount, formatTime(probeDuration));
        return frameCount;
    }

    private String formatTime(long elapsed) {
        long min = TimeUnit.MILLISECONDS.toMinutes(elapsed);
        long sec = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60;
        long ms = elapsed % 1000;
        return min + " min " + sec + " sec " + ms + " ms";
    }
}