package com.mindrevol.backend.common.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@Slf4j
public class ImageMetadataService {

    /**
     * Trích xuất thời gian chụp ảnh gốc từ EXIF Data
     * Trả về null nếu không tìm thấy (ảnh đã bị xóa info hoặc chụp từ app không lưu exif)
     */
    public LocalDateTime getCreationDate(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            
            // Tìm thư mục Exif SubIFD (Nơi chứa ngày chụp gốc)
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            
            if (directory != null) {
                // Lấy ngày gốc (Date/Time Original)
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract date metadata from image: {}", e.getMessage());
        }
        return null; // Không tìm thấy metadata
    }

    /**
     * [THÊM MỚI] 
     * Trích xuất tọa độ GPS (Vĩ độ, Kinh độ) từ EXIF Data của ảnh.
     * Dùng để tự động gắn bài check-in lên Bản đồ.
     * @return mảng double[] chứa {latitude, longitude}, hoặc null nếu ảnh không có GPS.
     */
    public double[] extractCoordinates(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            // Tìm thư mục GPS trong siêu dữ liệu của ảnh
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);

            if (gpsDirectory != null) {
                // Lấy đối tượng GeoLocation chứa sẵn logic giải mã tọa độ
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                
                if (geoLocation != null && !geoLocation.isZero()) {
                    double latitude = geoLocation.getLatitude();
                    double longitude = geoLocation.getLongitude();
                    
                    return new double[]{latitude, longitude};
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract GPS metadata from image: {}", e.getMessage());
        }
        return null; // Trả về null nếu ảnh tải từ Facebook/Zalo về (bị xóa EXIF) hoặc người dùng tắt định vị máy ảnh
    }
}