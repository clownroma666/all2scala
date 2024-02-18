package ru.tinkoff.edu.all.to.scala.stack.check.task1;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.lang.Thread.sleep;

public class HandlerImpl implements Handler {

    private final Client client;

    public HandlerImpl(Client client) {
        this.client = client;
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        CompletableFuture<ApplicationStatusResponse> serv1 = asyncServiceCall(id, client::getApplicationStatus1);
        CompletableFuture<ApplicationStatusResponse> serv2 = asyncServiceCall(id, client::getApplicationStatus2);
        AtomicReference<ApplicationStatusResponse> result = new AtomicReference<>();
        serv1.acceptEither(serv2, result::set).orTimeout(15, TimeUnit.SECONDS).join();
        return result.get();
    }

    private CompletableFuture<ApplicationStatusResponse> asyncServiceCall(
            String id,
            Function<String, Response> serviceCall
    ) {
        return CompletableFuture.supplyAsync(() -> {
            int retriesCount = 0;
            while (true) {
                long start = System.currentTimeMillis();
                Response res = serviceCall.apply(id);
                if (res instanceof Response.Success success) {
                    return new ApplicationStatusResponse.Success(id, success.applicationStatus());
                }
                if (res instanceof Response.Failure failure) {
                    failure.ex().printStackTrace();
                    Duration requestTime = Duration.ofMillis(System.currentTimeMillis() - start);
                    return new ApplicationStatusResponse.Failure(requestTime, retriesCount);
                }
                Response.RetryAfter retryAfter = (Response.RetryAfter) res;
                try {
                    sleep(retryAfter.delay().toMillis());
                    retriesCount++;
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Wait between retries was interrupted", e);
                }
            }
        });
    }
}
