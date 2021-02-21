package com.iwayee.activity.func;

@FunctionalInterface
public interface Action2<T, E> {
  void run(T t, E e);
}
