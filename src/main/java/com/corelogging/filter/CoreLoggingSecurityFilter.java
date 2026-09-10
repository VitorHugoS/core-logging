package com.corelogging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class CoreLoggingSecurityFilter extends OncePerRequestFilter implements Ordered {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getName() != null) {
      MDC.put("user.id", authentication.getName());
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("user.id");
    }
  }

  @Override
  public int getOrder() {
    // Should run after Spring Security Filter Chain
    return Ordered.LOWEST_PRECEDENCE - 5;
  }
}
