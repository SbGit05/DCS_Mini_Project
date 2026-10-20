package com.distributed.banking.distributed;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe Lamport Logical Clock implementation.
 * Guarantees partial ordering of distributed events across branch nodes.
 *
 * Rule 1: A process increments its counter before each local event: L = L + 1.
 * Rule 2: When sending a message m, it includes timestamp L.
 * Rule 3: Upon receiving message m with timestamp L_msg, process updates: L = max(L, L_msg) + 1.
 */
@Component
public class LamportClock {

    private final AtomicLong clock;

    public LamportClock() {
        this.clock = new AtomicLong(0L);
    }

    public LamportClock(long initialTime) {
        this.clock = new AtomicLong(initialTime);
    }

    /**
     * Local event tick: L = L + 1
     */
    public long tick() {
        return clock.incrementAndGet();
    }

    /**
     * Message receive update: L = max(L, incomingTimestamp) + 1
     */
    public long updateAndTick(long incomingTimestamp) {
        return clock.updateAndGet(current -> Math.max(current, incomingTimestamp) + 1);
    }

    /**
     * Current clock value without advancing.
     */
    public long getTime() {
        return clock.get();
    }

    /**
     * Set explicit time (e.g. during recovery or node synchronization).
     */
    public void setTime(long newTime) {
        clock.updateAndGet(current -> Math.max(current, newTime));
    }

    @Override
    public String toString() {
        return "LamportClock{time=" + clock.get() + "}";
    }
}
