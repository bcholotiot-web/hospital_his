package com.hospital.his.reception.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.repository.BranchRepository;
import com.hospital.his.reception.dto.CreateEmergencyReceptionRequest;
import com.hospital.his.reception.dto.EmergencyReceptionResponse;
import com.hospital.his.reception.entity.EmergencyReception;
import com.hospital.his.reception.entity.EmergencyReceptionStatus;
import com.hospital.his.reception.repository.EmergencyReceptionRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmergencyReceptionService {

    private final EmergencyReceptionRepository
            emergencyReceptionRepository;

    private final UserRepository userRepository;

    private final BranchRepository branchRepository;

    private final AuditService auditService;

    public EmergencyReceptionService(
            EmergencyReceptionRepository emergencyReceptionRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            AuditService auditService
    ) {
        this.emergencyReceptionRepository =
                emergencyReceptionRepository;

        this.userRepository =
                userRepository;

        this.branchRepository =
                branchRepository;

        this.auditService =
                auditService;
    }

    @Transactional
    public EmergencyReceptionResponse registerEmergency(
            CreateEmergencyReceptionRequest request,
            String receptionistUsername
    ) {
        validateRequest(
                request,
                receptionistUsername
        );

        String cleanName =
                request.getPatientName().trim();

        String cleanDpi =
                request.getPatientDpi().trim();

        String cleanNote =
                normalizeNote(
                        request.getEmergencyNote()
                );

        Branch branch =
                branchRepository.findById(
                                request.getBranchId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Sucursal no encontrada."
                                )
                        );

        if (!Boolean.TRUE.equals(
                branch.getActive())) {

            throw new RuntimeException(
                    "La sucursal seleccionada no se encuentra activa."
            );
        }

        boolean alreadyPresent =
                emergencyReceptionRepository
                        .existsByPatientDpiAndStatusAndActiveTrue(
                                cleanDpi,
                                EmergencyReceptionStatus.PACIENTE_PRESENTE
                        );

        if (alreadyPresent) {
            throw new RuntimeException(
                    "El paciente ya tiene un registro activo de emergencia."
            );
        }

        Optional<User> existingPatient =
                userRepository.findByDpi(
                        cleanDpi
                );

        User patient =
                existingPatient.orElse(null);

        /*
         * Si existe, usamos el nombre registrado
         * para mantener la consistencia.
         */
        if (patient != null) {
            cleanName =
                    patient.getFullName();
        }

        EmergencyReception emergencyReception =
                EmergencyReception.builder()
                        .patient(patient)
                        .patientName(cleanName)
                        .patientDpi(cleanDpi)
                        .branch(branch)
                        .status(
                                EmergencyReceptionStatus.PACIENTE_PRESENTE
                        )
                        .arrivalTime(
                                LocalDateTime.now()
                        )
                        .receptionistUsername(
                                receptionistUsername
                        )
                        .emergencyNote(cleanNote)
                        .active(true)
                        .build();

        emergencyReception =
                emergencyReceptionRepository.saveAndFlush(
                        emergencyReception
                );

        auditService.log(
                receptionistUsername,
                "REGISTER_EMERGENCY_RECEPTION",
                "RECEPTION",
                "Paciente registrado con prioridad de EMERGENCIA. "
                        + "Registro de emergencia ID: "
                        + emergencyReception.getId()
                        + "."
        );

        return toResponse(
                emergencyReception
        );
    }

    private void validateRequest(
            CreateEmergencyReceptionRequest request,
            String receptionistUsername
    ) {
        if (receptionistUsername == null ||
                receptionistUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al recepcionista autenticado."
            );
        }

        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la emergencia son obligatorios."
            );
        }

        if (request.getPatientName() == null ||
                request.getPatientName().isBlank()) {

            throw new RuntimeException(
                    "El nombre del paciente es obligatorio."
            );
        }

        String cleanName =
                request.getPatientName().trim();

        if (cleanName.length() < 5 ||
                cleanName.length() > 100) {

            throw new RuntimeException(
                    "El nombre del paciente debe contener entre 5 y 100 caracteres."
            );
        }

        if (request.getPatientDpi() == null ||
                !request.getPatientDpi()
                        .trim()
                        .matches("\\d{13}")) {

            throw new RuntimeException(
                    "El DPI debe contener exactamente 13 dígitos numéricos."
            );
        }

        if (request.getBranchId() == null) {
            throw new RuntimeException(
                    "Debe seleccionar una sucursal."
            );
        }

        if (request.getEmergencyNote() != null &&
                request.getEmergencyNote()
                        .trim()
                        .length() > 500) {

            throw new RuntimeException(
                    "La nota de emergencia no puede exceder los 500 caracteres."
            );
        }
    }

    private String normalizeNote(
            String note
    ) {
        if (note == null ||
                note.isBlank()) {

            return null;
        }

        return note.trim();
    }

    private EmergencyReceptionResponse toResponse(
            EmergencyReception emergencyReception
    ) {
        return EmergencyReceptionResponse.builder()
                .emergencyReceptionId(
                        emergencyReception.getId()
                )
                .patientUserId(
                        emergencyReception.getPatient() != null
                                ? emergencyReception
                                .getPatient()
                                .getId()
                                : null
                )
                .registeredPatient(
                        emergencyReception.getPatient() != null
                )
                .patientName(
                        emergencyReception.getPatientName()
                )
                .patientDpi(
                        emergencyReception.getPatientDpi()
                )
                .branch(
                        emergencyReception
                                .getBranch()
                                .getName()
                )
                .priority("EMERGENCIA")
                .status(
                        emergencyReception
                                .getStatus()
                                .name()
                )
                .arrivalTime(
                        emergencyReception
                                .getArrivalTime()
                                .toString()
                )
                .emergencyNote(
                        emergencyReception
                                .getEmergencyNote()
                )
                .message(
                        "Paciente "
                                + emergencyReception
                                .getPatientName()
                                + " registrado con prioridad de EMERGENCIA. "
                                + "El paciente debe pasar directamente "
                                + "a toma de signos vitales."
                )
                .build();
    }
}