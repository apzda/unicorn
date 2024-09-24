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
package com.apzda.cloud.config.facade;

import com.apzda.cloud.config.app.service.SettingService;
import com.apzda.cloud.config.domain.entity.Revision;
import com.apzda.cloud.config.domain.entity.Setting;
import com.apzda.cloud.config.proto.*;
import com.apzda.cloud.gsvc.domain.PagerUtils;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigServiceImpl implements ConfigService {

    private final SettingService settingService;

    @Override
    public LoadRes load(KeyReq request) {
        val builder = LoadRes.newBuilder();
        val key = request.getKey();
        Setting setting = settingService.load(key);

        if (setting == null) {
            builder.setErrCode(404);
            builder.setErrMsg("Setting not found");
        }
        else {
            builder.setErrCode(0);
            builder.setSetting(ByteString.copyFromUtf8(setting.getSetting()));
        }

        return builder.build();
    }

    @Override
    public CommonRes save(SaveReq request) {
        val builder = CommonRes.newBuilder();
        try {
            Setting setting = settingService.save(request.getKey(), request.getSetting().toStringUtf8());
            builder.setErrCode(0);
            if (setting == null) {
                builder.setErrCode(503);
                builder.setErrMsg("Cannot save setting");
            }
        }
        catch (Exception e) {
            log.error("Cannot save setting({}) - {}", request.getKey(), e.getMessage());
            builder.setErrCode(500);
            builder.setErrMsg(e.getMessage());
        }
        return builder.build();
    }

    @Override
    public CommonRes delete(KeyReq request) {
        log.warn("Ignoring delete operation on {}", request.getKey());
        val builder = CommonRes.newBuilder();
        builder.setErrCode(0);
        return builder.build();
    }

    @Override
    public CommonRes restore(RestoreReq request) {
        val builder = CommonRes.newBuilder();
        boolean restored;
        if (request.hasRevision()) {
            restored = settingService.restore(request.getKey(), request.getRevision());
        }
        else {
            restored = settingService.restore(request.getKey());
        }
        if (restored) {
            builder.setErrCode(0);
        }
        else {
            builder.setErrCode(500);
            builder.setErrMsg(String.format("Cannot restore Setting(%s) to revision %d", request.getKey(),
                    request.getRevision()));
        }
        return builder.build();
    }

    @Override
    public RevisionRes revisions(RevisionReq request) {
        val builder = RevisionRes.newBuilder();
        val settingCls = request.getKey();
        val pager = PagerUtils.of(request.getPager());
        val revisions = settingService.revisions(settingCls, pager);
        log.trace("Revisions of {} - {}", settingCls, revisions);
        val pageInfo = PagerUtils.of(revisions);
        builder.setErrCode(0);
        for (Revision revision : revisions.getContent()) {
            val rb = com.apzda.cloud.config.proto.Revision.newBuilder();
            rb.setCreatedAt(revision.getCreatedAt());
            rb.setRevision(revision.getRevision());
            rb.setSetting(ByteString.copyFromUtf8(revision.getSetting()));
            builder.addRevision(rb);
        }
        builder.setPageInfo(pageInfo);
        return builder.build();
    }

}
