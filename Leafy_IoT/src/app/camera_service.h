#pragma once

#include <Arduino.h>

namespace leafy {

class CameraService {
 public:
  bool begin();
  bool isAvailable() const;
  String buildImageMetaPlaceholder() const;

 private:
  bool _available = false;
};

}  // namespace leafy
