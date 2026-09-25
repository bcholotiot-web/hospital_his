package com.hospital.his.appointments.service;

import com.hospital.his.appointments.dto.AvailableSlotResponse;
import com.hospital.his.appointments.dto.CreateFollowUpAppointmentRequest;
import com.hospital.his.appointments.dto.FollowUpAppointmentContextResponse;
import com.hospital.his.appointments.dto.FollowUpAppointmentResponse;
import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentOrigin;
import com.hospital.his.appointments.entity.AppointmentPriority;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.entity.FollowUpType;
import com.hospital.his.appointments.events.FollowUpAppointmentCreatedEvent;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import com.hospital.his.medicalconsultation.entity.MedicalConsultationStatus;
import com.hospital.his.medicalconsultation.repository.MedicalConsultationRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FollowUpAppointmentService {

    /**
     * Tiempo durante el cual se reserva el horario
     * mientras el paciente completa el pago.
     *
     * Se conserva la misma regla del agendamiento normal.
     */
    private static final long RESERVATION_MINUTES =
            5L;

    /**
     * Estados que no deben impedir una nueva
     * programación.
     */
    private static final Set<AppointmentStatus>
            NON_BLOCKING_STATUSES =
            Set.of(
                    AppointmentStatus.CANCELADA,
                    AppointmentStatus.EXPIRADA,
                    AppointmentStatus.NO_ASISTIO
            );

    private final AppointmentRepository
            appointmentRepository;

    private final MedicalConsultationRepository
            medicalConsultationRepository;

    private final UserRepository
            userRepository;

    private final AppointmentService
            appointmentService;

    private final AuditService
            auditService;

    private final ApplicationEventPublisher
            applicationEventPublisher;

    public FollowUpAppointmentService(
            AppointmentRepository appointmentRepository,
            MedicalConsultationRepository
                    medicalConsultationRepository,
            UserRepository userRepository,
            AppointmentService appointmentService,
            AuditService auditService,
            ApplicationEventPublisher
                    applicationEventPublisher
    ) {
        this.appointmentRepository =
                appointmentRepository;

        this.medicalConsultationRepository =
                medicalConsultationRepository;

        this.userRepository =
                userRepository;

        this.appointmentService =
                appointmentService;

        this.auditService =
                auditService;

        this.applicationEventPublisher =
                applicationEventPublisher;
    }

    /**
     * Obtiene los datos precargados del seguimiento.
     *
     * Paciente, médico, sucursal y especialidad se
     * recuperan desde la consulta padre.
     */
    @Transactional(readOnly = true)
    public FollowUpAppointmentContextResponse
    getFollowUpContext(
            Long parentConsultationId,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        MedicalConsultation consultation =
                findAndValidateParentConsultation(
                        parentConsultationId,
                        doctor
                );

        Appointment parentAppointment =
                consultation.getAppointment();

        validateContextEntities(
                parentAppointment,
                doctor
        );

        return FollowUpAppointmentContextResponse
                .builder()
                .parentConsultationId(
                        consultation.getId()
                )
                .parentAppointmentId(
                        parentAppointment.getId()
                )
                .patientUserId(
                        parentAppointment
                                .getPatient()
                                .getId()
                )
                .patientName(
                        parentAppointment
                                .getPatient()
                                .getFullName()
                )
                .patientDpi(
                        parentAppointment
                                .getPatient()
                                .getDpi()
                )
                .doctorUserId(
                        doctor.getId()
                )
                .doctorName(
                        doctor.getFullName()
                )
                .specialtyId(
                        parentAppointment
                                .getSpecialty()
                                .getId()
                )
                .specialtyName(
                        parentAppointment
                                .getSpecialty()
                                .getName()
                )
                .branchId(
                        parentAppointment
                                .getBranch()
                                .getId()
                )
                .branchName(
                        parentAppointment
                                .getBranch()
                                .getName()
                )
                .consultationStatus(
                        consultation
                                .getStatus()
                                .name()
                )
                .appointmentStatus(
                        parentAppointment
                                .getStatus()
                                .name()
                )
                .diagnosis(
                        consultation.getDiagnosis()
                )
                .treatmentPlan(
                        consultation
                                .getTreatmentPlan()
                )
                .canSchedule(true)
                .message(
                        "La consulta se encuentra habilitada "
                                + "para programar seguimiento."
                )
                .build();
    }

    /**
     * Consulta disponibilidad del mismo médico
     * que atendió la consulta padre.
     *
     * Reutiliza la lógica del agendamiento normal.
     */
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse>
    getAvailableSlots(
            Long parentConsultationId,
            String date,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        MedicalConsultation consultation =
                findAndValidateParentConsultation(
                        parentConsultationId,
                        doctor
                );

        validateContextEntities(
                consultation.getAppointment(),
                doctor
        );

        return appointmentService
                .getAvailableSlots(
                        doctor.getId(),
                        date
                );
    }

    /**
     * Crea una cita de seguimiento.
     *
     * El backend decide:
     *
     * - Paciente.
     * - Médico.
     * - Sucursal.
     * - Especialidad.
     * - Estado inicial.
     * - Origen.
     * - Reserva.
     *
     * El request no puede modificar esos valores.
     */
    @Transactional
    public FollowUpAppointmentResponse
    createFollowUpAppointment(
            CreateFollowUpAppointmentRequest request,
            String doctorUsername
    ) {
        validateCreateRequest(
                request
        );

        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        /*
         * Idempotencia:
         * devolver la cita anterior si la misma llave
         * ya fue procesada.
         */
        Appointment previousAppointment =
                appointmentRepository
                        .findByIdempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .orElse(null);

        if (previousAppointment != null) {
            if (
                    previousAppointment
                            .getParentConsultation()
                            == null
                            ||
                            !previousAppointment
                                    .getParentConsultation()
                                    .getId()
                                    .equals(
                                            request
                                                    .getParentConsultationId()
                                    )
            ) {
                throw new RuntimeException(
                        "La llave de idempotencia ya fue utilizada "
                                + "para otra solicitud."
                );
            }

            return toResponse(
                    previousAppointment,
                    "La solicitud ya fue procesada anteriormente."
            );
        }

        MedicalConsultation consultation =
                findAndValidateParentConsultation(
                        request.getParentConsultationId(),
                        doctor
                );

        Appointment parentAppointment =
                consultation.getAppointment();

        validateContextEntities(
                parentAppointment,
                doctor
        );

        FollowUpType followUpType =
                parseFollowUpType(
                        request.getFollowUpType()
                );

        AppointmentPriority priority =
                parsePriority(
                        request.getPriority()
                );

        LocalDateTime selectedDateTime =
                parseAppointmentDateTime(
                        request.getAppointmentDateTime()
                );

        String reason =
                validateReason(
                        request.getReason()
                );

        validateDuplicateFollowUp(
                consultation.getId(),
                followUpType
        );

        validateSelectedSlot(
                doctor,
                selectedDateTime
        );

        LocalDateTime now =
                LocalDateTime.now();

        Appointment appointment =
                Appointment.builder()
                        .patient(
                                parentAppointment
                                        .getPatient()
                        )
                        .doctor(doctor)
                        .branch(
                                parentAppointment
                                        .getBranch()
                        )
                        .specialty(
                                parentAppointment
                                        .getSpecialty()
                        )
                        .appointmentDateTime(
                                selectedDateTime
                        )
                        .reason(reason)
                        .status(
                                AppointmentStatus
                                        .PENDIENTE_DE_PAGO
                        )
                        .reservationExpiresAt(
                                now.plusMinutes(
                                        RESERVATION_MINUTES
                                )
                        )
                        .priority(priority)
                        .origin(
                                AppointmentOrigin.INTERNA
                        )
                        .followUp(true)
                        .followUpType(
                                followUpType
                        )
                        .parentConsultation(
                                consultation
                        )
                        .idempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .arrivalTime(null)
                        .active(true)
                        .build();

        try {
            appointment =
                    appointmentRepository
                            .saveAndFlush(
                                    appointment
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            /*
             * PostgreSQL protege:
             *
             * - horario del médico;
             * - idempotencia;
             * - seguimiento duplicado.
             */
            Appointment idempotentAppointment =
                    appointmentRepository
                            .findByIdempotencyKey(
                                    request
                                            .getIdempotencyKey()
                            )
                            .orElse(null);

            if (idempotentAppointment != null) {
                return toResponse(
                        idempotentAppointment,
                        "La solicitud ya fue procesada anteriormente."
                );
            }

            throw new RuntimeException(
                    "El horario seleccionado ya no está disponible. "
                            + "Por favor, elija otro horario."
            );
        }

        auditService.log(
                doctor.getUsername(),
                "CREATE_FOLLOW_UP_APPOINTMENT",
                "APPOINTMENTS",
                "Cita de seguimiento ID "
                        + appointment.getId()
                        + " creada desde consulta ID "
                        + consultation.getId()
                        + ". Tipo: "
                        + followUpType.name()
                        + "."
        );

        /*
         * El listener se ejecutará AFTER_COMMIT.
         */
        applicationEventPublisher.publishEvent(
                new FollowUpAppointmentCreatedEvent(
                        appointment.getId()
                )
        );

        return toResponse(
                appointment,
                "Cita de seguimiento creada correctamente. "
                        + "El horario permanecerá reservado "
                        + "durante 5 minutos mientras se "
                        + "completa el pago."
        );
    }

    /**
     * Lista los seguimientos creados desde una
     * consulta concreta.
     */
    @Transactional(readOnly = true)
    public List<FollowUpAppointmentResponse>
    getFollowUpsByConsultation(
            Long consultationId,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        MedicalConsultation consultation =
                findAndValidateParentConsultation(
                        consultationId,
                        doctor
                );

        return appointmentRepository
                .findByParentConsultation_IdAndFollowUpTrueOrderByAppointmentDateTimeDesc(
                        consultation.getId()
                )
                .stream()
                .map(appointment ->
                        toResponse(
                                appointment,
                                "Cita de seguimiento."
                        )
                )
                .toList();
    }

    /**
     * Valida el médico autenticado.
     */
    private User validateAuthenticatedDoctor(
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
                        ) ||
                        !"MEDICO".equals(
                                normalizeRole(
                                        doctor
                                                .getRole()
                                                .getName()
                                )
                        )
        ) {
            throw new RuntimeException(
                    "Solamente un médico puede programar "
                            + "citas de seguimiento."
            );
        }

        return doctor;
    }

    /**
     * Busca la consulta y valida que pertenezca
     * al médico autenticado.
     */
    private MedicalConsultation
    findAndValidateParentConsultation(
            Long consultationId,
            User doctor
    ) {
        if (consultationId == null) {
            throw new RuntimeException(
                    "Debe indicar la consulta médica de origen."
            );
        }

        MedicalConsultation consultation =
                medicalConsultationRepository
                        .findByIdAndDoctor_Username(
                                consultationId,
                                doctor.getUsername()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "La consulta no fue encontrada "
                                                + "o no pertenece al "
                                                + "médico autenticado."
                                )
                        );

        if (
                consultation.getStatus()
                        != MedicalConsultationStatus
                        .EN_CURSO
                        &&
                        consultation.getStatus()
                                != MedicalConsultationStatus
                                .FINALIZADA
        ) {
            throw new RuntimeException(
                    "La consulta no se encuentra habilitada "
                            + "para programar seguimiento."
            );
        }

        if (
                consultation.getAppointment() == null
        ) {
            throw new RuntimeException(
                    "La consulta no tiene una cita de origen."
            );
        }

        return consultation;
    }

    /**
     * Valida paciente, médico, sucursal y especialidad
     * obtenidos desde la consulta padre.
     */
    private void validateContextEntities(
            Appointment parentAppointment,
            User doctor
    ) {
        User patient =
                parentAppointment.getPatient();

        Branch branch =
                parentAppointment.getBranch();

        Specialty specialty =
                parentAppointment.getSpecialty();

        if (
                patient == null ||
                        !Boolean.TRUE.equals(
                                patient.getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El paciente se encuentra inactivo "
                            + "o no está disponible."
            );
        }

        if (
                parentAppointment.getDoctor() == null ||
                        !parentAppointment
                                .getDoctor()
                                .getId()
                                .equals(
                                        doctor.getId()
                                )
        ) {
            throw new RuntimeException(
                    "La consulta no pertenece al médico autenticado."
            );
        }

        if (
                branch == null ||
                        !Boolean.TRUE.equals(
                                branch.getActive()
                        )
        ) {
            throw new RuntimeException(
                    "La sucursal de la consulta "
                            + "se encuentra inactiva."
            );
        }

        if (
                specialty == null ||
                        !Boolean.TRUE.equals(
                                specialty.getActive()
                        )
        ) {
            throw new RuntimeException(
                    "La especialidad de la consulta "
                            + "se encuentra inactiva."
            );
        }

        if (
                doctor.getBranch() == null ||
                        !doctor
                                .getBranch()
                                .getId()
                                .equals(
                                        branch.getId()
                                )
        ) {
            throw new RuntimeException(
                    "El médico ya no pertenece a la "
                            + "sucursal de la consulta."
            );
        }

        if (
                doctor.getSpecialty() == null ||
                        !doctor
                                .getSpecialty()
                                .getId()
                                .equals(
                                        specialty.getId()
                                )
        ) {
            throw new RuntimeException(
                    "El médico ya no pertenece a la "
                            + "especialidad de la consulta."
            );
        }
    }

    /**
     * Impide crear dos seguimientos activos
     * del mismo tipo para una consulta.
     */
    private void validateDuplicateFollowUp(
            Long consultationId,
            FollowUpType followUpType
    ) {
        boolean exists =
                appointmentRepository
                        .existsByParentConsultation_IdAndFollowUpTypeAndStatusNotInAndActiveTrue(
                                consultationId,
                                followUpType,
                                NON_BLOCKING_STATUSES
                        );

        if (exists) {
            throw new RuntimeException(
                    "La consulta ya cuenta con una cita "
                            + "de seguimiento activa del tipo "
                            + formatEnumText(
                            followUpType.name()
                    )
                            + "."
            );
        }
    }

    /**
     * Revalida el horario en la transacción
     * de creación.
     */
    private void validateSelectedSlot(
            User doctor,
            LocalDateTime selectedDateTime
    ) {
        if (
                !selectedDateTime.isAfter(
                        LocalDateTime.now()
                )
        ) {
            throw new RuntimeException(
                    "Debe seleccionar una fecha y hora futuras."
            );
        }

        if (
                !getBaseSlots().contains(
                        selectedDateTime
                                .toLocalTime()
                )
        ) {
            throw new RuntimeException(
                    "El horario seleccionado no pertenece "
                            + "a los horarios disponibles."
            );
        }

        List<Appointment> appointments =
                appointmentRepository
                        .findByDoctor_IdAndAppointmentDateTime(
                                doctor.getId(),
                                selectedDateTime
                        );

        boolean occupied =
                appointments.stream()
                        .anyMatch(
                                this::isAppointmentBlockingSlot
                        );

        if (occupied) {
            throw new RuntimeException(
                    "El horario seleccionado ya no está disponible. "
                            + "Por favor, elija otro horario."
            );
        }
    }

    /**
     * Determina si una cita bloquea el horario.
     */
    private boolean isAppointmentBlockingSlot(
            Appointment appointment
    ) {
        if (
                !Boolean.TRUE.equals(
                        appointment.getActive()
                )
        ) {
            return false;
        }

        if (
                NON_BLOCKING_STATUSES.contains(
                        appointment.getStatus()
                )
        ) {
            return false;
        }

        if (
                appointment.getStatus()
                        == AppointmentStatus
                        .PENDIENTE_DE_PAGO
        ) {
            LocalDateTime expiration =
                    appointment
                            .getReservationExpiresAt();

            return expiration != null
                    && expiration.isAfter(
                    LocalDateTime.now()
            );
        }

        /*
         * Cualquier otra cita activa protege el horario.
         */
        return true;
    }

    private FollowUpType parseFollowUpType(
            String value
    ) {
        try {
            return FollowUpType.valueOf(
                    normalizeRequiredText(
                            value,
                            50,
                            "Debe seleccionar el tipo "
                                    + "de seguimiento."
                    ).toUpperCase(
                            Locale.ROOT
                    )
            );

        } catch (IllegalArgumentException exception) {
            throw new RuntimeException(
                    "El tipo de seguimiento no es válido. "
                            + "Use MONITOREO_TRATAMIENTO o "
                            + "REVISION_RESULTADOS_LABORATORIO."
            );
        }
    }

    private AppointmentPriority parsePriority(
            String value
    ) {
        try {
            return AppointmentPriority.valueOf(
                    normalizeRequiredText(
                            value,
                            20,
                            "Debe seleccionar la prioridad."
                    ).toUpperCase(
                            Locale.ROOT
                    )
            );

        } catch (IllegalArgumentException exception) {
            throw new RuntimeException(
                    "La prioridad no es válida. "
                            + "Use NORMAL o EMERGENCIA."
            );
        }
    }

    private LocalDateTime parseAppointmentDateTime(
            String value
    ) {
        try {
            return LocalDateTime.parse(
                    normalizeRequiredText(
                            value,
                            50,
                            "Debe seleccionar la fecha "
                                    + "y hora del seguimiento."
                    )
            );

        } catch (DateTimeParseException exception) {
            throw new RuntimeException(
                    "El formato de la fecha y hora "
                            + "no es válido."
            );
        }
    }

    private String validateReason(
            String value
    ) {
        String reason =
                normalizeRequiredText(
                        value,
                        2000,
                        "El motivo del seguimiento "
                                + "es obligatorio."
                );

        if (reason.length() < 10) {
            throw new RuntimeException(
                    "El motivo debe contener al menos "
                            + "10 caracteres."
            );
        }

        return reason;
    }

    private void validateCreateRequest(
            CreateFollowUpAppointmentRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del seguimiento son obligatorios."
            );
        }

        if (request.getParentConsultationId() == null) {
            throw new RuntimeException(
                    "Debe indicar la consulta médica de origen."
            );
        }

        if (request.getIdempotencyKey() == null) {
            throw new RuntimeException(
                    "La llave de idempotencia es obligatoria."
            );
        }
    }

    private String normalizeRequiredText(
            String value,
            int maximumLength,
            String requiredMessage
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
                cleanValue.length()
                        > maximumLength
        ) {
            throw new RuntimeException(
                    "El campo no puede exceder los "
                            + maximumLength
                            + " caracteres."
            );
        }

        return cleanValue;
    }

    private String normalizeRole(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private String formatEnumText(
            String value
    ) {
        return value == null
                ? "No disponible"
                : value.replace(
                "_",
                " "
        );
    }

    /**
     * Mantiene los mismos horarios del wizard normal.
     */
    private List<LocalTime> getBaseSlots() {
        return List.of(
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0)
        );
    }

    /**
     * Convierte una cita de seguimiento en DTO.
     */
    private FollowUpAppointmentResponse toResponse(
            Appointment appointment,
            String message
    ) {
        return FollowUpAppointmentResponse
                .builder()
                .appointmentId(
                        appointment.getId()
                )
                .parentConsultationId(
                        appointment
                                .getParentConsultation()
                                .getId()
                )
                .parentAppointmentId(
                        appointment
                                .getParentConsultation()
                                .getAppointment()
                                .getId()
                )
                .patientName(
                        appointment
                                .getPatient()
                                .getFullName()
                )
                .patientDpi(
                        appointment
                                .getPatient()
                                .getDpi()
                )
                .doctorName(
                        appointment
                                .getDoctor()
                                .getFullName()
                )
                .specialty(
                        appointment
                                .getSpecialty()
                                .getName()
                )
                .branch(
                        appointment
                                .getBranch()
                                .getName()
                )
                .appointmentDateTime(
                        appointment
                                .getAppointmentDateTime()
                                .toString()
                )
                .reason(
                        appointment.getReason()
                )
                .priority(
                        appointment
                                .getPriority()
                                .name()
                )
                .status(
                        appointment
                                .getStatus()
                                .name()
                )
                .origin(
                        appointment
                                .getOrigin()
                                .name()
                )
                .followUp(
                        appointment.getFollowUp()
                )
                .followUpType(
                        appointment
                                .getFollowUpType()
                                .name()
                )
                .reservationExpiresAt(
                        appointment
                                .getReservationExpiresAt()
                                .toString()
                )
                .pendingPayment(
                        appointment.getStatus()
                                == AppointmentStatus
                                .PENDIENTE_DE_PAGO
                )
                .active(
                        appointment.getActive()
                )
                .message(message)
                .build();
    }
}