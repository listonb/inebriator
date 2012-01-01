package com.inebriator.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inebriator.Inebriator;

public class EmergencyStopSensorChangeListener extends BaseSensorChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(EmergencyStopSensorChangeListener.class);

	@Override
	protected void sensorUp() {
		LOG.error("Emergency stop button has been pressed! Closing all solenoids...");
		getInebriator().reset();

		LOG.error("All solenoids have been closed.");
		Inebriator.snooze(2000);

		// TODO Inject the WrapperListener and attempt a clean shutdown.
		LOG.error("Shutting down the JVM *NOW* via System.exit");
		System.exit(1);
	}

	@Override
	protected void sensorDown() {
		// No-op
	}
}
