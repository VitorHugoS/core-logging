package com.corelogging;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.filter.TestAppender;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class CoreLoggerTest {

  private static final Logger log = LoggerFactory.getLogger(CoreLoggerTest.class);
  private CoreLogger coreLogger;

  @BeforeEach
  void setUp() {
    MDC.clear();
    TestAppender.clear();
    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
    coreLogger = new CoreLogger(log, mapper);
  }

  @Test
  void shouldLogInfoWithCustomFieldsAndRestoreMdc() {
    MDC.put("log_type", "in_request");

    coreLogger
        .info("Usuario {} criado", "joao")
        .with("business.user_id", 123)
        .with("business.status", "ACTIVE")
        .with("business.ignore_null", (String) null)
        .with("business.ignore_null_num", (Number) null)
        .with(null, "value")
        .with(null, 123)
        .log();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getFormattedMessage()).isEqualTo("Usuario joao criado");
    assertThat(event.getLevel().toString()).isEqualTo("INFO");
    assertThat(event.getMDCPropertyMap().get("log_type")).isEqualTo("application");
    assertThat(event.getMDCPropertyMap().get("business.user_id")).isEqualTo("123");
    assertThat(event.getMDCPropertyMap().get("business.status")).isEqualTo("ACTIVE");
    assertThat(event.getMDCPropertyMap().containsKey("business.ignore_null")).isFalse();

    assertThat(MDC.get("log_type")).isEqualTo("in_request");
    assertThat(MDC.get("business.user_id")).isNull();
  }

  @Test
  void shouldLogErrorWithException() {
    RuntimeException exception = new RuntimeException("DB Error");

    coreLogger.error("Falha ao salvar", exception).with("db.table", "users").log();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMessage()).isEqualTo("Falha ao salvar");
    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
    assertThat(event.getMDCPropertyMap().get("log_type")).isEqualTo("application");
    assertThat(event.getMDCPropertyMap().get("db.table")).isEqualTo("users");

    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("DB Error");

    assertThat(MDC.get("log_type")).isNull();
    assertThat(MDC.get("db.table")).isNull();
  }

  @Test
  void shouldLogWarnAndDebugWithoutArgs() {
    coreLogger.warn("Aviso simples").log();
    coreLogger.debug("Debug simples").log();

    assertThat(TestAppender.events).hasSize(2);

    ch.qos.logback.classic.spi.ILoggingEvent warnEvent = TestAppender.events.get(0);
    assertThat(warnEvent.getMessage()).isEqualTo("Aviso simples");
    assertThat(warnEvent.getLevel().toString()).isEqualTo("WARN");

    ch.qos.logback.classic.spi.ILoggingEvent debugEvent = TestAppender.events.get(1);
    assertThat(debugEvent.getMessage()).isEqualTo("Debug simples");
    assertThat(debugEvent.getLevel().toString()).isEqualTo("DEBUG");
  }

  @Test
  void shouldNotLogIfLevelIsDisabled() {
    Logger mockLogger = org.mockito.Mockito.mock(Logger.class);
    org.mockito.Mockito.when(mockLogger.isInfoEnabled()).thenReturn(false);
    org.mockito.Mockito.when(mockLogger.isErrorEnabled()).thenReturn(false);
    org.mockito.Mockito.when(mockLogger.isWarnEnabled()).thenReturn(false);
    org.mockito.Mockito.when(mockLogger.isDebugEnabled()).thenReturn(false);

    CoreLogger mockCoreLogger =
        new CoreLogger(mockLogger, new tools.jackson.databind.ObjectMapper());

    mockCoreLogger.info("test").log();
    mockCoreLogger.error("test").log();
    mockCoreLogger.warn("test").log();
    mockCoreLogger.debug("test").log();

    org.mockito.Mockito.verify(mockLogger, org.mockito.Mockito.never())
        .info(org.mockito.ArgumentMatchers.anyString());
    org.mockito.Mockito.verify(mockLogger, org.mockito.Mockito.never())
        .error(org.mockito.ArgumentMatchers.anyString());
    org.mockito.Mockito.verify(mockLogger, org.mockito.Mockito.never())
        .warn(org.mockito.ArgumentMatchers.anyString());
    org.mockito.Mockito.verify(mockLogger, org.mockito.Mockito.never())
        .debug(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void shouldLogAllLevelsWithArgs() {
    coreLogger.info("Info {}", "arg").log();
    coreLogger.error("Erro {}", "arg").log();
    coreLogger.warn("Aviso {}", "arg").log();
    coreLogger.debug("Debug {}", "arg").log();

    assertThat(TestAppender.events).hasSize(4);
    assertThat(TestAppender.events.get(0).getFormattedMessage()).isEqualTo("Info arg");
    assertThat(TestAppender.events.get(1).getFormattedMessage()).isEqualTo("Erro arg");
    assertThat(TestAppender.events.get(2).getFormattedMessage()).isEqualTo("Aviso arg");
    assertThat(TestAppender.events.get(3).getFormattedMessage()).isEqualTo("Debug arg");
  }

  @Test
  void shouldLogAllLevelsWithoutArgs() {
    TestAppender.clear();
    coreLogger.info("Info sem args").log();
    coreLogger.error("Erro sem args").log();
    coreLogger.warn("Aviso sem args").log();
    coreLogger.debug("Debug sem args").log();

    assertThat(TestAppender.events).hasSize(4);
    assertThat(TestAppender.events.get(0).getFormattedMessage()).isEqualTo("Info sem args");
    assertThat(TestAppender.events.get(1).getFormattedMessage()).isEqualTo("Erro sem args");
    assertThat(TestAppender.events.get(2).getFormattedMessage()).isEqualTo("Aviso sem args");
    assertThat(TestAppender.events.get(3).getFormattedMessage()).isEqualTo("Debug sem args");
  }

  @Test
  void shouldLogWithNullArgs() {
    TestAppender.clear();
    coreLogger.info("Null args", (Object[]) null).log();

    assertThat(TestAppender.events).hasSize(1);
    assertThat(TestAppender.events.get(0).getFormattedMessage()).isEqualTo("Null args");
  }

  @Test
  void shouldLogWithComplexObjectAndFlattening() {
    TestAppender.clear();

    // An anonymous object for flattening
    Object payload =
        new Object() {
          public String getName() {
            return "Vitor";
          }

          public int getAge() {
            return 30;
          }

          public Object getNested() {
            return new Object() {
              public String getCity() {
                return "SP";
              }
            };
          }
        };

    coreLogger
        .info("Test Object")
        .with("business.user", payload) // As JSON string
        .with(payload) // As flattened keys
        .log();

    assertThat(TestAppender.events).hasSize(1);
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    Map<String, String> mdc = event.getMDCPropertyMap();

    assertThat(mdc.get("name")).isEqualTo("Vitor");
    assertThat(mdc.get("age")).isEqualTo("30");
    assertThat(mdc.get("nested")).isEqualTo("{\"city\":\"SP\"}");
    assertThat(mdc.get("business.user"))
        .isEqualTo("{\"age\":30,\"name\":\"Vitor\",\"nested\":{\"city\":\"SP\"}}");
  }

  static class FailBean {
    public String getFail() {
      throw new RuntimeException("fail");
    }
  }

  @Test
  void shouldFallbackToStringWhenJacksonFails() {
    TestAppender.clear();
    FailBean failBean = new FailBean();

    coreLogger
        .info("Test Fallback")
        .with("business.fail", failBean)
        .with(failBean) // convertValue will fail
        .log();

    assertThat(TestAppender.events).hasSize(1);
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    Map<String, String> mdc = event.getMDCPropertyMap();

    assertThat(mdc.get("business.fail")).contains("FailBean");
  }

  @Test
  void enumCoverage() {
    CoreLogger.LogLevel[] values = CoreLogger.LogLevel.values();
    assertThat(values).contains(CoreLogger.LogLevel.INFO);
    assertThat(CoreLogger.LogLevel.valueOf("INFO")).isEqualTo(CoreLogger.LogLevel.INFO);
  }

  @Test
  void shouldSanitizeStringValuesInWithMethod() {
    CoreLogger logger =
        new CoreLogger(
            log, new tools.jackson.databind.ObjectMapper(), java.util.List.of("password"));
    logger.info("msg").with("password", "secret123").with("user", "john").log();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("password")).isEqualTo("***");
    assertThat(event.getMDCPropertyMap().get("user")).isEqualTo("john");
  }

  @Test
  void shouldSanitizeObjectValuesInWithMethod() {
    CoreLogger logger =
        new CoreLogger(log, new tools.jackson.databind.ObjectMapper(), java.util.List.of("token"));
    logger.info("msg").with("token", 12345).with("age", 30).log();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("token")).isEqualTo("***");
    assertThat(event.getMDCPropertyMap().get("age")).isEqualTo("30");
  }

  @Test
  void shouldSanitizeObjectPayloadInWithMethod() {
    CoreLogger logger =
        new CoreLogger(
            log, new tools.jackson.databind.ObjectMapper(), java.util.List.of("password"));

    java.util.Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("password", "secret");
    payload.put("user", "john");

    logger.info("msg").with(payload).log();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("password")).isEqualTo("***");
    assertThat(event.getMDCPropertyMap().get("user")).isEqualTo("john");
  }
}
