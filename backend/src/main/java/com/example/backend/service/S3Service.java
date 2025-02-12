package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client; // Config - bean에 의해 주입됨

    @Value("${BUCKET_NAME}")
    private String bucketName;

    @Value("${REGION}")
    private String region;

    // 파일 저장 경로(폴더) (articles 라는 폴더에 파일이 저장됨)
    private static final String FILE_PATH_PREFIX = "articles/";

    // S3 파일 업로드 처리 메서드
    // 파일(file)을 articleService에서 받은 후
    // s3 업로드 후 imageUrl과 s3Key를 반환하는 메서드
    public Map<String, String> uploadFile(MultipartFile file) {

        // s3Key 생성
        String s3Key = FILE_PATH_PREFIX + UUID.randomUUID() + "_" + file.getOriginalFilename();

        // s3 버킷에 파일을 업로드
        // 업로드할 file과 s3 객체 키(s3Key)를 전달
        uploadFileTos3(s3Key, file);

        // 템플릿 리터럴 '${변수}'
        // https://버킷명.s3.리전.amazonaws.com/객체키
        //
        String IMAGE_URL_FORMAT = "https://%s.s3.%s.amazonaws.com/%s";
        String imageUrl = String.format(IMAGE_URL_FORMAT, bucketName, region, s3Key);

        return Map.of(
                "imageUrl", imageUrl,
                "s3Key", s3Key
        );
    }

    // 실질적으로 S3 버킷에 파일(객체)을 업로드하는 메서드
    private void uploadFileTos3(String s3Key, MultipartFile file) {
         try {
            // s3에 요청할 객체
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            // s3에 파일 업로드 요청
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
             throw new RuntimeException("파일 업로드 실패" + e.getMessage());
         }
    }

    // s3 파일 삭제 처리 로직
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
