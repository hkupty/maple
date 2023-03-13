package penna.throughput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        int time = 10;
        int threads = 1;
        if (args.length > 0) {
            time = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            threads = Integer.parseInt(args[1]);
        }
        Logger logger = LoggerFactory.getLogger(Main.class);
        Instant target = Instant.now().plus(time, ChronoUnit.MILLIS);

        for (int i = 0; i < threads; i++) {
            int finalI = i;
            var thread = new Thread(() -> {
                while (Instant.now().isBefore(target)) {

                    try {
                        var ex = new IllegalArgumentException("wrong type!!!\nok???", new RuntimeException("hello", new ArrayIndexOutOfBoundsException(5)));
                        ex.addSuppressed(new RuntimeException());
                        throw ex;
                    } catch(Exception e) {
                        logger.atInfo()
                                .addKeyValue("version", 5)
                                .addKeyValue("hello", "a\ncouple\nlines")
                                .setCause(e)
                                .addKeyValue("blah", ("[" + finalI + "]").repeat(1000))
                                .log("Hello \n{}!", "world");
                    }
                }
            });
            thread.setName("penna-" + i);
            thread.start();
        }
    }
}