package com.apzda.cloud.config.domain.repository;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.apzda.cloud.config.TestApp;
import com.apzda.cloud.config.autoconfig.ConfigAutoConfiguration;
import com.apzda.cloud.config.domain.entity.Setting;
import com.apzda.cloud.test.autoconfig.AutoConfigureGsvcTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.AutoConfigureDataRedis;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@DataJpaTest
@ContextConfiguration(classes = TestApp.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration({ ConfigAutoConfiguration.class })
@AutoConfigureJson
@AutoConfigureDataRedis
@AutoConfigureGsvcTest
@ActiveProfiles({ "test" })
@Testcontainers(parallel = true)
class SettingRepositoryTest {

    @Autowired
    private SettingRepository settingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @Modifying
    void soft_delete_should_be_ok() {
        // given
        val setting = new Setting();
        setting.setSettingKey("123");
        setting.setSetting("123");
        setting.setSettingCls("Object");

        // when
        val saved = settingRepository.save(setting);
        val id = saved.getId();
        assertThat(id).isNotNull();

        // flush change to database
        entityManager.flush();
        // delete
        settingRepository.deleteById(id);
        // flush change to database
        entityManager.flush();

        val setting1 = settingRepository.findBySettingKey("123");
        assertThat(setting1).isNull();

        val setting2 = settingRepository.findById(id);
        assertThat(setting2).isEmpty();
    }

    @Test
    @Modifying
    void version_optimistic_should_be_ok() {

        assertThatThrownBy(() -> {
            updateSetting("bb").join();
        }).cause().isInstanceOf(OptimisticLockingFailureException.class);
    }

    CompletableFuture<Void> updateSetting(String str) {
        return CompletableFuture.supplyAsync(() -> transactionTemplate.execute(status -> {
            // given
            var setting = new Setting();
            setting.setSettingKey("a");
            setting.setSetting("123");
            setting.setSettingCls("Object");

            // when
            settingRepository.save(setting);
            entityManager.flush();

            setting = settingRepository.findBySettingKey("a");
            val oriSetting = BeanUtil.copyProperties(setting, Setting.class);
            System.out.println("setting[1] = " + oriSetting);
            setting.setSetting(DateUtil.now());
            settingRepository.save(setting);
            return oriSetting;
        })).thenAcceptAsync((setting) -> {
            transactionTemplate.executeWithoutResult(status -> {
                val s = new Setting();
                s.setId(setting.getId());
                s.setVersion(setting.getVersion());
                s.setSetting(str + "_2");
                System.out.println("setting[2] = " + s);
                settingRepository.save(s);
                // entityManager.merge(s);
            });
        });
    }

}
