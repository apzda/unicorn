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
package cn.apzda.cloud.audit;

import com.apzda.cloud.audit.ValueSanitizer;
import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@SpringBootApplication(proxyBeanMethods = false)
public class AuditApp {

    @Bean
    CurrentUserProvider currentUserProvider() {
        return new CurrentUserProvider() {
            @Override
            protected CurrentUser currentUser() {
                val builder = CurrentUser.builder();
                builder.runAs("123");
                builder.uid("admin");
                builder.device("test");
                return builder.build();
            }
        };
    }

    @Bean
    ValueSanitizer<TestVo> valueSanitizer() {
        return new ValueSanitizer<>() {
            @Nonnull
            @Override
            public TestVo doSanitize(@Nonnull TestVo value) {
                val phone = value.getPhone();
                value.setPhone(phone.substring(0, 3) + "****" + phone.substring(7, 11));
                return value;
            }

            @Override
            public boolean support(Object value) {
                return value instanceof TestVo;
            }
        };
    }

}
