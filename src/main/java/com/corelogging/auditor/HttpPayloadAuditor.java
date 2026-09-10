package com.corelogging.auditor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HttpPayloadAuditor {

  void auditAndProceed(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws Exception;
}
