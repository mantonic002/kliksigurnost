package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.model.SupportAppointment;
import com.kliksigurnost.demo.repository.SupportAppointmentRepository;
import com.kliksigurnost.demo.service.SupportAppointmentService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportAppointmentServiceImpl implements SupportAppointmentService {
    private final SupportAppointmentRepository appointmentRepository;
    private final UserService userService;

    @Override
    public SupportAppointment scheduleAppointment(SupportAppointment appointment) {
        if (isSlotAvailable(appointment.getAppointmentDateTime())) {
            appointment.setUser(userService.getCurrentUser());
            return appointmentRepository.save(appointment);
        } else {
            throw new RuntimeException("Time slot is not available");
        }
    }

    @Override
    public List<SupportAppointment> getAppointmentsForUser() {
        return appointmentRepository.findByUser(userService.getCurrentUser());
    }

    @Override
    public List<LocalDateTime> getAvailableSlots(LocalDate date) {
        // working hours (9 AM to 5 PM)
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        int slotDurationMinutes = 30;

        // Get all appointments for the given date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<SupportAppointment> appointments = appointmentRepository.findByAppointmentDateTimeBetween(startOfDay, endOfDay);

        // Generate all possible slots for the day
        List<LocalDateTime> allSlots = new ArrayList<>();
        LocalDateTime currentSlot = date.atTime(startTime);
        while (currentSlot.isBefore(date.atTime(endTime))) {
            allSlots.add(currentSlot);
            currentSlot = currentSlot.plusMinutes(slotDurationMinutes);
        }

        // Filter out booked slots
        List<LocalDateTime> bookedSlots = appointments.stream()
                .map(SupportAppointment::getAppointmentDateTime)
                .collect(Collectors.toList());

        return allSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAppointment(Integer appointmentId) {
        appointmentRepository.deleteById(appointmentId);
    }

    private boolean isSlotAvailable(LocalDateTime dateTime) {
        LocalDateTime start = dateTime.minusMinutes(29);
        LocalDateTime end = dateTime.plusMinutes(29);
        return appointmentRepository.findByAppointmentDateTimeBetween(start, end).isEmpty();
    }
}
