package com.hospital.his.prescriptions.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import com.hospital.his.medicalconsultation.entity.MedicalConsultationStatus;
import com.hospital.his.medicalconsultation.repository.MedicalConsultationRepository;
import com.hospital.his.medications.entity.Medication;
import com.hospital.his.medications.repository.MedicationRepository;
import com.hospital.his.prescriptions.dto.*;
import com.hospital.his.prescriptions.entity.*;
import com.hospital.his.prescriptions.repository.PrescriptionItemRepository;
import com.hospital.his.prescriptions.repository.PrescriptionRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PrescriptionService {

    private static final long MAXIMUM_VALIDITY_DAYS =
            7L;

    private final PrescriptionRepository
            prescriptionRepository;

    private final PrescriptionItemRepository
            prescriptionItemRepository;

    private final MedicationRepository
            medicationRepository;

    private final AppointmentRepository
            appointmentRepository;

    private final MedicalConsultationRepository
            medicalConsultationRepository;

    private final UserRepository
            userRepository;

    private final AuditService auditService;

    public PrescriptionService(
            PrescriptionRepository prescriptionRepository,
            PrescriptionItemRepository prescriptionItemRepository,
            MedicationRepository medicationRepository,
            AppointmentRepository appointmentRepository,
            MedicalConsultationRepository
                    medicalConsultationRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.prescriptionRepository =
                prescriptionRepository;

        this.prescriptionItemRepository =
                prescriptionItemRepository;

        this.medicationRepository =
                medicationRepository;

        this.appointmentRepository =
                appointmentRepository;

        this.medicalConsultationRepository =
                medicalConsultationRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Crea una receta para una consulta finalizada.
     *
     * La operación completa es transaccional:
     * si falla un medicamento, no se guarda la receta.
     */
    @Transactional
    public PrescriptionResponse createPrescription(
            CreatePrescriptionRequest request,
            String doctorUsername
    ) {
        validateCreateRequest(request);

        /*
         * Identificar y validar al médico.
         */
        User doctor =
                validateDoctor(
                        doctorUsername
                );

        /*
         * Buscar la cita y comprobar que pertenece
         * al médico autenticado.
         */
        Appointment appointment =
                appointmentRepository
                        .findById(
                                request.getAppointmentId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Cita médica no encontrada."
                                )
                        );

        validateAppointment(
                appointment,
                doctor
        );

        /*
         * Buscar la consulta finalizada del mismo médico.
         */
        MedicalConsultation consultation =
                medicalConsultationRepository
                        .findByAppointment_IdAndDoctor_Username(
                                appointment.getId(),
                                doctor.getUsername()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No se encontró la consulta "
                                                + "médica asociada."
                                )
                        );

        validateConsultation(
                consultation
        );

        /*
         * Una consulta solo puede tener una receta activa.
         */
        boolean existingPrescription =
                prescriptionRepository
                        .existsByMedicalConsultation_IdAndActiveTrue(
                                consultation.getId()
                        );

        if (existingPrescription) {
            throw new RuntimeException(
                    "La consulta médica ya cuenta con "
                            + "una receta activa."
            );
        }

        /*
         * Impedir medicamentos duplicados.
         */
        validateNoDuplicateMedications(
                request.getItems()
        );

        LocalDateTime now =
                LocalDateTime.now();

        Prescription prescription =
                Prescription.builder()
                        .prescriptionNumber(
                                generatePrescriptionNumber()
                        )
                        .medicalConsultation(
                                consultation
                        )
                        .appointment(
                                appointment
                        )
                        .patient(
                                appointment.getPatient()
                        )
                        .doctor(doctor)
                        .status(
                                PrescriptionStatus.ACTIVA
                        )
                        .notes(
                                normalizeOptionalText(
                                        request.getNotes(),
                                        1000,
                                        "Las observaciones no pueden "
                                                + "exceder los 1000 caracteres."
                                )
                        )
                        .issuedAt(now)
                        .updatedAt(now)
                        .fullyDispensedAt(null)
                        .active(true)
                        .items(
                                new ArrayList<>()
                        )
                        .build();

        /*
         * Cargar cada medicamento directamente
         * desde PostgreSQL.
         */
        for (
                CreatePrescriptionItemRequest itemRequest
                : request.getItems()
        ) {
            Medication medication =
                    medicationRepository
                            .findByIdAndActiveTrue(
                                    itemRequest.getMedicationId()
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "El medicamento con ID "
                                                    + itemRequest
                                                    .getMedicationId()
                                                    + " no existe o "
                                                    + "se encuentra inactivo."
                                    )
                            );

            validateMedication(
                    medication
            );

            PrescriptionItem item =
                    PrescriptionItem.builder()
                            .prescription(
                                    prescription
                            )
                            .medication(
                                    medication
                            )
                            .dosage(
                                    normalizeRequiredText(
                                            itemRequest.getDosage(),
                                            100,
                                            "La dosis del medicamento "
                                                    + "es obligatoria.",
                                            "La dosis no puede exceder "
                                                    + "los 100 caracteres."
                                    )
                            )
                            .frequency(
                                    normalizeRequiredText(
                                            itemRequest.getFrequency(),
                                            150,
                                            "La frecuencia del medicamento "
                                                    + "es obligatoria.",
                                            "La frecuencia no puede exceder "
                                                    + "los 150 caracteres."
                                    )
                            )
                            .duration(
                                    normalizeRequiredText(
                                            itemRequest.getDuration(),
                                            100,
                                            "La duración del tratamiento "
                                                    + "es obligatoria.",
                                            "La duración no puede exceder "
                                                    + "los 100 caracteres."
                                    )
                            )
                            .prescribedQuantity(
                                    validatePrescribedQuantity(
                                            itemRequest
                                                    .getPrescribedQuantity()
                                    )
                            )
                            .dispensedQuantity(0)
                            .specialInstructions(
                                    normalizeOptionalText(
                                            itemRequest
                                                    .getSpecialInstructions(),
                                            1000,
                                            "Las indicaciones especiales "
                                                    + "no pueden exceder los "
                                                    + "1000 caracteres."
                                    )
                            )
                            .controlledMedication(
                                    Boolean.TRUE.equals(
                                            medication
                                                    .getControlled()
                                    )
                            )
                            .active(true)
                            .build();

            prescription
                    .getItems()
                    .add(item);
        }

        try {
            prescription =
                    prescriptionRepository
                            .saveAndFlush(
                                    prescription
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "No fue posible crear la receta. "
                            + "La consulta puede tener otra "
                            + "receta activa.",
                    exception
            );
        }

        auditService.log(
                doctor.getUsername(),
                "CREATE_PRESCRIPTION",
                "PRESCRIPTIONS",
                "Receta "
                        + prescription
                        .getPrescriptionNumber()
                        + " creada para la consulta ID "
                        + consultation.getId()
                        + ". Medicamentos: "
                        + prescription
                        .getItems()
                        .size()
                        + "."
        );

        return toResponse(
                prescription,
                "Receta médica creada correctamente. "
                        + "La receta tiene una vigencia "
                        + "máxima de 7 días."
        );
    }

    /**
     * Lista las recetas emitidas por el médico
     * autenticado.
     */
    @Transactional(readOnly = true)
    public List<PrescriptionSummaryResponse>
    getMyPrescriptions(
            String doctorUsername
    ) {
        User doctor =
                validateDoctor(
                        doctorUsername
                );

        return prescriptionRepository
                .findByDoctor_UsernameAndActiveTrueOrderByIssuedAtDesc(
                        doctor.getUsername()
                )
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Obtiene una receta emitida por el médico.
     *
     * Impide que un médico consulte recetas
     * pertenecientes a otro profesional.
     */
    @Transactional(readOnly = true)
    public PrescriptionResponse getMyPrescription(
            Long prescriptionId,
            String doctorUsername
    ) {
        User doctor =
                validateDoctor(
                        doctorUsername
                );

        Prescription prescription =
                findActivePrescription(
                        prescriptionId
                );

        if (
                !prescription
                        .getDoctor()
                        .getId()
                        .equals(
                                doctor.getId()
                        )
        ) {
            throw new RuntimeException(
                    "La receta no pertenece al médico autenticado."
            );
        }

        return toResponse(
                prescription,
                "Receta médica encontrada."
        );
    }

    /**
     * Valida al médico autenticado.
     */
    private User validateDoctor(
            String username
    ) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al médico autenticado."
            );
        }

        User doctor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Médico autenticado no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        doctor.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del médico se encuentra inactiva."
            );
        }

        if (
                doctor.getRole() == null ||
                        !Boolean.TRUE.equals(
                                doctor
                                        .getRole()
                                        .getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El médico no tiene un rol activo asignado."
            );
        }

        if (
                !"MEDICO".equals(
                        normalizeRole(
                                doctor.getRole()
                                        .getName()
                        )
                )
        ) {
            throw new RuntimeException(
                    "Solamente un médico puede generar recetas."
            );
        }

        return doctor;
    }

    /**
     * Valida propiedad, estado y vigencia funcional
     * de la cita para generar una receta.
     */
    private void validateAppointment(
            Appointment appointment,
            User doctor
    ) {
        if (
                !Boolean.TRUE.equals(
                        appointment.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        if (
                appointment.getDoctor() == null ||
                        !appointment
                                .getDoctor()
                                .getId()
                                .equals(
                                        doctor.getId()
                                )
        ) {
            throw new RuntimeException(
                    "La cita no pertenece al médico autenticado."
            );
        }

        if (
                appointment.getStatus()
                        != AppointmentStatus
                        .CONSULTA_EVALUADA
        ) {
            throw new RuntimeException(
                    "La receta solamente puede generarse "
                            + "después de finalizar la evaluación médica "
                            + "y antes de finalizar completamente "
                            + "la atención."
            );
        }
    }

    /**
     * Valida que la consulta médica esté finalizada
     * y tenga diagnóstico.
     */
    private void validateConsultation(
            MedicalConsultation consultation
    ) {
        if (
                consultation.getStatus()
                        != MedicalConsultationStatus
                        .FINALIZADA
        ) {
            throw new RuntimeException(
                    "La consulta médica debe estar finalizada "
                            + "antes de generar la receta."
            );
        }

        if (
                consultation.getDiagnosis() == null ||
                        consultation.getDiagnosis()
                                .isBlank()
        ) {
            throw new RuntimeException(
                    "Debe existir un diagnóstico antes "
                            + "de generar la receta."
            );
        }
    }

    /**
     * Valida los datos principales del request.
     */
    private void validateCreateRequest(
            CreatePrescriptionRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la receta son obligatorios."
            );
        }

        if (request.getAppointmentId() == null) {
            throw new RuntimeException(
                    "Debe indicar la cita médica."
            );
        }

        if (
                request.getItems() == null ||
                        request.getItems().isEmpty()
        ) {
            throw new RuntimeException(
                    "Debe agregar al menos un medicamento "
                            + "a la receta."
            );
        }

        if (request.getItems().size() > 30) {
            throw new RuntimeException(
                    "No puede agregar más de 30 "
                            + "medicamentos por receta."
            );
        }
    }

    /**
     * Impide repetir el mismo medicamento.
     */
    private void validateNoDuplicateMedications(
            List<CreatePrescriptionItemRequest> items
    ) {
        Set<Long> medicationIds =
                new HashSet<>();

        for (
                CreatePrescriptionItemRequest item
                : items
        ) {
            if (
                    item == null ||
                            item.getMedicationId() == null
            ) {
                throw new RuntimeException(
                        "La receta contiene un medicamento inválido."
                );
            }

            if (
                    !medicationIds.add(
                            item.getMedicationId()
                    )
            ) {
                throw new RuntimeException(
                        "No debe agregar el mismo medicamento "
                                + "más de una vez."
                );
            }
        }
    }

    /**
     * Verifica que el medicamento tenga información
     * mínima válida.
     */
    private void validateMedication(
            Medication medication
    ) {
        if (
                !Boolean.TRUE.equals(
                        medication.getActive()
                )
        ) {
            throw new RuntimeException(
                    "El medicamento se encuentra inactivo."
            );
        }

        if (
                medication.getUnitPrice() == null ||
                        medication.getUnitPrice()
                                .signum() <= 0
        ) {
            throw new RuntimeException(
                    "El medicamento "
                            + medication.getCode()
                            + " no tiene un precio válido."
            );
        }
    }

    /**
     * Valida la cantidad prescrita.
     */
    private Integer validatePrescribedQuantity(
            Integer quantity
    ) {
        if (
                quantity == null ||
                        quantity <= 0
        ) {
            throw new RuntimeException(
                    "La cantidad prescrita debe ser "
                            + "mayor que cero."
            );
        }

        if (quantity > 10000) {
            throw new RuntimeException(
                    "La cantidad prescrita no puede "
                            + "exceder 10000 unidades."
            );
        }

        return quantity;
    }

    private Prescription findActivePrescription(
            Long prescriptionId
    ) {
        if (prescriptionId == null) {
            throw new RuntimeException(
                    "Debe indicar la receta médica."
            );
        }

        return prescriptionRepository
                .findByIdAndActiveTrue(
                        prescriptionId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Receta médica no encontrada."
                        )
                );
    }

    /**
     * Calcula cuántos días han transcurrido.
     *
     * Día de emisión = 0.
     */
    private long calculateElapsedDays(
            Prescription prescription
    ) {
        LocalDate issuedDate =
                prescription
                        .getIssuedAt()
                        .toLocalDate();

        return ChronoUnit.DAYS.between(
                issuedDate,
                LocalDate.now()
        );
    }

    private boolean isValid(
            Prescription prescription
    ) {
        long elapsedDays =
                calculateElapsedDays(
                        prescription
                );

        return elapsedDays >= 0
                && elapsedDays
                <= MAXIMUM_VALIDITY_DAYS
                && prescription.getStatus()
                != PrescriptionStatus.CANCELADA;
    }

    private String generatePrescriptionNumber() {
        return "REC-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeRole(
            String roleName
    ) {
        if (roleName == null) {
            return "";
        }

        return Normalizer
                .normalize(
                        roleName,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredText(
            String value,
            int maximumLength,
            String requiredMessage,
            String maximumMessage
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    requiredMessage
            );
        }

        String cleanValue =
                value.trim();

        if (
                cleanValue.length() >
                        maximumLength
        ) {
            throw new RuntimeException(
                    maximumMessage
            );
        }

        return cleanValue;
    }

    private String normalizeOptionalText(
            String value,
            int maximumLength,
            String maximumMessage
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        String cleanValue =
                value.trim();

        if (
                cleanValue.length() >
                        maximumLength
        ) {
            throw new RuntimeException(
                    maximumMessage
            );
        }

        return cleanValue;
    }

    private PrescriptionItemResponse toItemResponse(
            PrescriptionItem item
    ) {
        int prescribedQuantity =
                item.getPrescribedQuantity();

        int dispensedQuantity =
                item.getDispensedQuantity();

        int pendingQuantity =
                Math.max(
                        prescribedQuantity -
                                dispensedQuantity,
                        0
                );

        Medication medication =
                item.getMedication();

        return PrescriptionItemResponse
                .builder()
                .prescriptionItemId(
                        item.getId()
                )
                .medicationId(
                        medication.getId()
                )
                .medicationCode(
                        medication.getCode()
                )
                .medicationName(
                        medication.getName()
                )
                .activeIngredient(
                        medication.getActiveIngredient()
                )
                .concentration(
                        medication.getConcentration()
                )
                .presentation(
                        medication.getPresentation()
                )
                .currentUnitPrice(
                        medication.getUnitPrice()
                )
                .dosage(
                        item.getDosage()
                )
                .frequency(
                        item.getFrequency()
                )
                .duration(
                        item.getDuration()
                )
                .prescribedQuantity(
                        prescribedQuantity
                )
                .dispensedQuantity(
                        dispensedQuantity
                )
                .pendingQuantity(
                        pendingQuantity
                )
                .specialInstructions(
                        item.getSpecialInstructions()
                )
                .controlledMedication(
                        item.getControlledMedication()
                )
                .fullyDispensed(
                        pendingQuantity == 0
                )
                .active(
                        item.getActive()
                )
                .build();
    }

    private PrescriptionResponse toResponse(
            Prescription prescription,
            String message
    ) {
        List<PrescriptionItem> items =
                prescriptionItemRepository
                        .findByPrescription_IdAndActiveTrueOrderByIdAsc(
                                prescription.getId()
                        );

        int fullyDispensed =
                (int) items.stream()
                        .filter(item ->
                                item.getDispensedQuantity()
                                        >= item
                                        .getPrescribedQuantity()
                        )
                        .count();

        long elapsedDays =
                calculateElapsedDays(
                        prescription
                );

        return PrescriptionResponse.builder()
                .prescriptionId(
                        prescription.getId()
                )
                .prescriptionNumber(
                        prescription
                                .getPrescriptionNumber()
                )
                .appointmentId(
                        prescription
                                .getAppointment()
                                .getId()
                )
                .medicalConsultationId(
                        prescription
                                .getMedicalConsultation()
                                .getId()
                )
                .patientUserId(
                        prescription
                                .getPatient()
                                .getId()
                )
                .patientName(
                        prescription
                                .getPatient()
                                .getFullName()
                )
                .patientDpi(
                        prescription
                                .getPatient()
                                .getDpi()
                )
                .doctorUserId(
                        prescription
                                .getDoctor()
                                .getId()
                )
                .doctorName(
                        prescription
                                .getDoctor()
                                .getFullName()
                )
                .branch(
                        prescription
                                .getAppointment()
                                .getBranch()
                                .getName()
                )
                .status(
                        prescription
                                .getStatus()
                                .name()
                )
                .notes(
                        prescription.getNotes()
                )
                .issuedAt(
                        prescription
                                .getIssuedAt()
                                .toString()
                )
                .updatedAt(
                        prescription
                                .getUpdatedAt()
                                .toString()
                )
                .fullyDispensedAt(
                        prescription
                                .getFullyDispensedAt()
                                != null
                                ? prescription
                                .getFullyDispensedAt()
                                .toString()
                                : null
                )
                .totalMedications(
                        items.size()
                )
                .fullyDispensedMedications(
                        fullyDispensed
                )
                .allMedicationsDispensed(
                        !items.isEmpty()
                                && fullyDispensed
                                == items.size()
                )
                .valid(
                        isValid(prescription)
                )
                .elapsedDays(
                        elapsedDays
                )
                .active(
                        prescription.getActive()
                )
                .items(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )
                .message(message)
                .build();
    }

    private PrescriptionSummaryResponse
    toSummaryResponse(
            Prescription prescription
    ) {
        List<PrescriptionItem> items =
                prescriptionItemRepository
                        .findByPrescription_IdAndActiveTrueOrderByIdAsc(
                                prescription.getId()
                        );

        long elapsedDays =
                calculateElapsedDays(
                        prescription
                );

        return PrescriptionSummaryResponse
                .builder()
                .prescriptionId(
                        prescription.getId()
                )
                .prescriptionNumber(
                        prescription
                                .getPrescriptionNumber()
                )
                .appointmentId(
                        prescription
                                .getAppointment()
                                .getId()
                )
                .medicalConsultationId(
                        prescription
                                .getMedicalConsultation()
                                .getId()
                )
                .patientName(
                        prescription
                                .getPatient()
                                .getFullName()
                )
                .patientDpi(
                        prescription
                                .getPatient()
                                .getDpi()
                )
                .doctorName(
                        prescription
                                .getDoctor()
                                .getFullName()
                )
                .branch(
                        prescription
                                .getAppointment()
                                .getBranch()
                                .getName()
                )
                .status(
                        prescription
                                .getStatus()
                                .name()
                )
                .issuedAt(
                        prescription
                                .getIssuedAt()
                                .toString()
                )
                .totalMedications(
                        items.size()
                )
                .valid(
                        isValid(prescription)
                )
                .elapsedDays(
                        elapsedDays
                )
                .active(
                        prescription.getActive()
                )
                .message(
                        isValid(prescription)
                                ? "Receta vigente."
                                : "Receta fuera del periodo de vigencia."
                )
                .build();
    }
}