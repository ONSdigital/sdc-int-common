package uk.gov.ons.ctp.common.error;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

/** Test of the Controller Advice for Spring MVC exception handling */
@ExtendWith(MockitoExtension.class)
public class RestExceptionHandlerTest {

  @RestController
  @RequestMapping(value = "/test")
  private interface TestController {

    @RequestMapping(value = "/run", method = RequestMethod.GET)
    ResponseEntity<String> runTest() throws CTPException;
  }

  private MockMvc mockMvc;

  @Mock private TestController testController;

  @BeforeEach
  public void setup() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(testController)
            .setControllerAdvice(new RestExceptionHandler())
            .build();
  }

  private static Stream<Arguments> dataForCtpException() {
    return Stream.of(
        arguments(Fault.RESOURCE_NOT_FOUND, status().isNotFound()),
        arguments(Fault.RESOURCE_VERSION_CONFLICT, status().isConflict()),
        arguments(Fault.ACCESS_DENIED, status().isUnauthorized()),
        arguments(Fault.BAD_REQUEST, status().isBadRequest()),
        arguments(Fault.VALIDATION_FAILED, status().isBadRequest()),
        arguments(Fault.TOO_MANY_REQUESTS, status().isTooManyRequests()),
        arguments(Fault.SYSTEM_ERROR, status().isInternalServerError()),
        arguments(Fault.ACCEPTED_UNABLE_TO_PROCESS, status().isAccepted()));
  }

  @ParameterizedTest
  @MethodSource("dataForCtpException")
  public void shouldHandleException(Fault fault, ResultMatcher expected) throws Exception {
    Mockito.doThrow(new CTPException(fault)).when(testController).runTest();

    MockHttpServletResponse response =
        mockMvc.perform(get("/test/run")).andExpect(expected).andReturn().getResponse();
    assertTrue(response.getContentAsString().contains(fault.toString()));
  }

  private static Stream<Arguments> dataForResponseStatusException() {
    return Stream.of(
        arguments(HttpStatus.NOT_FOUND, status().isNotFound(), Fault.RESOURCE_NOT_FOUND),
        arguments(HttpStatus.CONFLICT, status().isConflict(), Fault.RESOURCE_VERSION_CONFLICT),
        arguments(HttpStatus.UNAUTHORIZED, status().isUnauthorized(), Fault.ACCESS_DENIED),
        arguments(HttpStatus.BAD_REQUEST, status().isBadRequest(), Fault.BAD_REQUEST),
        arguments(
            HttpStatus.TOO_MANY_REQUESTS, status().isTooManyRequests(), Fault.TOO_MANY_REQUESTS),
        arguments(
            HttpStatus.INTERNAL_SERVER_ERROR, status().isInternalServerError(), Fault.SYSTEM_ERROR),
        arguments(HttpStatus.ACCEPTED, status().isAccepted(), Fault.ACCEPTED_UNABLE_TO_PROCESS),
        arguments(HttpStatus.I_AM_A_TEAPOT, status().isIAmATeapot(), Fault.SYSTEM_ERROR));
  }

  @ParameterizedTest
  @MethodSource("dataForResponseStatusException")
  public void shouldHandleResponseStatusException(
      HttpStatus status, ResultMatcher expected, Fault expectedFault) throws Exception {
    Mockito.doThrow(new ResponseStatusException(status)).when(testController).runTest();

    MockHttpServletResponse response =
        mockMvc.perform(get("/test/run")).andExpect(expected).andReturn().getResponse();
    assertTrue(response.getContentAsString().contains(expectedFault.toString()));
  }
}
