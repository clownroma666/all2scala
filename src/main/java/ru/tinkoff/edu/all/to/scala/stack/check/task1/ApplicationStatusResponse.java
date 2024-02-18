package ru.tinkoff.edu.all.to.scala.stack.check.task1;

import java.time.Duration;

public sealed interface ApplicationStatusResponse {
    record Failure(Duration lastRequestTime, int retriesCount) implements ApplicationStatusResponse {}
    record Success(String id, String status) implements ApplicationStatusResponse {}
}
