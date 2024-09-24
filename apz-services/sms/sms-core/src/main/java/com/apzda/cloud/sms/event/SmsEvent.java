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
package com.apzda.cloud.sms.event;

import com.apzda.cloud.sms.dto.Sms;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Getter
public class SmsEvent extends ApplicationEvent {

    private final boolean success;

    private final Sms sms;

    private final Throwable exception;

    public SmsEvent(Sms sms, Throwable exception) {
        this(sms, false, exception);
    }

    public SmsEvent(Sms sms) {
        this(sms, true, null);
    }

    public SmsEvent(Sms sms, boolean success, Throwable exception) {
        super(sms);
        this.sms = sms;
        this.success = success;
        this.exception = exception;
    }

}
