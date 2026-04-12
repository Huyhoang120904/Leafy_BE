from typing import Generic, Optional, TypeVar
from pydantic import BaseModel

from app.i18n import get_message

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    """
    Standard response envelope used by all controllers.
    Mirrors the auth-service / disease-detection-service pattern.
    """

    code: int = 200
    message: str = get_message("response.success", "en")
    result: Optional[T] = None

    @classmethod
    def success(
        cls,
        result: T = None,
        message: Optional[str] = None,
        locale: str = "en",
    ) -> "ApiResponse[T]":
        return cls(code=200, message=message or get_message("response.success", locale), result=result)

    @classmethod
    def error(cls, code: int, message: str) -> "ApiResponse[None]":
        return cls(code=code, message=message, result=None)
