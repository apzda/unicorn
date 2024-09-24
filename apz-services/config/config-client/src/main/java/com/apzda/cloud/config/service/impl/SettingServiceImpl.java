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
package com.apzda.cloud.config.service.impl;

import com.apzda.cloud.config.Revision;
import com.apzda.cloud.config.Setting;
import com.apzda.cloud.config.exception.SettingUnavailableException;
import com.apzda.cloud.config.proto.*;
import com.apzda.cloud.config.service.SettingService;
import com.apzda.cloud.gsvc.domain.PagerUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class SettingServiceImpl extends CacheLoader<String, Setting> implements SettingService {

    private final ObjectMapper objectMapper;

    private final ConfigService configService;

    private final LoadingCache<String, Setting> cache;

    public SettingServiceImpl(ObjectMapper objectMapper, ConfigService configService) {
        this.objectMapper = objectMapper;
        this.configService = configService;
        this.cache = CacheBuilder.newBuilder().build(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Setting> T load(@NonNull Class<T> tClass, @NonNull String tenantId)
            throws SettingUnavailableException {
        val settingKey = Setting.genSettingKey(tClass, tenantId);
        try {
            val setting = cache.get(settingKey);
            return (T) setting;
        }
        catch (ExecutionException e) {
            throw new SettingUnavailableException(settingKey, e);
        }
    }

    @Override
    public <T extends Setting> boolean save(@NonNull T setting, @NonNull String tenantId)
            throws SettingUnavailableException {
        val builder = SaveReq.newBuilder();
        val aClass = setting.getClass();
        val settingKey = Setting.genSettingKey(aClass, tenantId);

        builder.setKey(settingKey);
        try {
            builder.setSetting(ByteString.copyFrom(objectMapper.writeValueAsBytes(setting)));
            val save = configService.save(builder.build());
            if (save.getErrCode() != 0) {
                log.error("Setting({}) save failed: {}", settingKey, save.getErrMsg());
                throw new SettingUnavailableException(settingKey, save.getErrMsg());
            }
            cache.put(settingKey, setting);
            log.debug("Setting({}) saved", settingKey);
            return true;
        }
        catch (Exception e) {
            throw new SettingUnavailableException(settingKey, e);
        }
    }

    @Override
    public <T extends Setting> List<Revision<T>> revisions(@NonNull Class<T> tClass, @NonNull String tenantId,
            Pageable pager) {
        val req = RevisionReq.newBuilder();
        val settingKey = Setting.genSettingKey(tClass, tenantId);
        req.setKey(settingKey);
        req.setPager(PagerUtils.to(pager));

        val res = configService.revisions(req.build());
        if (res.getErrCode() == 0) {
            return res.getRevisionList().stream().map(revision -> {
                val rev = new Revision<T>();
                rev.setSettingKey(settingKey);
                rev.setCreatedAt(revision.getCreatedAt());
                rev.setRevision(revision.getRevision());
                try {
                    rev.setSetting(objectMapper.readValue(revision.getSetting().toByteArray(), tClass));
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return rev;
            }).toList();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean restore(@NonNull Revision<?> revision) {
        val builder = RestoreReq.newBuilder();
        builder.setKey(revision.getSettingKey());
        builder.setRevision(revision.getRevision());
        val res = configService.restore(builder.build());
        if (res.getErrCode() == 0) {
            return true;
        }
        else {
            log.error("Cannot restore setting({}) to {} - {}", revision.getSettingKey(), revision.getRevision(),
                    res.getErrMsg());
        }
        return false;
    }

    @Override
    @SuppressWarnings("all")
    public Setting load(@NonNull String settingKey) throws Exception {
        val builder = KeyReq.newBuilder();
        builder.setKey(settingKey);
        val settingMeta = SettingService.getSettingMeta(settingKey);
        val aClass = settingMeta.settingClass();
        if (aClass == null) {
            throw new ClassNotFoundException(settingMeta.clazz());
        }

        val res = configService.load(builder.build());
        val errCode = res.getErrCode();

        if (errCode == 404) {
            // 应用使用默认值
            val setting = BeanUtils.instantiateClass(aClass);
            return setting;
        }
        else if (errCode != 0) {
            log.error("Cannot load setting({}): {}", settingKey, res.getErrMsg());
            throw new SettingUnavailableException(settingKey, res.getErrMsg());
        }
        val setting = res.getSetting();
        log.debug("Setting({}) loaded from ConfiService", settingKey);

        return objectMapper.readValue(setting.toByteArray(), aClass);
    }

    @Override
    public void refresh(String key) {
        val settingMeta = SettingService.getSettingMeta(key);
        val settingKey = settingMeta.getSettingKey();
        cache.invalidate(settingKey);
        log.debug("Setting({}) cache invalidated", settingKey);
    }

}
