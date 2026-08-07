package com.practical.leavemaster.mcp;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppUserMcpTools {

    private final AppUserService appUserService;

    @Tool(description = "Get all application users")
    public List<AppUser> getAllUsers() {
        return appUserService.findAll();
    }

    @Tool(description = "Get an application user by login name")
    public Optional<AppUser> getUserByLoginName(String loginName) {
        return appUserService.findByLoginName(loginName);
    }

    @Tool(description = "Create a new application user")
    public AppUser createUser(AppUser user) {
        return appUserService.save(user);
    }

    @Tool(description = "Update an existing application user by login name")
    public AppUser updateUser(String loginName, AppUser user) {
        return appUserService.update(loginName, user);
    }

    @Tool(description = "Change password for an application user by login name")
    public AppUser changePassword(String loginName, String newPassword) {
        return appUserService.changePassword(loginName, newPassword);
    }

    @Tool(description = "Activate an application user by login name")
    public AppUser activateUser(String loginName) {
        return appUserService.activate(loginName);
    }

    @Tool(description = "Deactivate an application user by login name")
    public AppUser deactivateUser(String loginName) {
        return appUserService.deactivate(loginName);
    }

    @Tool(description = "Delete an application user by login name")
    public void deleteUser(String loginName) {
        appUserService.delete(loginName);
    }
}
