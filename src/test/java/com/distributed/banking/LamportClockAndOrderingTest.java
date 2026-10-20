package com.distributed.banking;

import com.distributed.banking.distributed.LamportClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LamportClockAndOrderingTest {

    @Test
    @DisplayName("Scenario 14: Lamport Logical Clock Progression and Partial Ordering")
    void testLamportClockRules() {
        LamportClock clockNode1 = new LamportClock(0L);
        LamportClock clockNode2 = new LamportClock(0L);

        // Rule 1: Local event on Node 1 increments clock
        long e1 = clockNode1.tick();
        assertEquals(1L, e1);
        long e2 = clockNode1.tick();
        assertEquals(2L, e2);
        assertTrue(e1 < e2, "Local event ordering must satisfy L(e1) < L(e2)");

        // Rule 2: Message sent with timestamp of Node 1
        long msgTimestamp = clockNode1.tick(); // e3 on Node 1 = 3
        assertEquals(3L, msgTimestamp);

        // Node 2 has independent clock at 0
        assertEquals(0L, clockNode2.getTime());

        // Rule 3: Node 2 receives message m from Node 1
        // L2 = max(L2, L_msg) + 1 = max(0, 3) + 1 = 4
        long receiveEvent = clockNode2.updateAndTick(msgTimestamp);
        assertEquals(4L, receiveEvent);
        assertTrue(msgTimestamp < receiveEvent, "Causal ordering: L(send) < L(receive)");

        // Another message where Node 2 clock is higher:
        // Node 2 advances to 10
        clockNode2.setTime(10L);
        long higherMsg = clockNode2.tick(); // 11
        // Node 1 receives 11 when Node 1 is at 5
        clockNode1.setTime(5L);
        long node1Update = clockNode1.updateAndTick(higherMsg); // max(5, 11) + 1 = 12
        assertEquals(12L, node1Update);
    }
}
