package com.redhat.engineering.jiracf.rest;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("issue")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CreateIssueWithCFService {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final UserManager userManager;
    private final CrowdService crowdService;
    private final ProjectManager projectManager;
    private final IssueService issueService;

    public CreateIssueWithCFService(UserManager userManager, CrowdService crowdService, ProjectManager projectManager,
            IssueService issueService) {
        this.userManager = userManager;
        this.crowdService = crowdService;
        this.projectManager = projectManager;
        this.issueService = issueService;
    }

    @GET
    @Path("/project/{projectKey}")
    public Response createIssue(@Context UriInfo uriInfo, @PathParam("projectKey") final String projectKey) {
        Project prj = projectManager.getProjectObjByKey(projectKey);
        UserProfile currentUser = userManager.getRemoteUser();
        User user = crowdService.getUser(currentUser.getUsername());
        ApplicationUser appUser = ApplicationUsers.from(user);

        try {
            MutableIssue issue = createNewIssue(appUser, prj);
            return Response.ok(issue.getKey()).build();
        } catch (IllegalStateException e) {
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
            LOGGER.warn(issue.toString());
            return Response.ok(issue.getKey()).build();
        } catch (IllegalStateException e) {
            LOGGER.error("Issue GET ERROR", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    private MutableIssue createNewIssue(ApplicationUser user, Project project) {
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.setProjectId(project.getId()).setIssueTypeId("1").setSummary("Test project issue")
                .setReporterId(user.getUsername()).setAssigneeId(user.getUsername()).setDescription("Test project issue")
                .setStatusId("1").setPriorityId("1").setResolutionId("1");
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

    //    @GET
    //    public Response getIssue(@Context UriInfo uriInfo) {
    //        return Response.ok().build();
    //    }
}
