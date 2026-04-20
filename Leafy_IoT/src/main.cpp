#include <Arduino.h>

#include "app/device_runtime.h"
#include "utils/logger.h"

leafy::DeviceRuntime runtime;

void setup() {
  leafy::Logger::begin(115200);
  leafy::Logger::info("Leafy IoT firmware starting");
  runtime.begin();
}

void loop() {
  runtime.loop();
}
