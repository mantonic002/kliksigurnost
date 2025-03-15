package com.kliksigurnost.demo.repository;

import com.kliksigurnost.demo.model.SupportAppointment;
import com.kliksigurnost.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SupportAppointmentRepository  extends JpaRepository<SupportAppointment, Integer> {
    List<SupportAppointment> findByAppointmentDateTimeBetween(LocalDateTime start, LocalDateTime end);
    List<SupportAppointment> findByUserEmail(String userEmail);

    Boolean existsByUserEmailAndAppointmentDateTimeAfter(String userEmail, LocalDateTime appointmentDateTimeBefore);
}
