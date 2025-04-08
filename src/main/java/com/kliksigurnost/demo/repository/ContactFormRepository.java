package com.kliksigurnost.demo.repository;

import com.kliksigurnost.demo.model.ContactForm;
import com.kliksigurnost.demo.model.ContactFormStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactFormRepository extends JpaRepository<ContactForm, Long> {
    List<ContactForm> findContactFormsByStatus(ContactFormStatus status);
}
