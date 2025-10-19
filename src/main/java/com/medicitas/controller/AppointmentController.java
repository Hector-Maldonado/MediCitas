package com.medicitas.controller;

import com.medicitas.dto.ApiResponse;
import com.medicitas.dto.AppointmentDTO;
import com.medicitas.model.Appointment;
import com.medicitas.model.Doctor;
import com.medicitas.model.Patient;
import com.medicitas.repository.AppointmentRepository;
import com.medicitas.repository.DoctorRepository;
import com.medicitas.repository.PatientRepository;
import com.medicitas.service.factory.NotificationFactory;
import com.medicitas.notification.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private NotificationFactory notificationFactory;

    // Formato de fecha legible con AM/PM
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' hh:mm a");

    // Convierte Appointment a DTO
    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setPatientId(appointment.getPatient().getId());
        dto.setDoctorId(appointment.getDoctor().getId());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setReason(appointment.getReason());
        return dto;
    }

    // Convierte DTO a Appointment
    private Appointment convertToEntity(AppointmentDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setId(dto.getId());

        Patient patient = patientRepository.findById(dto.getPatientId()).orElse(null);
        Doctor doctor = doctorRepository.findById(dto.getDoctorId()).orElse(null);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setReason(dto.getReason());
        return appointment;
    }

    // Obtener todas las citas
    @GetMapping
    public List<AppointmentDTO> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Obtener cita por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getAppointmentById(@PathVariable Long id) {
        Optional<Appointment> appointment = appointmentRepository.findById(id);
        if (appointment.isPresent()) {
            return ResponseEntity.ok(convertToDTO(appointment.get()));
        } else {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Cita con id " + id + " no fue encontrada"));
        }
    }

    // Crear nueva cita
    @PostMapping
    public ResponseEntity<?> createAppointment(@Valid @RequestBody AppointmentDTO dto,
                                               BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors()
                    .stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        Appointment appointment = convertToEntity(dto);
        if (appointment.getPatient() == null || appointment.getDoctor() == null) {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("El paciente o el doctor no existen"));
        }

        Appointment saved = appointmentRepository.save(appointment);

        // 🔹 Enviar notificación por correo usando nombre completo y fecha legible
        NotificationService notificationService = notificationFactory.createNotification("email");
        String fechaFormateada = appointment.getAppointmentDate().format(FORMATTER);
        notificationService.sendNotification(
                appointment.getPatient().getEmail(),
                "Confirmación de cita médica",
                "Hola " + appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName() +
                        ", tu cita con el Dr. " + appointment.getDoctor().getFirstName() + " " +
                        appointment.getDoctor().getLastName() +
                        " ha sido registrada para el día " + fechaFormateada
        );

        return ResponseEntity
                .created(URI.create("/api/appointments/" + saved.getId()))
                .body(convertToDTO(saved));
    }

    // Actualizar cita existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable Long id,
                                               @Valid @RequestBody AppointmentDTO dto,
                                               BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors()
                    .stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        Optional<Appointment> existing = appointmentRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Cita con id " + id + " no fue encontrada"));
        }

        Appointment appointment = existing.get();

        Patient patient = patientRepository.findById(dto.getPatientId()).orElse(null);
        Doctor doctor = doctorRepository.findById(dto.getDoctorId()).orElse(null);
        if (patient == null || doctor == null) {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("El paciente o el doctor no existen"));
        }

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setReason(dto.getReason());

        Appointment updated = appointmentRepository.save(appointment);

        // 🔹 Notificación al actualizar usando nombre completo y fecha legible
        NotificationService notificationService = notificationFactory.createNotification("email");
        String fechaFormateada = appointment.getAppointmentDate().format(FORMATTER);
        notificationService.sendNotification(
                appointment.getPatient().getEmail(),
                "Actualización de cita médica",
                "Hola " + appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName() +
                        ", tu cita con el Dr. " + appointment.getDoctor().getFirstName() + " " +
                        appointment.getDoctor().getLastName() +
                        " ha sido actualizada para el día " + fechaFormateada
        );

        return ResponseEntity.ok(convertToDTO(updated));
    }

    // Eliminar cita
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable Long id) {
        if (!appointmentRepository.existsById(id)) {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Cita con id " + id + " no fue encontrada"));
        }

        appointmentRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse("Cita eliminada exitosamente"));
    }
}