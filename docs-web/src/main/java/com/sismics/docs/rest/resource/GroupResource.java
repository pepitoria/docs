package com.sismics.docs.rest.resource;

import java.text.MessageFormat;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Strings;
import com.sismics.docs.core.dao.jpa.GroupDao;
import com.sismics.docs.core.dao.jpa.UserDao;
import com.sismics.docs.core.dao.jpa.criteria.GroupCriteria;
import com.sismics.docs.core.dao.jpa.dto.GroupDto;
import com.sismics.docs.core.model.jpa.Group;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.model.jpa.UserGroup;
import com.sismics.docs.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.util.ValidationUtil;

/**
 * Group REST resources.
 * 
 * @author bgamard
 */
@Path("/group")
public class GroupResource extends BaseResource {
    /**
     * Add a group.
     * 
     * @return Response
     */
    @PUT
    public Response add(@FormParam("parent") String parentName,
            @FormParam("name") String name) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
        
        // Validate input
        name = ValidationUtil.validateLength(name, "name", 1, 50, false);
        ValidationUtil.validateAlphanumeric(name, "name");
        
        // Avoid duplicates
        GroupDao groupDao = new GroupDao();
        Group existingGroup = groupDao.getActiveByName(name);
        if (existingGroup != null) {
            throw new ClientException("GroupAlreadyExists", MessageFormat.format("This group already exists: {0}", name));
        }
        
        // Validate parent
        String parentId = null;
        if (!Strings.isNullOrEmpty(parentName)) {
            Group parentGroup = groupDao.getActiveByName(parentName);
            if (parentGroup == null) {
                throw new ClientException("ParentGroupNotFound", MessageFormat.format("This group does not exists: {0}", parentName));
            }
            parentId = parentGroup.getId();
        }
        
        // Create the group
        groupDao.create(new Group()
                .setName(name)
                .setParentId(parentId), principal.getId());
        
        // Always return OK
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Add a user to a group.
     * 
     * @param groupName Group name
     * @param username Username
     * @return Response
     */
    @PUT
    @Path("{groupName: [a-zA-Z0-9_]+}")
    public Response addMember(@PathParam("groupName") String groupName,
            @FormParam("username") String username) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
        
        // Validate input
        groupName = ValidationUtil.validateLength(groupName, "name", 1, 50, false);
        username = ValidationUtil.validateLength(username, "username", 1, 50, false);
        
        // Get the group
        GroupDao groupDao = new GroupDao();
        Group group = groupDao.getActiveByName(groupName);
        if (group == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        // Get the user
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        if (user == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        // Avoid duplicates
        List<GroupDto> groupDtoList = groupDao.findByCriteria(new GroupCriteria().setUserId(user.getId()), null);
        boolean found = false;
        for (GroupDto groupDto : groupDtoList) {
            if (groupDto.getId().equals(group.getId())) {
                found = true;
            }
        }
        
        if (!found) {
            // Add the membership
            UserGroup userGroup = new UserGroup();
            userGroup.setGroupId(group.getId());
            userGroup.setUserId(user.getId());
            groupDao.addMember(userGroup);
        }
        
        // Always return OK
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }
    
    /**
     * Remove an user from a group.
     * 
     * @param groupName Group name
     * @param username Username
     * @return Response
     */
    @DELETE
    @Path("{groupName: [a-zA-Z0-9_]+}/{username: [a-zA-Z0-9_]+}")
    public Response removeMember(@PathParam("groupName") String groupName,
            @PathParam("username") String username) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
        
        // Validate input
        groupName = ValidationUtil.validateLength(groupName, "name", 1, 50, false);
        username = ValidationUtil.validateLength(username, "username", 1, 50, false);
        
        // Get the group
        GroupDao groupDao = new GroupDao();
        Group group = groupDao.getActiveByName(groupName);
        if (group == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        // Get the user
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        if (user == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        // Remove the membership
        groupDao.removeMember(group.getId(), user.getId());
        
        // Always return OK
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }
}