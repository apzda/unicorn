package com.apzda.cloud.captcha.utils;

import cn.hutool.core.codec.Base64Encoder;
import com.apzda.cloud.captcha.SerializableStream;
import com.apzda.cloud.captcha.SliderCaptcha;
import com.apzda.cloud.gsvc.io.Base64DecodeMultipartFile;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.Random;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class SliderUtil {

    private static final int BOLD = 5;

    private static final String IMG_FILE_TYPE = "jpg";

    private static final String TEMP_IMG_FILE_TYPE = "png";

    public static SliderCaptcha createSliderCaptcha(@Nonnull SerializableStream sliderFile,
            @Nonnull SerializableStream noiseFile, SerializableStream originalFile, String watermark, int noise)
            throws Exception {

        val random = new Random();
        val sliderImage = ImageIO
            .read(Objects.requireNonNull(Base64DecodeMultipartFile.base64ToInputStream(sliderFile.getBase64())));
        val sliderWidth = sliderImage.getWidth();
        val sliderHeight = sliderImage.getHeight();

        val originalImage = ImageIO
            .read(Objects.requireNonNull(Base64DecodeMultipartFile.base64ToInputStream(originalFile.getBase64())));
        val originalWidth = originalImage.getWidth();
        val originalHeight = originalImage.getHeight();

        val randomX = random.nextInt(originalWidth - 3 * sliderWidth) + 2 * sliderWidth;
        val randomY = random.nextInt(originalHeight - sliderHeight);

        var newImage = new BufferedImage(sliderWidth, sliderHeight, sliderImage.getType());
        val graphics = newImage.createGraphics();
        newImage = graphics.getDeviceConfiguration()
            .createCompatibleImage(sliderWidth, sliderHeight, Transparency.TRANSLUCENT);

        ImageUtil.cutByTemplate(originalImage, sliderImage, newImage, randomX, randomY);

        if (noise > 0) {
            val interfereSliderImage = ImageIO
                .read(Objects.requireNonNull(Base64DecodeMultipartFile.base64ToInputStream(noiseFile.getBase64())));
            for (int i = 0; i < noise; i++) {
                int interfereX = random.nextInt(originalWidth - 3 * sliderWidth) + 2 * sliderWidth;
                int interfereY = random.nextInt(originalHeight - sliderHeight);
                ImageUtil.interfereTemplate(originalImage, interfereSliderImage, interfereX, interfereY);
            }
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(BOLD, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        graphics.drawImage(newImage, 0, 0, null);
        graphics.dispose();

        ImageUtil.watermark(originalImage, watermark);
        val newImageOs = new ByteArrayOutputStream();
        ImageIO.write(newImage, TEMP_IMG_FILE_TYPE, newImageOs);
        val newImagery = newImageOs.toByteArray();
        val oriImagesOs = new ByteArrayOutputStream();
        ImageIO.write(originalImage, IMG_FILE_TYPE, oriImagesOs);
        val oriImageByte = oriImagesOs.toByteArray();

        return SliderCaptcha.builder()
            .sliderImg("data:image/png;base64," + Base64Encoder.encode(newImagery))
            .sliderWidth(sliderWidth)
            .sliderHeight(sliderHeight)
            .backImg("data:image/png;base64," + Base64Encoder.encode(oriImageByte))
            .backWidth(originalWidth)
            .backHeight(originalHeight)
            .rx(randomX)
            .ry(randomY)
            .build();

    }

}
