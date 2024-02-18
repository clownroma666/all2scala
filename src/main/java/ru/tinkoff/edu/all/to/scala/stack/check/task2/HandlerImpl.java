package ru.tinkoff.edu.all.to.scala.stack.check.task2;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class HandlerImpl implements Handler {

    private final Client client;
    private final Duration timeout;

    public HandlerImpl(Client client, Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    @Override
    public Duration timeout() {
        return timeout;
    }

    @Override
    public void performOperation() {
        long timeout = timeout().toMillis();
        while (true) {
            Event event = client.readData();
            long currentTime = System.currentTimeMillis();
            Map<Address, Long> toSend = new HashMap<>();
            for (Address address : event.recipients()) {
                toSend.put(address, currentTime);
            }
            final long[] nearestSendTime = {0};
            while (!toSend.isEmpty()) {
                waitForNextSendTime(nearestSendTime);

                Map<Address, Long> rejected = new HashMap<>();

                nearestSendTime[0] = Long.MAX_VALUE;
                toSend.forEach((address, nextSendTime) -> {
                    if (System.currentTimeMillis() >= nextSendTime) {
                        // ready to send
                        if (Result.ACCEPTED == client.sendData(address, event.payload())) {
                            nextSendTime = Long.MAX_VALUE;
                        } else {
                            nextSendTime = System.currentTimeMillis() + timeout;
                            rejected.put(address, nextSendTime);
                        }
                    } else {
                        rejected.put(address, nextSendTime);
                    }
                    nearestSendTime[0] = Math.min(nextSendTime, nearestSendTime[0]);
                });

                toSend = rejected;
            }
        }
    }

    private static void waitForNextSendTime(long[] nearestSendTime) {
        long sleepTime = nearestSendTime[0] - System.currentTimeMillis();
        if (sleepTime > 0) {
            // wait next send time
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Infinite operation method interrupted", e);
            }
        }
    }
}
