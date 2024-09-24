package com.apzda.cloud.uc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@SpringBootApplication
@ComponentScan(
    excludeFilters = {
        @ComponentScan.Filter(classes = {SpringBootApplication.class, AutoConfiguration.class})
    }
)
@TestPropertySource(properties = {"apzda.ucenter.security.auto-sync=false"})
public class TestApp {
}
