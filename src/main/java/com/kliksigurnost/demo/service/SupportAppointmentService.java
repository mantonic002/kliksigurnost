package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.SupportAppointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SupportAppointmentService {
    SupportAppointment scheduleAppointment(SupportAppointment appointment);

    List<SupportAppointment> getAppointmentsForUser();

    List<SupportAppointment> getAllAppointmentsBetween(LocalDateTime start, LocalDateTime end);

    List<LocalDateTime> getAvailableSlots(LocalDate date);

    void deleteAppointment(Integer appointmentId);
}
