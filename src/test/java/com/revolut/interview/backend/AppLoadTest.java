package com.revolut.interview.backend;

import org.jsmart.zerocode.core.domain.JsonTestCase;
import org.jsmart.zerocode.core.domain.TargetEnv;
import org.jsmart.zerocode.core.runner.ZeroCodeUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@TargetEnv("host.properties")
@RunWith(ZeroCodeUnitRunner.class)
public class AppLoadTest {

  @Test
  @JsonTestCase("transfer.json")
  public void transfer() {
  }
}
