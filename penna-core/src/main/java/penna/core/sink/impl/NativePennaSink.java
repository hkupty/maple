package penna.core.sink.impl;

import org.slf4j.MDC;
import org.slf4j.event.Level;
import penna.api.models.LogField;
import penna.core.internals.Clock;
import penna.core.internals.DirectJson;
import penna.core.models.PennaLogEvent;
import penna.core.sink.SinkImpl;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class NativePennaSink implements SinkImpl {
    private static final String[] LEVEL_MAPPING = new String[5];

    static {
        LEVEL_MAPPING[Level.TRACE.ordinal()] = "TRACE";
        LEVEL_MAPPING[Level.DEBUG.ordinal()] = "DEBUG";
        LEVEL_MAPPING[Level.INFO.ordinal()] = "INFO";
        LEVEL_MAPPING[Level.WARN.ordinal()] = "WARN";
        LEVEL_MAPPING[Level.ERROR.ordinal()] = "ERROR";
    }

    /**
     * The Emitter functional interface allows us to define the specific
     * function signature `PennaLogEvent -> ()` that throws an exception without using generics.
     * This, in itself, is not a huge advantage, but allows us to go for a very straightforward
     * approach when writing the log messages, by picking the fields we want to write from the message
     * based on {@link PennaLogEvent#fieldsToLog} and mapping to the appropriate function.
     * <br />
     * In {@link NativePennaSink}, it is defined as a local reference to each emit* method in an
     * array, where each method is in the same position in the mapping array as the
     * respective {@link LogField#ordinal()} for that log field.
     * <br />
     * In other words, we do a very fast lookup based on array position to determine which emit* methods to call,
     * in which order.
     * While it might get duplicated in other sinks, the compiler seems to like it better when
     * kept as a private interface, as more optimizations are possible.
     */
    @FunctionalInterface
    private interface Emitter {
        void apply(final PennaLogEvent event) throws IOException;
    }

    private final Emitter[] emitters;
    private static final int MAX_STACK_DEPTH = 15;

    private final AtomicLong counter = new AtomicLong(0L);

    private DirectJson jsonGenerator;



    public NativePennaSink() {
        // WARNING! Introducing new log fields requires this array to be updated.
        emitters = new Emitter[LogField.values().length];
        emitters[LogField.COUNTER.ordinal()] = this::emitCounter;
        emitters[LogField.TIMESTAMP.ordinal()] = this::emitTimestamp;
        emitters[LogField.LEVEL.ordinal()] = this::emitLevel;
        emitters[LogField.MESSAGE.ordinal()] = this::emitMessage;
        emitters[LogField.LOGGER_NAME.ordinal()] = this::emitLogger;
        emitters[LogField.THREAD_NAME.ordinal()] = this::emitThreadName;
        emitters[LogField.MDC.ordinal()] = this::emitMDC;
        emitters[LogField.MARKERS.ordinal()] = this::emitMarkers;
        emitters[LogField.THROWABLE.ordinal()] = this::emitThrowable;
        emitters[LogField.KEY_VALUE_PAIRS.ordinal()] = this::emitKeyValuePair;
        emitters[LogField.EXTRA.ordinal()] = this::emitExtra;
    }

    @Override
    public void init(final FileChannel channel) throws IOException {
        jsonGenerator = new DirectJson(channel);
    }

    private void writeThrowable(final Throwable throwable) throws IOException {
        final String message;
        StackTraceElement[] frames;
        Throwable cause;

        if((message = throwable.getMessage()) != null) {
            jsonGenerator.writeStringValue("message", message);
        }

        if ((frames = throwable.getStackTrace()) != null) {
            jsonGenerator.writeString("stacktrace");
            jsonGenerator.writeEntrySep();
            jsonGenerator.openArray();
            for (int index = 0; index < Math.min(frames.length, MAX_STACK_DEPTH); index++) {
                jsonGenerator.writeString(frames[index].toString());
            }

            if (frames.length > MAX_STACK_DEPTH) {
                jsonGenerator.writeString("...");
            }
            jsonGenerator.closeArray();
            jsonGenerator.writeSep();
        }

        if ((cause = throwable.getCause()) != null) {
            jsonGenerator.writeString("cause");
            jsonGenerator.writeEntrySep();
            jsonGenerator.openObject();
            writeThrowable(cause);
            jsonGenerator.closeObject();
            jsonGenerator.writeSep();
        }
    }

    private void writeMap(final Map map) throws IOException {
        jsonGenerator.openObject();
        for (var key : map.keySet()) {
            jsonGenerator.writeString(key.toString());
            jsonGenerator.writeEntrySep();
            writeObject(map.get(key));
            jsonGenerator.writeSep();
        }
        jsonGenerator.closeObject();
        jsonGenerator.writeSep();
    }

    private void writeArray(final List lst) throws IOException {
        jsonGenerator.openArray();
        for(var value : lst){
            writeObject(value);
        }
        jsonGenerator.closeArray();
        jsonGenerator.writeSep();
    }

    private void writeArray(final Object... lst) throws IOException {
        jsonGenerator.openArray();
        for (var value : lst) {
            writeObject(value);
        }
        jsonGenerator.closeArray();
        jsonGenerator.writeSep();
    }

    private void writeObject(final Object object) throws IOException {
        if (object instanceof Throwable throwable) {
            writeThrowable(throwable);
        } else if (object instanceof Map map) {
            writeMap(map);
        } else if (object instanceof List lst) {
            writeArray(lst);
        } else if (object instanceof Object[] lst) {
            writeArray(lst);
        } else if (object instanceof String str){
            jsonGenerator.writeString(str);
        } else if (object instanceof Long lng){
            jsonGenerator.writeNumber(lng);
        } else if (object instanceof Double dbl){
            jsonGenerator.writeNumber(dbl);
        } else {
            jsonGenerator.writeString(object.toString());
        }
    }

    private void emitMessage(final PennaLogEvent logEvent) throws IOException {
        jsonGenerator.writeStringValue(LogField.MESSAGE.fieldName, logEvent.message);
    }


    // The method must conform to the functional interface, so we should ignore this rule here.
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void emitTimestamp(final PennaLogEvent logEvent) throws IOException {
        jsonGenerator.writeNumberValue(LogField.TIMESTAMP.fieldName, Clock.getTimestamp());
    }

    // The method must conform to the functional interface, so we should ignore this rule here.
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void emitMDC(final PennaLogEvent logEvent) throws IOException {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc != null) {
            jsonGenerator.openObject(LogField.MDC.fieldName);
            for (var kv : mdc.entrySet()) {
                jsonGenerator.writeStringValue(kv.getKey(), kv.getValue());
            }
            jsonGenerator.closeObject();
        }
    }

    private void emitLogger(final PennaLogEvent logEvent) throws IOException {
        jsonGenerator.writeStringValue(LogField.LOGGER_NAME.fieldName, logEvent.getLoggerName());
    }

    private void emitLevel(final PennaLogEvent logEvent) throws IOException {
        jsonGenerator.writeStringValue(LogField.LEVEL.fieldName, LEVEL_MAPPING[logEvent.level.ordinal()]);
    }

    private void emitThreadName(final PennaLogEvent logEvent) throws IOException {
        jsonGenerator.writeStringValue(LogField.THREAD_NAME.fieldName, logEvent.getThreadName());
    }

    // The method must conform to the functional interface, so we should ignore this rule here.
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void emitCounter(final PennaLogEvent logEvent) throws IOException {
        jsonGenerator.writeNumberValue(LogField.COUNTER.fieldName, counter.getAndIncrement());
    }

    private void emitMarkers(final PennaLogEvent logEvent) throws IOException {
        if (!logEvent.markers.isEmpty()) {
            jsonGenerator.openArray(LogField.MARKERS.fieldName);
            for (int i = 0; i < logEvent.markers.size(); i++) {
                jsonGenerator.writeString(logEvent.markers.get(i).getName());
            }
            jsonGenerator.closeArray();
            jsonGenerator.writeSep();
        }
    }

    private void emitThrowable(final PennaLogEvent logEvent) throws IOException {
        if (logEvent.throwable != null) {
            jsonGenerator.openObject(LogField.THROWABLE.fieldName);
            writeThrowable(logEvent.throwable);
            jsonGenerator.closeObject();
            jsonGenerator.writeSep();
        }
    }

    private void emitKeyValuePair(final PennaLogEvent logEvent) throws IOException {
        if (!logEvent.keyValuePairs.isEmpty()) {
            jsonGenerator.openObject(LogField.KEY_VALUE_PAIRS.fieldName);
            for (int i = 0; i < logEvent.keyValuePairs.size(); i++) {
                var kvp = logEvent.keyValuePairs.get(i);
                jsonGenerator.writeString(kvp.key);
                jsonGenerator.writeEntrySep();
                writeObject(kvp.value);
            }
            jsonGenerator.closeObject();
            jsonGenerator.writeSep();
        }
    }

    private void emitExtra (final PennaLogEvent logEvent) throws IOException {
        if (logEvent.extra != null) {
            jsonGenerator.openObject(LogField.THROWABLE.fieldName);
            writeObject(logEvent.throwable);
            jsonGenerator.closeObject();
            jsonGenerator.writeSep();
        }
    }

    @Override
    public void write(final PennaLogEvent logEvent) throws IOException {
        jsonGenerator.openObject();
        for (int i = 0; i < logEvent.fieldsToLog.length; i++){
            emitters[logEvent.fieldsToLog[i].ordinal()].apply(logEvent);
        }
        jsonGenerator.closeObject();
        jsonGenerator.flush();
    }
}
