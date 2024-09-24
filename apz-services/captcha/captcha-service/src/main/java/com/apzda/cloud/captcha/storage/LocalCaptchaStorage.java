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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class LocalCaptchaStorage implements CaptchaStorage {

    private final Cache<String, Captcha> cache;

    private final LoadingCache<String, AtomicInteger> counterCache;

    public LocalCaptchaStorage(Duration expired) {
        this.cache = CacheBuilder.newBuilder().expireAfterWrite(expired).build();
        counterCache = CacheBuilder.newBuilder().expireAfterWrite(expired).build(new CacheLoader<>() {
            @Override
            @NonNull
            public AtomicInteger load(@NonNull String key) throws Exception {
                return new AtomicInteger(0);
            }
        });
    }

    @Override
    public Captcha load(String uuid, Captcha captcha) {
        return cache.getIfPresent(genId(uuid, captcha));
    }

    @Override
    public void save(String uuid, Captcha captcha) throws Exception {
        cache.put(genId(uuid, captcha), captcha);
    }

    @Override
    public void remove(String uuid, Captcha captcha) {
        try {
            val key = genId(uuid, captcha);
            cache.invalidate(key);
            counterCache.invalidate(key);
        }
        catch (Exception e) {
            log.warn("Cannot delete captcha(uuid:{},id: {}): {}", uuid, captcha.getId(), e.getMessage());
        }
    }

    @Override
    public int getErrorCount(String uuid, String id) {
        try {
            val ai = counterCache.get(genId(uuid, id));
            return ai.addAndGet(1);
        }
        catch (Exception e) {
            log.warn("Cannot get try count for captcha(uuid:{}, id:{}) - {}", uuid, id, e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getIpCount(String remoteAddr) {
        try {
            final long a = DateUtil.currentSeconds() / 60;
            val ai = counterCache.get(genId(remoteAddr + a, ""));
            return ai.addAndGet(1);
        }
        catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

}
