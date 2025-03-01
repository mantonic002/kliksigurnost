package com.kliksigurnost.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Builder
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    User user;
    String message;
    Boolean isSeen;
    Instant timestamp;

    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private String deviceId;
}

