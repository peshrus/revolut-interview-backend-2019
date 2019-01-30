package com.revolut.interview.backend;

import com.google.inject.AbstractModule;
import com.revolut.interview.backend.dao.AccountDao;
import com.revolut.interview.backend.dao.AccountDaoImpl;
import com.revolut.interview.backend.dao.TransferJedisPool;
import com.revolut.interview.backend.dao.TransferJedisPool.RedisHost;
import com.revolut.interview.backend.dao.TransferJedisPool.RedisPort;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.JedisPoolConfig;

public class TransferModule extends AbstractModule {

  private final String redisHost;
  private final int redisPort;

  TransferModule(String redisHost, int redisPort) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
  }

  @Override
  protected void configure() {
    bindConstant().annotatedWith(RedisHost.class).to(redisHost);
    bindConstant().annotatedWith(RedisPort.class).to(redisPort);

    bind(GenericObjectPoolConfig.class).to(JedisPoolConfig.class);
    bind(JedisPoolAbstract.class).to(TransferJedisPool.class);
    bind(AccountDao.class).to(AccountDaoImpl.class);
  }

}
