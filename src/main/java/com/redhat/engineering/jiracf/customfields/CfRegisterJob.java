package com.redhat.engineering.jiracf.customfields;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.context.GlobalIssueContext;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.jql.context.AllIssueTypesContext;
import com.atlassian.jira.plugin.customfield.CustomFieldTypeModuleDescriptor;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.redhat.engineering.jiracf.PluginJobComponent;
import org.ofbiz.core.entity.GenericEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This plugin job creates any global custom fields needed for the  plugin.
 * <p>
 * This is done by a plugin job rather than during plugin initialization because {@link CustomFieldManager} apparently
 * doesn't know about the custom field types provided by this plugin until after initialization completes.
 * <p>
 * The plugin job uses a very large interval to be effectively a one-shot asynchronous process.
 */
public class CfRegisterJob extends PluginJobComponent {
	private static final Logger LOGGER = LoggerFactory.getLogger(CfRegisterJob.class);
	
	private static final long DELAY_SECONDS = 5L;//sleep this job 5 seconds wait until the plugin install finished

	private static final Set<Class<? extends CustomFieldType<?,?>>> RHBZ_FIELD_TYPES
			= ImmutableSet.<Class<? extends CustomFieldType<?,?>>> of(
			JiraSelectCFType.class
	);

	private final CustomFieldManager cfManager;
	private final FieldScreenManager fieldScreenManager;

	public CfRegisterJob(PluginScheduler pluginScheduler, CustomFieldManager cfManager, FieldScreenManager fieldScreenManager) {
		super(pluginScheduler);
		this.cfManager = cfManager;
		this.fieldScreenManager = fieldScreenManager;
	}

	@Override
	protected long jobInterval() {
		return TimeUnit.DAYS.toMillis(1000L);
	}

	/**
	 * Anything left in fieldsToCreate will be created, using the name and description from atlassian-plugin.xml
	 * @return true if register success
	 */
	protected boolean registerField() {
		final List<CustomField> customFieldObjects = cfManager.getGlobalCustomFieldObjects();
		final Set<Class<? extends CustomFieldType<?,?>>> fieldsToCreate = new HashSet<>(RHBZ_FIELD_TYPES);

		for (final CustomField cf : customFieldObjects) {
			final Class<?> klass = cf.getCustomFieldType().getClass();
			final boolean rhbzType = fieldsToCreate.remove(klass);
			LOGGER.debug("known custom field type {} (rhbz: {})", klass, rhbzType);
		}
		
		for (final Class<? extends CustomFieldType<?, ?>> klass : fieldsToCreate) {
			// find the CustomFieldType of this class
			final List<CustomFieldType<?, ?>> customFieldTypes = cfManager.getCustomFieldTypes();
			boolean found = false;
			for (CustomFieldType<?, ?> cfType : customFieldTypes) {
				if (klass.isAssignableFrom(cfType.getClass())) {
					found = true;
					try {
						CustomField cField = registerCustomField(cfType);

						// Add field to default Screen
						FieldScreen defaultScreen = fieldScreenManager.getFieldScreen(FieldScreen.DEFAULT_SCREEN_ID);
						if (!defaultScreen.containsField(cField.getId())) {
							FieldScreenTab firstTab = defaultScreen.getTab(0);
							firstTab.addFieldScreenLayoutItem(cField.getId());
						}
					} catch (GenericEntityException e) {
						throw Throwables.propagate(e);
					}
					break;
				}
			}
			if (!found) {
				// no known reasons for this to happen
				LOGGER.error("Class {} failed to register with CustomFieldManager", klass);
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected void execute() {
		if (!registerField()) {
			LOGGER.debug("sleep cfRegisterjob 5 seconds wait until the plugin install finished");
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(DELAY_SECONDS));
			} catch (InterruptedException e) {
				LOGGER.error("InterruptedException when sleep the cfRegisterjob");
				throw Throwables.propagate(e);
			}
			LOGGER.debug("try register custom field again");
			registerField();
		}
	}

	private CustomField registerCustomField(CustomFieldType<?, ?> cfType) throws GenericEntityException {
		final CustomFieldTypeModuleDescriptor descriptor = cfType.getDescriptor();
		final String name = descriptor.getName();
		LOGGER.info("Creating global custom field {} of type {}", name, cfType);
		final IssueType issueType = ComponentAccessor.getConstantsManager()
			.getIssueTypeObject(AllIssueTypesContext.getInstance().getIssueTypeId());
		return cfManager.createCustomField(
				name,
				descriptor.getDescription(),
				cfType,
				cfManager.getDefaultSearcher(cfType),
				Collections.singletonList(GlobalIssueContext.getInstance()),
				Collections.singletonList(issueType)
		);
	}
}
