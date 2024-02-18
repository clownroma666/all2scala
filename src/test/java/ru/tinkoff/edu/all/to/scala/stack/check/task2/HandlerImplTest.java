package ru.tinkoff.edu.all.to.scala.stack.check.task2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HandlerImplTest {

    @Test
    void performOperation() throws InterruptedException {
        Client client = mock();
        HandlerImpl handler = new HandlerImpl(client, Duration.of(100, ChronoUnit.MILLIS));
        CountDownLatch testFinished = new CountDownLatch(1);

        List<Address> recipients1 = new ArrayList<>();
        Address address11 = new Address("datacenter1", "node1");
        recipients1.add(address11);
        Address address12 = new Address("datacenter2", "node2");
        recipients1.add(address12);
        Payload payload1 = new Payload("origin1", null);
        Event event1 = new Event(recipients1, payload1);
        when(client.readData())
                .thenReturn(event1)
                .thenAnswer(invocationOnMock -> {
                    testFinished.countDown();
                    throw new IllegalThreadStateException("Test exit");
                });

        when(client.sendData(address11, payload1))
                .thenReturn(Result.REJECTED)
                .thenReturn(Result.ACCEPTED)
                .thenThrow(new IllegalStateException("client.sendData(address11, payload1)"));
        when(client.sendData(address12, payload1)).thenReturn(Result.ACCEPTED)
                .thenThrow(new IllegalStateException("client.sendData(address12, payload1)"));

        Thread thread = new Thread(handler::performOperation);
        thread.start();

        boolean success = testFinished.await(1, TimeUnit.SECONDS);
        if (!success) {
            thread.interrupt();
        }
        Assertions.assertTrue(success);
        verify(client, times(2)).readData();
        verify(client, times(2)).sendData(address11, payload1);
        verify(client, times(1)).sendData(address12, payload1);
    }
}