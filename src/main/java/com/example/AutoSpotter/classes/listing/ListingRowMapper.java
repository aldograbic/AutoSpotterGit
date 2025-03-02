package com.example.AutoSpotter.classes.listing;

import org.springframework.jdbc.core.RowMapper;

import com.example.AutoSpotter.classes.user.User;
import com.example.AutoSpotter.classes.user.UserRepository;
import com.example.AutoSpotter.classes.vehicle.SafetyFeature;
import com.example.AutoSpotter.classes.vehicle.Vehicle;
import com.example.AutoSpotter.classes.vehicle.VehicleExtra;
import com.example.AutoSpotter.classes.vehicle.VehicleRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ListingRowMapper implements RowMapper<Listing> {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public ListingRowMapper(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Listing mapRow(ResultSet rs, int rowNum) throws SQLException {
        Listing listing = new Listing();
        listing.setId(rs.getInt("id"));
        listing.setListingDescription(rs.getString("listing_description"));
        listing.setListingPrice(rs.getBigDecimal("listing_price"));
        listing.setVehicleId(rs.getInt("vehicle_id"));
        listing.setUserId(rs.getInt("user_id"));
        listing.setStatus(rs.getBoolean("status"));
        listing.setIsFeatured(rs.getBoolean("is_featured"));
        listing.setCreatedAt(rs.getTimestamp("created_at"));
        listing.setUpdatedAt(rs.getTimestamp("updated_at"));

        int vehicleId = rs.getInt("vehicle_id");
        Vehicle vehicle = vehicleRepository.getVehicleById(vehicleId);
        listing.setVehicle(vehicle);

        int userId = rs.getInt("user_id");
        User user = userRepository.getUserById(userId);
        listing.setUser(user);

        List<SafetyFeature> safetyFeatures = vehicleRepository.getVehicleSafetyFeatures(vehicle.getVehicleSafetyFeaturesId());
        listing.getVehicle().setSafetyFeatures(safetyFeatures);

        List<VehicleExtra> extras = vehicleRepository.getVehicleExtras(vehicle.getVehicleExtrasId());
        listing.getVehicle().setExtras(extras);

        return listing;
    }
}