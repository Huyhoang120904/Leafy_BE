#include "app/camera_service.h"

namespace leafy {

bool CameraService::begin() {
#ifdef LEAFY_ESP32_CAM
  // TODO: initialize ESP32-CAM hardware after telemetry/config flow is stable.
  _available = false;
#else
  _available = false;
#endif
  return true;
}

bool CameraService::isAvailable() const {
  return _available;
}

String CameraService::buildImageMetaPlaceholder() const {
  // Backend currently logs image/meta but does not process it fully.
  return "{}";
}

}  // namespace leafy
