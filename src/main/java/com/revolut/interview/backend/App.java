package com.revolut.interview.backend;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.revolut.interview.backend.dao.AccountNotFoundException;
import com.revolut.interview.backend.rest.TransferHandler;
import com.revolut.interview.backend.service.NotEnoughMoneyException;
import io.javalin.ExceptionHandler;
import io.javalin.Javalin;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Protocol;

public class App {

  private static final Logger LOG = LoggerFactory.getLogger(App.class);
  private static final int DEFAULT_REST_PORT = 7000;

  private Javalin app;

  public static void main(String[] args) {
    final boolean mandatoryArgsSpecified = args != null && args.length > 2;

    if (!mandatoryArgsSpecified) {
      throw new IllegalArgumentException();
    }

    final int redisPort = parsePort(args[1], Protocol.DEFAULT_PORT);
    final int port = parsePort(args[2], DEFAULT_REST_PORT);

    new App().start(args[0], redisPort, port);
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

  void start() {
    start(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT, DEFAULT_REST_PORT);
  }

  private void start(String redisHost, int redisPort, int restPort) {
    final Injector injector = Guice.createInjector(new TransferModule(redisHost, redisPort));
    final TransferHandler transferHandler = injector.getInstance(TransferHandler.class);
    final ExceptionHandler<Exception> exceptionHandler = getExceptionExceptionHandler();

    app = Javalin.create().start(restPort);
    app.post(TransferHandler.PATH, transferHandler);
    app.exception(IllegalArgumentException.class, exceptionHandler);
    app.exception(NotEnoughMoneyException.class, exceptionHandler);
    app.exception(AccountNotFoundException.class, exceptionHandler);
  }

  private ExceptionHandler<Exception> getExceptionExceptionHandler() {
    return (e, ctx) -> {
      LOG.error("Error", e);
      ctx.status(HttpStatus.BAD_REQUEST_400);
      ctx.result(e.getMessage());
    };
  }

  void stop() {
    app.stop();
  }
}
