package com.github.hkupty.maple.sink;

import com.github.hkupty.maple.models.JsonLog;
import com.github.hkupty.maple.sink.providers.LogFieldProvider;
import com.github.hkupty.maple.sink.providers.LoggerProvider;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.locks.ReentrantLock;


public class JakartaSink implements Sink, Sink.SinkWriter{
    private transient final ReentrantLock lock;
    private transient final OutputStream channel;
    private transient final JsonGeneratorFactory factory;
    private transient JsonGenerator generator;
    private transient final LogFieldProvider[] providers;

    public JakartaSink(LogFieldProvider[] providers) {
        channel = SharedSinkLogic.getOutputStream();
        factory = Json.createGeneratorFactory(null);
        lock = new ReentrantLock();

        this.providers = providers;

    }

    @Override
    public void render(JsonLog jsonLog) {
        try {
            lock.lock();
            generator = factory.createGenerator(channel);
            generator.writeStartObject();
            for (int i = 0; i < providers.length; i++) {
                providers[i].logField(this, jsonLog);
            }
            generator.writeEnd();
            generator.flush();
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void writeString(String key, String value) {
        generator.write(key, value);
    }

    @Override
    public void writeString(String value) {
        generator.write(value);
    }

    @Override
    public void writeDouble(double value) {
        generator.write(value);
    }

    @Override
    public void writeDouble(String key, double value) {
        generator.write(key, value);
    }

    @Override
    public void writeInteger(String key, int value) {
        generator.write(key, value);
    }

    @Override
    public void writeInteger(int value) {
        generator.write(value);
    }

    @Override
    public void writeLong(String key, long value) {
        generator.write(key, value);
    }

    @Override
    public void writeLong(long value) {
        generator.write(value);
    }

    @Override
    public void writeBigDecimal(String key, BigDecimal value) {
        generator.write(key, value);
    }

    @Override
    public void writeBigDecimal(BigDecimal value) {
        generator.write(value);
    }

    @Override
    public void writeBigInteger(String key, BigInteger value) {
        generator.write(key, value);
    }

    @Override
    public void writeBigInteger(BigInteger value) {
        generator.write(value);
    }

    @Override
    public void writeAny(String key, Object value) {
        // TODO re-evaluate
        generator.write(key, value.toString());
    }

    @Override
    public void writeAny(Object value) {
        // TODO re-evaluate
        generator.write(value.toString());
    }

    @Override
    public void writeBoolean(String key, Boolean value) {
        generator.write(key, value);
    }

    @Override
    public void writeBoolean(Boolean value) {
        generator.write(value);
    }

    @Override
    public void startObject(String objectKey) {
        generator.writeStartObject(objectKey);
    }

    @Override
    public void startObject() {
        generator.writeStartObject();
    }

    @Override
    public void startArray(String arrayKey) {
        generator.writeStartArray(arrayKey);

    }

    @Override
    public void startArray() {
        generator.writeStartArray();
    }

    @Override
    public void endArray() {
        generator.writeEnd();
    }

    @Override
    public void endObject() {
        generator.writeEnd();
    }
}
