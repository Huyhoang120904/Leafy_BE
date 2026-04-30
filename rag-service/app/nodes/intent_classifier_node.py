"""Intent Routing Classifier Node.

Fast-path keyword detection + LLM classification to route direct messages away
from the full RAG pipeline. Agricultural queries proceed through the normal
pipeline.

Decision:
    - "direct"            -> direct node -> END (no retrieval, no safety audit)
    - "agriculture_query" -> env_state -> full RAG pipeline
"""

import re
import logging

from pydantic import BaseModel, Field

from app.agents.rag_state import GraphState
from app.core.ai_providers import get_gemini_flash

logger = logging.getLogger(__name__)

# ─────────────────────────────────────────────────────────────────────────────
# Keyword patterns (no LLM cost)
# ─────────────────────────────────────────────────────────────────────────────

_GREETING_RE = re.compile(
    r"^(hi|hello|hey|xin chào|chào|howdy|"
    r"good\s*(morning|afternoon|evening|day|evening)|"
    r"hola|bonjour|ciao|yo|sup|what'?s\s*up|whats?\s*up|hiya|greetings?)[\W]*$",
    re.IGNORECASE,
)

_DIRECT_SIGNAL_RE = re.compile(
    r"\b(how are you|how'?re you|bạn (có )?khỏe (không|ko)|"
    r"you'?re (great|awesome|amazing|cool|nice)|"
    r"thank(s| you)|cảm ơn|thanks?|"
    r"what('?s| is) your name|who are you|bạn tên gì|bạn là ai|"
    r"tell me (a )?joke|make me (laugh|smile)|"
    r"good(bye| night)| bye |tạm biệt|see you (later|soon)|"
    r"what can you do\??|what do you know\??|can you help)\b",
    re.IGNORECASE,
)

# Strong agricultural signal — presence means we skip the LLM classifier entirely
_AGRI_SIGNAL_RE = re.compile(
    r"\b(coffee|cà phê|plant|cây|pest|sâu|bệnh|disease|fertilizer|phân bón|"
    r"harvest|thu hoạch|soil|đất|leaf|lá|root|rễ|fungicide|thuốc|spray|phun|"
    r"treatment|điều trị|irrigation|tưới|crop|mùa vụ|rust|gỉ sắt|borer|mọt|"
    r"robusta|arabica|tây nguyên|central highland|agri|nông|vườn|farm)\b",
    re.IGNORECASE,
)


class IntentClassificationDecision(BaseModel):
    """Structured classification output from the LLM."""
    intent: str = Field(
        description=(
            "Either 'agriculture_query' for any farming, plant-care, or crop question, "
            "or 'direct' for greetings, small talk, jokes, identity questions, etc."
        )
    )
    reasoning: str = Field(description="One-sentence explanation of this classification")


def classify_query_intent(state: GraphState) -> dict:
    """
    Classify user intent as 'direct' or 'agriculture_query'.

    Evaluation order (cheapest first):
    1. Agricultural keyword fast-pass → 'agriculture_query' (no LLM)
    2. Greeting / direct pattern      → 'direct'            (no LLM)
    3. Very short message (<= 5 words, no agri terms) → 'direct' (heuristic)
    4. Gemini Flash structured output for ambiguous cases

    Args:
        state: Current graph state with 'question'

    Returns:
        Dict with 'intent' set to 'direct' or 'agriculture_query'
    """
    question = state.get("question", "").strip()
    logger.info("[CLASSIFIER] Classifying intent — '%.80s'", question)

    # ── 1. Strong agricultural signals → skip LLM ──────────────────────────
    if _AGRI_SIGNAL_RE.search(question):
        logger.info("[CLASSIFIER] Agricultural signal detected → agriculture_query (fast-pass)")
        return {"intent": "agriculture_query"}

    # ── 2. Definite greeting / direct patterns → skip LLM ──────────────────
    if _GREETING_RE.match(question) or _DIRECT_SIGNAL_RE.search(question):
        logger.info("[CLASSIFIER] Direct pattern matched → direct (fast-pass)")
        return {"intent": "direct"}

    # ── 3. Very short message without agricultural terms ────────────────────
    if len(question.split()) <= 5:
        logger.info("[CLASSIFIER] Short non-agricultural message → direct (heuristic)")
        return {"intent": "direct"}

    # ── 4. LLM fallback for ambiguous messages ──────────────────────────────
    try:
        llm = get_gemini_flash(temperature=0)
        structured_llm = llm.with_structured_output(IntentClassificationDecision)

        prompt = (
            "You are a routing classifier for the Leafy Coffee Advisory AI.\n\n"
            "Classify the user message as:\n"
            "- 'agriculture_query': questions about coffee plants, farming, pests, diseases, "
            "soil, fertilisers, weather effects on crops, treatment plans, irrigation, harvest, etc.\n"
            "- 'direct': greetings, small talk, personal questions about the AI, jokes, "
            "or any conversation unrelated to agriculture or plant care.\n\n"
            f"User message: {question}\n\n"
            "Classify concisely."
        )

        result = structured_llm.invoke(prompt)
        intent = result.intent if result.intent in ("direct", "agriculture_query") else "agriculture_query"
        logger.info("[CLASSIFIER] LLM → %s (%s)", intent, result.reasoning)
        return {"intent": intent}

    except Exception as exc:
        logger.warning("[CLASSIFIER] LLM failed (%s) — defaulting to agriculture_query", exc)
        return {"intent": "agriculture_query"}


# Backward-compatible alias for existing imports.
classify_intent = classify_query_intent
