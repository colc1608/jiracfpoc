package com.redhat.engineering.jiracf.rest;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.common.collect.Lists;
import com.redhat.engineering.jiracf.model.AgilePriority;
import com.redhat.engineering.jiracf.model.AgileStatus;
import com.redhat.engineering.jiracf.model.AgileType;
import com.redhat.engineering.jiracf.model.SimpleIssueView;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Path("issue")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CreateIssueWithCFService {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final UserManager userManager;

    private final CrowdService crowdService;

    private final ProjectManager projectManager;

    private final IssueService issueService;

    private final CustomFieldManager customFieldManager;

    public CreateIssueWithCFService(UserManager userManager, CrowdService crowdService, ProjectManager projectManager,
            IssueService issueService, CustomFieldManager customFieldManager) {
        this.userManager = userManager;
        this.crowdService = crowdService;
        this.projectManager = projectManager;
        this.issueService = issueService;
        this.customFieldManager = customFieldManager;
    }

    @POST
    @Path("/project/{projectKey}")
    public Response createIssue(@Context UriInfo uriInfo, @PathParam("projectKey") final String projectKey) {
        Project prj = projectManager.getProjectObjByKey(projectKey);
        UserProfile currentUser = userManager.getRemoteUser();
        User user = crowdService.getUser(currentUser.getUsername());
        ApplicationUser appUser = ApplicationUsers.from(user);

        try {
            MutableIssue issue = createNewIssue(appUser, prj);
            SimpleIssueView view = new SimpleIssueView(issue.getKey(), issue.getIssueTypeId(), issue.getStatusId(),
                    issue.getPriority().getId(), issue.getResolutionId());
            ObjectMapper mapper = new ObjectMapper();
            return Response.ok(mapper.writeValueAsString(view)).build();
        } catch (IllegalStateException | IOException e) {
            LOGGER.error("Issue Creation ERROR", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("{issueKey}")
    public Response getIssue(@Context UriInfo uriInfo, @PathParam("issueKey") final String issueKey) {
        UserProfile currentUser = userManager.getRemoteUser();
        User user = crowdService.getUser(currentUser.getUsername());
        ApplicationUser appUser = ApplicationUsers.from(user);

        try {
            MutableIssue issue = getIssue(appUser, issueKey);

            SimpleIssueView view = new SimpleIssueView(issue.getKey(), issue.getIssueTypeId(), issue.getStatusId(),
                    issue.getPriority().getId(), issue.getResolutionId());
            ObjectMapper mapper = new ObjectMapper();
            return Response.ok(mapper.writeValueAsString(view)).build();
        } catch (IllegalStateException | IOException e) {
            LOGGER.error("Issue GET ERROR", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @PUT
    @Path("/epic/{issueKey}/{epicKey}")
    public Response linkToEpic(@Context UriInfo uriInfo, @PathParam("issueKey") final String issueKey,
            @PathParam("epicKey") final String epicKey) {
        UserProfile currentUser = userManager.getRemoteUser();
        User user = crowdService.getUser(currentUser.getUsername());
        ApplicationUser appUser = ApplicationUsers.from(user);

        MutableIssue issue = getIssue(appUser, issueKey);
        if (issue == null) {
            LOGGER.error("Epic Error: Not found issue {}", issueKey);
            return Response.status(Response.Status.NOT_FOUND).entity("{\"msg\": \"Not found issue\"}").build();
        }
        List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);
        CustomField epicLink = customFields.stream().filter(cf -> cf.getName().equals("Epic Link")).findFirst().orElse(null);
        if (epicLink == null) {
            LOGGER.error("Epic Error: Not found epic link field for issue {}", issueKey);
            return Response.status(Response.Status.NOT_FOUND).entity("{\"msg\": \"Not found epic link field for issue\"}")
                    .build();
        }

        LOGGER.info("Epic link field found:{}", epicLink.getValue(issue));

        MutableIssue epic = getIssue(appUser, epicKey);
        if (epic == null) {
            LOGGER.error("Epic Error: Not found epic {}", epicKey);
            return Response.status(Response.Status.NOT_FOUND).entity("{\"msg\": \"Not found epic\"}").build();
        }

        IssueInputParameters parameters = issueService.newIssueInputParameters();
        if (parameters.skipScreenCheck()) {
            parameters.setRetainExistingValuesWhenParameterNotProvided(true, true);
        }

        parameters.addCustomFieldValue(epicLink.getId(), epicKey);
        Collection<String> currentValues = parameters.getProvidedFields() != null ?
                Lists.newArrayList(parameters.getProvidedFields()) :
                Lists.newArrayList(epicLink.getId());
        parameters.setProvidedFields(currentValues);

        IssueService.UpdateValidationResult result = issueService.validateUpdate(appUser, issue.getId(), parameters);
        if (result.isValid()) {
            issueService.update(appUser, result);
        } else {
            ErrorCollection collection = result.getErrorCollection();
            throw new IllegalStateException("issue created incorrectly: " + collection.toString());
        }

        return Response.ok().build();
    }

    private MutableIssue createNewIssue(ApplicationUser user, Project project) {
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        String randomString = generateRandomString();
        String randomType = genRandomIssueTypeId();
        String randomStatus = genRandomIssueStatusId();
        String randomPriority = genRandomIssuePriorityId();
        issueInputParameters.setProjectId(project.getId()).setIssueTypeId(randomType)
                .setSummary("Test project issue " + randomString).setReporterId(user.getUsername())
                .setAssigneeId(user.getUsername()).setDescription("Test project issue" + randomString).setStatusId(randomStatus)
                .setPriorityId(randomPriority);
        IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(user, issueInputParameters);
        if (createValidationResult.isValid()) {
            IssueService.IssueResult issueResult = issueService.create(user, createValidationResult);
            if (!issueResult.isValid()) {
                ErrorCollection collection = issueResult.getErrorCollection();
                throw new IllegalStateException("issue created incorrectly: " + collection.toString());
            } else {
                return issueResult.getIssue();
            }
        } else {
            ErrorCollection collection = createValidationResult.getErrorCollection();
            throw new IllegalStateException("issue created incorrectly: " + collection.toString());
        }
    }

    private MutableIssue getIssue(ApplicationUser user, String issueKey) {
        final IssueService.IssueResult issueResult = issueService.getIssue(user, issueKey);
        return issueResult.getIssue();
    }

    private String generateRandomString() {
        int ranLength = ThreadLocalRandom.current().nextInt(1, 100);
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= ranLength; i++) {
            int index = ThreadLocalRandom.current().nextInt(97, 123);
            builder.append((char) index);
        }
        return builder.toString();
    }

    private String genRandomIssueTypeId() {
        final AgileType[] enums = AgileType.values();
        final int ranLength = ThreadLocalRandom.current().nextInt(1, enums.length + 1);
        return enums[ranLength - 1].getId();
    }

    private String genRandomIssueStatusId() {
        final AgileStatus[] enums = AgileStatus.values();
        final int ranLength = ThreadLocalRandom.current().nextInt(1, enums.length + 1);
        return enums[ranLength - 1].getId();
    }

    private String genRandomIssuePriorityId() {
        final AgilePriority[] enums = AgilePriority.values();
        final int ranLength = ThreadLocalRandom.current().nextInt(1, enums.length + 1);
        return enums[ranLength - 1].getId();
    }

    //    @GET
    //    public Response getIssue(@Context UriInfo uriInfo) {
    //        return Response.ok().build();
    //    }
}
