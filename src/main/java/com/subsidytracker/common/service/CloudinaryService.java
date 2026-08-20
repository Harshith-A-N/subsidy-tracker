package com.subsidytracker.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.subsidytracker.common.exception.InvalidOperationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Thin wrapper around the Cloudinary Java SDK.
 *
 * Uploads a file to the "subsidy-tracker/documents" Cloudinary folder and
 * returns the HTTPS secure_url that clients can use to view the file directly.
 * The public_id is also available in the result map if asset deletion is
 * needed in a future phase.
 */
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Uploads the given multipart file to Cloudinary.
     *
     * @param file the file received from the HTTP request
     * @return the Cloudinary HTTPS secure_url for the uploaded asset
     * @throws InvalidOperationException if the upload fails for any reason
     */
    @SuppressWarnings("unchecked")
    public String upload(MultipartFile file) {
        try {
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",        "Subsidy Tracker/documents",
                            "resource_type", "auto"
                    )
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new InvalidOperationException("Failed to upload document to Cloudinary: " + e.getMessage());
        }
    }
}
