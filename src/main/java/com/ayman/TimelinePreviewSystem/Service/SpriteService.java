package com.ayman.TimelinePreviewSystem.Service;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
@Service
@Getter
public class SpriteService
{
    @Value("${app.sprite.thumbnail-width:160}")
    private int THUMBNAIL_WIDTH;

    @Value("${app.sprite.thumbnail-height:90}")
    private int THUMBNAIL_HEIGHT;

    @Value("${app.sprite.columns:10}")
    private int COLUMNS;

    public Path generateSprite(List<Path> frames , Path outputPath) throws IOException
    {
        int totalFrames = frames.size();
        int rows = (int) Math.ceil((double) totalFrames / COLUMNS);

        BufferedImage spriteSheet = new BufferedImage(THUMBNAIL_WIDTH *COLUMNS , THUMBNAIL_HEIGHT * rows ,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = spriteSheet.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
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
