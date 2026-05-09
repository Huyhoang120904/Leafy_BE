"""
General Planner Node

Generates a structured, chronological agronomic action schedule (Plan)
for coffee operations including routine care, treatment, pruning/denoting,
and seasonal field activities using retrieved knowledge and Gemini Pro.

This is the final node in the pipeline - it enriches the text generation
with machine-readable structured data for direct consumption by a frontend
calendar / event scheduler.
"""

import logging
import re

from langchain_core.messages import AIMessage

from app.agents.rag_state import GraphState
from app.core.ai_providers import get_gemini_pro
from app.schemas import Plan

logger = logging.getLogger(__name__)


# ── Plan horizon extraction ────────────────────────────────────────────────────

_DURATION_PATTERNS = [
    # Vietnamese
    (r'(\d+)\s*tháng',      30),   # n tháng → n * 30 days
    (r'(\d+)\s*tuần',        7),   # n tuần  → n * 7 days
    (r'(\d+)\s*ngày',         1),  # n ngày  → n days
    # English
    (r'(\d+)\s*month',       30),
    (r'(\d+)\s*week',         7),
    (r'(\d+)\s*day',          1),
]

def _extract_plan_horizon_days(question: str) -> int:
    """
    Scan the question for an explicit duration request (e.g. '1 tháng', '2 tuần').
    Returns the horizon in days, or 0 if not found.
    """
    q = question.lower()
    for pattern, multiplier in _DURATION_PATTERNS:
        m = re.search(pattern, q)
        if m:
            return int(m.group(1)) * multiplier
    return 0


def _build_event_density_guidance(horizon_days: int) -> str:
    """
    Return a tailored event-density block for the system prompt based on
    how many days the requested plan should cover.
    """
    if horizon_days <= 0:
        return ""

    # Derive recommended counts from the horizon
    irrigation_count   = max(2, horizon_days // 7)          # ~weekly
    nutrition_count    = max(1, horizon_days // 14)          # bi-weekly
    scouting_count     = max(2, horizon_days // 7)           # weekly
    weed_count         = max(1, horizon_days // 14)          # bi-weekly
    pruning_count      = 1 if horizon_days >= 14 else 0
    phenology_count    = max(1, horizon_days // 14)          # bi-weekly milestones

    lines = [
        f"PLAN HORIZON: {horizon_days} days.",
        "You MUST distribute events evenly across the full period.",
        f"Minimum event counts for a {horizon_days}-day plan:",
        f"  • IRRIGATION       : ≥{irrigation_count} events (one per ~7 days, e.g. days 0, 7, 14, 21…)",
        f"  • NUTRITION        : ≥{nutrition_count} events (one per ~14 days)",
        f"  • SCOUTING         : ≥{scouting_count} events (one per ~7 days)",
        f"  • WEED_CONTROL     : ≥{weed_count} events (one per ~14 days)",
    ]
    if pruning_count:
        lines.append(f"  • PRUNING          : ≥{pruning_count} event (toward the end of the period)")
    lines += [
        f"  • PHENOLOGY        : ≥{phenology_count} milestone(s) (record growth stage changes)",
        f"Total events expected: ≥{irrigation_count + nutrition_count + scouting_count + weed_count + pruning_count + phenology_count}",
        "",
        "durationDays rules for this plan:",
        "  • IRRIGATION window: set durationDays = 1 (single task each occurrence)",
        "  • NUTRITION application: set durationDays = 1",
        "  • SCOUTING: set durationDays = 1",
        "  • WEED_CONTROL: set durationDays = 1–2 depending on scale",
        "  • PRUNING: set durationDays = 1–3 depending on canopy size",
        "  • PHENOLOGY: set durationDays = 1 (point-in-time observation)",
        "",
        "CRITICAL: Do NOT cluster all events at the start.",
        f"Spread them across the full {horizon_days} days.",
        "Use daysFromNow values like: 0, 7, 14, 21, 28 for a 30-day plan.",
    ]
    return "\n".join(lines)


def planner(state: GraphState) -> dict:
    """
    Generate a structured Plan from retrieved agronomic documents.

    Uses Gemini Pro with structured output (via Pydantic) to produce a
    chronological list of EmbeddedPlanEvent objects that are stored as
    an embedded array inside the Plan document in plant-management-service.

    Post-processing sorts events by daysFromNow; actual ISO dates are
    computed at apply time by the plant-management-service consumer.

    Args:
        state: Current graph state. Reads `question`, `documents`,
               and optionally `safety_issues` for context.

    Returns:
        Updated state with:
          - `generated_plan`: dict (serialized Plan incl. embedded schedule)
          - `plant_id`: str extracted by the LLM from the question
    """
    question = state["question"]
    documents = state.get("documents", [])
    web_results = state.get("web_search_results") or []
    language = state.get("language", "English")
    refinement_guidance = (state.get("refinement_guidance") or "").strip()

    logger.info("[GENERAL PLANNER] Building structured agronomic plan")

    if not documents and not web_results:
        logger.warning("[GENERAL PLANNER] No documents or web results available - skipping plan generation")
        return {"generated_plan": None, "plant_id": None}

    # ── Plan horizon & event-density guidance ────────────────────────────────
    horizon_days = _extract_plan_horizon_days(question)
    density_guidance = _build_event_density_guidance(horizon_days)
    if horizon_days:
        logger.info("[GENERAL PLANNER] Detected plan horizon: %d days", horizon_days)

    llm = get_gemini_pro(temperature=0)
    structured_llm = llm.with_structured_output(Plan)

    docs_context = "\n---\n".join(
        f"Agronomic Source {i + 1}:\n{doc.page_content}"
        for i, doc in enumerate(documents[:5])
    ) if documents else "(No internal knowledge base documents available for this query.)"

    web_context = ""
    if web_results:
        web_lines = "\n---\n".join(
            f"Web Source {i + 1}: {r.get('title', 'Untitled')}\n"
            f"URL: {r.get('url', '')}\n"
            f"Content: {r.get('content', '')}"
            for i, r in enumerate(web_results[:5])
        )
        web_context = f"""

Web Sources (current regulations, local research, recent outbreak data):
{web_lines}"""
        logger.info("[GENERAL PLANNER] Incorporating %d web sources into plan", len(web_results))

    safety_issues = state.get("safety_issues", [])
    safety_note = ""
    if safety_issues:
        issues_text = "\n".join(f"  - {issue}" for issue in safety_issues)
        safety_note = (
            "\n\nSAFETY CONSTRAINTS - the following issues were flagged by the safety auditor. "
            f"Your plan MUST avoid these:\n{issues_text}"
        )
        if refinement_guidance:
            safety_note += f"\n\nACTIONABLE REFINEMENT GUIDANCE:\n{refinement_guidance}"

    export_context_keywords = [
        "export", "xuat khau", "eu", "usda", "japan", "premium retail", "international",
        "rainforest alliance", "4c", "mrl", "residue",
    ]
    export_context = any(k in question.lower() for k in export_context_keywords)

    env_state = state.get("env_state") or {}
    env_context = ""
    if env_state:
        soil = env_state.get("soil", {})
        weather = env_state.get("weather", {})
        gps = env_state.get("gps", {})
        farm_info = env_state.get("farm_info", {})
        farm_context = ""
        if farm_info:
            farm_context = (
                f"  Farm      : {farm_info.get('plot_name') or 'N/A'}"
                f" (code: {farm_info.get('plot_code') or 'N/A'})"
                f", area={farm_info.get('plot_area_m2') or 'N/A'}m²"
                f", address={farm_info.get('plot_address') or 'N/A'}\n"
                f"  Zone      : {farm_info.get('zone_name') or 'N/A'}"
                f" (code: {farm_info.get('zone_code') or 'N/A'})"
                f", area={farm_info.get('zone_area_m2') or 'N/A'}m²"
                f", soil_type={farm_info.get('soil_type') or 'N/A'}"
                f", crop_type={farm_info.get('crop_type') or 'N/A'}\n"
            )
        env_context = f"""
Current Environmental Context (from IoT sensors - use this to adjust recommendations):
{farm_context}  Location  : lat={gps.get('latitude')}, lon={gps.get('longitude')}, altitude={gps.get('altitude_m')}m
  Soil      : pH={soil.get('ph')}, moisture={soil.get('moisture_pct')}%, temp={soil.get('temperature_c')}C
              N={soil.get('nitrogen_ppm')}ppm, P={soil.get('phosphorus_ppm')}ppm, K={soil.get('potassium_ppm')}ppm
  Weather   : {weather.get('air_temp_c')}C, humidity={weather.get('humidity_pct')}%, wind={weather.get('wind_speed_kmh')}km/h
              Rain last 7d={weather.get('rainfall_mm_last_7d')}mm, Rain forecast 24h={weather.get('forecast_rain_24h')}
  Note: If rain is forecast in the next 24h, DO NOT schedule spray events for today - reschedule to after the rain.
        If humidity > 80%, increase scouting frequency and adjust fungal prevention timing.
"""

    system_prompt = f"""You are an Expert Agronomist Planner for coffee cultivation operations.
Your task is to create a precise, actionable general plan that can include:
- routine care (irrigation, nutrition, scouting),
- treatment actions (including pesticide/fungicide when truly needed),
- pruning/denoting/stumping/canopy-reset tasks,
- field maintenance and lifecycle events.

The plan must match user intent. Do NOT force disease treatment when the user only asks for care/maintenance.
{env_context}
{density_guidance}

EventType values and when to use them:
  Routine Care:
    IRRIGATION         - any watering task
    NUTRITION          - fertiliser / soil amendment (NOT medicine)
    WEED_CONTROL       - weeding, cleanup, ground management, herbicide when justified
    PRUNING            - structural/sanitary pruning and denoting/stumping/canopy reset tasks
  Health & Medical:
    SCOUTING           - routine field inspection / monitoring
    DISEASE_DETECTED   - visual confirmation of disease/pest (only when relevant)
    TREATMENT_APPLICATION - curative spray, biocontrol agent, fungicide, etc.
    QUARANTINE         - isolate infected plant to prevent spread
    HEALTH_RECOVERY    - end-of-treatment health check (only when treatment path is used)
  Growth & Lifecycle:
    PHENOLOGY          - record a growth stage milestone
    REPOT              - transplant to larger container or field
    HARVEST            - cherry picking event

Rules:
- Build a plan type that matches user intent: CARE / TREATMENT / MIXED.
- CARE plans should emphasize IRRIGATION, NUTRITION, SCOUTING, WEED_CONTROL, PRUNING.
- TREATMENT plans should include DISEASE_DETECTED and HEALTH_RECOVERY when disease/pest context is explicit.
- MIXED plans may combine routine care with treatment and pruning/denoting actions.
- Do not include DISEASE_DETECTED or HEALTH_RECOVERY if there is no disease/pest context.
- Calculate `daysFromNow` to distribute events EVENLY across the full horizon.
  For a 30-day plan use offsets like 0, 7, 14, 21, 28 for weekly events;
  for bi-weekly events use 0, 14, 28; never cluster all events at the beginning.
- Each recurring event type (IRRIGATION, NUTRITION, SCOUTING, etc.) must appear
  as MULTIPLE separate PlantEvent entries — one entry per occurrence, not one entry
  with a long description. E.g. four weekly irrigations = four IRRIGATION events.
- Be specific in `description`: include exact dosage, concentration, PPE, and method when chemical treatment is involved.
- Do NOT invent or guess a `plantId`. Leave `plantId` as null — it will be injected from the caller's request context.
- Generate a concise, descriptive `planName` that conveys the primary objective and scope.
    Examples: "Coffee Leaf Rust Treatment Plan", "Post-Harvest Pruning & Fertilisation Plan",
              "Phytophthora Root Rot Recovery — 4-Week Protocol", "Routine Care Plan — Dry Season".
- This schema requires `diseaseName`. If this is NOT a disease-specific request, set:
    diseaseName = "General Plant Care"
- For TREATMENT_APPLICATION events, you MUST populate these dedicated fields:
    * `phiDays` -> integer - the Pre-Harvest Interval in days from the product label
                        (e.g. 7, 14, 21). Set to null for all other event types.
    * `ppeRequired` -> string - full PPE list required for the application
                        (e.g. "Respirator, chemical-resistant gloves, rubber boots, protective coveralls").
                        Set to null for all other event types.
    * `mrlNote` -> string - required ONLY when produce targets export/premium retail
                        channels or user explicitly asks for MRL/compliance details.
                        If EXPORT_CONTEXT_DETECTED is false, set to null unless a warning is still useful.
- Provide a realistic `estimated_cost` covering chemicals, tools, and effort required.
  (e.g., "$10-$20" or "500,000 VND").
- For pruning/denoting tasks, include clear cutback intensity and sanitation workflow
  (tool disinfection, debris handling, follow-up scouting).
- For events with multiple distinct steps, populate the `tasks` list to break them down.
  Each task must have a short `title`, an optional `description`, and an `order` starting at 0.
  Assign tasks when it genuinely helps (e.g. TREATMENT_APPLICATION: mix → spray → clean sprayer;
  NUTRITION: prepare mix → apply to root zone → optionally foliar spray;
  IRRIGATION: check soil moisture → water → record amount;
  PRUNING: mark branches → cut → disinfect tools → dispose of debris;
  SCOUTING: inspect leaves → check trunk → record findings).
  Even routine events like IRRIGATION and SCOUTING benefit from 2–3 tasks.
  Do NOT leave tasks as null for NUTRITION, IRRIGATION, SCOUTING, WEED_CONTROL, or PRUNING events.

EXPORT_CONTEXT_DETECTED={export_context}

CHEMICAL APPLICATION SAFETY - MANDATORY for every TREATMENT_APPLICATION event:
When the treatment involves chemical pesticides or fungicides, the `description` field MUST
include ALL of the following (the "4 Rights" principle + regulatory requirements):

  1. RIGHT CHEMICAL:
     - Name the exact registered product and active ingredient.
     - Confirm it targets the specific pest/disease.
     - State: "Only use chemicals on the approved list - banned substances are prohibited."

  2. RIGHT TIME:
     - Specify the optimal window.

  3. RIGHT DOSAGE:
     - Give the exact concentration from the label.
     - Warn: "Do NOT under-dose (causes resistance) or over-dose (phytotoxicity / MRL violation)."

  4. RIGHT METHOD:
     - Describe spraying technique and target zones.

  5. PRE-HARVEST INTERVAL (PHI):
     - State the mandatory withdrawal period before harvest.

  6. PERSONAL PROTECTIVE EQUIPMENT (PPE):
     - Include full PPE requirements.

  7. MAXIMUM RESIDUE LIMIT (MRL):
     - If produce targets export or premium retail, include explicit MRL compliance notes.{safety_note}"""

    prompt = f"""{system_prompt}

Agronomic Knowledge (from knowledge base):
{docs_context}{web_context}

User Query:
{question}

Generate the complete Plan object now.
IMPORTANT: The entire output, including plan descriptions, notes, and ALL events MUST be detailed in the following language: {language}."""

    try:
        plan: Plan = structured_llm.invoke(prompt)
    except Exception as e:
        logger.error("[GENERAL PLANNER] Structured output failed: %s", e, exc_info=True)
        return {"generated_plan": None, "plant_id": None}

    final_plan = plan.dict()

    schedule = final_plan.get("schedule") or []
    if not schedule:
        logger.warning("[GENERAL PLANNER] Empty schedule generated - skipping plan")
        return {"generated_plan": None, "plant_id": None}

    # Sort by daysFromNow so the schedule is chronological.
    # Absolute dates (calculatedStartDate/calculatedEndDate) are computed
    # at apply time by the plant-management-service consumer.
    schedule.sort(key=lambda e: e["daysFromNow"])

    if web_results:
        final_plan["source"] = "websearch"
    elif documents:
        final_plan["source"] = "documents"

    if not final_plan.get("diseaseName"):
        final_plan["diseaseName"] = "General Plant Care"

    if not final_plan.get("urgency"):
        final_plan["urgency"] = "NORMAL"

    # Always use the caller-supplied plant_id from the request context.
    # Never trust an LLM-hallucinated value — set to None if nothing was passed.
    request_plant_id = state.get("plant_id") or None
    final_plan["plantId"] = request_plant_id

    generation_lines = [
        f"[{ev['eventType']}] {ev['note']}: {ev['description']}"
        for ev in schedule
    ]
    generation_text = "\n\n".join(generation_lines)

    logger.info(
        "[GENERAL PLANNER] Plan generated for plantId=%s | objective=%s | source=%s | events=%d | confidence=%.2f",
        final_plan.get("plantId"),
        final_plan.get("diseaseName"),
        final_plan.get("source"),
        len(schedule),
        final_plan.get("confidenceScore", 0.0),
    )

    return {
        "generated_plan": final_plan,
        "plant_id": final_plan.get("plantId"),
        "generation": generation_text,
        "messages": [AIMessage(content=generation_text)],
    }


# Backward-compatible alias for existing imports.
planner = planner
