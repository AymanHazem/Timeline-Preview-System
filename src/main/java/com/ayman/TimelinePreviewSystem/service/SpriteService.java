package com.ayman.TimelinePreviewSystem.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
@Service

public class SpriteService
{
    private static final int THUMBNAIL_WIDTH = 160;
    private static final int THUMBNAIL_HEIGHT = 90;
    private static final int COLUMNS = 10;

    public Path generateSprite(List<Path> frames , Path outputPath) throws IOException
    {
        int totalFrames = frames.size();
        int rows = (int) Math.ceil((double) totalFrames / COLUMNS);

        BufferedImage spriteSheet = new BufferedImage(THUMBNAIL_WIDTH *COLUMNS , THUMBNAIL_HEIGHT * rows ,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = spriteSheet.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        for (int i = 0; i < totalFrames; i++)
        {
            BufferedImage frame = ImageIO.read(frames.get(i).toFile());
            int col = i%COLUMNS; int row = i/COLUMNS;
            int x = col*THUMBNAIL_WIDTH; int y = row*THUMBNAIL_HEIGHT;

            if(null!=frame)
                graphics.drawImage(frame, x, y, THUMBNAIL_WIDTH , THUMBNAIL_HEIGHT, null);
        }
        graphics.dispose();
        ImageIO.write(spriteSheet , "jpg" , outputPath.toFile());
        return outputPath;

    }
}
