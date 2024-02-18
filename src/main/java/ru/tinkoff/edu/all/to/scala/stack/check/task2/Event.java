package ru.tinkoff.edu.all.to.scala.stack.check.task2;

import java.util.List;

public record Event(List<Address> recipients, Payload payload) {}