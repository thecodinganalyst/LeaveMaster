package com.practical.leavemaster.mcp;

import com.practical.leavemaster.location.Location;
import com.practical.leavemaster.location.LocationService;
import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocationMcpTools {

    private final LocationService locationService;

    @Tool(description = "Get all locations")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LOCATION_READ + "')")
    public List<Location> getAllLocations() {
        return locationService.findAll();
    }

    @Tool(description = "Get a location by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LOCATION_READ + "')")
    public Optional<Location> getLocationById(String id) {
        return locationService.findById(id);
    }

    @Tool(description = "Create a new location")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LOCATION_WRITE + "')")
    public Location createLocation(Location location) {
        return locationService.save(location);
    }

    @Tool(description = "Update an existing location")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LOCATION_WRITE + "')")
    public Location updateLocation(String id, Location location) {
        return locationService.update(id, location);
    }

    @Tool(description = "Delete a location by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LOCATION_WRITE + "')")
    public void deleteLocation(String id) {
        locationService.delete(id);
    }
}
