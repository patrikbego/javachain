package com.javachain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TestApplicationRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestApplicationRunner.class);

    public TestApplicationRunner() {
        LOGGER.info("Test Application Runner started!");
    }

    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("About to do nothing!");
        // Do nothing...
    }

}
