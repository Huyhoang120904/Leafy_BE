#include "app/device_runtime.h"

#include "utils/logger.h"

namespace leafy {

void DeviceRuntime::begin() {
  Logger::info("Runtime begin");
  _state = RuntimeState::BOOT;
}

void DeviceRuntime::loop() {
  switch (_state) {
    case RuntimeState::BOOT:
      enterBoot();
      transitionTo(RuntimeState::LOAD_LOCAL_CONFIG);
      break;

    case RuntimeState::LOAD_LOCAL_CONFIG:
      enterLoadLocalConfig();
      break;

    case RuntimeState::WIFI_SETUP_MODE:
      _wifiManager.loop();
      _setupPortal.loop();
      break;

    case RuntimeState::WIFI_CONNECTING:
      _wifiManager.loop();
      if (_wifiManager.isConnected()) {
        transitionTo(RuntimeState::MQTT_CONNECTING);
        enterMqttConnecting();
      }
      break;

    case RuntimeState::MQTT_CONNECTING:
      _wifiManager.loop();
      _mqttManager.loop(_wifiManager.isConnected());
      if (!_wifiManager.isConnected()) {
        transitionTo(RuntimeState::WIFI_CONNECTING);
      } else if (_mqttManager.isConnected() && _mqttManager.isConfigSubscribed()) {
        transitionTo(RuntimeState::READY);
      }
      break;

    case RuntimeState::READY:
      enterReady();
      break;

    case RuntimeState::RUNNING:
    {
      bool mqttWasConnected = _mqttManager.isConnected();
      runCommonLoops();
      if (!_wifiManager.isConnected()) {
        transitionTo(RuntimeState::WIFI_CONNECTING);
      } else if (!_mqttManager.isConnected()) {
        transitionTo(RuntimeState::MQTT_CONNECTING);
        enterMqttConnecting();
      } else if (!mqttWasConnected && _mqttManager.isConfigSubscribed()) {
        _statusService.publishOnlineNow();
      }
      break;
    }

    case RuntimeState::APPLYING_CONFIG:
      // Config callbacks are handled synchronously by ConfigService in v1 scaffold.
      transitionTo(RuntimeState::RUNNING);
      break;

    case RuntimeState::ERROR_RECOVERY:
      // TODO: add bounded recovery/reboot policy.
      delay(1000);
      transitionTo(RuntimeState::LOAD_LOCAL_CONFIG);
      break;
  }
}

RuntimeState DeviceRuntime::state() const {
  return _state;
}

void DeviceRuntime::transitionTo(RuntimeState next) {
  if (_state == next) {
    return;
  }
  Logger::info(String("State ") + stateName(_state) + " -> " + stateName(next));
  _state = next;
}

void DeviceRuntime::enterBoot() {
  Logger::info("Booting Leafy IoT device runtime");
}

void DeviceRuntime::enterLoadLocalConfig() {
  if (!_configStore.begin() || !_configStore.load(_config)) {
    transitionTo(RuntimeState::ERROR_RECOVERY);
    return;
  }

  if (!_configStore.hasRequiredSetup(_config)) {
    Logger::warn("Required local setup is missing; Wi-Fi setup mode required");
    transitionTo(RuntimeState::WIFI_SETUP_MODE);
    enterWifiSetupMode();
    return;
  }

  initializeRuntimeModules();
  transitionTo(RuntimeState::WIFI_CONNECTING);
  enterWifiConnecting();
}

void DeviceRuntime::initializeRuntimeModules() {
  if (_modulesInitialized) {
    return;
  }

  _configService.begin(&_config, &_configStore, &_mqttManager);
  _configService.onApplyRuntimeConfig([this](const RuntimeConfig& runtime, String& errorMessage) {
    return applyRuntimeConfig(runtime, errorMessage);
  });
  _mqttManager.onConfigMessage([this](const String& payload) {
    transitionTo(RuntimeState::APPLYING_CONFIG);
    _configService.handleConfigMessage(payload);
    transitionTo(RuntimeState::RUNNING);
  });

  initializeSensorModules();
  _telemetryService.begin(_config, &_sensorManager, &_mqttManager);
  _statusService.begin(&_mqttManager, &_wifiManager, DEFAULT_STATUS_HEARTBEAT_SEC);
  _modulesInitialized = true;
}

void DeviceRuntime::enterWifiSetupMode() {
  initializeSensorModules();
  _setupPortal.begin(
      &_config,
      &_configStore,
      &_wifiManager,
      &_sensorManager,
      &_mqttManager,
      [this]() { return String(stateName(_state)); });
  _wifiManager.enterSetupMode(_setupPortal.apSsid());
  _setupPortal.start();
}

void DeviceRuntime::enterWifiConnecting() {
  _wifiManager.begin(_config.wifi);
}

void DeviceRuntime::enterMqttConnecting() {
  _mqttManager.begin(_config);
}

void DeviceRuntime::enterReady() {
  if (_statusService.publishOnlineNow()) {
    transitionTo(RuntimeState::RUNNING);
  } else {
    Logger::warn("Initial online status publish failed; returning to MQTT_CONNECTING");
    transitionTo(RuntimeState::MQTT_CONNECTING);
  }
}

void DeviceRuntime::runCommonLoops() {
  uint32_t now = millis();
  _wifiManager.loop();
  _mqttManager.loop(_wifiManager.isConnected());
  _statusService.loop(now);
  _telemetryService.loop(now, _wifiManager.rssi());
}

void DeviceRuntime::initializeSensorModules() {
  if (_sensorModulesInitialized) {
    return;
  }

  _sensorManager.begin(_config.calibration);
  _cameraService.begin();
  _sensorModulesInitialized = true;
}

bool DeviceRuntime::applyRuntimeConfig(const RuntimeConfig& runtime, String& errorMessage) {
  if (!_modulesInitialized) {
    errorMessage = "runtime modules are not initialized";
    return false;
  }

  _config.runtime = runtime;
  _telemetryService.updateConfig(runtime);

  // The backend config includes offlineTimeoutSec, but the current firmware
  // has no backend-defined status heartbeat field. Keep status heartbeat fixed
  // for v1 and leave offlineTimeoutSec as persisted liveness metadata.
  Logger::info("Runtime config is active: version=" + String(runtime.configVersion) +
               ", sampleSec=" + String(runtime.samplingIntervalSec) +
               ", publishSec=" + String(runtime.publishIntervalSec) +
               ", offlineSec=" + String(runtime.offlineTimeoutSec) +
               ", alertEnabled=" + String(runtime.alertEnabled ? "true" : "false") +
               ", statusHeartbeatSec=" + String(DEFAULT_STATUS_HEARTBEAT_SEC));
  return true;
}

const char* DeviceRuntime::stateName(RuntimeState state) const {
  switch (state) {
    case RuntimeState::BOOT:
      return "BOOT";
    case RuntimeState::LOAD_LOCAL_CONFIG:
      return "LOAD_LOCAL_CONFIG";
    case RuntimeState::WIFI_SETUP_MODE:
      return "WIFI_SETUP_MODE";
    case RuntimeState::WIFI_CONNECTING:
      return "WIFI_CONNECTING";
    case RuntimeState::MQTT_CONNECTING:
      return "MQTT_CONNECTING";
    case RuntimeState::READY:
      return "READY";
    case RuntimeState::RUNNING:
      return "RUNNING";
    case RuntimeState::APPLYING_CONFIG:
      return "APPLYING_CONFIG";
    case RuntimeState::ERROR_RECOVERY:
      return "ERROR_RECOVERY";
  }
  return "UNKNOWN";
}

}  // namespace leafy
