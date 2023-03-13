package penna.core.sink;

import penna.core.minilog.MiniLogger;
import penna.core.sink.impl.*;

import java.io.*;
import java.util.function.Supplier;

public final class PennaSink {
    private static Supplier<SinkImpl> defaultImplementation;

    private PennaSink() {}

    // The methods below are correctly typed.
    @SuppressWarnings("unchecked")
    private static final Supplier<Supplier<SinkImpl>>[] candidates = new Supplier[]{
            PennaSink::tryNative,
            PennaSink::tryJackson,
            PennaSink::tryGson,
            PennaSink::tryJakarta
    };

    private static void pickDefaultImplementation() {
        int counter = 0;
        do {
            defaultImplementation = candidates[counter].get();
            counter++;
        } while (counter < candidates.length && defaultImplementation == null);

        if (defaultImplementation == null) {
            MiniLogger.error("""
                           No implementation found.
                           Please add jackson, gson or a jakarta-compatible json library.
                           No log messages will be written because we can't format them.""");
            defaultImplementation = NOPSink::getInstance;
        }
    }

    // From the same ticket that PMD references, https://bugs.openjdk.org/browse/JDK-8080225, it is noted that
    // in JDK 10 the problem was solved. We are targeting JDK 17+, so the problem won't affect us.
    // Plus, any other alternative is significantly slower.
    @SuppressWarnings("PMD.AvoidFileStream")
    public static SinkImpl getSink() {
        if (defaultImplementation == null){
            pickDefaultImplementation();
        }

        try {
            SinkImpl sinkImpl = defaultImplementation.get();

            sinkImpl.init(new FileOutputStream(FileDescriptor.out).getChannel());
            return sinkImpl;
        } catch (IOException e) {
            MiniLogger.error("Unable to create logger", e);
        }
        return null;
    }

    private static Supplier<SinkImpl> tryNative() {
        return NativePennaSink::new;
    }

    private static Supplier<SinkImpl> tryJackson() {
        try {
            Class.forName("com.fasterxml.jackson.core.JsonGenerator");
            return JacksonPennaSink::new;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Supplier<SinkImpl> tryGson() {
        try {
            Class.forName("com.google.gson.stream.JsonWriter");
            return GsonPennaSink::new;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Supplier<SinkImpl> tryJakarta() {
        try {
            Class.forName("jakarta.json.stream.JsonGenerator");
            return JakartaPennaSink::new;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}