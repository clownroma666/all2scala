package ru.tinkoff.edu.all.to.scala.stack.check.task1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlerImplTest {

    private final Client client = mock();
    private final HandlerImpl handler = new HandlerImpl(client);

    @Test
    void ensureFastestWillSucceeded() {
        when(client.getApplicationStatus1(anyString())).thenAnswer(invocationOnMock -> {
            Thread.sleep(1000);
            return new Response.Success("fastest", invocationOnMock.getArgument(0, String.class));
        });
        when(client.getApplicationStatus2(anyString())).thenAnswer(invocationOnMock -> {
            Thread.sleep(2000);
            return new Response.Success("slowest", invocationOnMock.getArgument(0, String.class));
        });
        ApplicationStatusResponse applicationStatusResponse = handler.performOperation("id");

        Assertions.assertInstanceOf(ApplicationStatusResponse.Success.class, applicationStatusResponse);
        ApplicationStatusResponse.Success success = (ApplicationStatusResponse.Success) applicationStatusResponse;

        Assertions.assertEquals("id", success.id());
        Assertions.assertEquals("fastest", success.status());
    }

    @Test
    void ensureFastestFailureWillSucceeded() {
        when(client.getApplicationStatus1(anyString())).thenAnswer(invocationOnMock -> {
            Thread.sleep(1000);
            return new Response.Failure(new Throwable());
        });
        when(client.getApplicationStatus2(anyString())).thenAnswer(invocationOnMock -> {
            Thread.sleep(2000);
            return new Response.Success("slowest", invocationOnMock.getArgument(0, String.class));
        });
        ApplicationStatusResponse applicationStatusResponse = handler.performOperation("id");

        Assertions.assertInstanceOf(ApplicationStatusResponse.Failure.class, applicationStatusResponse);
        ApplicationStatusResponse.Failure failure = (ApplicationStatusResponse.Failure) applicationStatusResponse;

        Assertions.assertTrue(Duration.of(1, ChronoUnit.SECONDS).compareTo(failure.lastRequestTime()) < 0);
        Assertions.assertEquals(0, failure.retriesCount());
    }

    @Test
    void ensureFastestWillSucceedWithRetries() {
        when(client.getApplicationStatus1(anyString()))
                .thenAnswer(invocationOnMock -> new Response.RetryAfter(Duration.ofMillis(500)))
                .thenAnswer(invocationOnMock -> {
                    Thread.sleep(500);
                    return new Response.Success("fastest", invocationOnMock.getArgument(0, String.class));
                });
        when(client.getApplicationStatus2(anyString())).thenAnswer(invocationOnMock -> {
            Thread.sleep(2000);
            return new Response.Success("slowest", invocationOnMock.getArgument(0, String.class));
        });
        ApplicationStatusResponse applicationStatusResponse = handler.performOperation("id");

        Assertions.assertInstanceOf(ApplicationStatusResponse.Success.class, applicationStatusResponse);
        ApplicationStatusResponse.Success success = (ApplicationStatusResponse.Success) applicationStatusResponse;

        Assertions.assertEquals("id", success.id());
        Assertions.assertEquals("fastest", success.status());
    }

    @Test
    void ensureFastestFailureWillSucceededWithRetries() {
        when(client.getApplicationStatus1(anyString()))
                .thenAnswer(invocationOnMock -> {
                    return new Response.RetryAfter(Duration.ofMillis(500));
                })
                .thenAnswer(invocationOnMock -> {
                    Thread.sleep(500);
                    return new Response.Failure(new Throwable());
                });
        when(client.getApplicationStatus2(anyString())).thenAnswer(invocationOnMock -> {
            Thread.sleep(2000);
            return new Response.Success("slowest", invocationOnMock.getArgument(0, String.class));
        });
        ApplicationStatusResponse applicationStatusResponse = handler.performOperation("id");

        Assertions.assertInstanceOf(ApplicationStatusResponse.Failure.class, applicationStatusResponse);
        ApplicationStatusResponse.Failure failure = (ApplicationStatusResponse.Failure) applicationStatusResponse;

        Assertions.assertTrue(Duration.of(500, ChronoUnit.MILLIS).compareTo(failure.lastRequestTime()) < 0);
        Assertions.assertEquals(1, failure.retriesCount());
    }

    @Test
    void ensureAfter15SecsTimeoutOperationWillFail() {
        when(client.getApplicationStatus1(anyString())).thenAnswer(invocationOnMock -> {
            Thread.sleep(20000);
            return new Response.Success("fastest", invocationOnMock.getArgument(0, String.class));
        });
        when(client.getApplicationStatus2(anyString())).thenAnswer(invocationOnMock -> {
            Thread.sleep(20000);
            return new Response.Success("slowest", invocationOnMock.getArgument(0, String.class));
        });
        Assertions.assertThrowsExactly(
                CompletionException.class,
                () -> handler.performOperation("id")
        );
    }
}