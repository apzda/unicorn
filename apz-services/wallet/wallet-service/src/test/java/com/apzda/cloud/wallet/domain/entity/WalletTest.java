package com.apzda.cloud.wallet.domain.entity;

import com.apzda.cloud.wallet.config.WalletConfig;
import com.apzda.cloud.wallet.config.WalletProperties;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class WalletTest {

    @Test
    void long_value_should_be_ok() {
        // given
        try (val wc = Mockito.mockStatic(WalletConfig.class)) {
            val walletConfig = Mockito.mock(WalletProperties.CurrencyConfig.class);
            when(walletConfig.getPrecision()).thenReturn((short) 8);
            when(walletConfig.getScale()).thenReturn((short) 2);
            wc.when(() -> WalletConfig.getCurrencyConfig("CNY")).thenReturn(walletConfig);
            val wallet = new Wallet();
            wallet.setCurrency("CNY");

            // when
            val amount = wallet.longValue(10.250001D);

            // then
            assertThat(amount).isEqualTo(1025000100L);

            // when
            val d = wallet.doubleValue(amount);
            // then
            assertThat(d).isEqualTo(10.250001D);

            // when
            val amount1 = wallet.longValue(10D);

            // then
            assertThat(amount1).isEqualTo(1000000000L);

            // when
            val d1 = wallet.doubleValue(amount1);
            assertThat(d1).isEqualTo(10D);
        }
    }

}
