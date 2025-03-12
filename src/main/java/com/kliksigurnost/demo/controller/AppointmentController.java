package com.kliksigurnost.demo.controller;

import com.kliksigurnost.demo.exception.CloudflareApiException;
import com.kliksigurnost.demo.exception.NotFoundException;
import com.kliksigurnost.demo.exception.UnauthorizedAccessException;
import com.kliksigurnost.demo.model.SupportAppointment;
import com.kliksigurnost.demo.service.SupportAppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private SupportAppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody SupportAppointment appointment) {
        try {
            SupportAppointment savedAppointment = appointmentService.scheduleAppointment(appointment);
            return ResponseEntity.ok(savedAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<SupportAppointment>> getAppointments() {
        return ResponseEntity.ok(appointmentService.getAppointmentsForUser());
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(@PathVariable Integer appointmentId) {
        log.info("Deleting policy with ID: {}", appointmentId);
        try {
            appointmentService.deleteAppointment(appointmentId);
            return ResponseEntity.ok("Policy deleted successfully");
        } catch (NotFoundException e) {
            log.warn("Appointment not found: {}", appointmentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedAccessException e) {
            log.warn("Unauthorized access to delete appointment: {}", appointmentId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/available")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(@RequestParam LocalDate date) {
        List<LocalDateTime> availableSlots = appointmentService.getAvailableSlots(date);
        return ResponseEntity.ok(availableSlots);
    }
}