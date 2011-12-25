package com.inebriator;

public interface SolenoidController {

	public void openSolenoid(Solenoid solenoid);
	
	public void closeSolenoid(Solenoid solenoid);
	
	public void disconnect();
}
