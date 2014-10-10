package com.lunex.eventprocessor.core;

import java.util.Date;
import java.util.Map;

public class EventQueryException {

  public static enum ExptionAction {
    VERIFIED("verified");
    private String contentType;

    private ExptionAction(String stringVal) {
      contentType = stringVal;
    }

    public String toString() {
      return contentType;
    }

    public static ExptionAction getContentType(String verd) {
      if (verd == null) {
        return null;
      }
      for (ExptionAction e : ExptionAction.values()) {
        if (verd.equalsIgnoreCase(e.contentType))
          return e;
      }
      return null;
    }
  };

  private String eventName;
  private String ruleName;
  private ExptionAction action;
  private Date expiredDate;
  private Map<String, Object> conditionFilter;

  public EventQueryException(String eventName, String ruleName, ExptionAction action, Date expDate,
      Map<String, Object> conditionFilter) {
    this.eventName = eventName;
    this.ruleName = ruleName;
    this.action = action;
    this.expiredDate = expDate;
    this.conditionFilter = conditionFilter;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public String getRuleName() {
    return ruleName;
  }

  public void setRuleName(String ruleName) {
    this.ruleName = ruleName;
  }

  public ExptionAction getAction() {
    return action;
  }

  public void setAction(ExptionAction action) {
    this.action = action;
  }

  public Date getExpiredDate() {
    return expiredDate;
  }

  public void setExpiredDate(Date expiredDate) {
    this.expiredDate = expiredDate;
  }

  public Map<String, Object> getConditionFilter() {
    return conditionFilter;
  }

  public void setConditionFilter(Map<String, Object> conditionFilter) {
    this.conditionFilter = conditionFilter;
  }

}
