package com.inebriator.listener;

import com.inebriator.Inebriator;
import com.inebriator.PhidgetSolenoidController;
import com.inebriator.SolenoidController;
import com.phidgets.Phidget;
import com.phidgets.event.SensorChangeEvent;
import com.phidgets.event.SensorChangeListener;

public abstract class BaseSensorChangeListener implements SensorChangeListener {

	private Inebriator inebriator;
	private int phidgetId;
	private int indexNumber;

	public Inebriator getInebriator() {
		return inebriator;
	}

	public void setInebriator(Inebriator inebriator) {
		this.inebriator = inebriator;
	}

	public void setPhidgetSerialNumber(int serialNumber) {
		this.phidgetId = serialNumber;
	}
	
	public void setIndexNumber(int indexNumber) {
		this.indexNumber = indexNumber;
	}
	
	public int getPhidgetSerialNumber() {
		return phidgetId;
	}
	
	public int getIndexNumber() {
		return indexNumber;
	}

	@Override
	public void sensorChanged(SensorChangeEvent event) {
		int phidgetSerialNumber = PhidgetSolenoidController.getSerialNumberFromPhidget(event.getSource());
		int indexNumber = event.getIndex();
		
		SolenoidController solenoidController = getInebriator().getSolenoidController();
		int phidgetId = -1;

		if (solenoidController instanceof PhidgetSolenoidController) {
			Phidget[] phidgets = ((PhidgetSolenoidController) solenoidController).getPhidgets();

			for (int i = 0; i < phidgets.length; i++) {
				if (PhidgetSolenoidController.getSerialNumberFromPhidget(phidgets[i]) == phidgetSerialNumber) {
					phidgetId = i;
					break;
				}
			}

			if (phidgetId < 0) {
				throw new RuntimeException("Can't find phidget with serial number [" + phidgetSerialNumber + "]");
			}
		} else {
			throw new RuntimeException("Event are not supported with mock solenoid controller");
		}

		if (phidgetId == this.phidgetId && indexNumber == this.indexNumber) {
			if (event.getValue() > 0) {
				sensorUp();
			} else {
				sensorDown();
			}
		}
	}

	protected abstract void sensorUp();

	protected abstract void sensorDown();
}
