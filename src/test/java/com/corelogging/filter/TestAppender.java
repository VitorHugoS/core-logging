package com.corelogging.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;

public class TestAppender extends AppenderBase<ILoggingEvent> {
  public static final List<ILoggingEvent> events = new ArrayList<>();

  @Override
  protected void append(ILoggingEvent eventObject) {
    eventObject.prepareForDeferredProcessing();
    events.add(eventObject);
  }

  public static void clear() {
    events.clear();
  }
}
