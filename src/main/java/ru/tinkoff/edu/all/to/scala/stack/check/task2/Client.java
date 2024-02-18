package ru.tinkoff.edu.all.to.scala.stack.check.task2;

/**
 * Assuming implementations is not thread safe
 */
public interface Client {
    Event readData();

    Result sendData(Address dest, Payload payload);
}