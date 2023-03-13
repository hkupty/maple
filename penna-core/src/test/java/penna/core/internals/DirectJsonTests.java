package penna.core.internals;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DirectJsonTests {

    @Test
    void can_write_longs_to_buffer() {
        DirectJson directJson = new DirectJson();

        directJson.writeNumber(123);
        directJson.buffer.flip();
        var charset = StandardCharsets.UTF_8;
        var chars = charset.decode(directJson.buffer).toString();
        assertEquals("123,", chars);
    }


    @Test
    void can_write_strings_to_buffer() {
        DirectJson directJson = new DirectJson();

        directJson.writeString("hello");
        directJson.buffer.flip();
        var charset = StandardCharsets.UTF_8;
        var chars = charset.decode(directJson.buffer).toString();
        assertEquals("\"hello\",", chars);
    }

    @Test
    void can_write_kv_to_buffer() {
        DirectJson directJson = new DirectJson();

        directJson.openObject();
        directJson.writeStringValue("hello", "world");
        directJson.closeObject();
        directJson.buffer.flip();
        var charset = StandardCharsets.UTF_8;
        var chars = charset.decode(directJson.buffer).toString();
        assertEquals("{\"hello\":\"world\"}", chars);
    }

    @Test
    void can_write_array_to_buffer() {
        DirectJson directJson = new DirectJson();

        directJson.openArray();
        directJson.writeString("hello");
        directJson.writeString("world");
        directJson.closeArray();
        directJson.buffer.flip();
        var charset = StandardCharsets.UTF_8;
        var chars = charset.decode(directJson.buffer).toString();
        assertEquals("[\"hello\",\"world\"]", chars);
    }

    @Test
    void can_write_number_kv_to_buffer() {
        DirectJson directJson = new DirectJson();

        directJson.openObject();
        directJson.writeNumberValue("hello", 1337);
        directJson.closeObject();
        directJson.buffer.flip();
        var charset = StandardCharsets.UTF_8;
        var chars = charset.decode(directJson.buffer).toString();
        assertEquals("{\"hello\":1337}", chars);
    }

    @Test
    void write_to_stdout() {
        DirectJson directJson = new DirectJson();
        directJson.openObject();
        directJson.writeNumberValue("hello", 1337.1);
        directJson.closeObject();
        try {
            directJson.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
