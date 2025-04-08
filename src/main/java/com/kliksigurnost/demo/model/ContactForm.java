package com.kliksigurnost.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String userEmail;
    @Column(nullable = false)
    private String phoneNumber;
    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    private ContactFormStatus status;
    private LocalDateTime submissionDate;
}

