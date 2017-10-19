package com.redhat.engineering.jiracf.customfields;

import com.atlassian.jira.issue.customfields.impl.SelectCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;

public class JiraSelectCFType extends SelectCFType {
    public JiraSelectCFType(CustomFieldValuePersister customFieldValuePersister, OptionsManager optionsManager,
            GenericConfigManager genericConfigManager, JiraBaseUrls jiraBaseUrls) {
        super(customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls);
    }
}
