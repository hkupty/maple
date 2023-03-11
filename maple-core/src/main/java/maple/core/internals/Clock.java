package maple.core.internals;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public final class Clock {
    private Clock() {}

    // TODO make precision configurable;
    //  i.e. allow the clock to be updated every second, to reduce the load.
    //  Also, it is not necessarily precise already because it takes half a ms to update.
    private static final long precision = 1_000_000;
    private static Lock startThreadLock = new ReentrantLock();

    private static final AtomicLong timestamp = new AtomicLong(0L);
    private static final Thread clockThread = ThreadCreator.newThread("maple-clock-ticker", () -> {
        while(true) {
            timestamp.set(System.currentTimeMillis());
            LockSupport.parkNanos(precision);
        }
    });

    public static long getTimestamp() {
        if (!clockThread.isAlive() && startThreadLock.tryLock()) {
            // We preload the current value just in case we return before the thread updates
            timestamp.updateAndGet(ignored -> System.currentTimeMillis());
            clockThread.start();
            startThreadLock.unlock();
        }

        return timestamp.get();
    }
}
