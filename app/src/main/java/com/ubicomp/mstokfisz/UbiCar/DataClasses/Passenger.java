package com.ubicomp.mstokfisz.UbiCar.DataClasses;

public class Passenger {
    private String name;
    private double debt;

    public Passenger(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDebt() {
        return debt;
    }

    public void setDebt(double debt) {
        this.debt = debt;
    }
}