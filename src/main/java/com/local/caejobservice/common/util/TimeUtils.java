package com.local.caejobservice.common.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class TimeUtils {

  private TimeUtils() {}

  public static String localNowIso() {
    return ZonedDateTime.now().withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

  public static String newJobId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
