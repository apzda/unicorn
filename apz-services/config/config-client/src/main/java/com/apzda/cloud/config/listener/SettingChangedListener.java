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
package com.apzda.cloud.config.listener;

import com.apzda.cloud.config.event.SettingChangedEvent;
import com.apzda.cloud.config.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
@Slf4j
public class SettingChangedListener implements ApplicationListener<SettingChangedEvent> {

    public static final String BEAN_NAME = "com.apzda.cloud.config.listener.SettingChangedListenerBean";

    private final SettingService settingService;

    @Override
    public void onApplicationEvent(@NonNull SettingChangedEvent event) {
        val source = event.getSource();
        val settingKey = source.toString();
        log.debug("Refresh Setting({}) locally", settingKey);
        settingService.refresh(settingKey);
    }

}
