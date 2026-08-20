package com.subsidytracker.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.subsidytracker.common.exception.InvalidOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        cloudinaryService = new CloudinaryService(cloudinary);
    }

    @Test
    void upload_SuccessfulUpload_ReturnsSecureUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy content".getBytes()
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/sample.jpg"));

        String url = cloudinaryService.upload(file);

        assertThat(url).isEqualTo("https://res.cloudinary.com/demo/image/upload/sample.jpg");
    }

    @Test
    void upload_IOException_ThrowsInvalidOperationException() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy content".getBytes()
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenThrow(new IOException("Cloudinary connection failed"));

        assertThatThrownBy(() -> cloudinaryService.upload(file))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Failed to upload document to Cloudinary");
    }
}
