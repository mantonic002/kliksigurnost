package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.model.SupportAppointment;
import com.kliksigurnost.demo.service.SupportAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private SupportAppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<SupportAppointment> createAppointment(@RequestBody SupportAppointment appointment) {
        SupportAppointment savedAppointment = appointmentService.scheduleAppointment(appointment);
        return ResponseEntity.ok(savedAppointment);
    }

    @GetMapping
    public ResponseEntity<List<SupportAppointment>> getAppointments() {
        return ResponseEntity.ok(appointmentService.getAppointmentsForUser());
    }

    @GetMapping("/available")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(@RequestParam LocalDate date) {
        List<LocalDateTime> availableSlots = appointmentService.getAvailableSlots(date);
        return ResponseEntity.ok(availableSlots);
    }
}