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
package com.apzda.cloud.sms.listener;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.sms.domain.SmsStatus;
import com.apzda.cloud.sms.domain.repository.SmsLogRepository;
import com.apzda.cloud.sms.event.SmsEvent;
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
@Slf4j
@RequiredArgsConstructor
public class SmsEventListener implements ApplicationListener<SmsEvent> {

    private final SmsLogRepository smsLogRepository;

    @Override
    public void onApplicationEvent(@NonNull SmsEvent event) {
        val source = event.getSms();
        val smsLogId = source.getSmsLogId();
        if (smsLogId != null) {
            val smsLog = smsLogRepository.findById(smsLogId);
            if (smsLog.isPresent()) {
                val sms = smsLog.get();
                sms.setSentTime(DateUtil.currentSeconds());
                sms.setStatus(event.isSuccess() ? SmsStatus.SENT : SmsStatus.FAILED);
                val exception = event.getException();
                if (exception != null) {
                    sms.setError(exception.getMessage());
                }
                smsLogRepository.save(sms);
            }
        }
    }

}
