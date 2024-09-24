package com.apzda.cloud.sms.provider;

import com.apzda.cloud.sms.config.ProviderProperties;
import com.apzda.cloud.sms.dto.Sms;
import com.apzda.cloud.sms.dto.Variable;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Disabled
class DayuSmsProviderTest {

    @Test
    void send() throws Exception {
        // given
        val pp = new ProviderProperties();
        pp.setAccessKey("LTAxxxxxxxxxxxxxx");
        pp.setSecretKey("KsG9KWbyyyyyyyyy");
        pp.setRegionId("cn-hangzhou");
        val smsProvider = new DayuSmsProvider();
        smsProvider.init(pp);
        val sms = new Sms();
        sms.setPhone("18088888888");
        sms.setSignName("签名");
        sms.setTemplateId("SMS_123456789");
        sms.setVariables(List.of(new Variable("code", "123456", 0)));

        // when
        var result = smsProvider.send(sms);

        // then
        assertThat(result).isTrue();
    }

}
