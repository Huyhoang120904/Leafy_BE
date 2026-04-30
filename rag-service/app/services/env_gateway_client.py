import logging
from typing import Any, Dict, List, Optional

import httpx

from app.config.settings import settings

logger = logging.getLogger(__name__)


class EnvGatewayClient:
    """Gateway client for resolving plant -> plot -> zone and fetching IoT zone overview."""

    def __init__(self, base_url: Optional[str] = None, timeout_seconds: Optional[float] = None) -> None:
        self.base_url = (base_url or settings.api_gateway_url).rstrip("/")
        self.timeout_seconds = timeout_seconds or settings.env_lookup_timeout_seconds

    def resolve_zone_context(self, plant_id: str, auth_header: Optional[str]) -> Optional[Dict[str, Any]]:
        """Resolve a single deterministic zone context from a plant id.

        Returns None when the mapping is missing or ambiguous.
        """
        plant = self._get_wrapped_data(f"/api/plants/{plant_id}", auth_header)
        if not isinstance(plant, dict):
            logger.info("[ENV LOOKUP] Plant not found or unreadable for plant_id=%s", plant_id)
            return None

        farm_plot_id = plant.get("farmPlotId")
        if not farm_plot_id:
            logger.info("[ENV LOOKUP] Missing farmPlotId for plant_id=%s", plant_id)
            return None

        zones_payload = self._get_wrapped_data(f"/api/farms/plots/{farm_plot_id}/zones", auth_header)
        if not isinstance(zones_payload, list):
            logger.info("[ENV LOOKUP] Zone list unavailable for farm_plot_id=%s", farm_plot_id)
            return None

        zones = [zone for zone in zones_payload if isinstance(zone, dict) and zone.get("id")]
        if len(zones) != 1:
            logger.info(
                "[ENV LOOKUP] Expected exactly one zone for farm_plot_id=%s but got=%d",
                farm_plot_id,
                len(zones),
            )
            return None

        zone = zones[0]
        return {
            "plant_id": plant_id,
            "farm_plot_id": str(farm_plot_id),
            "zone_id": str(zone.get("id")),
            "altitude_m": self._to_float(zone.get("elevationM")),
        }

    def get_zone_overview(self, zone_id: str, auth_header: Optional[str]) -> Optional[Dict[str, Any]]:
        """Fetch IoT zone overview for a resolved zone id."""
        payload = self._get_json(f"/api/iot/farm-zones/{zone_id}/overview", auth_header)
        if not isinstance(payload, dict):
            logger.info("[ENV LOOKUP] IoT zone overview unavailable for zone_id=%s", zone_id)
            return None

        # IoT responses are usually raw objects. If wrapped, unwrap data.
        if "data" in payload and "code" in payload:
            data = payload.get("data")
            return data if isinstance(data, dict) else None

        return payload

    def _get_wrapped_data(self, path: str, auth_header: Optional[str]) -> Optional[Any]:
        payload = self._get_json(path, auth_header)
        if not isinstance(payload, dict):
            return None

        if "data" not in payload:
            logger.warning("[ENV LOOKUP] Expected wrapped response for path=%s", path)
            return None

        return payload.get("data")

    def _get_json(self, path: str, auth_header: Optional[str]) -> Optional[Any]:
        url = f"{self.base_url}{path}"
        headers: Dict[str, str] = {"Accept": "application/json"}
        if auth_header:
            headers["Authorization"] = auth_header

        try:
            with httpx.Client(timeout=self.timeout_seconds) as client:
                response = client.get(url, headers=headers)
        except httpx.RequestError as exc:
            logger.warning("[ENV LOOKUP] Request failed for %s: %s", url, exc)
            return None

        if response.status_code >= 400:
            logger.info("[ENV LOOKUP] Non-success status=%d for %s", response.status_code, url)
            return None

        try:
            return response.json()
        except ValueError:
            logger.warning("[ENV LOOKUP] Non-JSON response for %s", url)
            return None

    @staticmethod
    def _to_float(value: Any) -> Optional[float]:
        try:
            if value is None:
                return None
            return float(value)
        except (TypeError, ValueError):
            return None


_gateway_client = EnvGatewayClient()


def get_env_gateway_client() -> EnvGatewayClient:
    return _gateway_client
