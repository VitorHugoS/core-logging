#!/bin/bash
cat << 'INNER_EOF' > src/test/java/com/corelogging/filter/CoreLoggingSecurityFilterTest.java
package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class CoreLoggingSecurityFilterTest {

  private CoreLoggingSecurityFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new CoreLoggingSecurityFilter();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  void shouldAddUserIdToMdcIfAuthenticated() throws Exception {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("user-123");
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    org.mockito.Mockito.doAnswer(
            invocation -> {
              assertThat(MDC.get("user.id")).isEqualTo("user-123");
              return null;
            })
        .when(filterChain)
        .doFilter(request, response);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(MDC.get("user.id")).isNull();
  }

  @Test
  void shouldNotAddUserIdToMdcIfNotAuthenticated() throws Exception {
    org.mockito.Mockito.doAnswer(
            invocation -> {
              assertThat(MDC.get("user.id")).isNull();
              return null;
            })
        .when(filterChain)
        .doFilter(request, response);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldReturnCorrectOrder() {
    assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.LOWEST_PRECEDENCE - 5);
  }
}
INNER_EOF
