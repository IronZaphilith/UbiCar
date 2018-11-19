package com.ubicomp.mstokfisz.UbiCar.DataClasses;

import java.util.ArrayList;

public class Data {
    public Trip getCurrentTrip() {
        return currentTrip;
    }

    public void setCurrentTrip(Trip currentTrip) {
        this.currentTrip = currentTrip;
    }

    private Trip currentTrip;
    private ArrayList<Car> cars;
    private ArrayList<Passenger> passengers;
    private ArrayList<Trip> trips;

    public Data() {
        cars = new ArrayList<>();
        passengers = new ArrayList<>();
        trips = new ArrayList<>();
    }

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
            currentTrip = trip;
        }
    }

    public void removeTrip (Trip trip) {
        if (trips.contains(trip)) {
            trips.remove(trip);
        }
    }
}
