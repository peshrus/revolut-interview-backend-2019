package com.revolut.interview.backend.rest;

import com.google.inject.Inject;
import com.revolut.interview.backend.service.TransferService;
import io.javalin.Context;
import io.javalin.Handler;
import java.math.BigDecimal;
import java.util.Objects;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

public class TransferHandler implements Handler {

  private static final String PARAM_SUM = "sum";
  private static final String PARAM_FROM = "from";
  private static final String PARAM_TO = "to";

  /**
   * An example: /transfer/:sum?from=1&to=2
   */
  public static String PATH = "/transfer/:" + PARAM_SUM;

  private final TransferService transferService;

  @Inject
  public TransferHandler(TransferService transferService) {
    this.transferService = transferService;
  }

  @Override
  public void handle(@NotNull Context ctx) throws Exception {
    final String sumStr = ctx.pathParam(PARAM_SUM);
    final String fromStr = ctx.queryParam(PARAM_FROM);
    final String toStr = ctx.queryParam(PARAM_TO);
    final BigDecimal sum;
    final long fromAccountId;
    final long toAccountId;

    try {
      sum = new BigDecimal(sumStr);
      fromAccountId = Long.parseLong(Objects.requireNonNull(fromStr));
      toAccountId = Long.parseLong(Objects.requireNonNull(toStr));
    } catch (NullPointerException | NumberFormatException e) {
      throw new IllegalArgumentException(
          "Expected format: /transfer/<BigDecimal>?from=<AccountLongId>&to=<AccountLongId>");
    }

    transferService.transferMoney(sum, fromAccountId, toAccountId);

    ctx.status(HttpStatus.NO_CONTENT_204);
  }
}
