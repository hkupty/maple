package penna.core.sink;

import penna.core.models.PennaLogEvent;
import penna.core.sink.impl.*;

import java.io.IOException;
import java.nio.channels.FileChannel;

public sealed interface SinkImpl permits DummySink, GsonPennaSink, JacksonPennaSink, JakartaPennaSink, NOPSink, NativePennaSink {
    void init(FileChannel channel) throws IOException;
    void write(PennaLogEvent logEvent) throws IOException;
}
