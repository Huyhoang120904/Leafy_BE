package com.leafy.fileservice.service.file;

import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.fileservice.dto.request.FileUpdateRequest;
import com.leafy.fileservice.dto.request.FileUploadRequest;
import com.leafy.fileservice.dto.response.FileDetailsResponse;
import com.leafy.fileservice.dto.response.FileResponse;
import com.leafy.fileservice.mapper.FileMapper;
import com.leafy.fileservice.model.File;
import com.leafy.fileservice.repository.FileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of FileService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileServiceImpl implements FileService {

    FileRepository fileRepository;
    FileMapper fileMapper;

    @Override
    public FileResponse createFile(FileUploadRequest request) {
        log.info("Creating file metadata with S3 key: {}", request.getS3Key());

        // Map request to entity
        File file = fileMapper.toEntity(request);

        // Save file
        File savedFile = fileRepository.save(file);
        log.info("File metadata created successfully with ID: {}", savedFile.getId());

        return fileMapper.toResponse(savedFile);
    }

    @Override
    public FileResponse updateFile(String fileId, FileUpdateRequest request) {
        log.info("Updating file metadata with ID: {}", fileId);

        // Find existing file
        File file = getFileEntityById(fileId);

        // Update file fields
        fileMapper.updateEntityFromRequest(request, file);

        // Save updated file
        File updatedFile = fileRepository.save(file);
        log.info("File metadata updated successfully with ID: {}", updatedFile.getId());

        return fileMapper.toResponse(updatedFile);
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(String fileId) {
        log.info("Getting file by ID: {}", fileId);
        File file = getFileEntityById(fileId);
        return fileMapper.toResponse(file);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDetailsResponse getFileDetailsById(String fileId) {
        log.info("Getting file details by ID: {}", fileId);
        File file = getFileEntityById(fileId);
        return fileMapper.toDetailsResponse(file);
    }

    @Override
    @Transactional(readOnly = true)
    public File getFileEntityById(String fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("File not found with ID: {}", fileId);
                    return new AppException(ErrorCode.FILE_NOT_FOUND);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileByS3Key(String s3Key) {
        log.info("Getting file by S3 key: {}", s3Key);
        File file = fileRepository.findByS3Key(s3Key)
                .orElseThrow(() -> {
                    log.error("File not found with S3 key: {}", s3Key);
                    return new AppException(ErrorCode.FILE_NOT_FOUND);
                });
        return fileMapper.toResponse(file);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> getAllFiles(Pageable pageable) {
        log.info("Getting all files with pagination");
        Page<File> files = fileRepository.findAll(pageable);
        return files.map(fileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> getActiveFiles(Pageable pageable) {
        log.info("Getting all active files with pagination");
        Page<File> files = fileRepository.findByActiveTrue(pageable);
        return files.map(fileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> getFilesByUploadedBy(String uploadedBy, Pageable pageable) {
        log.info("Getting files by uploader: {} with pagination", uploadedBy);
        Page<File> files = fileRepository.findByUploadedBy(uploadedBy, pageable);
        return files.map(fileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileResponse> searchFiles(String searchTerm, Pageable pageable) {
        log.info("Searching files with term: {}", searchTerm);
        Page<File> files = fileRepository.searchByOriginalFileName(searchTerm, pageable);
        return files.map(fileMapper::toResponse);
    }

    @Override
    public void deleteFile(String fileId) {
        log.info("Deleting (deactivating) file with ID: {}", fileId);
        File file = getFileEntityById(fileId);
        file.setActive(false);
        fileRepository.save(file);
        log.info("File deactivated successfully with ID: {}", fileId);
    }

    @Override
    public FileResponse activateFile(String fileId) {
        log.info("Activating file with ID: {}", fileId);
        File file = getFileEntityById(fileId);
        file.setActive(true);
        File activatedFile = fileRepository.save(file);
        log.info("File activated successfully with ID: {}", fileId);
        return fileMapper.toResponse(activatedFile);
    }

    @Override
    public FileResponse deactivateFile(String fileId) {
        log.info("Deactivating file with ID: {}", fileId);
        File file = getFileEntityById(fileId);
        file.setActive(false);
        File deactivatedFile = fileRepository.save(file);
        log.info("File deactivated successfully with ID: {}", fileId);
        return fileMapper.toResponse(deactivatedFile);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByS3Key(String s3Key) {
        return fileRepository.existsByS3Key(s3Key);
    }
}
