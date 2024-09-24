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
package com.apzda.cloud.captcha.storage;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.captcha.Captcha;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
@Slf4j
public class RedisCaptchaStorage implements CaptchaStorage {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public Captcha load(String uuid, Captcha captcha) {
        val key = "captcha." + genId(uuid, captcha);
        try {
            val value = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(value)) {
                return objectMapper.readValue(value, Captcha.class);
            }
        }
        catch (Exception e) {
            log.error("Cannot load captcha - {}@{} - {}", uuid, captcha.getId(), e.getMessage());
        }
        return null;
    }

    @Override
    public void save(String uuid, Captcha captcha) throws Exception {
        val key = "captcha." + genId(uuid, captcha);
        val ca = objectMapper.writeValueAsString(captcha);
        stringRedisTemplate.opsForValue()
            .set(key, ca, captcha.getExpireTime() - DateUtil.currentSeconds() + Duration.ofSeconds(3600).toSeconds(),
                    TimeUnit.SECONDS);
    }

    @Override
    public void remove(String uuid, Captcha captcha) {
        val key = "captcha." + genId(uuid, captcha);
        val key2 = "captcha.err." + genId(uuid, captcha);
        try {
            stringRedisTemplate.delete(List.of(key, key2));
        }
        catch (Exception e) {
            log.warn("Cannot delete captcha(uuid:{},id: {}): {}", uuid, captcha.getId(), e.getMessage());
        }
    }

    @Override
    public int getErrorCount(String uuid, String id) {
        val key = "captcha.err." + genId(uuid, id);
        try {
            val intExact = Math.toIntExact(Objects.requireNonNull(stringRedisTemplate.opsForValue().increment(key)));
            stringRedisTemplate.expire(key, 180, TimeUnit.MINUTES);
            return intExact;
        }
        catch (Exception e) {
            log.warn("Cannot get try count for captcha(uuid:{}, id:{}) - {}", uuid, id, e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getIpCount(String remoteAddr) {
        final long a = DateUtil.currentSeconds() / 60;
        val key = "captcha.max." + genId(remoteAddr + a, "");
        try {
            val intExact = Math.toIntExact(Objects.requireNonNull(stringRedisTemplate.opsForValue().increment(key)));
            stringRedisTemplate.expire(key, 61, TimeUnit.SECONDS);
            return intExact;
        }
        catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

}
