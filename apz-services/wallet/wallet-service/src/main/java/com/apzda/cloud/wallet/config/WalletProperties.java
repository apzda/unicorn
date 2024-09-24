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
package com.apzda.cloud.wallet.config;

import com.apzda.cloud.wallet.error.WalletError;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@ConfigurationProperties(prefix = "apzda.cloud.wallet")
@Data
public class WalletProperties {

    private final Map<String, CurrencyConfig> currency = new LinkedHashMap<>();

    private String format = "#";

    @Data
    public static final class CurrencyConfig {

        /**
         * 名称
         */
        @NotNull
        private String name;

        /**
         * 与法币的汇率
         */
        @Min(value = 0)
        private double rate = 1d;

        /**
         * 小数位
         */
        @Min(value = 0)
        private short scale = 2;

        /**
         * 精度
         */
        @Min(value = 1)
        private short precision = 1;

        /**
         * 启用过期机制
         */
        private boolean enabledExpire;

        /**
         * 是否可提现
         */
        private boolean withdrawAble;

        /**
         * 输出格式化
         */
        private String format;

        /**
         * 货币符号
         */
        private String symbol;

        /**
         * 业务线
         */
        private final Map<String, BizConfig> biz = new LinkedHashMap<>();

        @NonNull
        public BizSubject getBizSubject(String currency, @NonNull String biz, @NonNull String bizSubject) {
            val bizConfig = this.biz.get(biz);
            if (bizConfig == null) {
                WalletError.BIZ_SUBJECT_NOT_FOUND.emitBizError(currency, biz, bizSubject);
            }
            val bizSubjectConfig = bizConfig.getSubjects().get(bizSubject);
            if (bizSubjectConfig == null) {
                WalletError.BIZ_SUBJECT_NOT_FOUND.emitBizError(currency, biz, bizSubject);
            }
            return bizSubjectConfig;
        }

    }

    @Data
    public static final class BizConfig {

        /**
         * 业务线名称
         */
        @NotNull
        private String name;

        /**
         * 业务线
         */
        @NotEmpty
        private final Map<String, BizSubject> subjects = new LinkedHashMap<>();

    }

    @Data
    public static final class BizSubject {

        /**
         * 业务主题（活动）
         */
        @NotNull
        private String name;

        /**
         * 该业务产生的金额是否可提现
         */
        private boolean withdrawAble = false;

        /**
         * 该业务可支出用户的余额
         */
        private boolean outlay = true;

        /**
         * 支持冻结操作
         */
        private boolean needFrozen = false;

    }

}
