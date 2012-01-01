package com.inebriator.listener;


public class NextLiquidSensorChangeListener extends BaseSensorChangeListener {

	@Override
	protected void sensorUp() {
		// TODO Change some shared pointer to the next pourable liquid
		// This pointer is shared with PourCurrentLiquidSensorChangeListener
	}

	@Override
	protected void sensorDown() {
		// No-op
	}

}
