package com.leafy.fileservice.controller;

import com.leafy.common.dto.ApiResponse;
import com.leafy.fileservice.dto.request.FileUpdateRequest;
import com.leafy.fileservice.dto.request.FileUploadRequest;
import com.leafy.fileservice.dto.response.FileDetailsResponse;
import com.leafy.fileservice.dto.response.FileResponse;
import com.leafy.fileservice.service.s3.S3Service;
import com.leafy.fileservice.service.file.FileService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for File management
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileController {

    FileService fileService;
    S3Service s3Service;

    /**
     * Upload file to S3 and create metadata
     *
     * @param filePart   the file part from multipart request
     * @param uploadedBy the user ID who uploaded the file
     * @return Mono containing the created file response
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<ApiResponse<FileResponse>>> uploadFile(
            @RequestPart("file") FilePart filePart,
            @RequestParam String uploadedBy) {
        log.info("POST /files/upload - Uploading file: {}", filePart.filename());

        return s3Service.uploadFile(filePart)
                .flatMap(s3Key -> {
                    // Create file metadata
                    FileUploadRequest request = FileUploadRequest.builder()
                            .s3Key(s3Key)
                            .originalFileName(filePart.filename())
                            .contentType(filePart.headers().getContentType() != null
                                    ? filePart.headers().getContentType().toString()
                                    : "application/octet-stream")
                            .fileSize(filePart.headers().getContentLength())
                            .uploadedBy(uploadedBy)
                            .build();

                    FileResponse response = fileService.createFile(request);
                    log.info("File uploaded and metadata created: fileId={}, s3Key={}", response.getId(), s3Key);

                    return Mono.just(ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponse.success(response)));
                })
                .onErrorResume(error -> {
                    log.error("Error uploading file: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(9999, "Failed to upload file", null)));
                });
    }

    /**
     * Download file from S3 by file ID
     *
     * @param fileId the file ID
     * @return Mono containing the file data
     */
    @GetMapping("/download/{fileId}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFileById(@PathVariable String fileId) {
        log.info("GET /files/download/{} - Downloading file", fileId);

        return Mono.fromCallable(() -> {
            // Get file metadata
            FileResponse fileResponse = fileService.getFileById(fileId);
            String s3Key = fileResponse.getS3Key();

            // Download from S3
            Flux<DataBuffer> fileData = s3Service.downloadFile(s3Key);

            return ResponseEntity.ok()
                    .header("Content-Type", fileResponse.getContentType())
                    .header("Content-Disposition",
                            "attachment; filename=\"" + fileResponse.getOriginalFileName() + "\"")
                    .body(fileData);
        }).onErrorResume(error -> {
            log.error("Error downloading file: {}", error.getMessage(), error);
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        });
    }

    /**
     * Download file from S3 by S3 key
     *
     * @param s3Key the S3 key
     * @return Mono containing the file data
     */
    @GetMapping("/download/s3-key/{s3Key}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFileByS3Key(@PathVariable String s3Key) {
        log.info("GET /files/download/s3-key/{} - Downloading file", s3Key);

        return Mono.fromCallable(() -> {
            Flux<DataBuffer> fileData = s3Service.downloadFile(s3Key);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"" + s3Key + "\"")
                    .body(fileData);
        }).onErrorResume(error -> {
            log.error("Error downloading file: {}", error.getMessage(), error);
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        });
    }

    /**
     * Generate presigned URL for file download
     *
     * @param fileId            the file ID
     * @param expirationMinutes expiration time in minutes (default: 60)
     * @return Mono containing the presigned URL
     */
    @GetMapping("/presigned-url/{fileId}")
    public Mono<ResponseEntity<ApiResponse<String>>> generatePresignedUrl(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "60") int expirationMinutes) {
        log.info("GET /files/presigned-url/{} - Generating presigned URL", fileId);

        return Mono.fromCallable(() -> fileService.getFileById(fileId))
                .flatMap(fileResponse -> s3Service.generatePresignedUrl(fileResponse.getS3Key(), expirationMinutes)
                        .map(url -> {
                            log.info("Presigned URL generated for fileId={}", fileId);
                            return ResponseEntity.ok(ApiResponse.success(url));
                        }))
                .onErrorResume(error -> {
                    log.error("Error generating presigned URL: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(9999, "Failed to generate presigned URL", null)));
                });
    }

    /**
     * Create file metadata record
     *
     * @param request the file upload request
     * @return the created file response
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FileResponse>> createFile(@Valid @RequestBody FileUploadRequest request) {
        log.info("POST /files - Creating file metadata");
        FileResponse response = fileService.createFile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * Update file metadata
     *
     * @param fileId  the file ID
     * @param request the file update request
     * @return the updated file response
     */
    @PutMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponse>> updateFile(
            @PathVariable String fileId,
            @Valid @RequestBody FileUpdateRequest request) {
        log.info("PUT /files/{} - Updating file metadata", fileId);
        FileResponse response = fileService.updateFile(fileId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get file by ID
     *
     * @param fileId the file ID
     * @return the file response
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponse>> getFileById(@PathVariable String fileId) {
        log.info("GET /files/{} - Getting file by ID", fileId);
        FileResponse response = fileService.getFileById(fileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get file details by ID (includes all audit fields)
     *
     * @param fileId the file ID
     * @return the file details response
     */
    @GetMapping("/{fileId}/details")
    public ResponseEntity<ApiResponse<FileDetailsResponse>> getFileDetailsById(@PathVariable String fileId) {
        log.info("GET /files/{}/details - Getting file details by ID", fileId);
        FileDetailsResponse response = fileService.getFileDetailsById(fileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get file by S3 key
     *
     * @param s3Key the S3 key
     * @return the file response
     */
    @GetMapping("/s3-key/{s3Key}")
    public ResponseEntity<ApiResponse<FileResponse>> getFileByS3Key(@PathVariable String s3Key) {
        log.info("GET /files/s3-key/{} - Getting file by S3 key", s3Key);
        FileResponse response = fileService.getFileByS3Key(s3Key);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all files with pagination and sorting
     *
     * @param page    page number (default: 0)
     * @param size    page size (default: 20)
     * @param sortBy  field to sort by (default: createdAt)
     * @param sortDir sort direction (default: DESC)
     * @return page of file responses
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FileResponse>>> getAllFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("GET /files - Getting all files with pagination");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FileResponse> response = fileService.getAllFiles(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all active files with pagination
     *
     * @param page    page number (default: 0)
     * @param size    page size (default: 20)
     * @param sortBy  field to sort by (default: createdAt)
     * @param sortDir sort direction (default: DESC)
     * @return page of active file responses
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<FileResponse>>> getActiveFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("GET /files/active - Getting all active files with pagination");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FileResponse> response = fileService.getActiveFiles(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get files by uploader with pagination
     *
     * @param uploadedBy the user ID who uploaded the file
     * @param page       page number (default: 0)
     * @param size       page size (default: 20)
     * @param sortBy     field to sort by (default: createdAt)
     * @param sortDir    sort direction (default: DESC)
     * @return page of file responses
     */
    @GetMapping("/user/{uploadedBy}")
    public ResponseEntity<ApiResponse<Page<FileResponse>>> getFilesByUploadedBy(
            @PathVariable String uploadedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("GET /files/user/{} - Getting files by uploader with pagination", uploadedBy);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FileResponse> response = fileService.getFilesByUploadedBy(uploadedBy, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Search files by filename
     *
     * @param searchTerm the search term
     * @param page       page number (default: 0)
     * @param size       page size (default: 20)
     * @param sortBy     field to sort by (default: createdAt)
     * @param sortDir    sort direction (default: DESC)
     * @return page of file responses
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<FileResponse>>> searchFiles(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("GET /files/search - Searching files with term: {}", searchTerm);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FileResponse> response = fileService.searchFiles(searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete (deactivate) file by ID
     *
     * @param fileId the file ID
     * @return success response
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String fileId) {
        log.info("DELETE /files/{} - Deleting file", fileId);
        fileService.deleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    /**
     * Activate file
     *
     * @param fileId the file ID
     * @return the activated file response
     */
    @PatchMapping("/{fileId}/activate")
    public ResponseEntity<ApiResponse<FileResponse>> activateFile(@PathVariable String fileId) {
        log.info("PATCH /files/{}/activate - Activating file", fileId);
        FileResponse response = fileService.activateFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Deactivate file
     *
     * @param fileId the file ID
     * @return the deactivated file response
     */
    @PatchMapping("/{fileId}/deactivate")
    public ResponseEntity<ApiResponse<FileResponse>> deactivateFile(@PathVariable String fileId) {
        log.info("PATCH /files/{}/deactivate - Deactivating file", fileId);
        FileResponse response = fileService.deactivateFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Check if S3 key exists
     *
     * @param s3Key the S3 key to check
     * @return true if exists, false otherwise
     */
    @GetMapping("/check-s3-key")
    public ResponseEntity<ApiResponse<Boolean>> checkS3KeyExists(@RequestParam String s3Key) {
        log.info("GET /files/check-s3-key - Checking if S3 key exists: {}", s3Key);
        boolean exists = fileService.existsByS3Key(s3Key);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}
