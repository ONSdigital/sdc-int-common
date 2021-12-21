package uk.gov.ons.ctp.integration.eqlaunch.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
public class EqLaunchData extends EqLaunchCoreData {
	  @NonNull private CaseUpdate caseUpdate;
	  @NonNull private SurveyType surveyType;
	  @NonNull private CollectionExercise collectionExercise;
	  @NonNull private String userId;
	  private String accountServiceUrl;
	  private String accountServiceLogoutUrl;
}
