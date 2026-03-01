import io
import time
import logging
import numpy as np
from PIL import Image
from fastapi import UploadFile

from app.config.config import config
from app.inference.ai_model_inference import AIModelInference
from app.dto.prediction.prediction_dto import PredictionResponse, PredictionResult
from app.exceptions.app_exception import AppException
from app.exceptions.error_code import ErrorCode

logger = logging.getLogger(__name__)

class PredictionService:
    @staticmethod
    def preprocess_image(image_bytes: bytes) -> np.ndarray:
        try:
            image = Image.open(io.BytesIO(image_bytes))
            if image.mode != 'RGB':
                image = image.convert('RGB')
            image = image.resize(config.MODEL_INPUT_SIZE, Image.Resampling.BILINEAR)
            img_array = np.array(image, dtype=np.float32)
            img_array = np.expand_dims(img_array, axis=0)
            return img_array
        except Exception as e:
            logger.error(f"Image preprocessing failed: {e}")
            raise AppException(ErrorCode.BAD_REQUEST, f"Invalid image format: {str(e)}")

    @staticmethod
    def validate_file(file: UploadFile) -> bytes:
        if not file.content_type or not file.content_type.startswith('image/'):
             raise AppException(ErrorCode.INVALID_FILE_TYPE)
        
        image_bytes = file.file.read()
        if len(image_bytes) == 0:
             raise AppException(ErrorCode.EMPTY_FILE)
        return image_bytes

    @classmethod
    def predict(cls, file: UploadFile, model) -> dict:
        start_time = time.time()
        
        try:
             image_bytes = cls.validate_file(file)
             image_array = cls.preprocess_image(image_bytes)
             
             if not model:
                  raise AppException(ErrorCode.MODEL_NOT_LOADED)

             predictions = AIModelInference.perform_inference(model, image_array)
             processing_time = (time.time() - start_time) * 1000
             
             prediction_results = [PredictionResult(**p) for p in predictions]
             
             response = PredictionResponse(
                  predictions=prediction_results,
                  modelName=config.MODEL_NAME,
                  processingTimeMs=round(processing_time, 2)
             )
             return response.model_dump()
        finally:
             file.file.close()

    @classmethod
    def predict_tflite(cls, file: UploadFile, tflite_interpreter) -> dict:
         start_time = time.time()
         
         try:
             image_bytes = cls.validate_file(file)
             image_array = cls.preprocess_image(image_bytes)
             
             if not tflite_interpreter:
                  raise AppException(ErrorCode.MODEL_NOT_LOADED)

             predictions = AIModelInference.perform_tflite_inference(tflite_interpreter, image_array)
             processing_time = (time.time() - start_time) * 1000
             
             prediction_results = [PredictionResult(**p) for p in predictions]
             
             response = PredictionResponse(
                  predictions=prediction_results,
                  modelName=f"{config.MODEL_NAME}-TFLite",
                  processingTimeMs=round(processing_time, 2)
             )
             return response.model_dump()
         finally:
             file.file.close()
