package com.leafy.fileservice.service.file;

import com.leafy.fileservice.dto.request.FileUpdateRequest;
import com.leafy.fileservice.dto.request.FileUploadRequest;
import com.leafy.fileservice.dto.response.FileDetailsResponse;
import com.leafy.fileservice.dto.response.FileResponse;
import com.leafy.fileservice.model.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for File management
 */
public interface FileService {

    /**
     * Create a new file metadata record
     *
     * @param request the file upload request
     * @return the created file response
     */
    FileResponse createFile(FileUploadRequest request);

    /**
     * Update existing file metadata
     *
     * @param fileId  the file ID
     * @param request the file update request
     * @return the updated file response
     */
    FileResponse updateFile(String fileId, FileUpdateRequest request);

    /**
     * Get file by ID
     *
     * @param fileId the file ID
     * @return the file response
     */
    FileResponse getFileById(String fileId);

    /**
     * Get file details by ID (includes all audit fields)
     *
     * @param fileId the file ID
     * @return the file details response
     */
    FileDetailsResponse getFileDetailsById(String fileId);

    /**
     * Get file entity by ID
     *
     * @param fileId the file ID
     * @return the file entity
     */
    File getFileEntityById(String fileId);

    /**
     * Get file by S3 key
     *
     * @param s3Key the S3 key
     * @return the file response
     */
    FileResponse getFileByS3Key(String s3Key);

    /**
     * Get all files with pagination
     *
     * @param pageable pagination information
     * @return page of file responses
     */
    Page<FileResponse> getAllFiles(Pageable pageable);

    /**
     * Get all active files with pagination
     *
     * @param pageable pagination information
     * @return page of active file responses
     */
    Page<FileResponse> getActiveFiles(Pageable pageable);

    /**
     * Get files by uploader with pagination
     *
     * @param uploadedBy the user ID who uploaded the file
     * @param pageable   pagination information
     * @return page of file responses
     */
    Page<FileResponse> getFilesByUploadedBy(String uploadedBy, Pageable pageable);

    /**
     * Search files by filename
     *
     * @param searchTerm the search term
     * @param pageable   pagination information
     * @return page of file responses
     */
    Page<FileResponse> searchFiles(String searchTerm, Pageable pageable);

    /**
     * Delete file by ID (soft delete)
     *
     * @param fileId the file ID
     */
    void deleteFile(String fileId);

    /**
     * Activate file
     *
     * @param fileId the file ID
     * @return the updated file response
     */
    FileResponse activateFile(String fileId);

    /**
     * Deactivate file
     *
     * @param fileId the file ID
     * @return the updated file response
     */
    FileResponse deactivateFile(String fileId);

    /**
     * Check if S3 key exists
     *
     * @param s3Key the S3 key
     * @return true if exists, false otherwise
     */
    boolean existsByS3Key(String s3Key);
}
