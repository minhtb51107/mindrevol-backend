//package com.mindrevol.backend.config;
//
//import io.imagekit.sdk.ImageKit;
//import io.imagekit.sdk.config.Configuration;
//import io.imagekit.sdk.utils.Utils;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ImageKitConfig {
//
//    @Value("${imagekit.url-endpoint}")
//    private String urlEndpoint;
//
//    @Value("${imagekit.public-key}")
//    private String publicKey;
//
//    @Value("${imagekit.private-key}")
//    private String privateKey;
//
//    @Bean
//    public ImageKit imageKit() {
//        ImageKit imageKit = ImageKit.getInstance();
//        Configuration config = Utils.getSystemConfig(
//            getApplicationContext() // Một cách trick nhỏ để lấy context, hoặc đơn giản tự set tay
//        );
//        
//        // Set cấu hình thủ công để đảm bảo chính xác
//        config.setUrlEndpoint(urlEndpoint);
//        config.setPublicKey(publicKey);
//        config.setPrivateKey(privateKey);
//        
//        imageKit.setConfig(config);
//        return imageKit;
//    }
//    
//    // Helper method giả lập context (vì SDK ImageKit yêu cầu param này nhưng không thực sự dùng sâu)
//    private String getApplicationContext() {
//        return "mindrevol-app";
//    }
//}