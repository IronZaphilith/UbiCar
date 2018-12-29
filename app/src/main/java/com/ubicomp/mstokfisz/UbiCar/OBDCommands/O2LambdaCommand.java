package com.ubicomp.mstokfisz.UbiCar.OBDCommands;

import com.github.pires.obd.commands.ObdCommand;

public class O2LambdaCommand extends ObdCommand {
    private double ER = 0;
    private double lambdaVoltage = 0;

    /**
     * <p>Constructor for O2LambdaCommand.</p>
     */
    public O2LambdaCommand() {
        super("01 24");
    }

    @Override
    protected void performCalculations() {
        // ignore first two bytes [01 44] of the response
        float A = buffer.get(2);
        float B = buffer.get(3);
        float C = buffer.get(4);
        float D = buffer.get(5);
        ER = ((A * 256) + B) / 32768;//((A*256)+B)/3276;
        lambdaVoltage = ((C*256)+D)/8192;//((C*256)+D)/8192 V
    }

    @Override
    public String getFormattedResult() {
        return String.format("%.2f", getEquivalenceRatio()) + ":1 ER";
    }

    /** {@inheritDoc} */
    @Override
    public String getCalculatedResult() {
        return String.valueOf(getEquivalenceRatio());
    }

    public double getEquivalenceRatio() {
        return ER;
    }

    public String getFormattedLambdaVoltage() {
        return String.format("%.2f", getEquivalenceRatio()) + " V";
    }

    public double getLambdaVoltage() {
        return lambdaVoltage;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "O2 Lambda";
    }
}
