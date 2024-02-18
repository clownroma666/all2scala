package ru.tinkoff.edu.all.to.scala.stack.check.task2;

import java.time.Duration;

public interface Handler {
    Duration timeout();

    void performOperation();
}