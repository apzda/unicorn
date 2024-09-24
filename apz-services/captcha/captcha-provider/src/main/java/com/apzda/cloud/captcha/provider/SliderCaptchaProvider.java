/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.captcha.provider;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.apzda.cloud.captcha.Captcha;
import com.apzda.cloud.captcha.SerializableStream;
import com.apzda.cloud.captcha.ValidateStatus;
import com.apzda.cloud.captcha.storage.CaptchaStorage;
import com.apzda.cloud.captcha.utils.SliderUtil;
import com.apzda.cloud.gsvc.config.Props;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 滑动验证码.
 *
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class SliderCaptchaProvider implements CaptchaProvider {

    private int imagesCnt;

    private List<File> sliders;

    private int slidersCnt;

    private LoadingCache<Integer, SerializableStream> imagesCache;

    private LoadingCache<Integer, SerializableStream> sliderCache;

    private CaptchaStorage captchaStorage;

    private String watermark;

    private int noise;

    private int tolerant;

    private String path;

    @Override
    public void init(@NonNull CaptchaStorage storage, @NonNull Props props) throws Exception {
        this.captchaStorage = storage;
        this.watermark = props.getString("watermark", "");
        this.noise = props.getInt("noise", 1);
        this.tolerant = props.getInt("tolerant", 5);
        this.path = props.getString("path", "slider");

        List<File> images = loadImages(false);
        imagesCnt = images.size();

        sliders = loadImages(true);
        slidersCnt = sliders.size();

        if (imagesCnt == 0 || slidersCnt == 0) {
            throw new IllegalStateException("images(" + imagesCnt + ") or slider(" + slidersCnt + ") is empty");
        }

        imagesCache = CacheBuilder.newBuilder().build(new ImageCacheLoader(images));
        sliderCache = CacheBuilder.newBuilder().build(new ImageCacheLoader(sliders));

        log.info("SliderCaptchaProvider initialized: images({}), slider({}), watermark({}), noise({}), tolerant({})",
                imagesCnt, slidersCnt, watermark, noise, tolerant);
    }

    @Override
    public String getId() {
        return "slider";
    }

    @Override
    public Captcha create(String uuid, int width, int height, Duration timeout) throws Exception {
        val random = new Random();
        val imgIdx = random.nextInt(imagesCnt);
        val sliderIdx = random.nextInt(slidersCnt);

        val originalFile = imagesCache.get(imgIdx);
        val sliderFile = sliderCache.get(sliderIdx);
        SerializableStream noiseFile = null;

        if (noise > 0) {
            noiseFile = sliderCache.get(sliderIdx == sliders.size() - 1 ? sliderIdx - 1 : sliderIdx + 1);
        }

        val sliderCaptcha = SliderUtil.createSliderCaptcha(sliderFile, noiseFile, originalFile, watermark, noise);
        val ca = new Captcha();
        val code = sliderCaptcha.getRx() + "";
        val id = UUID.randomUUID().toString();
        ca.setId(id);
        ca.setCode(code);
        ca.setExpireTime(DateUtil.currentSeconds() + timeout.toSeconds());
        captchaStorage.save(uuid, ca);
        ca.setCode(sliderCaptcha.getBackImg() + "&&" + sliderCaptcha.getSliderImg() + "&&" + sliderCaptcha.getRy());
        return ca;
    }

    @Override
    public ValidateStatus validate(String uuid, String id, String code, boolean removeOnInvalid) {
        log.debug("Validate Captcha(uuid:{}, id:{}, code: {})", uuid, id, code);
        val xy = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(code);
        if (xy.size() != 2) {
            return ValidateStatus.ERROR;
        }
        val captcha = new Captcha();
        captcha.setId(id);
        val ca = captchaStorage.load(uuid, captcha);
        if (ca == null) {
            log.debug("Cannot load captcha(uuid: {}, id:{})", uuid, id);
            return ValidateStatus.ERROR;
        }
        log.debug("Captcha(uuid: {}, id:{}, code: {}) loaded", uuid, id, ca.getCode());
        val now = DateUtil.currentSeconds();
        if (now > ca.getExpireTime()) {
            captchaStorage.remove(uuid, captcha);
            return ValidateStatus.EXPIRED;
        }
        val xPos = Integer.parseInt(ca.getCode());
        val randomX = Integer.parseInt(xy.get(0));
        if (Math.abs(randomX - xPos) <= tolerant) {
            // captchaStorage.remove(uuid, captcha);
            return ValidateStatus.OK;
        }
        if (removeOnInvalid) {
            captchaStorage.remove(uuid, captcha);
        }
        return ValidateStatus.ERROR;
    }

    private List<File> loadImages(boolean slider) {
        List<URL> images;

        if (slider) {
            images = ResourceUtil.getResources(StrUtil.format("{}/sliders", this.path));
        }
        else {
            images = ResourceUtil.getResources(StrUtil.format("{}/images", this.path));
        }

        return images.stream().flatMap(url -> {
            try {
                val file = ResourceUtils.getFile(url);
                if (file.isDirectory()) {
                    val files = file.listFiles(File::isFile);
                    return files != null ? Stream.of(files) : Stream.empty();
                }
                return Stream.of(file);
            }
            catch (FileNotFoundException e) {
                return Stream.empty();
            }
        }).collect(Collectors.toList());
    }

}
