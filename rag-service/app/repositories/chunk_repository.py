"""
Chunk Repository — MongoDB-backed storage for document chunks.

Mirrors every chunk that gets upserted into Qdrant so there is an
auditable, queryable copy of the ingested content in MongoDB.
"""

import logging
import uuid
from datetime import datetime
from typing import Any, Dict, List, Optional

import pymongo
from pymongo import MongoClient

from app.config.settings import settings

logger = logging.getLogger(__name__)

_COLLECTION_NAME = "document_chunks"


class ChunkRepository:
    """Singleton that manages the ``document_chunks`` MongoDB collection."""

    _instance: Optional["ChunkRepository"] = None

    def __new__(cls) -> "ChunkRepository":
        if cls._instance is None:
            instance = super().__new__(cls)
            try:
                instance._initialize()
                cls._instance = instance
            except Exception as exc:
                logger.error("ChunkRepository failed to initialize: %s", exc)
                cls._instance = None
                raise
        return cls._instance

    # ─────────────────────────────────────────────────────────────────────────

    def _initialize(self) -> None:
        client: MongoClient = MongoClient(settings.MONGODB_URI)
        db = client[settings.MONGODB_DATABASE_RAG]
        self._collection = db[_COLLECTION_NAME]

        # Indexes ─────────────────────────────────────────────────────────────
        # Fast lookup by document hash (all chunks from the same source file)
        self._collection.create_index(
            [("document_id", pymongo.ASCENDING)],
        )
        # Quick duplicate detection
        self._collection.create_index(
            [("chunk_id", pymongo.ASCENDING)],
            unique=True,
        )
        # Filtering by section for analytics / debugging
        self._collection.create_index(
            [("metadata.section", pymongo.ASCENDING)],
        )

        logger.info("ChunkRepository initialized — collection: %s", _COLLECTION_NAME)

    # ─────────────────────────────────────────────────────────────────────────

    def store_chunks(
        self,
        document_id: str,
        chunks: List[Dict[str, Any]],
        source_file: str,
    ) -> int:
        """
        Persist a batch of chunks to MongoDB.

        Parameters
        ----------
        document_id : str
            The file hash (SHA-256) that uniquely identifies the source document.
        chunks : list[dict]
            Each dict must contain ``text`` (str) and ``metadata`` (dict).
        source_file : str
            Original filename for human reference.

        Returns
        -------
        int
            Number of chunks successfully inserted.
        """
        if not chunks:
            return 0

        docs = []
        for i, chunk in enumerate(chunks):
            docs.append(
                {
                    "document_id": document_id,
                    "chunk_id": str(uuid.uuid4()),
                    "chunk_index": i,
                    "text": chunk["text"],
                    "metadata": {
                        **chunk.get("metadata", {}),
                        "source_file": source_file,
                    },
                    "created_at": datetime.utcnow(),
                }
            )

        try:
            result = self._collection.insert_many(docs, ordered=False)
            inserted = len(result.inserted_ids)
            logger.info(
                "Stored %d / %d chunks for document_id=%s",
                inserted,
                len(docs),
                document_id,
            )
            return inserted
        except pymongo.errors.BulkWriteError as bwe:
            inserted = bwe.details.get("nInserted", 0)
            logger.warning(
                "BulkWriteError storing chunks for document_id=%s — %d inserted, errors: %s",
                document_id,
                inserted,
                bwe.details.get("writeErrors", []),
            )
            return inserted

    def delete_by_document_id(self, document_id: str) -> int:
        """Remove all chunks belonging to a specific document."""
        result = self._collection.delete_many({"document_id": document_id})
        logger.info(
            "Deleted %d chunks for document_id=%s",
            result.deleted_count,
            document_id,
        )
        return result.deleted_count

    def count_by_document_id(self, document_id: str) -> int:
        """Return the number of chunks stored for a given document."""
        return self._collection.count_documents({"document_id": document_id})

    def find_by_document_id(
        self, document_id: str, section: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Retrieve chunks for a document, optionally filtered by section.
        """
        query: Dict[str, Any] = {"document_id": document_id}
        if section:
            query["metadata.section"] = section
        return list(
            self._collection.find(query, {"_id": 0}).sort("chunk_index", pymongo.ASCENDING)
        )


def get_chunk_repository() -> ChunkRepository:
    """Global singleton accessor."""
    return ChunkRepository()
