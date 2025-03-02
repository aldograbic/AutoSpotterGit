package com.example.AutoSpotter.classes.listing;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import com.example.AutoSpotter.classes.user.UserRepository;
import com.example.AutoSpotter.classes.vehicle.VehicleRepository;

@Repository
public class JdbcListingRepository implements ListingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public JdbcListingRepository(JdbcTemplate jdbcTemplate, VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void createListing(Listing listing) {
        String sql = "INSERT INTO listing (listing_description, listing_price, vehicle_id, user_id, status, is_featured, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(
                sql,
                listing.getListingDescription(),
                listing.getListingPrice(),
                listing.getVehicleId(),
                listing.getUserId(),
                1,
                listing.getIsFeatured(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Override
    public void editListing(Listing listing) {
        int listingId = listing.getId();
        
        String sql = "UPDATE listing l " +
            "INNER JOIN vehicle v ON l.vehicle_id = v.id " +
            "SET l.listing_description = ?, l.listing_price = ?, l.updated_at = ?, v.mileage = ?, v.state = ?, v.city_id = ? " +
            "WHERE l.id = ? ";

        jdbcTemplate.update(
        sql,
        listing.getListingDescription(),
        listing.getListingPrice(),
        LocalDateTime.now(),
        listing.getVehicle().getMileage(),
        listing.getVehicle().getState(),
        listing.getVehicle().getCityId(),
        listingId);
    }

    @Override
    public void deleteListing(int id) {
        String sql = "DELETE FROM listing WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public Listing getListingById(int id) {
        String sql = "SELECT id, listing_description, listing_price, vehicle_id, user_id, status, is_featured, created_at, updated_at FROM listing WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new ListingRowMapper(vehicleRepository, userRepository), id);
    }

    @Override
    public List<Listing> getNewListings() {
        String sql = "SELECT l.id, l.listing_description, l.listing_price, l.vehicle_id, l.user_id, l.status, l.is_featured, l.created_at, l.updated_at, " +
                    "v.year, v.manufacturer, v.model, v.mileage, c.city_name, v.state, u.username " +
                    "FROM listing l " +
                    "INNER JOIN vehicle v ON l.vehicle_id = v.id " +
                    "INNER JOIN user u ON l.user_id = u.id " +
                    "INNER JOIN cities c ON v.city_id = c.id " +
                    "WHERE l.status = 1 " +
                    "ORDER BY l.created_at DESC";

        return jdbcTemplate.query(sql, new ListingRowMapper(vehicleRepository, userRepository));
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<Listing> getFilteredListings(String vehicleType, String manufacturer, String model,
                                            String bodyType, String engineType, String motorcycleEngineType, String transmission,
                                            String county, Integer mileageFrom, Integer mileageTo,
                                            Integer yearFrom, Integer yearTo, Integer priceFrom,
                                            Integer priceTo, String userType) {
        String sql = "SELECT l.id, l.listing_description, l.listing_price, l.vehicle_id, l.user_id, l.status, l.is_featured, l.created_at, l.updated_at, " +
                "v.year, v.manufacturer, v.model, v.mileage, c.city_name, v.state, u.username " +
                "FROM listing l " +
                "INNER JOIN vehicle v ON l.vehicle_id = v.id " +
                "INNER JOIN user u ON l.user_id = u.id " +
                "INNER JOIN cities c ON v.city_id = c.id " +
                "INNER JOIN counties co ON c.county_id = co.id " +
                "INNER JOIN vehicle_type vt ON v.vehicle_type_id = vt.id " +
                "WHERE l.status = 1 ";

        List<Object> params = new ArrayList<>();

        if (vehicleType != null && !vehicleType.isEmpty()) {
            sql += "AND vt.name = ? ";
            params.add(vehicleType);
        }

        if (manufacturer != null && !manufacturer.isEmpty()) {
            sql += "AND v.manufacturer = ? ";
            params.add(manufacturer);
        }

        if (model != null && !model.isEmpty()) {
            sql += "AND v.model = ? ";
            params.add(model);
        }

        if (bodyType != null && !bodyType.isEmpty()) {
            sql += "AND v.body_type = ? ";
            params.add(bodyType);
        }

        if (engineType != null && !engineType.isEmpty()) {
            sql += "AND v.engine_type = ? ";
            params.add(engineType);
        }

        if (motorcycleEngineType != null && !motorcycleEngineType.isEmpty()) {
            sql += "AND v.motorcycle_engine_type = ? ";
            params.add(motorcycleEngineType);
        }

        if (transmission != null && !transmission.isEmpty()) {
            sql += "AND v.transmission = ? ";
            params.add(transmission);
        }

        if (county != null && !county.isEmpty()) {
            sql += "AND co.county_name = ? ";
            params.add(county);
        }

        if (mileageFrom != null) {
            sql += "AND v.mileage >= ? ";
            params.add(mileageFrom);
        }

        if (mileageTo != null) {
            sql += "AND v.mileage <= ? ";
            params.add(mileageTo);
        }

        if (yearFrom != null) {
            sql += "AND v.year >= ? ";
            params.add(yearFrom);
        }

        if (yearTo != null) {
            sql += "AND v.year <= ? ";
            params.add(yearTo);
        }

        if (priceFrom != null) {
            sql += "AND l.listing_price >= ? ";
            params.add(priceFrom);
        }

        if (priceTo != null) {
            sql += "AND l.listing_price <= ? ";
            params.add(priceTo);
        }

        if (userType != null) {
            if ("Poslovni".equals(userType)) {
                sql += "AND u.company_name IS NOT NULL ";
            } else if ("Privatni".equals(userType)) {
                sql += "AND u.first_name IS NOT NULL ";
            }
        }

        sql += "ORDER BY l.created_at DESC";

        return jdbcTemplate.query(sql, params.toArray(), new ListingRowMapper(vehicleRepository, userRepository));
    }

    @Scheduled(cron = "0 0 0 * * *") // Runs every day at midnight
    public void updateListingStatus() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        String sql = "UPDATE listing SET status = 0 WHERE created_at < ?";
        jdbcTemplate.update(sql, thirtyDaysAgo);
    }

    @Override
    public int getListingsCountByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM listing WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    @Override
    public int getLikedListingsCountByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM user_likes WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    @Override
    public List<Listing> getListingsByUserId(int userId) {
        String sql = "SELECT l.id, l.listing_description, l.listing_price, l.vehicle_id, l.user_id, l.status, l.is_featured, l.created_at, l.updated_at, " +
                "v.year, v.manufacturer, v.model, v.mileage, c.city_name, v.state, u.username " +
                "FROM listing l " +
                "INNER JOIN vehicle v ON l.vehicle_id = v.id " +
                "INNER JOIN user u ON l.user_id = u.id " +
                "INNER JOIN cities c ON v.city_id = c.id " +
                "WHERE l.user_id = ? " +
                "ORDER BY l.created_at DESC";
    
        return jdbcTemplate.query(sql, new ListingRowMapper(vehicleRepository, userRepository), userId);
    }

    @Override
    public void likeListing(int userId, int listingId) {
        String sql = "INSERT INTO user_likes (user_id, listing_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, userId, listingId);
    }

    @Override
    public void dislikeListing(int userId, int listingId) {
        String sql = "DELETE FROM user_likes WHERE user_id = ? AND listing_id = ?";
        jdbcTemplate.update(sql, userId, listingId);
    }

    @Override
    public boolean hasUserLikedListing(int userId, int listingId) {
        String sql = "SELECT COUNT(*) FROM user_likes WHERE user_id = ? AND listing_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, listingId);
        return count != null && count > 0;
    }

    @Override
    public List<Listing> getListingsLikedByUser(int userId) {
        String sql = "SELECT l.id, l.listing_description, l.listing_price, l.vehicle_id, l.user_id, l.status, l.is_featured, l.created_at, l.updated_at, " +
                    "v.year, v.manufacturer, v.model, v.mileage, c.city_name, v.state, u.username " +
                    "FROM listing l " +
                    "INNER JOIN vehicle v ON l.vehicle_id = v.id " +
                    "INNER JOIN user u ON l.user_id = u.id " +
                    "INNER JOIN cities c ON v.city_id = c.id " +
                    "INNER JOIN user_likes ul ON l.id = ul.listing_id " +
                    "WHERE ul.user_id = ? " +
                    "ORDER BY l.created_at DESC";

        return jdbcTemplate.query(sql, new ListingRowMapper(vehicleRepository, userRepository), userId);
    }

    @Override
    public List<Listing> getSimilarListings(int currentListingId, String vehicleType, String manufacturer, String model) {
        String sql = "SELECT l.id, l.listing_description, l.listing_price, l.vehicle_id, l.user_id, l.status, l.is_featured, l.created_at, l.updated_at " +
                "FROM listing l " +
                "INNER JOIN vehicle v ON l.vehicle_id = v.id " +
                "INNER JOIN vehicle_type vt ON v.vehicle_type_id = vt.id " +
                "WHERE l.id != ? " +
                "AND ( " +
                    "(l.vehicle_id IN (SELECT l.id FROM vehicle WHERE manufacturer = ? AND model = ?)) " +
                    "OR " +
                    "(l.vehicle_id IN (SELECT l.id FROM vehicle WHERE manufacturer = ?) AND l.id NOT IN (?, ?)) " +
                    "OR " +
                    "(l.vehicle_id IN (SELECT l.id FROM vehicle WHERE vt.name = ?) AND l.id NOT IN (?, ?, ?)) " +
                ") " +
                "ORDER BY " +
                "  CASE " +
                "    WHEN v.manufacturer = ? AND v.model = ? THEN 1 " +
                "    WHEN v.manufacturer = ? THEN 2 " +
                "    ELSE 3 " +
                "  END, l.created_at DESC " +
                "LIMIT 6";

        return jdbcTemplate.query(
                sql,
                new ListingRowMapper(vehicleRepository, userRepository),
                currentListingId,
                manufacturer, model,
                manufacturer,
                currentListingId, currentListingId,
                vehicleType,
                currentListingId, currentListingId, currentListingId,
                manufacturer, model,
                manufacturer
        );
    }


    @Override
    public List<Listing> getSimilarListingsOfFilteredListings(String vehicleType, String manufacturer, String model) {
        String sql = "SELECT l.id, l.listing_description, l.listing_price, l.vehicle_id, l.user_id, l.status, l.is_featured, l.created_at, l.updated_at, " +
                "v.year, v.manufacturer, v.model, v.mileage, c.city_name, v.state, u.username " +
                "FROM listing l " +
                "INNER JOIN vehicle v ON l.vehicle_id = v.id " +
                "INNER JOIN user u ON l.user_id = u.id " +
                "INNER JOIN cities c ON v.city_id = c.id " +
                "WHERE l.status = 1 " +
                "AND ( " +
                    "(l.vehicle_id IN (SELECT v1.id FROM vehicle v1 WHERE v1.manufacturer = ? AND v1.model = ?)) " +
                    "OR " +
                    "(l.vehicle_id IN (SELECT v2.id FROM vehicle v2 WHERE v2.manufacturer = ?) AND l.id NOT IN (?, ?)) " +
                    "OR " +
                    "(l.vehicle_id IN (SELECT v3.id FROM vehicle v3 INNER JOIN vehicle_type vt ON v3.vehicle_type_id = vt.id WHERE vt.name = ?) AND l.id NOT IN (?, ?, ?)) " +
                ") " +
                "ORDER BY " +
                "  CASE " +
                "    WHEN v.manufacturer = ? AND v.model = ? THEN 1 " +
                "    WHEN v.manufacturer = ? THEN 2 " +
                "    ELSE 3 " +
                "  END, l.created_at DESC ";

        return jdbcTemplate.query(
                sql,
                new ListingRowMapper(vehicleRepository, userRepository),
                manufacturer, model,
                manufacturer,
                manufacturer, manufacturer,
                vehicleType,
                manufacturer, manufacturer, manufacturer,
                manufacturer, model,
                manufacturer
        );
    }

    @Override
    public void saveImageUrlsForVehicle(ListingImage listingImage) {
        String sql = "INSERT INTO images (vehicle_id, image_url) VALUES (?, ?)";
        jdbcTemplate.update(sql, listingImage.getVehicle().getId(), listingImage.getImageUrl());
    }

    @Override
    public List<String> getImageUrlsForVehicle(int vehicleId) {
        String sql = "SELECT image_url FROM images WHERE vehicle_id = ?";
        return jdbcTemplate.queryForList(sql, String.class, vehicleId);
    }

    @Override
    public String getFirstImageUrlForVehicle(int vehicleId) {
        String sql = "SELECT image_url FROM images WHERE vehicle_id = ? LIMIT 1";
        List<String> imageUrls = jdbcTemplate.queryForList(sql, String.class, vehicleId);
        if (!imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return null;
    }

    @Override
    public List<Listing> getNewestListings() {
        String sql = "SELECT l.id, l.listing_description, l.listing_price, l.vehicle_id, l.user_id, l.status, l.is_featured, l.created_at, l.updated_at, " +
                    "v.year, v.manufacturer, v.model, v.mileage, c.city_name, v.state, u.username " +
                    "FROM listing l " +
                    "INNER JOIN vehicle v ON l.vehicle_id = v.id " +
                    "INNER JOIN user u ON l.user_id = u.id " +
                    "INNER JOIN cities c ON v.city_id = c.id " +
                    "WHERE l.status = 1 " +
                    "ORDER BY l.created_at DESC LIMIT 4";

        return jdbcTemplate.query(sql, new ListingRowMapper(vehicleRepository, userRepository));
    }
}