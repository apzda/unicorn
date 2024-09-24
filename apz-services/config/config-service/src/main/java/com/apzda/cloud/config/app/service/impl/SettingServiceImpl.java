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
package com.apzda.cloud.config.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.apzda.cloud.config.app.service.SettingService;
import com.apzda.cloud.config.domain.entity.Revision;
import com.apzda.cloud.config.domain.entity.Setting;
import com.apzda.cloud.config.domain.repository.RevisionRepository;
import com.apzda.cloud.config.domain.repository.SettingRepository;
import com.apzda.cloud.config.event.SettingChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;

    private final RevisionRepository revisionRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Setting load(String settingCls) {
        return settingRepository.findBySettingKey(genSettingKey(settingCls));
    }

    @Override
    @Transactional
    @Modifying(flushAutomatically = true)
    public boolean save(Setting setting) {
        Assert.notNull(setting, "Setting must not be null");
        val settingKey = genSettingKey(setting.getSettingCls());
        setting.setSettingKey(settingKey);

        val oldSetting = settingRepository.findBySettingKey(settingKey);
        val lastRevision = revisionRepository.findFirstBySettingKeyOrderByRevisionDesc(settingKey);

        if (oldSetting != null) {
            val revision = BeanUtil.copyProperties(oldSetting, Revision.class, "id", "createdAt", "createdBy");
            revision
                .setRevision(lastRevision.orElseGet(() -> Revision.builder().revision(0).build()).getRevision() + 1);
            revisionRepository.save(revision);
            BeanUtil.copyProperties(setting, oldSetting,
                    CopyOptions.create().ignoreNullValue().setIgnoreProperties("id"));
            settingRepository.save(oldSetting);
            eventPublisher.publishEvent(new SettingChangedEvent(setting.getSettingCls()));
            return true;
        }
        else {
            settingRepository.save(setting);
            if (setting.getId() != null) {
                eventPublisher.publishEvent(new SettingChangedEvent(setting.getSettingCls()));
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    @Modifying(flushAutomatically = true)
    public Setting save(String settingCls, String setting) {
        val builder = Setting.builder();
        builder.settingCls(settingCls).setting(setting);
        val entity = builder.build();
        if (this.save(entity)) {
            return entity;
        }
        return null;
    }

    @Override
    public boolean delete(String settingCls) {
        return false;
    }

    @Override
    @Transactional
    public boolean restore(String settingCls, int revision) {
        Optional<Revision> rev;
        if (revision > 0) {
            rev = revisionRepository.findBySettingKeyAndRevision(genSettingKey(settingCls), revision);
        }
        else {
            rev = revisionRepository.findFirstBySettingKeyOrderByRevisionDesc(genSettingKey(settingCls));
        }
        if (rev.isPresent()) {
            val setting = rev.get();
            val restoredSetting = BeanUtil.copyProperties(setting, Setting.class, "id", "createdAt", "createdBy");
            return save(restoredSetting);
        }
        return false;
    }

    @Override
    public Page<Revision> revisions(String settingCls, Pageable page) {
        return revisionRepository.findAllBySettingKeyOrderByRevisionDesc(genSettingKey(settingCls), page);
    }

}
