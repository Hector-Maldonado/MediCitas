package com.medicitas.controller;

import com.medicitas.dto.ApiResponse;
import com.medicitas.dto.DoctorDTO;
import com.medicitas.model.Doctor;
import com.medicitas.repository.DoctorRepository;
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
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired
    private DoctorRepository doctorRepository;

    // Convierte Doctor a DoctorDTO
    private DoctorDTO convertToDTO(Doctor doctor) {
        DoctorDTO dto = new DoctorDTO();
        dto.setId(doctor.getId());
        dto.setFirstName(doctor.getFirstName());
        dto.setLastName(doctor.getLastName());
        dto.setEmail(doctor.getEmail());
        dto.setPhone(doctor.getPhone());
        dto.setEspecialidad(doctor.getEspecialidad());
        return dto;
    }

    // Convierte DoctorDTO a Doctor
    private Doctor convertToEntity(DoctorDTO dto) {
        Doctor doctor = new Doctor();
        doctor.setId(dto.getId());
        doctor.setFirstName(dto.getFirstName());
        doctor.setLastName(dto.getLastName());
        doctor.setEmail(dto.getEmail());
        doctor.setPhone(dto.getPhone());
        doctor.setEspecialidad(dto.getEspecialidad());
        return doctor;
    }

    // Obtener todos los doctores
    @GetMapping
    public List<DoctorDTO> getAllDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Obtener doctor por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable Long id) {
        Optional<Doctor> doctor = doctorRepository.findById(id);

        if (doctor.isPresent()) {
            return ResponseEntity.ok(convertToDTO(doctor.get()));
        } else {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Doctor con id " + id + " no fue encontrado"));
        }
    }

    // Crear nuevo doctor
    @PostMapping
    public ResponseEntity<?> createDoctor(@Valid @RequestBody DoctorDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        Doctor saved = doctorRepository.save(convertToEntity(dto));
        return ResponseEntity
                .created(URI.create("/api/doctors/" + saved.getId()))
                .body(convertToDTO(saved));
    }

    // Actualizar doctor existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Long id,
                                          @Valid @RequestBody DoctorDTO dto,
                                          BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        Optional<Doctor> existing = doctorRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Doctor con id " + id + " no fue encontrado"));
        }

        Doctor doctor = existing.get();
        doctor.setFirstName(dto.getFirstName());
        doctor.setLastName(dto.getLastName());
        doctor.setEmail(dto.getEmail());
        doctor.setPhone(dto.getPhone());
        doctor.setEspecialidad(dto.getEspecialidad());

        Doctor updated = doctorRepository.save(doctor);
        return ResponseEntity.ok(convertToDTO(updated));
    }

    // Eliminar doctor
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        if (!doctorRepository.existsById(id)) {
            return ResponseEntity
                    .status(404)
                    .body(new ApiResponse("Doctor con id " + id + " no fue encontrado"));
        }

        doctorRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse("Doctor eliminado exitosamente"));
    }
}