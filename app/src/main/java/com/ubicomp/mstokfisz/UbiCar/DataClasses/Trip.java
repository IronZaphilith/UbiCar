package com.ubicomp.mstokfisz.UbiCar.DataClasses;

import java.util.ArrayList;

public class Trip {
    private String name;
    private Car car;
    private ArrayList<Passenger> passengers;
    private long numberOfCalculations = 0;
    private double distance = 0;
    private int avgSpeed = 0;
    private long speedSum = 0;
    private double fuelConsumptionSum = 0;
    private long workingTime = 0;
    private long travelTime = 0;
    private long time;



    public Trip(String name, Car car, ArrayList<Passenger> passengers) {
        this.name = name;
        this.car = car;
        this.passengers = passengers;
    }

    public long getWorkingTime() {
        return workingTime;
    }

    public void setWorkingTime(long workingTime) {
        this.workingTime = workingTime;
    }

    public long getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(long travelTime) {
        this.travelTime = travelTime;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public ArrayList<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(ArrayList<Passenger> passengers) {
        this.passengers = passengers;
    }

    public void addPassenger(Passenger passenger) {
        if (!passengers.contains(passenger))
            passengers.add(passenger);
    }

    public void removePassenger(Passenger passenger) {
        if (passengers.contains(passenger))
            passengers.remove(passenger);
    }

    public long getNumberOfCalculations() {
        return numberOfCalculations;
    }

    public void setNumberOfCalculations(long numberOfCalculations) {
        this.numberOfCalculations = numberOfCalculations;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(int avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public long getSpeedSum() {
        return speedSum;
    }

    public void setSpeedSum(long speedSum) {
        this.speedSum = speedSum;
    }

    public double getFuelConsumptionSum() {
        return fuelConsumptionSum;
    }

    public void setFuelConsumptionSum(double fuelConsumptionSum) {
        this.fuelConsumptionSum = fuelConsumptionSum;
    }
}
