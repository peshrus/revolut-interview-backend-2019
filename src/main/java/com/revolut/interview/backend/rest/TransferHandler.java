package com.revolut.interview.backend.rest;

import com.google.inject.Inject;
import com.revolut.interview.backend.dao.AccountDao;
import com.revolut.interview.backend.dao.AccountNotFoundException;
import com.revolut.interview.backend.dao.FromAndToAccountsTheSameException;
import com.revolut.interview.backend.dao.NotEnoughMoneyException;
import io.javalin.Context;
import io.javalin.Handler;
import java.math.BigDecimal;
import java.util.Objects;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

public class TransferHandler implements Handler {

  public static final String PARAM_FROM = "from";
  public static final String PARAM_TO = "to";
  static final String PARAM_SUM = "sum";
  /**
   * An example: /transfer/:sum?from=1&to=2
   * NOTE: in case of URL length restrictions, we can pass from & to params in the request body
   */
  public static final String PATH = "/transfer/:" + PARAM_SUM;
  static final String ERR_MSG = "Expected format: /transfer/<BigDecimal>?from=<AccountLongId>&to=<AccountLongId>";

  private final AccountDao accountDao;

  @Inject
  public TransferHandler(AccountDao accountDao) {
    this.accountDao = accountDao;
  }

  @Override
  public void handle(@NotNull Context ctx)
      throws AccountNotFoundException, NotEnoughMoneyException, FromAndToAccountsTheSameException {
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

    accountDao.transferMoneyTransactionally(sum, fromAccountId, toAccountId);

    ctx.status(HttpStatus.NO_CONTENT_204);
  }
}
