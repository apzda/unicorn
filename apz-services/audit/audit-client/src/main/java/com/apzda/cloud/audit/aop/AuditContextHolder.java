/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.audit.aop;

import com.apzda.cloud.audit.ValueSanitizer;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public abstract class AuditContextHolder {

    private static final ThreadLocal<Context> context = new InheritableThreadLocal<>();

    static ObjectProvider<ValueSanitizer<?>> valueSanitizerProvider;

    static void create() {
        context.set(new Context(null, null, new HashMap<>()));
    }

    public static void setContext(Context context) {
        AuditContextHolder.context.set(context);
    }

    public static Context getContext() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }

    public static void restore(Context oldContext) {
        context.remove();
        if (oldContext != null) {
            context.set(oldContext);
        }
    }

    @Getter
    public static class Context {

        @Setter
        private Object newValue;

        @Setter
        private Object oldValue;

        @Setter
        private String username;

        @Setter
        private String tenantId;

        private final Map<String, Object> data;

        Context(Object newValue, Object oldValue, Map<String, Object> data) {
            this.newValue = newValue;
            this.oldValue = oldValue;
            this.data = data;
        }

        public void set(String name, Object value) {
            this.data.put(name, value);
        }

        public String getNewValueAsString() {
            return toJsonString(newValue);
        }

        public String getOldValueAsString() {
            return toJsonString(oldValue);
        }

        public static String toJsonString(Object value) {
            try {
                if (value == null) {
                    return null;
                }
                if (BeanUtils.isSimpleValueType(value.getClass())) {
                    return value.toString();
                }
                else if (value instanceof String) {
                    return (String) value;
                }
                else {
                    for (ValueSanitizer<?> valueSanitizer : valueSanitizerProvider.orderedStream().toList()) {
                        if (valueSanitizer.support(value)) {
                            value = valueSanitizer.sanitize(value);
                        }
                    }
                    return ResponseUtils.OBJECT_MAPPER.writeValueAsString(value);
                }
            }
            catch (JsonProcessingException e) {
                log.warn("Cannot serialize old value: {} - {}", value, e.getMessage());
            }
            return "";
        }

    }

}
