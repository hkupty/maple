package penna.core.logger.guard;

import penna.core.logger.PennaLogger;
import penna.core.logger.event.PennaLogEventBuilder;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;

public final class InfoLevelGuard implements LevelGuard {
    private InfoLevelGuard(){}

    private static final LevelGuard instance = new InfoLevelGuard();
    public static LevelGuard singleton() {
        return instance;
    }
    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public LoggingEventBuilder trace(PennaLogger logger) {
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public LoggingEventBuilder debug(PennaLogger logger) {
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public LoggingEventBuilder info(PennaLogger logger) {
        return PennaLogEventBuilder.Factory.get(logger, Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public LoggingEventBuilder warn(PennaLogger logger) {
        return PennaLogEventBuilder.Factory.get(logger, Level.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public LoggingEventBuilder error(PennaLogger logger) {
        return PennaLogEventBuilder.Factory.get(logger, Level.ERROR);
    }
}
