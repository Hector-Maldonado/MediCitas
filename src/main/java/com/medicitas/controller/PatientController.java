package com.medicitas.controller;

import com.medicitas.dto.ApiResponse;
import com.medicitas.dto.PatientDTO;
import com.medicitas.model.Patient;
import com.medicitas.repository.PatientRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
public class PatientController {

    @Autowired
    private PatientRepository patientRepository;

    // Convierte Patient a DTO
    private PatientDTO convertToDTO(Patient patient) {
        PatientDTO dto = new PatientDTO();
        dto.setId(patient.getId());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setEmail(patient.getEmail());
        dto.setPhone(patient.getPhone());
        dto.setDateOfBirth(patient.getDateOfBirth());
        return dto;
    }

    // Convierte DTO a Patient
    private Patient convertToEntity(PatientDTO dto) {
        Patient patient = new Patient();
        patient.setId(dto.getId());
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setEmail(dto.getEmail());
        patient.setPhone(dto.getPhone());
        patient.setDateOfBirth(dto.getDateOfBirth());
        return patient;
    }

    // Obtener todos los pacientes
    @GetMapping
    public List<PatientDTO> getAllPatients() {
        return patientRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Obtener paciente por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPatientById(@PathVariable Long id) {
        Optional<Patient> paciente = patientRepository.findById(id);

        if (paciente.isPresent()) {
            return ResponseEntity.ok(convertToDTO(paciente.get()));
        } else {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Paciente con id " + id + " no fue encontrado"));
        }
    }

    // Crear nuevo paciente
    @PostMapping
    public ResponseEntity<?> createPatient(@Valid @RequestBody PatientDTO dto,
                                           BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        if (patientRepository.existsByEmail(dto.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(List.of("El correo '" + dto.getEmail() + "' ya está registrado"));
        }

        Patient saved = patientRepository.save(convertToEntity(dto));
        return ResponseEntity
                .created(URI.create("/api/patients/" + saved.getId()))
                .body(convertToDTO(saved));
    }

    // Actualizar paciente existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePatient(@PathVariable Long id,
                                           @Valid @RequestBody PatientDTO dto,
                                           BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        Optional<Patient> existing = patientRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Paciente con id " + id + " no fue encontrado"));
        }

        Patient patient = existing.get();
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setEmail(dto.getEmail());
        patient.setPhone(dto.getPhone());
        patient.setDateOfBirth(dto.getDateOfBirth());

        Patient updated = patientRepository.save(patient);
        return ResponseEntity.ok(convertToDTO(updated));
    }

    // Eliminar paciente
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable Long id) {
        if (!patientRepository.existsById(id)) {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Paciente con id " + id + " no fue encontrado"));
        }

        patientRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse("Paciente eliminado exitosamente"));
    }

}
