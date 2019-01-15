package de.dfki.iui.mmir.plugins.gadgetbridge;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface IDeviceManager {
  GBDevice getDevice();
  // TODO: boolean selectDevice(String id);
}
