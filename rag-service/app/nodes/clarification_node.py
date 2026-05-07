"""
Clarification Check Node

Intercepts agricultural queries BEFORE expensive retrieval/search steps.
If the question is too vague to produce a useful response, Leafy proactively
asks the user for the missing information instead of generating a low-quality
or irrelevant answer.

What triggers clarification:
  - No crop/plant type mentioned at all AND intent is ambiguous
  - Treatment/disease request with no symptom or disease description
  - Completely generic request (e.g. "lập kế hoạch cho cây" with no crop)

What does NOT trigger clarification:
  - Any mention of coffee / cà phê / robusta / arabica / a crop name
  - Routine care plan requests (no disease context required)
  - General agriculture questions even if brief
"""

import logging
import re
from typing import List

from langchain_core.messages import AIMessage, HumanMessage
from pydantic import BaseModel, Field

from app.agents.rag_state import GraphState
from app.core.ai_providers import get_gemini_flash

# Mirror the router's planning-intent pattern so we can tag questions that
# need clarification but also carry a planning intent to the next turn.
_PLANNING_INTENT_RE = re.compile(
    r"\b(treatment plan|recovery plan|action plan|care plan|"
    r"maintenance plan|pruning plan|step-by-step|what steps|schedule|"
    r"spray calendar|care schedule|irrigation schedule|fertilizer schedule|"
    r"give me a plan|plan for|"
    r"kế hoạch|lịch trình|lịch phun|phác đồ|quy trình xử lý|"
    r"lập kế hoạch|lịch chăm sóc|lịch bón phân|chăm sóc)\b",
    re.IGNORECASE,
)

# How many prior message pairs to send to the clarification LLM for context.
_HISTORY_PAIRS = 3

logger = logging.getLogger(__name__)


# ── Structured output schema ───────────────────────────────────────────────────

class ClarificationDecision(BaseModel):
    """LLM decision on whether a user question needs more detail."""

    needs_clarification: bool = Field(
        description=(
            "True ONLY if the question is so vague that generating a useful answer "
            "is impossible without more information. False for most questions."
        )
    )
    clarification_question: str = Field(
        description=(
            "The short, friendly follow-up question to send to the user. "
            "Ask only for the single most critical missing piece. "
            "Empty string when needs_clarification=False."
        )
    )
    missing_info: List[str] = Field(
        description="Brief list of what is missing or ambiguous. Empty list when not needed."
    )


# ── System prompt ──────────────────────────────────────────────────────────────

_SYSTEM_PROMPT = """\
You are a quality-check assistant for an AI agronomist named Leafy that advises \
Vietnamese coffee farmers.

Your ONLY job: decide whether the user's question has enough context to generate \
a specific, actionable agricultural response.

IMPORTANT: You will receive the recent conversation history BEFORE the user's \
current message. If the crop type, plant, or relevant context was already \
established in any prior message, treat the current question as SUFFICIENT — \
do NOT ask again.

--- SUFFICIENT (needs_clarification = false) ---
A question is sufficient when ANY of these are true:
• It names a specific crop or plant (coffee, cà phê, robusta, arabica, lúa, tiêu, …)
• It describes observable symptoms (lá vàng, rụng lá, đốm nâu, héo, rỉ sắt, …)
• It asks for a routine care/schedule plan — no disease context required
• It asks a general agricultural concept question (fertiliser ratio, pruning timing, …)
• The conversation history already established the crop / context for this request
• It is a follow-up question referencing prior context ("cây đó", "bệnh này", "kế hoạch trên")

--- NEEDS CLARIFICATION (needs_clarification = true) ---
Only flag as needing clarification when ALL of these apply:
• No crop or plant type can be inferred from EITHER the current message OR the recent history
• AND the request is for a personalised plan or specific treatment (not a general question)
• AND answering without knowing the crop would produce a meaningless generic response

EXAMPLES that do NOT need clarification:
  "kế hoạch chăm sóc cà phê 1 tháng"         → false (coffee mentioned)
  "cây cà phê bị gì"                           → false (coffee + symptom implied)
  "how do I treat leaf rust on coffee"         → false (clear crop + disease)
  "lập lịch tưới cho vườn"                    → false (irrigation schedule is general enough)
  "cây bị vàng lá phải làm gì"                → false (clear symptom, can advise)
  "lập kế hoạch cho tôi" [history has cà phê] → false (crop known from history)

EXAMPLES that DO need clarification (no prior context):
  "lập kế hoạch cho cây của tôi"               → true (no crop in message or history)
  "cây nhà tôi bị bệnh, điều trị thế nào"     → true (no crop, no symptom, treatment needed)

If needs_clarification=true, write clarification_question as a SINGLE SHORT sentence \
in {language}. Ask only for the most critical missing piece. Do not explain why you need it.
Example: "Bạn đang trồng loại cây gì và triệu chứng cụ thể là gì?"

Respond with valid JSON matching the schema. Be conservative — lean toward false.
"""


# ── Node functions ─────────────────────────────────────────────────────────────

def check_clarification(state: GraphState) -> dict:
    """
    Determine whether the user's question needs more detail before processing.

    Uses Gemini Flash structured output for a fast, cheap decision.
    Falls back to 'proceed' (no clarification) on any LLM error to avoid
    blocking the pipeline.

    Args:
        state: Current graph state with 'question' and 'language'.

    Returns:
        Dict with:
          - needs_clarification (bool)
          - clarification_question (str, empty if not needed)
    """
    question = state.get("question", "").strip()
    language = state.get("language") or "Vietnamese"

    logger.info("[CLARIFICATION] Checking question sufficiency: '%.80s'", question)

    # ── Detect planning intent in the current question ──────────────────────
    # This is stored in state so the router can force the planning path on the
    # NEXT turn even when the user's reply (e.g. "cây cà phê") has no planning
    # keywords of its own.
    has_planning_intent = bool(_PLANNING_INTENT_RE.search(question))

    # Also carry forward a pending planning intent from the PREVIOUS turn
    # (e.g. if last turn asked clarification and user is now providing the crop).
    prior_pending = state.get("pending_planning_intent", False)

    # ── Build recent conversation history for context ───────────────────────
    # Include the last _HISTORY_PAIRS * 2 messages (excluding the current human
    # message which is `question`) so the LLM can see whether the crop was
    # already established in prior turns.
    all_messages = state.get("messages", [])
    # Exclude the last message (current HumanMessage already captured in question)
    history = all_messages[:-1] if len(all_messages) > 1 else []
    history = history[-((_HISTORY_PAIRS) * 2):]

    # Convert to simple role/content dicts for the LLM call
    history_msgs = []
    for msg in history:
        if isinstance(msg, HumanMessage):
            # Truncate very long pasted messages to avoid bloating the prompt
            content = msg.content[:400] + "…" if len(msg.content) > 400 else msg.content
            history_msgs.append({"role": "user", "content": content})
        else:
            content = msg.content[:200] + "…" if len(msg.content) > 200 else msg.content
            history_msgs.append({"role": "assistant", "content": content})

    try:
        llm = get_gemini_flash(temperature=0).with_structured_output(ClarificationDecision)
        messages = (
            [{"role": "system", "content": _SYSTEM_PROMPT.format(language=language)}]
            + history_msgs
            + [{"role": "user", "content": question}]
        )
        decision: ClarificationDecision = llm.invoke(messages)

        if decision.needs_clarification:
            logger.info(
                "[CLARIFICATION] Insufficient — missing: %s | asking: '%s'",
                decision.missing_info,
                decision.clarification_question,
            )
            return {
                "needs_clarification": True,
                "clarification_question": decision.clarification_question,
                # Carry planning intent forward so the router forces the planning
                # path once the user provides the missing crop/context.
                "pending_planning_intent": has_planning_intent or prior_pending,
            }
        else:
            logger.info("[CLARIFICATION] Question is sufficient → proceed to pipeline")
            return {
                "needs_clarification": False,
                "clarification_question": "",
                # Preserve any pending planning intent so the router still picks
                # it up (e.g. user replied "cây cà phê" — no planning keywords).
                "pending_planning_intent": has_planning_intent or prior_pending,
            }

    except Exception as exc:
        logger.warning("[CLARIFICATION] LLM error, skipping check: %s", exc)
        return {
            "needs_clarification": False,
            "clarification_question": "",
            "pending_planning_intent": has_planning_intent or prior_pending,
        }


def clarification_response(state: GraphState) -> dict:
    """
    Return the clarification question as the pipeline's final generation.

    Mirrors the pattern used by direct_response: pushes an AIMessage so the
    multi-turn history remains consistent, and marks safety_passed=True so
    the graph can route cleanly to END.

    Args:
        state: Current graph state with 'clarification_question'.

    Returns:
        Dict with 'generation', appended AIMessage, and safety metadata.
    """
    message = state.get("clarification_question") or (
        "Bạn có thể cho tôi biết thêm về loại cây trồng và vấn đề bạn đang gặp phải không?"
    )
    logger.info("[CLARIFICATION] Sending clarification prompt to user")

    return {
        "generation": message,
        "messages": [AIMessage(content=message)],
        "safety_passed": True,
        "safety_issues": [],
    }
