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
package com.apzda.cloud.sms.config;

import com.apzda.cloud.sms.SmsTemplate;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NonNull;
import lombok.val;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
public class ProviderProperties {

    private static final Map<String, SmsTemplate> TEMPLATES = new HashMap<>();

    @NotBlank
    private String id;

    private String name;

    private String accessKey;

    private String secretKey;

    private String username;

    private String password;

    private String endpoint;

    private String regionId;

    private String signName;

    private boolean enabled = true;

    private boolean testMode = false;

    private final Map<String, TemplateProperties> templates = new HashMap<>();

    private final Map<String, String> props = new HashMap<>();

    @NonNull
    public synchronized Map<String, SmsTemplate> templates(Map<String, TemplateProperties> globalProperties) {
        if (TEMPLATES.isEmpty()) {
            val templateIds = new HashSet<String>();
            templateIds.addAll(templates.keySet());
            templateIds.addAll(globalProperties.keySet());
            for (String tid : templateIds) {
                val gtp = globalProperties.get(tid);
                val tp = templates.get(tid);
                val template = new SmsTemplate(tid);
                if (gtp != null) {
                    template.setProperties(gtp);
                }
                if (tp != null) {
                    template.setProperties(tp);
                }
                template.setSignName(signName);
                TEMPLATES.put(tid, template);
            }
        }
        return TEMPLATES;
    }

}
