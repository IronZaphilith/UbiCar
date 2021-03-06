package com.ubicomp.mstokfisz.UbiCar.DataClasses;

import com.ubicomp.mstokfisz.UbiCar.Utils.FuelType;

public class Car {
    private String name;
    private FuelType fuelType;
    private int engineSize;
    private int offSet;


    public Car(String name, FuelType fuelType, int engineSize) {
        this.name = name;
        this.fuelType = fuelType;
        this.engineSize = engineSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public int getOffSet() {
        return offSet;
    }

    public void setOffSet(int offSet) {
        this.offSet = offSet;
    }

    public int getEngineSize() {
        return engineSize;
    }

    public void setEngineSize(int engineSize) {
        this.engineSize = engineSize;
    }
}
