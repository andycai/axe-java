package com.iwayee.activity.func;

@FunctionalInterface
public interface Action<T> {
  void run(T t);
}
