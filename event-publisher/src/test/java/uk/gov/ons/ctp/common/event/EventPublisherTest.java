package uk.gov.ons.ctp.common.event;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ctp.common.event.EventPublisherTestUtil.assertHeader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentEvent;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.common.event.model.RefusalEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateEvent;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.persistence.EventBackupData;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

@ExtendWith(MockitoExtension.class)
public class EventPublisherTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private EventSender sender;
  @Mock private FirestoreEventPersistence eventPersistence;

  ObjectMapper objectMapper = new CustomObjectMapper();

  @Captor private ArgumentCaptor<FulfilmentEvent> fulfilmentRequestedEventCaptor;
  @Captor private ArgumentCaptor<UacAuthenticateEvent> respondentAuthenticatedEventCaptor;
  @Captor private ArgumentCaptor<RefusalEvent> respondentRefusalEventCaptor;
  @Captor private ArgumentCaptor<UacEvent> uacEventCaptor;
  @Captor private ArgumentCaptor<SurveyLaunchEvent> surveyLaunchedEventCaptor;
  @Captor private ArgumentCaptor<CaseEvent> caseEventCaptor;
  @Captor private ArgumentCaptor<SurveyUpdateEvent> surveyUpdateArgumentCaptor;
  @Captor private ArgumentCaptor<CollectionExerciseUpdateEvent> collectionExerciseArgumentCaptor;

  private Date startOfTestDateTime;

  @BeforeEach
  public void setup() {
    this.startOfTestDateTime = new Date();
  }

  @Test
  public void shouldCreateWithoutEventPersistence() {
    EventPublisher ep = EventPublisher.createWithoutEventPersistence(sender);
    assertNull(ReflectionTestUtils.getField(ep, "eventPersistence"));
    assertEquals(sender, ReflectionTestUtils.getField(ep, "sender"));
  }

  @Test
  public void shouldCreateWithEventPersistence() {
    EventPublisher ep = EventPublisher.createWithEventPersistence(sender, eventPersistence, null);
    assertNotNull(ReflectionTestUtils.getField(ep, "eventPersistence"));
    assertEquals(sender, ReflectionTestUtils.getField(ep, "sender"));
  }

  @Test
  public void sendEventSurveyLaunchedPayload() {
    SurveyLaunchResponse surveyLaunchedResponse = loadJson(SurveyLaunchResponse[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse);

    EventTopic eventTopic = EventTopic.forType(EventType.SURVEY_LAUNCH);
    verify(sender, times(1)).sendEvent(eq(eventTopic), surveyLaunchedEventCaptor.capture());
    SurveyLaunchEvent event = surveyLaunchedEventCaptor.getValue();
    assertHeader(event, transactionId, EventType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(surveyLaunchedResponse, event.getPayload().getResponse());
  }

  @Test
  public void sendEventRespondentAuthenticatedPayload() {
    UacAuthenticateResponse respondentAuthenticatedResponse =
        loadJson(UacAuthenticateResponse[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.UAC_AUTHENTICATE,
            Source.RESPONDENT_HOME,
            Channel.RH,
            respondentAuthenticatedResponse);

    EventTopic eventTopic = EventTopic.forType(EventType.UAC_AUTHENTICATE);
    verify(sender, times(1))
        .sendEvent(eq(eventTopic), respondentAuthenticatedEventCaptor.capture());
    UacAuthenticateEvent event = respondentAuthenticatedEventCaptor.getValue();

    assertHeader(
        event, transactionId, EventType.UAC_AUTHENTICATE, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(respondentAuthenticatedResponse, event.getPayload().getResponse());
  }

  @Test
  public void sendEventFulfilmentRequestPayload() {
    FulfilmentRequest fulfilmentRequest = loadJson(FulfilmentRequest[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.FULFILMENT, Source.CONTACT_CENTRE_API, Channel.CC, fulfilmentRequest);

    EventTopic eventTopic = EventTopic.forType(EventType.FULFILMENT);
    verify(sender, times(1)).sendEvent(eq(eventTopic), fulfilmentRequestedEventCaptor.capture());
    FulfilmentEvent event = fulfilmentRequestedEventCaptor.getValue();

    assertHeader(event, transactionId, EventType.FULFILMENT, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals("id-123", event.getPayload().getFulfilmentRequest().getCaseId());
  }

  @Test
  public void sendEventRespondentRefusalDetailsPayload() {
    RefusalDetails respondentRefusalDetails = loadJson(RefusalDetails[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.REFUSAL, Source.CONTACT_CENTRE_API, Channel.CC, respondentRefusalDetails);

    EventTopic eventTopic = EventTopic.forType(EventType.REFUSAL);
    verify(sender, times(1)).sendEvent(eq(eventTopic), respondentRefusalEventCaptor.capture());
    RefusalEvent event = respondentRefusalEventCaptor.getValue();

    assertHeader(event, transactionId, EventType.REFUSAL, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(respondentRefusalDetails, event.getPayload().getRefusal());
  }

  /** Test build of Respondent Authenticated event message with wrong pay load */
  @Test
  public void sendEventRespondentAuthenticatedWrongPayload() {

    boolean exceptionThrown = false;

    try {
      eventPublisher.sendEvent(
          EventType.UAC_AUTHENTICATE,
          Source.RECEIPT_SERVICE,
          Channel.CC,
          Mockito.mock(EventPayload.class));
    } catch (Exception e) {
      exceptionThrown = true;
      assertThat(e.getMessage(), containsString("incompatible for event type"));
    }

    assertTrue(exceptionThrown);
  }

  private void assertSendCase(EventType type) {
    CollectionCase payload = loadJson(CollectionCase[].class);

    String transactionId =
        eventPublisher.sendEvent(type, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    EventTopic eventTopic = EventTopic.forType(type);
    verify(sender).sendEvent(eq(eventTopic), caseEventCaptor.capture());
    CaseEvent event = caseEventCaptor.getValue();

    assertHeader(event, transactionId, type, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getCollectionCase());
  }

  @Test
  public void shouldSendCaseUpdated() {
    assertSendCase(EventType.CASE_UPDATE);
  }

  private void assertSendUac(EventType type) {
    UAC payload = loadJson(UAC[].class);

    String transactionId =
        eventPublisher.sendEvent(type, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    EventTopic eventTopic = EventTopic.forType(type);
    verify(sender).sendEvent(eq(eventTopic), uacEventCaptor.capture());
    UacEvent event = uacEventCaptor.getValue();

    assertHeader(event, transactionId, type, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getUac());
  }

  @Test
  public void shouldSendUacUpdated() {
    assertSendUac(EventType.UAC_UPDATE);
  }

  private void assertSendSurveyUpdate(EventType type) {
    SurveyUpdate payload = loadJson(SurveyUpdate[].class);

    String transactionId =
        eventPublisher.sendEvent(type, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    EventTopic eventTopic = EventTopic.forType(type);
    verify(sender).sendEvent(eq(eventTopic), surveyUpdateArgumentCaptor.capture());
    SurveyUpdateEvent event = surveyUpdateArgumentCaptor.getValue();

    assertHeader(event, transactionId, type, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getSurveyUpdate());
  }

  @Test
  public void shouldSendSurveyUpdate() {
    assertSendSurveyUpdate(EventType.SURVEY_UPDATE);
  }

  private void assertSendCollectionExercise(EventType type) {
    CollectionExercise payload = loadJson(CollectionExercise[].class);

    String transactionId =
        eventPublisher.sendEvent(type, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    EventTopic eventTopic = EventTopic.forType(type);
    verify(sender).sendEvent(eq(eventTopic), collectionExerciseArgumentCaptor.capture());
    CollectionExerciseUpdateEvent event = collectionExerciseArgumentCaptor.getValue();

    assertHeader(event, transactionId, type, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getCollectionExerciseUpdate());
  }

  @Test
  public void shouldSendCollectionExerciseUpdate() {
    assertSendCollectionExercise(EventType.COLLECTION_EXERCISE_UPDATE);
  }

  @Test
  public void shouldRejectSendForMismatchingPayload() {
    SurveyLaunchResponse surveyLaunchedResponse = loadJson(SurveyLaunchResponse[].class);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            eventPublisher.sendEvent(
                EventType.CASE_UPDATE, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse));
  }

  // -- replay send backup event tests ...

  @Test
  public void shouldSendBackupFulfilmentEvent() throws Exception {
    FulfilmentEvent ev = aFulfilmentRequestedEvent();
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(EventType.FULFILMENT);
    verify(sender).sendEvent(eq(eventTopic), fulfilmentRequestedEventCaptor.capture());
    verifyEventSent(ev, fulfilmentRequestedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupRepondentAuthenticatedEvent() throws Exception {
    UacAuthenticateEvent ev = aRespondentAuthenticatedEvent();
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(EventType.UAC_AUTHENTICATE);
    verify(sender).sendEvent(eq(eventTopic), respondentAuthenticatedEventCaptor.capture());
    verifyEventSent(ev, respondentAuthenticatedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupRefusalReceivedEvent() throws Exception {
    RefusalEvent ev = aRefusalEvent();
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(EventType.REFUSAL);
    verify(sender).sendEvent(eq(eventTopic), respondentRefusalEventCaptor.capture());
    verifyEventSent(ev, respondentRefusalEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupUacUpdatedEvent() throws Exception {
    UacEvent ev = aUacEvent();
    ev.getEvent().setType(EventType.UAC_UPDATE);
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(EventType.UAC_UPDATE);
    verify(sender).sendEvent(eq(eventTopic), uacEventCaptor.capture());
    verifyEventSent(ev, uacEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupSurveyLaunchedEvent() throws Exception {
    SurveyLaunchEvent ev = aSurveyLaunchedEvent();
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(EventType.SURVEY_LAUNCH);
    verify(sender).sendEvent(eq(eventTopic), surveyLaunchedEventCaptor.capture());
    verifyEventSent(ev, surveyLaunchedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupCaseUpdatedEvent() throws Exception {
    CaseEvent ev = aCaseEvent();
    ev.getEvent().setType(EventType.CASE_UPDATE);
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(EventType.CASE_UPDATE);
    verify(sender).sendEvent(eq(eventTopic), caseEventCaptor.capture());
    verifyEventSent(ev, caseEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupSurveyUpdateEvent() throws Exception {
    SurveyUpdateEvent ev = aSurveyUpdateEvent();
    ev.getEvent().setType(EventType.SURVEY_UPDATE);
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(EventType.SURVEY_UPDATE);
    verify(sender).sendEvent(eq(eventTopic), surveyUpdateArgumentCaptor.capture());
    verifyEventSent(ev, surveyUpdateArgumentCaptor.getValue());
  }

  @Test
  public void shouldSendBackupCollectionExerciseUpdateEvent() throws Exception {
    CollectionExerciseUpdateEvent ev = aCollectionExerciseEvent();
    ev.getEvent().setType(EventType.COLLECTION_EXERCISE_UPDATE);
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(EventType.COLLECTION_EXERCISE_UPDATE);
    verify(sender).sendEvent(eq(eventTopic), collectionExerciseArgumentCaptor.capture());
    verifyEventSent(ev, collectionExerciseArgumentCaptor.getValue());
  }

  @Test
  public void shouldRejectMalformedEventBackupJson() {
    SurveyLaunchEvent ev = aSurveyLaunchedEvent();
    EventBackupData data = createEvent(ev);
    data.setEvent("xx" + data.getEvent()); // create broken Json
    assertThrows(EventPublishException.class, () -> eventPublisher.sendEvent(data));
  }

  @Test
  public void shouldSendEventWithJsonPayload() {
    String payload = FixtureHelper.loadPackageObjectNode("SurveyLaunchResponse").toString();

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, payload);

    EventTopic eventTopic = EventTopic.forType(EventType.SURVEY_LAUNCH);
    verify(sender, times(1)).sendEvent(eq(eventTopic), surveyLaunchedEventCaptor.capture());
    SurveyLaunchEvent event = surveyLaunchedEventCaptor.getValue();
    assertHeader(event, transactionId, EventType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH);
  }

  // --- helpers

  private void sendBackupEvent(GenericEvent ev) throws Exception {
    EventBackupData data = createEvent(ev);
    String txId = eventPublisher.sendEvent(data);
    assertNotNull(txId);
  }

  private String serialise(Object obj) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialise event to JSON", e);
    }
  }

  private EventBackupData createEvent(GenericEvent event) {
    long failureTimeMillis = 123L;
    EventBackupData data = new EventBackupData();
    data.setId(event.getEvent().getTransactionId());
    data.setEventType(event.getEvent().getType());
    data.setMessageFailureDateTimeInMillis(failureTimeMillis);
    data.setMessageSentDateTimeInMillis(null);
    data.setEvent(serialise(event));
    return data;
  }

  FulfilmentEvent aFulfilmentRequestedEvent() {
    return FixtureHelper.loadPackageFixtures(FulfilmentEvent[].class).get(0);
  }

  RefusalEvent aRefusalEvent() {
    return FixtureHelper.loadPackageFixtures(RefusalEvent[].class).get(0);
  }

  UacAuthenticateEvent aRespondentAuthenticatedEvent() {
    return FixtureHelper.loadPackageFixtures(UacAuthenticateEvent[].class).get(0);
  }

  UacEvent aUacEvent() {
    return FixtureHelper.loadPackageFixtures(UacEvent[].class).get(0);
  }

  SurveyLaunchEvent aSurveyLaunchedEvent() {
    return FixtureHelper.loadPackageFixtures(SurveyLaunchEvent[].class).get(0);
  }

  CaseEvent aCaseEvent() {
    return FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
  }

  SurveyUpdateEvent aSurveyUpdateEvent() {
    return FixtureHelper.loadPackageFixtures(SurveyUpdateEvent[].class).get(0);
  }

  CollectionExerciseUpdateEvent aCollectionExerciseEvent() {
    return FixtureHelper.loadPackageFixtures(CollectionExerciseUpdateEvent[].class).get(0);
  }

  private <T> T loadJson(Class<T[]> clazz) {
    return FixtureHelper.loadPackageFixtures(clazz).get(0);
  }

  private void verifyEventSent(GenericEvent orig, GenericEvent sent) {
    Header origHeader = orig.getEvent();
    Header sentHeader = sent.getEvent();
    Date sentDate = sentHeader.getDateTime();
    assertTrue(
        sentDate.after(this.startOfTestDateTime) || sentDate.equals(this.startOfTestDateTime));
    assertTrue(sentDate.after(origHeader.getDateTime()));
    assertFalse(sentHeader.getTransactionId().equals(origHeader.getTransactionId()));

    // check all other fields are the same
    origHeader.setDateTime(sentDate);
    origHeader.setTransactionId(sentHeader.getTransactionId());
    assertEquals(orig, sent);
  }
}
