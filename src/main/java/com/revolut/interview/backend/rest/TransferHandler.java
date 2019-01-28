package com.revolut.interview.backend.rest;

import com.google.inject.Inject;
import com.revolut.interview.backend.dao.AccountNotFoundException;
import com.revolut.interview.backend.service.NotEnoughMoneyException;
import com.revolut.interview.backend.service.TransferService;
import io.javalin.Context;
import io.javalin.Handler;
import java.math.BigDecimal;
import java.util.Objects;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

public class TransferHandler implements Handler {

  static final String PARAM_SUM = "sum";
  /**
   * An example: /transfer/:sum?from=1&to=2
   * NOTE: in case of URL length restrictions, we can pass from & to params in the request body
   */
  public static final String PATH = "/transfer/:" + PARAM_SUM;
  public static final String PARAM_FROM = "from";
  public static final String PARAM_TO = "to";
  static final String ERR_MSG = "Expected format: /transfer/<BigDecimal>?from=<AccountLongId>&to=<AccountLongId>";

  private final TransferService transferService;

  @Inject
  public TransferHandler(TransferService transferService) {
    this.transferService = transferService;
  }

  @Override
  public void handle(@NotNull Context ctx)
      throws AccountNotFoundException, NotEnoughMoneyException {
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
    } catch (NullPointerException | IllegalArgumentException e) {
      throw new IllegalArgumentException(ERR_MSG);
    }

    transferService.transferMoney(sum, fromAccountId, toAccountId);

    ctx.status(HttpStatus.NO_CONTENT_204);
  }
}
