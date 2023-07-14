package com.example.AutoSpotter.classes.vehicle;

import java.util.List;
import java.util.Map;

public interface VehicleRepository {

    int saveVehicle(Vehicle vehicle);

    List<String> getManufacturersByVehicleType(int vehicleTypeId);

    int getVehicleTypeId(String vehicleType);

    int getCityIdByName(String cityName);

    List<String> getAllVehicleTypes();

    List<String> getAllBodyTypes();

    List<String> getAllEngineTypes();

    List<String> getAllTransmissionTypes();

    List<String> getAllDriveTrainTypes();

    List<String> getAllStates();

    int saveVehicleSafetyFeatures(List<String> safetyFeatures);

    int saveVehicleExtras(List<String> extras);

    List<String> getAllCities();

    List<String> getAllCounties();

    Map<String, List<String>> getCitiesByCounty();

    Vehicle getVehicleById(int id);
}