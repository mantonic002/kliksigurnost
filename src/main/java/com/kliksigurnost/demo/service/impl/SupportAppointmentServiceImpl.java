package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.model.SupportAppointment;
import com.kliksigurnost.demo.model.User;
import com.kliksigurnost.demo.repository.SupportAppointmentRepository;
import com.kliksigurnost.demo.service.SupportAppointmentService;
import com.kliksigurnost.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportAppointmentServiceImpl implements SupportAppointmentService {
    private final SupportAppointmentRepository appointmentRepository;
    private final UserService userService;
    private final Environment env;

    @Override
    public SupportAppointment scheduleAppointment(SupportAppointment appointment) throws RuntimeException {
        User currentUser = userService.getCurrentUser();
        if (!isSlotAvailable(appointment.getAppointmentDateTime())) {
            throw new RuntimeException(env.getProperty("appointment-slot-unavailable"));
        }
        log.debug("Schedule appointment for {}, time now: {}, user: {}", appointment.getAppointmentDateTime(), LocalDateTime.now(ZoneId.of("UTC")), currentUser.getEmail());
        if (appointmentRepository.existsByUserEmailAndAppointmentDateTimeAfter(currentUser.getEmail(), LocalDateTime.now(ZoneId.of("UTC")))) {
            throw new RuntimeException(env.getProperty("appointment-already-has"));
        }
        appointment.setUserEmail(currentUser.getEmail());
        return appointmentRepository.save(appointment);
    }

    @Override
    public List<SupportAppointment> getAppointmentsForUser() {
        return appointmentRepository.findByUserEmail(userService.getCurrentUser().getEmail());
    }

    @Override
    public List<SupportAppointment> getAllAppointmentsBetween(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByAppointmentDateTimeBetween(start, end);
    }

    @Override
    public List<LocalDateTime> getAvailableSlots(LocalDate date) {
        // Working hours (8 AM to 4 PM)
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(16, 0);

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

        // Filter out slots that are less than 2 hours in the future (only for today)
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        if (date.isEqual(now.toLocalDate())) {
            allSlots = allSlots.stream()
                    .filter(slot -> slot.isAfter(now.plusHours(2)))
                    .collect(Collectors.toList());
        }

        // Return available slots that are not booked
        return allSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAppointment(Integer appointmentId) {
        appointmentRepository.deleteById(appointmentId);
    }

    private boolean isSlotAvailable(LocalDateTime dateTime) {
        if (dateTime.isBefore(LocalDateTime.now(ZoneId.of("UTC")).plusHours(2))) {
            return false;
        }
        LocalDateTime start = dateTime.minusMinutes(29);
        LocalDateTime end = dateTime.plusMinutes(29);
        return appointmentRepository.findByAppointmentDateTimeBetween(start, end).isEmpty();
    }
}
