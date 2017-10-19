package com.redhat.engineering.jiracf.customfields;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.SelectCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;

import javax.annotation.Nonnull;
import java.util.Map;

public class JiraSelectCFType extends SelectCFType {
    public JiraSelectCFType(CustomFieldValuePersister customFieldValuePersister, OptionsManager optionsManager,
            GenericConfigManager genericConfigManager, JiraBaseUrls jiraBaseUrls) {
        super(customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls);
    }

    @Nonnull
    @Override
    public Map<String, Object> getVelocityParameters( Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem )
    {
        return super.getVelocityParameters( issue, field, fieldLayoutItem );
    }
}
