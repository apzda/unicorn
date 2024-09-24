package com.apzda.cloud.wallet.domain.service;

import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.gsvc.autoconfigure.MyBatisPlusAutoConfiguration;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.test.autoconfig.AutoConfigureGsvcTest;
import com.apzda.cloud.wallet.domain.entity.Transaction;
import com.apzda.cloud.wallet.proto.TradeDTO;
import com.apzda.cloud.wallet.test.TestApp;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@MybatisPlusTest
@ContextConfiguration(classes = TestApp.class)
@AutoConfigureGsvcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(MyBatisPlusAutoConfiguration.class)
@ActiveProfiles({ "test", "flyway" })
@Testcontainers
@EnableConfigurationProperties(ServiceConfigProperties.class)
class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private OutlayService outlayService;

    @Test
    public void user_wallet_should_be_opened() {
        // given
        val uid = 1L;
        val currency = "CNY";

        // when
        val wallet = walletService.openWallet(uid, currency);
        val lastLog = walletService.getLastLog(wallet);
        // then
        assertThat(wallet).isNotNull();
        assertThat(wallet.getPrecision()).isEqualTo(Short.valueOf("8"));
        assertThat(lastLog).isNotNull();
        assertThat(wallet.getBlock()).isEqualTo(lastLog.getBlock());
    }

    @Test
    void trade_should_be_ok() {
        // given
        val uid = 1L;
        val currency = "CNY";
        val builder = TradeDTO.newBuilder();
        builder.setUid(uid);
        builder.setCurrency(currency);
        builder.setBiz("test");
        // 充值
        {
            builder.setAmount(10.25D);
            builder.setBizSubject("deposit");
            builder.setBizId("10000");
            // when 充值
            val trans = walletService.trade(builder.build());
            val lastLog = walletService.getLastLog(uid, currency);
            val wallet = walletService.openWallet(uid, currency);

            // then
            assertThat(trans).isNotNull();
            assertThat(trans.getAmount()).isEqualTo(1025000000L);
            assertThat(trans.isOutlay()).isFalse();

            assertThat(lastLog).isNotNull();
            assertThat(lastLog.getAmount()).isEqualTo(1025000000L);
            assertThat(lastLog.getPreBalance()).isEqualTo(0L);
            assertThat(lastLog.getBalance()).isEqualTo(1025000000L);
            assertThat(lastLog.getPreFrozen()).isEqualTo(0);
            assertThat(lastLog.getFrozen()).isEqualTo(0);

            assertThat(wallet.getBalance()).isEqualTo(1025000000L);
            assertThat(wallet.getFrozen()).isEqualTo(0L);
            assertThat(wallet.getAmount()).isEqualTo(1025000000L);
            assertThat(wallet.getWithdrawal()).isEqualTo(1025000000L);
            assertThat(wallet.getOutlay()).isEqualTo(0L);
        }
        // 提现
        {
            // given
            builder.setAmount(0.05D);
            builder.setBizSubject("withdraw");
            builder.setBizId("10001");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(5000000L);
            assertThat(t1.isOutlay()).isTrue();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(5000000L);
            assertThat(l1.getPreBalance()).isEqualTo(1025000000L);
            assertThat(l1.getBalance()).isEqualTo(1020000000L);
            assertThat(l1.getPreFrozen()).isEqualTo(0);
            assertThat(l1.getFrozen()).isEqualTo(5000000L);

            assertThat(w1.getBalance()).isEqualTo(1020000000L);
            assertThat(w1.getFrozen()).isEqualTo(5000000L);
            assertThat(w1.getAmount()).isEqualTo(1025000000L);
            assertThat(w1.getWithdrawal()).isEqualTo(1020000000L);
            assertThat(w1.getOutlay()).isEqualTo(5000000L);
        }
        // 支付
        {
            // given
            builder.setAmount(1D);
            builder.setBizSubject("pay");
            builder.setBizId("10002");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(100000000L);
            assertThat(t1.isOutlay()).isTrue();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(100000000L);
            assertThat(l1.getPreBalance()).isEqualTo(1020000000L);
            assertThat(l1.getBalance()).isEqualTo(920000000L);
            assertThat(l1.getPreFrozen()).isEqualTo(5000000L);
            assertThat(l1.getFrozen()).isEqualTo(5000000L);

            assertThat(w1.getBalance()).isEqualTo(920000000L);
            assertThat(w1.getFrozen()).isEqualTo(5000000L);
            assertThat(w1.getAmount()).isEqualTo(925000000L);
            assertThat(w1.getWithdrawal()).isEqualTo(920000000L);
            assertThat(w1.getOutlay()).isEqualTo(105000000L);
        }
        // 任务
        {
            // given
            builder.setAmount(1D);
            builder.setBizSubject("earn");
            builder.setBizId("10022");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(100000000L);
            assertThat(t1.isOutlay()).isFalse();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(100000000L);
            assertThat(l1.getPreBalance()).isEqualTo(920000000L);
            assertThat(l1.getBalance()).isEqualTo(1020000000L);
            assertThat(l1.getPreFrozen()).isEqualTo(5000000L);
            assertThat(l1.getFrozen()).isEqualTo(5000000L);

            assertThat(w1.getBalance()).isEqualTo(1020000000L);
            assertThat(w1.getFrozen()).isEqualTo(5000000L);
            assertThat(w1.getAmount()).isEqualTo(1025000000L);
            assertThat(w1.getWithdrawal()).isEqualTo(920000000L);
            assertThat(w1.getOutlay()).isEqualTo(105000000L);
        }
        // 提现 - 余额不足
        {
            // given
            builder.setAmount(10D);
            builder.setBizSubject("withdraw");
            builder.setBizId("10011");
            // when
            assertThatThrownBy(() -> {
                walletService.trade(builder.build());
            }).hasMessage("withdrawal is not enough");
        }
        // 支付 - 余额不足
        {
            // given
            builder.setAmount(20.3D);
            builder.setBizSubject("pay");
            builder.setBizId("10013");
            // when
            assertThatThrownBy(() -> {
                walletService.trade(builder.build());
            }).hasMessage("balance is not enough");
        }
    }

    @Test
    void expire_trade_should_be_ok() {
        // given
        val uid = 1L;
        val currency = "INT";
        val builder = TradeDTO.newBuilder();
        builder.setUid(uid);
        builder.setCurrency(currency);
        builder.setBiz("test");
        val rst = new HashMap<String, Transaction>();
        // 增加1
        {
            builder.setAmount(100D);
            builder.setBizSubject("add");
            builder.setBizId("20000");
            builder.setExpiredAt(DateUtil.current() + Duration.ofDays(30).toMillis());
            // when 充值
            val trans = walletService.trade(builder.build());
            val lastLog = walletService.getLastLog(uid, currency);
            val wallet = walletService.openWallet(uid, currency);

            // then
            assertThat(trans).isNotNull();
            assertThat(trans.getAmount()).isEqualTo(100L);
            assertThat(trans.isOutlay()).isFalse();

            assertThat(lastLog).isNotNull();
            assertThat(lastLog.getAmount()).isEqualTo(100L);
            assertThat(lastLog.getPreBalance()).isEqualTo(0L);
            assertThat(lastLog.getBalance()).isEqualTo(100L);
            assertThat(lastLog.getPreFrozen()).isEqualTo(0);
            assertThat(lastLog.getFrozen()).isEqualTo(0);

            assertThat(wallet.getBalance()).isEqualTo(100L);
            assertThat(wallet.getFrozen()).isEqualTo(0L);
            assertThat(wallet.getAmount()).isEqualTo(100L);
            assertThat(wallet.getWithdrawal()).isEqualTo(0L);
            assertThat(wallet.getOutlay()).isEqualTo(0L);
            rst.put("t1", trans);
        }
        // 增加-2
        {
            // given
            builder.setAmount(10D);
            builder.setBizSubject("add");
            builder.setBizId("20001");
            builder.setExpiredAt(DateUtil.current() + Duration.ofDays(15).toMillis());
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(10L);

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(10L);
            assertThat(l1.getPreBalance()).isEqualTo(100L);
            assertThat(l1.getBalance()).isEqualTo(110L);
            assertThat(l1.getPreFrozen()).isEqualTo(0L);
            assertThat(l1.getFrozen()).isEqualTo(0L);

            assertThat(w1.getBalance()).isEqualTo(110L);
            assertThat(w1.getFrozen()).isEqualTo(0L);
            assertThat(w1.getAmount()).isEqualTo(110);
            assertThat(w1.getWithdrawal()).isEqualTo(0L);
            assertThat(w1.getOutlay()).isEqualTo(0L);
            rst.put("t2", t1);
        }
        // 增加-3
        {
            // given
            builder.setAmount(15D);
            builder.setBizSubject("add");
            builder.setBizId("20002");
            builder.setExpiredAt(DateUtil.current() + Duration.ofDays(5).toMillis());
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(15L);

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(15L);
            assertThat(l1.getPreBalance()).isEqualTo(110L);
            assertThat(l1.getBalance()).isEqualTo(125L);
            assertThat(l1.getPreFrozen()).isEqualTo(0L);
            assertThat(l1.getFrozen()).isEqualTo(0L);

            assertThat(w1.getBalance()).isEqualTo(125L);
            assertThat(w1.getFrozen()).isEqualTo(0L);
            assertThat(w1.getAmount()).isEqualTo(125L);
            assertThat(w1.getWithdrawal()).isEqualTo(0L);
            assertThat(w1.getOutlay()).isEqualTo(0L);
            rst.put("t3", t1);

            // when
            val outlays = outlayService.availableTransactions(uid, currency);
            // then

            assertThat(outlays.size()).isEqualTo(3);
        }
        // 支付
        {
            // given
            builder.setAmount(10D);
            builder.setBizSubject("sub");
            builder.setBizId("20011");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(10L);
            assertThat(t1.isOutlay()).isTrue();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(10L);
            assertThat(l1.getPreBalance()).isEqualTo(125L);
            assertThat(l1.getBalance()).isEqualTo(115L);

            assertThat(w1.getBalance()).isEqualTo(115L);
            assertThat(w1.getAmount()).isEqualTo(115L);
            assertThat(w1.getOutlay()).isEqualTo(10L);

            // when
            val outlays = outlayService.listByOutlayTransactionId(t1.getId());
            // then
            assertThat(outlays.size()).isEqualTo(1);
            assertThat(outlays.get(0).getAmount()).isEqualTo(10L);
            assertThat(outlays.get(0).getMargin()).isEqualTo(0L);
            assertThat(outlays.get(0).getTransactionId()).isEqualTo(rst.get("t3").getId());

            // when
            val income = outlayService.listByTransactionId(rst.get("t3").getId());
            assertThat(income.size()).isEqualTo(2);
            assertThat(income.get(0).getBalance()).isEqualTo(5L);
        }

        // 支付 - 2
        {
            // given
            builder.setAmount(35D);
            builder.setBizSubject("sub");
            builder.setBizId("20012");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(35L);
            assertThat(t1.isOutlay()).isTrue();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(35L);
            assertThat(l1.getPreBalance()).isEqualTo(115L);
            assertThat(l1.getBalance()).isEqualTo(80L);

            assertThat(w1.getBalance()).isEqualTo(80L);
            assertThat(w1.getAmount()).isEqualTo(80L);
            assertThat(w1.getOutlay()).isEqualTo(45L);

            // when
            val outlays = outlayService.listByOutlayTransactionId(t1.getId());
            // then
            assertThat(outlays.size()).isEqualTo(3);
            assertThat(outlays.get(0).getAmount()).isEqualTo(35L);
            assertThat(outlays.get(0).getUseAmount()).isEqualTo(5L);
            assertThat(outlays.get(0).getMargin()).isEqualTo(30L);
            assertThat(outlays.get(0).getTransactionId()).isEqualTo(rst.get("t3").getId());

            assertThat(outlays.get(1).getAmount()).isEqualTo(35L);
            assertThat(outlays.get(1).getUseAmount()).isEqualTo(10L);
            assertThat(outlays.get(1).getMargin()).isEqualTo(20L);
            assertThat(outlays.get(1).getTransactionId()).isEqualTo(rst.get("t2").getId());

            assertThat(outlays.get(2).getAmount()).isEqualTo(35L);
            assertThat(outlays.get(2).getUseAmount()).isEqualTo(20L);
            assertThat(outlays.get(2).getMargin()).isEqualTo(0L);
            assertThat(outlays.get(2).getTransactionId()).isEqualTo(rst.get("t1").getId());
            // when
            val income = outlayService.listByTransactionId(rst.get("t3").getId());
            assertThat(income.size()).isEqualTo(3);
            assertThat(income.get(0).getBalance()).isEqualTo(0L);

            val income1 = outlayService.listByTransactionId(rst.get("t2").getId());
            assertThat(income1.size()).isEqualTo(2);
            assertThat(income1.get(0).getBalance()).isEqualTo(0L);

            val income2 = outlayService.listByTransactionId(rst.get("t1").getId());
            assertThat(income2.size()).isEqualTo(2);
            assertThat(income2.get(0).getBalance()).isEqualTo(80L);
        }

        // 支付 - 余额不足
        {
            // given
            builder.setAmount(91D);
            builder.setBizSubject("sub");
            builder.setBizId("20013");
            // when
            assertThatThrownBy(() -> {
                walletService.trade(builder.build());
            }).hasMessage("balance is not enough");
        }

        // 支付 - 3
        {
            // given
            builder.setAmount(80D);
            builder.setBizSubject("sub");
            builder.setBizId("20014");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(80L);
            assertThat(t1.isOutlay()).isTrue();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(80L);
            assertThat(l1.getPreBalance()).isEqualTo(80L);
            assertThat(l1.getBalance()).isEqualTo(0L);

            assertThat(w1.getBalance()).isEqualTo(0L);
            assertThat(w1.getAmount()).isEqualTo(0L);
            assertThat(w1.getOutlay()).isEqualTo(125L);

            // when
            val outlays = outlayService.listByOutlayTransactionId(t1.getId());
            // then
            assertThat(outlays.size()).isEqualTo(1);
            assertThat(outlays.get(0).getAmount()).isEqualTo(80L);
            assertThat(outlays.get(0).getUseAmount()).isEqualTo(80L);
            assertThat(outlays.get(0).getMargin()).isEqualTo(0L);
            assertThat(outlays.get(0).getTransactionId()).isEqualTo(rst.get("t1").getId());

            // when
            val income2 = outlayService.listByTransactionId(rst.get("t1").getId());
            // then
            assertThat(income2.size()).isEqualTo(3);
            assertThat(income2.get(0).getBalance()).isEqualTo(0L);
        }
    }

    @Test
    void confirm() {
        // given
        val uid = 1L;
        val currency = "CNY";
        val builder = TradeDTO.newBuilder();
        builder.setUid(uid);
        builder.setCurrency(currency);
        builder.setBiz("test");
        // 充值
        {
            builder.setAmount(10.25D);
            builder.setBizSubject("deposit");
            builder.setBizId("10000");
            // when 充值
            val trans = walletService.trade(builder.build());
            val lastLog = walletService.getLastLog(uid, currency);
            val wallet = walletService.openWallet(uid, currency);

            // then
            assertThat(trans).isNotNull();
            assertThat(trans.getAmount()).isEqualTo(1025000000L);
            assertThat(trans.isOutlay()).isFalse();

            assertThat(lastLog).isNotNull();
            assertThat(lastLog.getAmount()).isEqualTo(1025000000L);
            assertThat(lastLog.getPreBalance()).isEqualTo(0L);
            assertThat(lastLog.getBalance()).isEqualTo(1025000000L);
            assertThat(lastLog.getPreFrozen()).isEqualTo(0);
            assertThat(lastLog.getFrozen()).isEqualTo(0);

            assertThat(wallet.getBalance()).isEqualTo(1025000000L);
            assertThat(wallet.getFrozen()).isEqualTo(0L);
            assertThat(wallet.getAmount()).isEqualTo(1025000000L);
            assertThat(wallet.getWithdrawal()).isEqualTo(1025000000L);
            assertThat(wallet.getOutlay()).isEqualTo(0L);
        }
        // 提现 - 提现完成
        {
            // given
            builder.setAmount(0.05D);
            builder.setBizSubject("withdraw");
            builder.setBizId("10001");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(5000000L);
            assertThat(t1.isOutlay()).isTrue();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(5000000L);
            assertThat(l1.getPreBalance()).isEqualTo(1025000000L);
            assertThat(l1.getBalance()).isEqualTo(1020000000L);
            assertThat(l1.getPreFrozen()).isEqualTo(0);
            assertThat(l1.getFrozen()).isEqualTo(5000000L);

            assertThat(w1.getBalance()).isEqualTo(1020000000L);
            assertThat(w1.getFrozen()).isEqualTo(5000000L);
            assertThat(w1.getAmount()).isEqualTo(1025000000L);
            assertThat(w1.getWithdrawal()).isEqualTo(1020000000L);
            assertThat(w1.getOutlay()).isEqualTo(5000000L);

            // when
            val confirmed = walletService.confirm(t1.getId());
            assertThat(confirmed).isTrue();
            val l2 = walletService.getLastLog(uid, currency);
            val w2 = walletService.openWallet(uid, currency);

            assertThat(l2).isNotNull();
            assertThat(l2.getAmount()).isEqualTo(5000000L);
            assertThat(l2.getPreBalance()).isEqualTo(1020000000L);
            assertThat(l2.getBalance()).isEqualTo(1020000000L);
            assertThat(l2.getPreFrozen()).isEqualTo(5000000L);
            assertThat(l2.getFrozen()).isEqualTo(0L);

            assertThat(w2.getBalance()).isEqualTo(1020000000L);
            assertThat(w2.getFrozen()).isEqualTo(0L);
            assertThat(w2.getAmount()).isEqualTo(1020000000L);
            assertThat(w2.getWithdrawal()).isEqualTo(1020000000L);
            assertThat(w2.getOutlay()).isEqualTo(5000000L);
        }
    }

    @Test
    void unfreeze() {
        // given
        val uid = 1L;
        val currency = "CNY";
        val builder = TradeDTO.newBuilder();
        builder.setUid(uid);
        builder.setCurrency(currency);
        builder.setBiz("test");
        // 充值
        {
            builder.setAmount(10.25D);
            builder.setBizSubject("deposit");
            builder.setBizId("10000");
            // when 充值
            val trans = walletService.trade(builder.build());
            val lastLog = walletService.getLastLog(uid, currency);
            val wallet = walletService.openWallet(uid, currency);

            // then
            assertThat(trans).isNotNull();
            assertThat(trans.getAmount()).isEqualTo(1025000000L);
            assertThat(trans.isOutlay()).isFalse();

            assertThat(lastLog).isNotNull();
            assertThat(lastLog.getAmount()).isEqualTo(1025000000L);
            assertThat(lastLog.getPreBalance()).isEqualTo(0L);
            assertThat(lastLog.getBalance()).isEqualTo(1025000000L);
            assertThat(lastLog.getPreFrozen()).isEqualTo(0);
            assertThat(lastLog.getFrozen()).isEqualTo(0);

            assertThat(wallet.getBalance()).isEqualTo(1025000000L);
            assertThat(wallet.getFrozen()).isEqualTo(0L);
            assertThat(wallet.getAmount()).isEqualTo(1025000000L);
            assertThat(wallet.getWithdrawal()).isEqualTo(1025000000L);
            assertThat(wallet.getOutlay()).isEqualTo(0L);
        }
        // 提现
        {
            // given
            builder.setAmount(0.05D);
            builder.setBizSubject("withdraw");
            builder.setBizId("10001");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(5000000L);
            assertThat(t1.isOutlay()).isTrue();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(5000000L);
            assertThat(l1.getPreBalance()).isEqualTo(1025000000L);
            assertThat(l1.getBalance()).isEqualTo(1020000000L);
            assertThat(l1.getPreFrozen()).isEqualTo(0);
            assertThat(l1.getFrozen()).isEqualTo(5000000L);

            assertThat(w1.getBalance()).isEqualTo(1020000000L);
            assertThat(w1.getFrozen()).isEqualTo(5000000L);
            assertThat(w1.getAmount()).isEqualTo(1025000000L);
            assertThat(w1.getWithdrawal()).isEqualTo(1020000000L);
            assertThat(w1.getOutlay()).isEqualTo(5000000L);
        }

        // 提现 - 提现完成
        {
            // given
            builder.setAmount(1D);
            builder.setBizSubject("withdraw");
            builder.setBizId("10002");
            // when
            val t1 = walletService.trade(builder.build());
            val l1 = walletService.getLastLog(uid, currency);
            val w1 = walletService.openWallet(uid, currency);

            assertThat(t1.getAmount()).isEqualTo(100000000L);
            assertThat(t1.isOutlay()).isTrue();

            assertThat(l1).isNotNull();
            assertThat(l1.getAmount()).isEqualTo(100000000L);
            assertThat(l1.getPreBalance()).isEqualTo(1020000000L);
            assertThat(l1.getBalance()).isEqualTo(920000000L);
            assertThat(l1.getPreFrozen()).isEqualTo(5000000L);
            assertThat(l1.getFrozen()).isEqualTo(105000000L);

            assertThat(w1.getBalance()).isEqualTo(920000000L);
            assertThat(w1.getFrozen()).isEqualTo(105000000L);
            assertThat(w1.getAmount()).isEqualTo(1025000000L);
            assertThat(w1.getWithdrawal()).isEqualTo(920000000L);
            assertThat(w1.getOutlay()).isEqualTo(105000000L);

            // when
            val confirmed = walletService.unfreeze(t1.getId());
            assertThat(confirmed).isTrue();
            val l2 = walletService.getLastLog(uid, currency);
            val w2 = walletService.openWallet(uid, currency);

            assertThat(l2).isNotNull();
            assertThat(l2.getAmount()).isEqualTo(100000000L);
            assertThat(l2.getPreBalance()).isEqualTo(920000000L);
            assertThat(l2.getBalance()).isEqualTo(1020000000L);
            assertThat(l2.getPreFrozen()).isEqualTo(105000000L);
            assertThat(l2.getFrozen()).isEqualTo(5000000L);

            assertThat(w2.getBalance()).isEqualTo(1020000000L);
            assertThat(w2.getFrozen()).isEqualTo(5000000L);
            assertThat(w2.getAmount()).isEqualTo(1025000000L);
            assertThat(w2.getWithdrawal()).isEqualTo(1020000000L);
            assertThat(w2.getOutlay()).isEqualTo(5000000L);
        }
    }

}
