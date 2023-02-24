package maple.core.sink.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import maple.api.models.LogField;
import maple.core.models.MapleLogEvent;
import maple.core.internals.ThreadCreator;
import maple.core.sink.SinkImpl;
import maple.core.sink.impl.jackson.NOPPrettyPrinter;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class JacksonMapleSink implements SinkImpl {
    private static final String LINE_BREAK = System.getProperty("line.separator");
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
     * function signature `MapleLogEvent -> ()` that throws an exception without using generics.
     * This, in itself, is not a huge advantage, but allows us to go for a very straightforward
     * approach when writing the log messages, by picking the fields we want to write from the message
     * based on {@link MapleLogEvent#fieldsToLog} and mapping to the appropriate function.
     * <br />
     * In {@link JacksonMapleSink}, it is defined as a local reference to each emit* method in an
     * array, where each method is in the same position in the mapping array as the
     * respective {@link LogField#ordinal()} for that log field.
     * <br />
     * In other words, we do a very fast lookup based on array position to determine which emit* methods to call,
     * in which order.
     */
    @FunctionalInterface
    private interface Emitter {
        void apply(MapleLogEvent event) throws IOException;
    }
    private final Emitter[] emitters;
    private static final int MAX_STACK_DEPTH = 15;

    private final AtomicLong counter = new AtomicLong(0L);
    private final AtomicLong timestamp = new AtomicLong(0L);

    private final Thread timestampTicker;

    private JsonGenerator jsonGenerator;



    public JacksonMapleSink() {
        timestampTicker = ThreadCreator.newThread("maple-timestamp-ticker", () -> {
          while (true)  {
              timestamp.set(System.currentTimeMillis());
              LockSupport.parkNanos(1_000_000);
          }
        });
        timestampTicker.start();

        // WARNING! Introducing new log fields requires this array to be updated.
        emitters = new Emitter[LogField.values().length];
        emitters[LogField.Counter.ordinal()] = this::emitCounter;
        emitters[LogField.Timestamp.ordinal()] = this::emitTimestamp;
        emitters[LogField.Level.ordinal()] = this::emitLevel;
        emitters[LogField.Message.ordinal()] = this::emitMessage;
        emitters[LogField.LoggerName.ordinal()] = this::emitLogger;
        emitters[LogField.ThreadName.ordinal()] = this::emitThreadName;
        emitters[LogField.MDC.ordinal()] = this::emitMDC;
        emitters[LogField.Markers.ordinal()] = this::emitMarkers;
        emitters[LogField.Throwable.ordinal()] = this::emitThrowable;
        emitters[LogField.KeyValuePairs.ordinal()] = this::emitKeyValuePair;
        emitters[LogField.Extra.ordinal()] = this::emitExtra;
    }

    @Override
    public void init(Writer writer) throws IOException {
        JsonFactory factory = JsonFactory.builder()
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
                .build();

        jsonGenerator = factory.createGenerator(writer);
        jsonGenerator.setPrettyPrinter(NOPPrettyPrinter.getInstance());
        jsonGenerator.enable(Feature.FLUSH_PASSED_TO_STREAM);
        jsonGenerator.disable(Feature.AUTO_CLOSE_TARGET);
        jsonGenerator.disable(Feature.STRICT_DUPLICATE_DETECTION);
        jsonGenerator.disable(Feature.AUTO_CLOSE_JSON_CONTENT);
        // Initialize with an empty line break
        jsonGenerator.writeRaw(LINE_BREAK);
    }

    private void writeThrowable(Throwable throwable) throws IOException {
        String message;
        StackTraceElement[] frames;
        Throwable cause;

        if((message = throwable.getMessage()) != null) {
            jsonGenerator.writeStringField("message", message);
        }

        if ((frames = throwable.getStackTrace()) != null) {
            jsonGenerator.writeArrayFieldStart("stacktrace");
            for (int index = 0; index < Math.min(frames.length, MAX_STACK_DEPTH); index++) {
                jsonGenerator.writeString(frames[index].toString());
            }

            if (frames.length > MAX_STACK_DEPTH) {
                jsonGenerator.writeString("...");
            }
            jsonGenerator.writeEndArray();
        }

        if ((cause = throwable.getCause()) != null) {
            jsonGenerator.writeObjectFieldStart("cause");
            writeThrowable(cause);
            jsonGenerator.writeEndObject();
        }
    }

    private void writeMap(Map map) throws IOException {
        jsonGenerator.writeStartObject();
        for (var key : map.keySet()) {
            jsonGenerator.writeFieldName(key.toString());
            writeObject(map.get(key));
        }
        jsonGenerator.writeEndObject();
    }

    private void writeArray(List lst) throws IOException {
        jsonGenerator.writeStartArray();
        for(var value : lst){
            writeObject(value);
        }
        jsonGenerator.writeEndArray();
    }

    private void writeArray(Object[] lst) throws IOException {
        jsonGenerator.writeStartArray();
        for (var value : lst) {
            writeObject(value);
        }
        jsonGenerator.writeEndArray();
    }

    private void writeObject(Object object) throws IOException {
        if (object instanceof Throwable throwable) {
            writeThrowable(throwable);
        } else if (object instanceof Map map) {
            writeMap(map);
        } else if (object instanceof List lst) {
            writeArray(lst);
        } else if (object instanceof Object[] lst) {
            writeArray(lst);
        } else {
            jsonGenerator.writeObject(object);
        }
    }

    private void emitMessage(MapleLogEvent logEvent) throws IOException {
        jsonGenerator.writeStringField(LogField.Message.fieldName, logEvent.message);
    }


    private void emitTimestamp(MapleLogEvent logEvent) throws IOException {
        jsonGenerator.writeNumberField(LogField.Timestamp.fieldName, timestamp.get());
    }

    private void emitMDC(MapleLogEvent logEvent) throws IOException {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc != null) {
            jsonGenerator.writeObjectFieldStart(LogField.MDC.fieldName);
            for (var kv : mdc.entrySet()) {
                jsonGenerator.writeStringField(kv.getKey(), kv.getValue());
            }
            jsonGenerator.writeEndObject();
        }
    }

    private void emitLogger(MapleLogEvent logEvent) throws IOException {
        jsonGenerator.writeStringField(LogField.LoggerName.fieldName, logEvent.getLoggerName());
    }

    private void emitLevel(MapleLogEvent logEvent) throws IOException {
        jsonGenerator.writeStringField(LogField.Level.fieldName, LEVEL_MAPPING[logEvent.level.ordinal()]);
    }

    private void emitThreadName(MapleLogEvent logEvent) throws IOException {
        jsonGenerator.writeStringField(LogField.ThreadName.fieldName, logEvent.getThreadName());
    }

    private void emitCounter(MapleLogEvent logEvent) throws IOException {
        jsonGenerator.writeNumberField(LogField.Counter.fieldName, counter.getAndIncrement());
    }

    private void emitMarkers(MapleLogEvent logEvent) throws IOException {
        if (!logEvent.markers.isEmpty()) {
            jsonGenerator.writeArrayFieldStart(LogField.Markers.fieldName);
            for (int i = 0; i < logEvent.markers.size(); i++) {
                jsonGenerator.writeString(logEvent.markers.get(i).getName());
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void emitThrowable(MapleLogEvent logEvent) throws IOException {
        if (logEvent.throwable != null) {
            jsonGenerator.writeObjectFieldStart(LogField.Throwable.fieldName);
            writeThrowable(logEvent.throwable);
            jsonGenerator.writeEndObject();
        }
    }

    private void emitKeyValuePair(MapleLogEvent logEvent) throws IOException {
        if (!logEvent.keyValuePairs.isEmpty()) {
            jsonGenerator.writeObjectFieldStart(LogField.KeyValuePairs.fieldName);
            for (int i = 0; i < logEvent.keyValuePairs.size(); i++) {
                var kvp = logEvent.keyValuePairs.get(i);
                jsonGenerator.writeFieldName(kvp.key);
                writeObject(kvp.value);
            }
            jsonGenerator.writeEndObject();
        }
    }

    private void emitExtra (MapleLogEvent logEvent) throws IOException {
        if (logEvent.extra != null) {
            jsonGenerator.writeObjectFieldStart(LogField.Throwable.fieldName);
            writeObject(logEvent.throwable);
            jsonGenerator.writeEndObject();
        }
    }

    @Override
    public void write(MapleLogEvent logEvent) throws IOException {
        jsonGenerator.writeStartObject();
        for (int i = 0; i < logEvent.fieldsToLog.length; i++){
            emitters[logEvent.fieldsToLog[i].ordinal()].apply(logEvent);
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.flush();
    }
}