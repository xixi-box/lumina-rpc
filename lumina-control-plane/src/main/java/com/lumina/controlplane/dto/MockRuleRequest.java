package com.lumina.controlplane.dto;

import com.lumina.controlplane.entity.MockRuleEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MockRuleRequest {

    @NotBlank(message = "serviceName cannot be blank")
    @Size(max = 255, message = "serviceName cannot exceed 255 characters")
    private String serviceName;

    @NotBlank(message = "methodName cannot be blank")
    @Size(max = 128, message = "methodName cannot exceed 128 characters")
    private String methodName = "*";

    @Pattern(regexp = "exact|regex|wildcard", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "matchType must be exact, regex, or wildcard")
    private String matchType = "exact";

    private String conditionRule;

    @Pattern(regexp = "SHORT_CIRCUIT|TAMPER", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "mockType must be SHORT_CIRCUIT or TAMPER")
    private String mockType = "SHORT_CIRCUIT";

    private String matchCondition;

    @Pattern(regexp = "success|SUCCESS|error|ERROR|exception|EXCEPTION",
            message = "responseType must be success, error, or exception")
    private String responseType = "success";

    private String responseBody;

    @Min(value = 0, message = "responseDelayMs cannot be negative")
    @Max(value = 60000, message = "responseDelayMs cannot exceed 60000")
    private Integer responseDelayMs = 0;

    @Min(value = 100, message = "httpStatus must be >= 100")
    @Max(value = 599, message = "httpStatus must be <= 599")
    private Integer httpStatus = 200;

    private Boolean enabled = true;

    @Min(value = -10000, message = "priority cannot be less than -10000")
    @Max(value = 10000, message = "priority cannot exceed 10000")
    private Integer priority = 0;

    private String description;

    private String tags;

    public MockRuleEntity toEntity() {
        MockRuleEntity entity = new MockRuleEntity();
        entity.setServiceName(serviceName);
        entity.setMethodName(methodName);
        entity.setMatchType(matchType);
        entity.setConditionRule(conditionRule);
        entity.setMockType(mockType);
        entity.setMatchCondition(matchCondition);
        entity.setResponseType(responseType);
        entity.setResponseBody(responseBody);
        entity.setResponseDelayMs(responseDelayMs);
        entity.setHttpStatus(httpStatus);
        entity.setEnabled(enabled);
        entity.setPriority(priority);
        entity.setDescription(description);
        entity.setTags(tags);
        return entity;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getConditionRule() {
        return conditionRule;
    }

    public void setConditionRule(String conditionRule) {
        this.conditionRule = conditionRule;
    }

    public String getMockType() {
        return mockType;
    }

    public void setMockType(String mockType) {
        this.mockType = mockType;
    }

    public String getMatchCondition() {
        return matchCondition;
    }

    public void setMatchCondition(String matchCondition) {
        this.matchCondition = matchCondition;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Integer getResponseDelayMs() {
        return responseDelayMs;
    }

    public void setResponseDelayMs(Integer responseDelayMs) {
        this.responseDelayMs = responseDelayMs;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
