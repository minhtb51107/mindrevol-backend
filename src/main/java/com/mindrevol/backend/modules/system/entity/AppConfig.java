package com.mindrevol.backend.modules.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "app_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {

    @Id
    @Column(name = "config_key", nullable = false, unique = true)
    private String key; // VD: SUPPORT_EMAIL, TERMS_URL, PRIVACY_URL, ANDROID_VERSION

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String value;
    
    @Column(name = "description")
    private String description; // Mô tả cho Admin hiểu config này là gì
}