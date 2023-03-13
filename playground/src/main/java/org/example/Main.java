package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class);
        Logger otherLogger = LoggerFactory.getLogger("simple.logger");

        logger.info("Hello world!", new RuntimeException("Something awful!"));
        logger.debug("This should be shown");
        otherLogger.info("Hello world!");
        otherLogger.debug("This should be not shown");

    }
}