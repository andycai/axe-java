package com.iwayee.activity.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Singleton<T> {
  private static final ConcurrentMap<Class, Object> map = new ConcurrentHashMap<>();

//  private static Map<Class<? extends BaseSystem>, BaseSystem> INSTANCE_MAP = new HashMap<>();
//    public synchronized static <E extends BaseSystem> BaseSystem getInstance(Class<E> instanceClass) throws Exception {
//    if (INSTANCE_MAP.containsKey(instanceClass)) {
//      return INSTANCE_MAP.get(instanceClass);
//    } else {
//      E instance = instanceClass.getDeclaredConstructor().newInstance();
//      INSTANCE_MAP.put(instanceClass, instance);
//      return instance;
//    }
//  }

  public static<T>T instance(Class<T> type) {
    Object obj = map.get(type);
    try {
      if (obj == null) {
        synchronized (map) {
          obj = type.getDeclaredConstructor().newInstance();
          map.put(type, obj);
        }
      }
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }

    return (T) obj;
  }
}
