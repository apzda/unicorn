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
package com.apzda.cloud.config.app.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.apzda.cloud.config.domain.entity.Revision;
import com.apzda.cloud.config.domain.entity.Setting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface SettingService {

    Setting load(String settingCls);

    boolean save(Setting setting);

    boolean delete(String settingCls);

    boolean restore(String settingCls, int revision);

    default boolean restore(String settingCls) {
        return restore(settingCls, -1);
    }

    Page<Revision> revisions(String settingCls, Pageable page);

    Setting save(String settingCls, String setting);

    default String genSettingKey(String settingCls) {
        return DigestUtil.md5Hex(settingCls);
    }

}
