/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
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
import com.apzda.cloud.captcha.Captcha;
import com.apzda.cloud.captcha.ValidateStatus;
import com.apzda.cloud.captcha.storage.CaptchaStorage;
import com.apzda.cloud.gsvc.config.Props;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class DragCaptchaProvider implements CaptchaProvider {

    private CaptchaStorage captchaStorage;

    private Props props;

    private double maxMultiple = 1.15d;

    @Override
    public void init(@Nonnull CaptchaStorage storage, @Nonnull Props props) throws Exception {
        this.captchaStorage = storage;
        this.props = props;
        maxMultiple = props.getDouble("max-multiple", maxMultiple);
        if (maxMultiple < 1.05d) {
            maxMultiple = 1.05d;
        }
        else if (maxMultiple > 2d) {
            maxMultiple = 2d;
        }
    }

    @Override
    public String getId() {
        return "drag";
    }

    @Override
    public Captcha create(String uuid, int width, int height, Duration timeout) throws Exception {
        val id = UUID.randomUUID().toString();
        val ca = new Captcha();
        ca.setId(id);
        var code = new Random().nextDouble(1.05d, maxMultiple);
        ca.setCode(String.valueOf(code));
        ca.setExpireTime(DateUtil.currentSeconds() + timeout.toSeconds());
        captchaStorage.save(uuid, ca);
        return ca;
    }

    @Override
    public ValidateStatus validate(String uuid, String id, String code, boolean removeOnInvalid) {
        val captcha = new Captcha();
        captcha.setId(id);
        val ca = captchaStorage.load(uuid, captcha);
        if (ca == null) {
            return ValidateStatus.ERROR;
        }
        val now = DateUtil.currentSeconds();
        if (now > ca.getExpireTime()) {
            captchaStorage.remove(uuid, captcha);
            return ValidateStatus.EXPIRED;
        }

        if (!verify(code, ca.getCode())) {
            if (removeOnInvalid) {
                captchaStorage.remove(uuid, captcha);
            }
            return ValidateStatus.ERROR;
        }
        return ValidateStatus.OK;
    }

    private boolean verify(String encoded, String code) {
        try {
            val dto = ResponseUtils.OBJECT_MAPPER.readValue(Base64.getDecoder().decode(encoded), DragDTO.class);
            if (dto.beginTime == null) {
                return false;
            }
            if (dto.endTime == null) {
                return false;
            }
            val time = dto.endTime - dto.beginTime;
            if (time < props.getLong("min-time", 350) || time > props.getLong("max-time", 3500)) {
                return false;
            }
            if (dto.points == null) {
                return false;
            }
            val size = dto.points.size();
            if (size < props.getLong("min-points", 15) || size > props.getLong("max-points", 500)) {
                return false;
            }

            val p0 = dto.points.get(0);
            val p1 = dto.points.get(size - 1);
            val d = Math.abs(p1.get(1) - p0.get(1));

            if (d < props.getLong("min-diff", 3)) {
                return false;
            }
            val dCode = Double.parseDouble(code);
            val width = (p1.get(0) - p0.get(0)) / dCode;
            if (width < dto.width) {
                return false;
            }
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }

    @Data
    public static class DragDTO {

        private Long beginTime;

        private Long endTime;

        private Double width;

        private List<List<Double>> points;

    }

}
