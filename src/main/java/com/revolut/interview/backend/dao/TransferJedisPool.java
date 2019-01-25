package com.revolut.interview.backend.dao;

import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;

public class TransferJedisPool extends JedisPool {

  @Inject
  public TransferJedisPool(GenericObjectPoolConfig poolConfig, @RedisHost String host,
      @RedisPort int port) {
    super(poolConfig, host, port);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.PARAMETER})
  @BindingAnnotation
  public @interface RedisHost {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.PARAMETER})
  @BindingAnnotation
  public @interface RedisPort {

  }
}
