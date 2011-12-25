package com.inebriator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockSolenoidController implements SolenoidController {

	private static final Logger LOG = LoggerFactory.getLogger(MockSolenoidController.class);

	@Override
	public void openSolenoid(Solenoid solenoid) {
		LOG.info("Open {}", solenoid);
	}

	@Override
	public void closeSolenoid(Solenoid solenoid) {
		LOG.info("Close {}", solenoid);
	}
	
	@Override
	public void disconnect() {
		
	}

}
