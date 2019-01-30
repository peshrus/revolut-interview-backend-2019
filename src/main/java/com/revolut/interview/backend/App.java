package com.revolut.interview.backend;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.revolut.interview.backend.dao.AccountNotFoundException;
import com.revolut.interview.backend.dao.FromAndToAccountsTheSameException;
import com.revolut.interview.backend.dao.NotEnoughMoneyException;
import com.revolut.interview.backend.rest.TransferHandler;
import io.javalin.ExceptionHandler;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.Protocol;
import redis.embedded.RedisServer;

public class App {

  static final int DEFAULT_REST_PORT = 7000;
  private static final Logger LOG = LoggerFactory.getLogger(App.class);
  private Injector injector;
  private RedisServer redisServer;
  private Javalin restApp;

  public static void main(String[] args) throws IOException {
    final boolean mandatoryArgsSpecified = args != null && args.length > 2;
    String redisHost = Protocol.DEFAULT_HOST;
    int redisPort = Protocol.DEFAULT_PORT;
    int restPort = DEFAULT_REST_PORT;

    if (mandatoryArgsSpecified) {
      redisHost = getRedisHost(args[0]);
      redisPort = parsePort(args[1], Protocol.DEFAULT_PORT);
      restPort = parsePort(args[2], DEFAULT_REST_PORT);
    }

    new App().start(redisHost, redisPort, restPort);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static String getRedisHost(String hostStr) {
    String result = hostStr;

    try {
      InetAddress.getAllByName(hostStr);
    } catch (UnknownHostException e) {
      LOG.warn("Wrong host: " + hostStr + ". Use default: " + Protocol.DEFAULT_HOST);
      result = Protocol.DEFAULT_HOST;
    }

    return result;
  }

  private static int parsePort(String portStr, int defaultValue) {
    int result;

    try {
      result = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      LOG.warn("Wrong port: " + portStr + ". Use default: " + defaultValue);
      result = defaultValue;
    }

    return result;
  }

  void start() throws IOException {
    start(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT, DEFAULT_REST_PORT);
  }

  private void start(String redisHost, int redisPort, int restPort) throws IOException {
    redisServer = new RedisServer(redisPort);

    // Use external Redis otherwise
    if (Protocol.DEFAULT_HOST.equals(redisHost)) {
      redisServer.start();
    }

    injector = Guice.createInjector(new TransferModule(redisHost, redisPort));
    final TransferHandler transferHandler = injector.getInstance(TransferHandler.class);
    final ExceptionHandler<Exception> exceptionHandler = getExceptionExceptionHandler();

    restApp = Javalin.create().start(restPort);
    restApp.get("/", ctx -> ctx.result("Revolut Backend Test"));
    restApp.post(TransferHandler.PATH, transferHandler);
    restApp.exception(IllegalArgumentException.class, exceptionHandler);
    restApp.exception(NotEnoughMoneyException.class, exceptionHandler);
    restApp.exception(AccountNotFoundException.class, exceptionHandler);
    restApp.exception(FromAndToAccountsTheSameException.class, exceptionHandler);
  }

  private ExceptionHandler<Exception> getExceptionExceptionHandler() {
    return (e, ctx) -> {
      LOG.error("Error", e);
      ctx.status(HttpStatus.BAD_REQUEST_400);
      ctx.result(e.toString());
    };
  }

  void stop() {
    restApp.stop();
    injector.getInstance(JedisPoolAbstract.class).destroy();
    redisServer.stop();
  }
}
