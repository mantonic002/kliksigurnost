package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.model.ContactForm;
import com.kliksigurnost.demo.model.ContactFormStatus;
import com.kliksigurnost.demo.repository.ContactFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contact")
public class ContactFormController {
    private final ContactFormRepository contactFormRepository;

    @PostMapping
    public ResponseEntity<?> submitContactForm(@RequestBody ContactForm contactForm) {
        contactForm.setSubmissionDate(LocalDateTime.now());
        contactForm.setStatus(ContactFormStatus.PENDING);
        contactFormRepository.save(contactForm);
        return ResponseEntity.ok().build();
    }
}
