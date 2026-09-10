package com.corelogging.trace;

@FunctionalInterface
public interface TraceAction {
  void execute(TraceContext context) throws Exception;
}
