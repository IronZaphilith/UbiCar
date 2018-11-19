package com.ubicomp.mstokfisz.test.DataClasses;

import java.util.ArrayList;

public class Data {
    private Trip currentTrip;
    private ArrayList<Car> cars;
    private ArrayList<Passenger> passengers;
    private ArrayList<Trip> trips;

    public ArrayList<Car> getCars() {
        return cars;
    }

    public ArrayList<Passenger> getPassengers() {
        return passengers;
    }

    public ArrayList<Trip> getTrips() {
        return trips;
    }

    public void addCar (Car car) {
        if (!cars.contains(car)) {
            cars.add(car);
        }
    }

    public void removeCar (Car car) {
        if (cars.contains(car)) {
            cars.remove(car);
        }
    }

    public void addPassenger (Passenger passenger) {
        if (!passengers.contains(passenger)) {
            passengers.add(passenger);
        }
    }

    public void removePassenger (Passenger passenger) {
        if (passengers.contains(passenger)) {
            passengers.remove(passenger);
        }
    }

    public void addTrip (Trip trip) {
        if (!trips.contains(trip)) {
            trips.add(trip);
        }
    }

    public void removeTrip (Trip trip) {
        if (trips.contains(trip)) {
            trips.remove(trip);
        }
    }
}
