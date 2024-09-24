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
package com.apzda.cloud.sms.sender;

import com.apzda.cloud.gsvc.config.Props;
import com.apzda.cloud.sms.SmsProvider;
import com.apzda.cloud.sms.SmsSender;
import com.apzda.cloud.sms.dto.Sms;
import com.apzda.cloud.sms.event.SmsEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@Slf4j
public class ThreadPoolSender implements SmsSender, ThreadFactory {

    private static final AtomicInteger counter = new AtomicInteger(0);

    private Map<String, SmsProvider> enabledSmsProviders;

    private int shutdownWait = 60;

    private ExecutorService sender;

    @Override
    public void init(Props props, Map<String, SmsProvider> enabledSmsProviders) throws Exception {
        this.enabledSmsProviders = enabledSmsProviders;
        val thread = Math.max(1, Math.abs(props.getInt("thread", Runtime.getRuntime().availableProcessors())));
        shutdownWait = props.getInt("shutdown.wait", shutdownWait);

        sender = Executors.newFixedThreadPool(thread, this);
    }

    @Override
    public void send(@NonNull Sms sms, @NonNull final ApplicationEventPublisher publisher) {
        val phone = sms.getPhone();
        val vendor = sms.getVendor();
        val smsProvider = enabledSmsProviders.get(vendor);
        if (smsProvider == null) {
            log.warn("Sms Vendor({}) is not available", vendor);
            publisher.publishEvent(new SmsEvent(sms, false,
                    new IllegalStateException(String.format("Sms Vendor(%s) is not available", vendor))));
            return;
        }
        if (sms.isSync()) {
            try {
                publisher.publishEvent(new SmsEvent(sms, smsProvider.send(sms), null));
            }
            catch (Exception e) {
                log.warn("Cannot send sms ({}) to {}", sms, phone);
                publisher.publishEvent(new SmsEvent(sms, e));
            }
        }
        else {
            sender.submit(() -> {
                try {
                    publisher.publishEvent(new SmsEvent(sms, smsProvider.send(sms), null));
                }
                catch (Exception e) {
                    log.warn("Cannot send sms ({}) to {} - {}", sms, phone, e.getMessage());
                    publisher.publishEvent(new SmsEvent(sms, e));
                }
            });
        }
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void stop() {
        try {
            sender.shutdown();
            if (!sender.awaitTermination(shutdownWait, TimeUnit.SECONDS)) {
                log.warn("There are may be some sms lost!");
            }
        }
        catch (InterruptedException e) {
            log.error("Shutdown Sms Sender Thread pool error: {}", e.getMessage());
        }
    }

    @Override
    public Thread newThread(@NonNull Runnable r) {
        val thread = new Thread(r);
        thread.setName("sms-sender-" + counter.addAndGet(1));
        thread.setDaemon(true);
        return thread;
    }

}
