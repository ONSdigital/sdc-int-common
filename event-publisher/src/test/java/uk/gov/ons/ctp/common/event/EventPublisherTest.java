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
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Date;
import java.util.UUID;
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
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentEvent;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.NewCaseEvent;
import uk.gov.ons.ctp.common.event.model.NewCasePayloadContent;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.common.event.model.RefusalEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.common.event.model.UacAuthenticationEvent;
import uk.gov.ons.ctp.common.event.model.UacAuthenticationResponse;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
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
  @Captor private ArgumentCaptor<UacAuthenticationEvent> respondentAuthenticationEventCaptor;
  @Captor private ArgumentCaptor<RefusalEvent> respondentRefusalEventCaptor;
  @Captor private ArgumentCaptor<UacEvent> uacEventCaptor;
  @Captor private ArgumentCaptor<SurveyLaunchEvent> surveyLaunchedEventCaptor;
  @Captor private ArgumentCaptor<CaseEvent> caseEventCaptor;
  @Captor private ArgumentCaptor<NewCaseEvent> newCaseEventCaptor;
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
    EventPublisher ep = EventPublisher.create(sender, eventPersistence, null);
    assertNotNull(ReflectionTestUtils.getField(ep, "eventPersistence"));
    assertEquals(sender, ReflectionTestUtils.getField(ep, "sender"));
  }

  @Test
  public void sendEventNewCasePayload() {
    NewCasePayloadContent newCaseEvent = loadJson(NewCasePayloadContent[].class);

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.NEW_CASE, Source.RESPONDENT_HOME, Channel.RH, newCaseEvent);

    EventTopic eventTopic = EventTopic.forType(TopicType.NEW_CASE);
    verify(sender, times(1)).sendEvent(eq(eventTopic), newCaseEventCaptor.capture());
    NewCaseEvent event = newCaseEventCaptor.getValue();
    assertHeader(
        event, messageId.toString(), EventTopic.NEW_CASE, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(newCaseEvent, event.getPayload().getNewCase());
  }

  @Test
  public void sendEventSurveyLaunchedPayload() {
    SurveyLaunchResponse surveyLaunchedResponse = loadJson(SurveyLaunchResponse[].class);

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse);

    EventTopic eventTopic = EventTopic.forType(TopicType.SURVEY_LAUNCH);
    verify(sender, times(1)).sendEvent(eq(eventTopic), surveyLaunchedEventCaptor.capture());
    SurveyLaunchEvent event = surveyLaunchedEventCaptor.getValue();
    assertHeader(
        event, messageId.toString(), EventTopic.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(surveyLaunchedResponse, event.getPayload().getResponse());
  }

  @Test
  public void sendEventRespondentAuthenticationPayload() {
    UacAuthenticationResponse respondentAuthenticationResponse =
        loadJson(UacAuthenticationResponse[].class);

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.UAC_AUTHENTICATION,
            Source.RESPONDENT_HOME,
            Channel.RH,
            respondentAuthenticationResponse);

    EventTopic eventTopic = EventTopic.forType(TopicType.UAC_AUTHENTICATION);
    verify(sender, times(1))
        .sendEvent(eq(eventTopic), respondentAuthenticationEventCaptor.capture());
    UacAuthenticationEvent event = respondentAuthenticationEventCaptor.getValue();

    assertHeader(
        event,
        messageId.toString(),
        EventTopic.UAC_AUTHENTICATION,
        Source.RESPONDENT_HOME,
        Channel.RH);
    assertEquals(respondentAuthenticationResponse, event.getPayload().getResponse());
  }

  @Test
  public void sendEventFulfilmentRequestPayload() {
    FulfilmentRequest fulfilmentRequest = loadJson(FulfilmentRequest[].class);

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.FULFILMENT, Source.CONTACT_CENTRE_API, Channel.CC, fulfilmentRequest);

    EventTopic eventTopic = EventTopic.forType(TopicType.FULFILMENT);
    verify(sender, times(1)).sendEvent(eq(eventTopic), fulfilmentRequestedEventCaptor.capture());
    FulfilmentEvent event = fulfilmentRequestedEventCaptor.getValue();

    assertHeader(
        event, messageId.toString(), EventTopic.FULFILMENT, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals("id-123", event.getPayload().getFulfilmentRequest().getCaseId());
  }

  @Test
  public void sendEventRespondentRefusalDetailsPayload() {
    RefusalDetails respondentRefusalDetails = loadJson(RefusalDetails[].class);

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.REFUSAL, Source.CONTACT_CENTRE_API, Channel.CC, respondentRefusalDetails);

    EventTopic eventTopic = EventTopic.forType(TopicType.REFUSAL);
    verify(sender, times(1)).sendEvent(eq(eventTopic), respondentRefusalEventCaptor.capture());
    RefusalEvent event = respondentRefusalEventCaptor.getValue();

    assertHeader(
        event, messageId.toString(), EventTopic.REFUSAL, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(respondentRefusalDetails, event.getPayload().getRefusal());
  }

  /** Test build of Respondent Authentication event message with wrong pay load */
  @Test
  public void sendEventRespondentAuthenticationWrongPayload() {

    boolean exceptionThrown = false;

    try {
      eventPublisher.sendEvent(
          TopicType.UAC_AUTHENTICATION,
          Source.RECEIPT_SERVICE,
          Channel.CC,
          Mockito.mock(EventPayload.class));
    } catch (Exception e) {
      exceptionThrown = true;
      assertThat(e.getMessage(), containsString("incompatible for event type"));
    }

    assertTrue(exceptionThrown);
  }

  private void assertSendCase(EventTopic topic) {
    CaseUpdate payload = loadJson(CaseUpdate[].class);
    assertEquals(7, payload.getSample().getCohort(), "integer should be mapped from string");

    UUID messageId =
        eventPublisher.sendEvent(topic.getType(), Source.CONTACT_CENTRE_API, Channel.CC, payload);

    verify(sender).sendEvent(eq(topic), caseEventCaptor.capture());
    CaseEvent event = caseEventCaptor.getValue();

    assertHeader(event, messageId.toString(), topic, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getCaseUpdate());
  }

  @Test
  public void shouldSendCaseUpdated() {
    assertSendCase(EventTopic.CASE_UPDATE);
  }

  @Test
  public void correctDateFormat() throws JsonProcessingException {
    CaseEvent caseEvent = loadJson(CaseEvent[].class);
    String body = objectMapper.writeValueAsString(caseEvent);

    PubsubMessage pubsubMessage =
        PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(body)).build();
    assertThat(pubsubMessage.toString(), containsString("2021-10-10T00:00:00.000Z"));
  }

  private void assertSendUac(EventTopic topic) {
    UacUpdate payload = loadJson(UacUpdate[].class);

    UUID messageId =
        eventPublisher.sendEvent(topic.getType(), Source.CONTACT_CENTRE_API, Channel.CC, payload);

    verify(sender).sendEvent(eq(topic), uacEventCaptor.capture());
    UacEvent event = uacEventCaptor.getValue();

    assertHeader(event, messageId.toString(), topic, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getUacUpdate());
  }

  @Test
  public void shouldSendUacUpdated() {
    assertSendUac(EventTopic.UAC_UPDATE);
  }

  private void assertSendSurveyUpdate(EventTopic topic) {
    SurveyUpdate payload = loadJson(SurveyUpdate[].class);

    UUID messageId =
        eventPublisher.sendEvent(topic.getType(), Source.CONTACT_CENTRE_API, Channel.CC, payload);

    verify(sender).sendEvent(eq(topic), surveyUpdateArgumentCaptor.capture());
    SurveyUpdateEvent event = surveyUpdateArgumentCaptor.getValue();

    assertHeader(event, messageId.toString(), topic, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getSurveyUpdate());
  }

  @Test
  public void shouldSendSurveyUpdate() {
    assertSendSurveyUpdate(EventTopic.SURVEY_UPDATE);
  }

  private void assertSendCollectionExercise(EventTopic topic) {
    CollectionExercise payload = loadJson(CollectionExercise[].class);

    UUID messageId =
        eventPublisher.sendEvent(topic.getType(), Source.CONTACT_CENTRE_API, Channel.CC, payload);

    verify(sender).sendEvent(eq(topic), collectionExerciseArgumentCaptor.capture());
    CollectionExerciseUpdateEvent event = collectionExerciseArgumentCaptor.getValue();

    assertHeader(event, messageId.toString(), topic, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getCollectionExerciseUpdate());
  }

  @Test
  public void shouldSendCollectionExerciseUpdate() {
    assertSendCollectionExercise(EventTopic.COLLECTION_EXERCISE_UPDATE);
  }

  @Test
  public void shouldRejectSendForMismatchingPayload() {
    SurveyLaunchResponse surveyLaunchedResponse = loadJson(SurveyLaunchResponse[].class);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            eventPublisher.sendEvent(
                TopicType.CASE_UPDATE, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse));
  }

  // -- replay send backup event tests ...

  @Test
  public void shouldSendBackupFulfilmentEvent() throws Exception {
    FulfilmentEvent ev = aFulfilmentRequestedEvent();
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(TopicType.FULFILMENT);
    verify(sender).sendEvent(eq(eventTopic), fulfilmentRequestedEventCaptor.capture());
    verifyEventSent(ev, fulfilmentRequestedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupRepondentAuthenticationEvent() throws Exception {
    UacAuthenticationEvent ev = aRespondentAuthenticationEvent();
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(TopicType.UAC_AUTHENTICATION);
    verify(sender).sendEvent(eq(eventTopic), respondentAuthenticationEventCaptor.capture());
    verifyEventSent(ev, respondentAuthenticationEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupRefusalReceivedEvent() throws Exception {
    RefusalEvent ev = aRefusalEvent();
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(TopicType.REFUSAL);
    verify(sender).sendEvent(eq(eventTopic), respondentRefusalEventCaptor.capture());
    verifyEventSent(ev, respondentRefusalEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupUacUpdatedEvent() throws Exception {
    UacEvent ev = aUacEvent();
    ev.getHeader().setTopic(EventTopic.UAC_UPDATE);
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(TopicType.UAC_UPDATE);
    verify(sender).sendEvent(eq(eventTopic), uacEventCaptor.capture());
    verifyEventSent(ev, uacEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupSurveyLaunchedEvent() throws Exception {
    SurveyLaunchEvent ev = aSurveyLaunchedEvent();
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(TopicType.SURVEY_LAUNCH);
    verify(sender).sendEvent(eq(eventTopic), surveyLaunchedEventCaptor.capture());
    verifyEventSent(ev, surveyLaunchedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupCaseUpdatedEvent() throws Exception {
    CaseEvent ev = aCaseEvent();
    ev.getHeader().setTopic(EventTopic.CASE_UPDATE);
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(TopicType.CASE_UPDATE);
    verify(sender).sendEvent(eq(eventTopic), caseEventCaptor.capture());
    verifyEventSent(ev, caseEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupSurveyUpdateEvent() throws Exception {
    SurveyUpdateEvent ev = aSurveyUpdateEvent();
    ev.getHeader().setTopic(EventTopic.SURVEY_UPDATE);
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(TopicType.SURVEY_UPDATE);
    verify(sender).sendEvent(eq(eventTopic), surveyUpdateArgumentCaptor.capture());
    verifyEventSent(ev, surveyUpdateArgumentCaptor.getValue());
  }

  @Test
  public void shouldSendBackupCollectionExerciseUpdateEvent() throws Exception {
    CollectionExerciseUpdateEvent ev = aCollectionExerciseEvent();
    ev.getHeader().setTopic(EventTopic.COLLECTION_EXERCISE_UPDATE);
    sendBackupEvent(ev);

    EventTopic eventTopic = EventTopic.forType(TopicType.COLLECTION_EXERCISE_UPDATE);
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

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, payload);

    EventTopic eventTopic = EventTopic.forType(TopicType.SURVEY_LAUNCH);
    verify(sender, times(1)).sendEvent(eq(eventTopic), surveyLaunchedEventCaptor.capture());
    SurveyLaunchEvent event = surveyLaunchedEventCaptor.getValue();
    assertHeader(
        event, messageId.toString(), EventTopic.SURVEY_LAUNCH, Source.RESPONDENT_HOME, Channel.RH);
  }

  // --- helpers

  private void sendBackupEvent(GenericEvent ev) throws Exception {
    EventBackupData data = createEvent(ev);
    UUID msgId = eventPublisher.sendEvent(data);
    assertNotNull(msgId);
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
    data.setId(event.getHeader().getMessageId().toString());
    data.setTopicType(event.getHeader().getTopic().getType());
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

  UacAuthenticationEvent aRespondentAuthenticationEvent() {
    return FixtureHelper.loadPackageFixtures(UacAuthenticationEvent[].class).get(0);
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
    Header origHeader = orig.getHeader();
    Header sentHeader = sent.getHeader();
    Date sentDate = sentHeader.getDateTime();
    assertTrue(
        sentDate.after(this.startOfTestDateTime) || sentDate.equals(this.startOfTestDateTime));
    assertTrue(sentDate.after(origHeader.getDateTime()));
    assertFalse(sentHeader.getMessageId().equals(origHeader.getMessageId()));

    // check all other fields are the same
    origHeader.setDateTime(sentDate);
    origHeader.setMessageId(sentHeader.getMessageId());
    origHeader.setCorrelationId(sentHeader.getCorrelationId());
    assertEquals(orig, sent);
  }
}
