"""HTTP client for creating treatment plans in the plant-management-service.

Forwards the caller's auth header so that the service can resolve the
authenticated user's profileId and set it as the plan owner.
"""
import logging
from typing import Any, Dict, List, Optional

import httpx

from app.config.settings import settings

logger = logging.getLogger(__name__)

_TIMEOUT = httpx.Timeout(connect=10.0, read=30.0, write=30.0, pool=10.0)


class PlantManagementClient:
    """Singleton client for the plant-management-service plan API."""

    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    @property
    def _base_url(self) -> str:
        return settings.api_gateway_url.rstrip("/")

    def create_plan(
        self,
        *,
        rag_plan_id: str,
        question: str,
        generated_plan: Dict[str, Any],
        plan_source: Optional[str],
        auth_header: str,
    ) -> Optional[str]:
        """POST /api/plans to plant-management-service.

        Returns the plant-management plan ID on success, or None on failure.
        Errors are logged but never propagated — plan creation in the RAG
        service's own MongoDB has already succeeded at this point.
        """
        payload = self._build_payload(
            rag_plan_id=rag_plan_id,
            question=question,
            generated_plan=generated_plan,
            plan_source=plan_source,
        )

        url = f"{self._base_url}/api/plans"
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Authorization": auth_header,
        }

        try:
            with httpx.Client(timeout=_TIMEOUT) as client:
                response = client.post(url, json=payload, headers=headers)

            if response.status_code >= 400:
                logger.warning(
                    "[PLAN SYNC] plant-management-service returned status=%d for ragPlanId=%s — body=%s",
                    response.status_code,
                    rag_plan_id,
                    response.text[:200],
                )
                return None

            data = response.json()
            # Unwrap standard ApiResponse envelope: { code, data: { id, ... } }
            if isinstance(data, dict) and "data" in data:
                data = data["data"]
            plan_id = data.get("id") if isinstance(data, dict) else None
            logger.info(
                "[PLAN SYNC] Plan created in plant-management-service — id=%s, ragPlanId=%s",
                plan_id,
                rag_plan_id,
            )
            return plan_id

        except httpx.RequestError as exc:
            logger.warning("[PLAN SYNC] HTTP request failed for ragPlanId=%s: %s", rag_plan_id, exc)
            return None
        except Exception as exc:
            logger.warning("[PLAN SYNC] Unexpected error for ragPlanId=%s: %s", rag_plan_id, exc, exc_info=True)
            return None

    # ── Payload builder ────────────────────────────────────────────────────────

    def _build_payload(
        self,
        *,
        rag_plan_id: str,
        question: str,
        generated_plan: Dict[str, Any],
        plan_source: Optional[str],
    ) -> Dict[str, Any]:
        schedule = generated_plan.get("schedule")
        payload: Dict[str, Any] = {
            "ragPlanId": rag_plan_id,
            "question": question,
            "planName": generated_plan.get("planName"),
            "source": plan_source,
            "plantId": generated_plan.get("plantId"),
            "farmPlotId": generated_plan.get("farmPlotId"),
            "farmZoneId": generated_plan.get("farmZoneId"),
            "diseaseName": generated_plan.get("diseaseName"),
            "confidenceScore": generated_plan.get("confidenceScore"),
            "severityLevel": generated_plan.get("severityLevel"),
            "urgency": generated_plan.get("urgency"),
            "requiredInputs": generated_plan.get("requiredInputs"),
            "safetyWarnings": generated_plan.get("safetyWarnings"),
            "successIndicators": generated_plan.get("successIndicators"),
            "estimatedCost": generated_plan.get("estimatedCost"),
            "schedule": self._normalise_schedule(schedule) if schedule else None,
            "isPublic": False,
        }
        # Strip explicit None values so we don't override server-side defaults
        return {k: v for k, v in payload.items() if v is not None}

    @staticmethod
    def _normalise_schedule(schedule: Any) -> Optional[List[Dict[str, Any]]]:
        """Ensure schedule items are plain dicts for JSON serialisation.

        ``sourcePlanId`` is intentionally stripped from every item — the RAG
        service only knows its own internal UUID, not the plant-management-
        service Plan._id that will be assigned upon creation.  The service is
        responsible for back-filling the correct Plan._id after saving.
        """
        if not isinstance(schedule, list):
            return None
        result = []
        for item in schedule:
            if hasattr(item, "model_dump"):
                d = item.model_dump(mode="json", exclude_none=True)
            elif isinstance(item, dict):
                d = dict(item)
            else:
                continue
            d.pop("sourcePlanId", None)
            result.append(d)
        return result or None


_plant_management_client = PlantManagementClient()


def get_plant_management_client() -> PlantManagementClient:
    return _plant_management_client
