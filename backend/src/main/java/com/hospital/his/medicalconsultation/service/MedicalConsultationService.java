package com.hospital.his.medicalconsultation.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentPriority;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.laboratory.repository.LaboratoryOrderRepository;
import com.hospital.his.medicalconsultation.dto.DoctorQueueResponse;
import com.hospital.his.medicalconsultation.dto.Icd10CodeResponse;
import com.hospital.his.medicalconsultation.dto.MedicalConsultationResponse;
import com.hospital.his.medicalconsultation.dto.SaveMedicalConsultationRequest;
import com.hospital.his.medicalconsultation.dto.StartConsultationResponse;
import com.hospital.his.medicalconsultation.entity.Icd10Code;
import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import com.hospital.his.medicalconsultation.entity.MedicalConsultationStatus;
import com.hospital.his.medicalconsultation.repository.Icd10CodeRepository;
import com.hospital.his.medicalconsultation.repository.MedicalConsultationRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import com.hospital.his.vitalsigns.dto.VitalSignsResponse;
import com.hospital.his.vitalsigns.entity.VitalSigns;
import com.hospital.his.vitalsigns.repository.VitalSignsRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MedicalConsultationService {

    private static final Set<AppointmentStatus>
            DOCTOR_QUEUE_STATUSES = Set.of(
            AppointmentStatus.SIGNOS_REGISTRADOS,
            AppointmentStatus.CONSULTA_MEDICA,
            AppointmentStatus.CONSULTA_EVALUADA
    );

    private final AppointmentRepository
            appointmentRepository;

    private final MedicalConsultationRepository
            medicalConsultationRepository;

    private final VitalSignsRepository
            vitalSignsRepository;

    private final Icd10CodeRepository
            icd10CodeRepository;

    private final UserRepository
            userRepository;

    private final AuditService
            auditService;

    private final LaboratoryOrderRepository
            laboratoryOrderRepository;

    public MedicalConsultationService(
            AppointmentRepository appointmentRepository,
            MedicalConsultationRepository medicalConsultationRepository,
            VitalSignsRepository vitalSignsRepository,
            Icd10CodeRepository icd10CodeRepository,
            UserRepository userRepository,
            AuditService auditService, LaboratoryOrderRepository laboratoryOrderRepository
    ) {
        this.appointmentRepository =
                appointmentRepository;

        this.medicalConsultationRepository =
                medicalConsultationRepository;

        this.vitalSignsRepository =
                vitalSignsRepository;

        this.icd10CodeRepository =
                icd10CodeRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
        this.laboratoryOrderRepository = laboratoryOrderRepository;
    }

    /*
     * Devuelve únicamente citas asignadas al médico
     * autenticado y que pertenecen al flujo de consulta.
     */
    @Transactional(readOnly = true)
    public List<DoctorQueueResponse> getDoctorQueue(
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        List<Appointment> appointments =
                appointmentRepository
                        .findByDoctor_UsernameAndStatusInAndActiveTrueOrderByPriorityDescArrivalTimeAsc(
                                doctor.getUsername(),
                                DOCTOR_QUEUE_STATUSES
                        );

        return appointments.stream()
                .map(this::toQueueResponse)
                .toList();
    }

    /*
     * Inicia la consulta médica y crea el documento
     * clínico inicialmente vacío y en curso.
     */
    @Transactional
    public StartConsultationResponse startConsultation(
            Long appointmentId,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        Appointment appointment =
                findDoctorAppointment(
                        appointmentId,
                        doctor.getUsername()
                );

        if (!Boolean.TRUE.equals(
                appointment.getActive()
        )) {
            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.CONSULTA_MEDICA) {

            MedicalConsultation existingConsultation =
                    medicalConsultationRepository
                            .findByAppointment_IdAndDoctor_Username(
                                    appointmentId,
                                    doctor.getUsername()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "La cita está en consulta, pero no se encontró su registro clínico."
                                    )
                            );

            return toStartResponse(
                    existingConsultation,
                    appointment,
                    "La consulta médica ya se encuentra en curso."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus.SIGNOS_REGISTRADOS) {

            throw new RuntimeException(
                    "La consulta solamente puede iniciarse cuando los signos vitales ya fueron registrados."
            );
        }

        if (!vitalSignsRepository
                .existsByAppointment_Id(
                        appointmentId
                )) {

            throw new RuntimeException(
                    "No se encontraron signos vitales registrados para esta cita."
            );
        }

        if (medicalConsultationRepository
                .existsByAppointment_Id(
                        appointmentId
                )) {

            throw new RuntimeException(
                    "La cita ya cuenta con una consulta médica."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        MedicalConsultation consultation =
                MedicalConsultation.builder()
                        .appointment(appointment)
                        .doctor(doctor)
                        .visitReason(null)
                        .clinicalFindings(null)
                        .icd10Code(null)
                        .icd10Description(null)
                        .diagnosis(null)
                        .treatmentPlan(null)
                        .additionalNotes(null)
                        .status(
                                MedicalConsultationStatus.EN_CURSO
                        )
                        .startedAt(now)
                        .updatedAt(now)
                        .finishedAt(null)
                        .careClosedAt(null)
                        .build();

        try {
            consultation =
                    medicalConsultationRepository
                            .saveAndFlush(
                                    consultation
                            );

        } catch (DataIntegrityViolationException exception) {
            throw new RuntimeException(
                    "La cita ya cuenta con una consulta médica."
            );
        }

        appointment.setStatus(
                AppointmentStatus.CONSULTA_MEDICA
        );

        try {
            appointmentRepository.saveAndFlush(
                    appointment
            );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "La cita fue actualizada por otro usuario. Actualice el panel e intente nuevamente."
            );
        }

        auditService.log(
                doctorUsername,
                "START_MEDICAL_CONSULTATION",
                "MEDICAL_CONSULTATION",
                "Consulta médica iniciada para la cita ID "
                        + appointment.getId()
                        + ". Consulta ID "
                        + consultation.getId()
                        + "."
        );

        return toStartResponse(
                consultation,
                appointment,
                "Consulta médica iniciada correctamente."
        );
    }

    /*
     * Devuelve la consulta y los signos vitales.
     */
    @Transactional(readOnly = true)
    public MedicalConsultationResponse
    getConsultationByAppointment(
            Long appointmentId,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        Appointment appointment =
                findDoctorAppointment(
                        appointmentId,
                        doctor.getUsername()
                );

        MedicalConsultation consultation =
                medicalConsultationRepository
                        .findByAppointment_IdAndDoctor_Username(
                                appointmentId,
                                doctor.getUsername()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La consulta médica no ha sido iniciada."
                                )
                        );

        VitalSigns vitalSigns =
                vitalSignsRepository
                        .findByAppointment_Id(
                                appointmentId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontraron signos vitales para esta cita."
                                )
                        );

        return toConsultationResponse(
                consultation,
                appointment,
                vitalSigns,
                "Consulta médica encontrada."
        );
    }

    /*
     * Guarda el formulario. Puede mantenerse EN_CURSO
     * o pasar a FINALIZADA.
     */
    @Transactional
    public MedicalConsultationResponse saveConsultation(
            Long appointmentId,
            SaveMedicalConsultationRequest request,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        validateSaveRequest(request);

        Appointment appointment =
                findDoctorAppointment(
                        appointmentId,
                        doctor.getUsername()
                );

        MedicalConsultation consultation =
                medicalConsultationRepository
                        .findByAppointment_IdAndDoctor_Username(
                                appointmentId,
                                doctor.getUsername()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La consulta médica no ha sido iniciada."
                                )
                        );

        if (appointment.getStatus()
                == AppointmentStatus.ATENCION_FINALIZADA) {

            throw new RuntimeException(
                    "La atención médica ya fue finalizada y no puede modificarse."
            );
        }

        MedicalConsultationStatus requestedStatus =
                parseConsultationStatus(
                        request.getStatus()
                );

        String visitReason =
                normalizeRequiredText(
                        request.getVisitReason(),
                        "El motivo de visita es obligatorio.",
                        1000
                );

        String diagnosis =
                normalizeOptionalText(
                        request.getDiagnosis(),
                        4000,
                        "El diagnóstico no puede exceder los 4000 caracteres."
                );

        if (requestedStatus
                == MedicalConsultationStatus.FINALIZADA
                && diagnosis == null) {

            throw new RuntimeException(
                    "No es posible finalizar la consulta sin registrar un diagnóstico. "
                            + "El campo Diagnóstico es obligatorio."
            );
        }

        Icd10Code selectedIcd10 =
                validateAndFindIcd10(
                        request.getIcd10Code()
                );

        consultation.setVisitReason(
                visitReason
        );

        consultation.setClinicalFindings(
                normalizeOptionalText(
                        request.getClinicalFindings(),
                        4000,
                        "Los hallazgos clínicos no pueden exceder los 4000 caracteres."
                )
        );

        if (selectedIcd10 != null) {
            consultation.setIcd10Code(
                    selectedIcd10.getCode()
            );

            consultation.setIcd10Description(
                    selectedIcd10.getDescription()
            );

        } else {
            consultation.setIcd10Code(null);
            consultation.setIcd10Description(null);
        }

        consultation.setDiagnosis(
                diagnosis
        );

        consultation.setTreatmentPlan(
                normalizeOptionalText(
                        request.getTreatmentPlan(),
                        4000,
                        "El plan de tratamiento no puede exceder los 4000 caracteres."
                )
        );

        consultation.setAdditionalNotes(
                normalizeOptionalText(
                        request.getAdditionalNotes(),
                        4000,
                        "Las notas adicionales no pueden exceder los 4000 caracteres."
                )
        );

        consultation.setUpdatedAt(
                LocalDateTime.now()
        );

        String message;

        if (requestedStatus
                == MedicalConsultationStatus.FINALIZADA) {

            consultation.setStatus(
                    MedicalConsultationStatus.FINALIZADA
            );

            if (consultation.getFinishedAt() == null) {
                consultation.setFinishedAt(
                        LocalDateTime.now()
                );
            }

            appointment.setStatus(
                    AppointmentStatus.CONSULTA_EVALUADA
            );

            message =
                    "La consulta ha sido finalizada exitosamente. "
                            + "El paciente puede proceder a las siguientes indicaciones médicas.";

        } else {
            consultation.setStatus(
                    MedicalConsultationStatus.EN_CURSO
            );

            /*
             * Si se vuelve a guardar en curso, la cita
             * permanece en la sección En Consulta.
             */
            appointment.setStatus(
                    AppointmentStatus.CONSULTA_MEDICA
            );

            message =
                    "La consulta médica fue guardada en curso.";
        }

        try {
            consultation =
                    medicalConsultationRepository
                            .saveAndFlush(
                                    consultation
                            );

            appointmentRepository.saveAndFlush(
                    appointment
            );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "La consulta fue actualizada por otro usuario. Actualice la información e intente nuevamente."
            );
        }

        auditService.log(
                doctorUsername,
                requestedStatus
                        == MedicalConsultationStatus.FINALIZADA
                        ? "FINISH_MEDICAL_CONSULTATION"
                        : "SAVE_MEDICAL_CONSULTATION",
                "MEDICAL_CONSULTATION",
                "Consulta ID "
                        + consultation.getId()
                        + " guardada con estado "
                        + consultation
                        .getStatus()
                        .name()
                        + ". Cita ID "
                        + appointment.getId()
                        + "."
        );

        VitalSigns vitalSigns =
                vitalSignsRepository
                        .findByAppointment_Id(
                                appointmentId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontraron signos vitales para esta cita."
                                )
                        );

        return toConsultationResponse(
                consultation,
                appointment,
                vitalSigns,
                message
        );
    }

    /*
     * Cierra la atención después de completar recetas,
     * órdenes o indicaciones necesarias.
     */
    @Transactional
    public DoctorQueueResponse finishCare(
            Long appointmentId,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        Appointment appointment =
                findDoctorAppointment(
                        appointmentId,
                        doctor.getUsername()
                );

        if (appointment.getStatus()
                == AppointmentStatus.ATENCION_FINALIZADA) {

            throw new RuntimeException(
                    "La atención de esta cita ya fue finalizada."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus.CONSULTA_EVALUADA) {

            throw new RuntimeException(
                    "La atención solamente puede finalizarse después de completar la evaluación médica."
            );
        }

        MedicalConsultation consultation =
                medicalConsultationRepository
                        .findByAppointment_IdAndDoctor_Username(
                                appointmentId,
                                doctor.getUsername()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontró la consulta médica de la cita."
                                )
                        );

        if (consultation.getStatus()
                != MedicalConsultationStatus.FINALIZADA) {

            throw new RuntimeException(
                    "La consulta clínica todavía no ha sido finalizada."
            );
        }

        if (consultation.getDiagnosis() == null
                || consultation
                .getDiagnosis()
                .isBlank()) {

            throw new RuntimeException(
                    "No es posible cerrar la atención sin diagnóstico."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        appointment.setStatus(
                AppointmentStatus.ATENCION_FINALIZADA
        );

        /*
         * La cita se mantiene activa como registro
         * clínico histórico. El horario ya ocurrió.
         */
        appointment.setActive(true);

        consultation.setCareClosedAt(now);
        consultation.setUpdatedAt(now);

        try {
            medicalConsultationRepository
                    .saveAndFlush(
                            consultation
                    );

            appointmentRepository.saveAndFlush(
                    appointment
            );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "La atención fue actualizada por otro usuario. Actualice el panel."
            );
        }

        auditService.log(
                doctorUsername,
                "FINISH_PATIENT_CARE",
                "MEDICAL_CONSULTATION",
                "Atención finalizada para la cita ID "
                        + appointment.getId()
                        + ". Consulta ID "
                        + consultation.getId()
                        + "."
        );

        return toQueueResponse(
                appointment
        );
    }

    /*
     * Marca como no asistió a un paciente que ya tenía
     * signos registrados, pero no respondió al llamado.
     */
    @Transactional
    public DoctorQueueResponse markNoShow(
            Long appointmentId,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        Appointment appointment =
                findDoctorAppointment(
                        appointmentId,
                        doctor.getUsername()
                );

        if (appointment.getStatus()
                == AppointmentStatus.NO_ASISTIO) {

            throw new RuntimeException(
                    "La cita ya fue marcada como No Asistió."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus.SIGNOS_REGISTRADOS) {

            throw new RuntimeException(
                    "La opción No Asistió solamente está disponible para pacientes en espera de consulta."
            );
        }

        appointment.setStatus(
                AppointmentStatus.NO_ASISTIO
        );

        /*
         * La cita queda cerrada y no debe bloquear
         * operaciones futuras.
         */
        appointment.setActive(false);

        try {
            appointment =
                    appointmentRepository
                            .saveAndFlush(
                                    appointment
                            );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "La cita fue actualizada por otro usuario. Actualice el panel."
            );
        }

        auditService.log(
                doctorUsername,
                "MARK_APPOINTMENT_NO_SHOW",
                "MEDICAL_CONSULTATION",
                "Cita ID "
                        + appointment.getId()
                        + " marcada como NO_ASISTIO."
        );

        return toQueueResponse(
                appointment
        );
    }

    /*
     * Autocompletado del catálogo CIE-10.
     */
    @Transactional(readOnly = true)
    public List<Icd10CodeResponse> searchIcd10(
            String query
    ) {
        if (query == null
                || query.isBlank()) {

            return List.of();
        }

        String cleanQuery =
                query.trim();

        if (cleanQuery.length() < 2) {
            return List.of();
        }

        if (cleanQuery.length() > 100) {
            throw new RuntimeException(
                    "El criterio de búsqueda CIE-10 no puede exceder los 100 caracteres."
            );
        }

        return icd10CodeRepository
                .searchActiveCodes(
                        cleanQuery,
                        PageRequest.of(0, 20)
                )
                .stream()
                .map(code ->
                        Icd10CodeResponse.builder()
                                .id(code.getId())
                                .code(code.getCode())
                                .description(
                                        code.getDescription()
                                )
                                .build()
                )
                .toList();
    }

    private User validateAuthenticatedDoctor(
            String doctorUsername
    ) {
        if (doctorUsername == null
                || doctorUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al médico autenticado."
            );
        }

        User doctor =
                userRepository
                        .findByUsername(
                                doctorUsername
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontró al médico autenticado."
                                )
                        );

        if (!Boolean.TRUE.equals(
                doctor.getActive()
        )) {
            throw new RuntimeException(
                    "La cuenta del médico se encuentra inactiva."
            );
        }

        String roleName =
                doctor.getRole() != null
                        ? doctor
                        .getRole()
                        .getName()
                        : null;

        if (!isDoctorRole(roleName)) {
            throw new RuntimeException(
                    "Solamente un médico puede realizar esta operación."
            );
        }

        return doctor;
    }

    private boolean isDoctorRole(
            String roleName
    ) {
        if (roleName == null) {
            return false;
        }

        String normalizedRole =
                roleName
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replace("É", "E");

        return normalizedRole.equals(
                "MEDICO"
        );
    }

    private Appointment findDoctorAppointment(
            Long appointmentId,
            String doctorUsername
    ) {
        if (appointmentId == null) {
            throw new RuntimeException(
                    "Debe indicar el número de cita."
            );
        }

        return appointmentRepository
                .findByIdAndDoctor_Username(
                        appointmentId,
                        doctorUsername
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "La cita no fue encontrada o no pertenece al médico autenticado."
                        )
                );
    }

    private void validateSaveRequest(
            SaveMedicalConsultationRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la consulta son obligatorios."
            );
        }

        if (request.getStatus() == null
                || request.getStatus().isBlank()) {

            throw new RuntimeException(
                    "Debe indicar el estado de la consulta."
            );
        }
    }

    private MedicalConsultationStatus
    parseConsultationStatus(
            String status
    ) {
        try {
            return MedicalConsultationStatus
                    .valueOf(
                            status
                                    .trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    )
                    );

        } catch (IllegalArgumentException exception) {
            throw new RuntimeException(
                    "El estado de consulta no es válido. Use EN_CURSO o FINALIZADA."
            );
        }
    }

    private String normalizeRequiredText(
            String value,
            String requiredMessage,
            int maximumLength
    ) {
        if (value == null
                || value.isBlank()) {

            throw new RuntimeException(
                    requiredMessage
            );
        }

        String cleanValue =
                value.trim();

        if (cleanValue.length() > maximumLength) {
            throw new RuntimeException(
                    "El campo no puede exceder los "
                            + maximumLength
                            + " caracteres."
            );
        }

        return cleanValue;
    }

    private String normalizeOptionalText(
            String value,
            int maximumLength,
            String lengthMessage
    ) {
        if (value == null
                || value.isBlank()) {

            return null;
        }

        String cleanValue =
                value.trim();

        if (cleanValue.length() > maximumLength) {
            throw new RuntimeException(
                    lengthMessage
            );
        }

        return cleanValue;
    }

    private Icd10Code validateAndFindIcd10(
            String icd10Code
    ) {
        if (icd10Code == null
                || icd10Code.isBlank()) {

            return null;
        }

        String cleanCode =
                icd10Code
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        return icd10CodeRepository
                .findByCodeIgnoreCaseAndActiveTrue(
                        cleanCode
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "El código CIE-10 seleccionado no existe o se encuentra inactivo."
                        )
                );
    }

    private DoctorQueueResponse toQueueResponse(
            Appointment appointment
    ) {

        MedicalConsultation consultation =
                medicalConsultationRepository
                        .findByAppointment_Id(
                                appointment.getId()
                        )
                        .orElse(null);

        boolean hasActiveLaboratoryOrder = consultation != null
                && laboratoryOrderRepository
                .existsByMedicalConsultation_IdAndActiveTrue(
                        consultation.getId()
                );

        return DoctorQueueResponse.builder()
                .appointmentId(
                        appointment.getId()
                )
                .consultationId(
                        consultation != null
                                ? consultation.getId()
                                : null
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
                .priority(
                        appointment
                                .getPriority()
                                .name()
                )
                .appointmentStatus(
                        appointment
                                .getStatus()
                                .name()
                )
                .consultationStatus(
                        consultation != null
                                ? consultation
                                .getStatus()
                                .name()
                                : null
                )
                .appointmentDateTime(
                        appointment
                                .getAppointmentDateTime()
                                .toString()
                )
                .arrivalTime(
                        appointment.getArrivalTime() != null
                                ? appointment
                                .getArrivalTime()
                                .toString()
                                : null
                )
                .emergency(
                        appointment.getPriority()
                                == AppointmentPriority.EMERGENCIA
                )
                .canStartConsultation(
                        appointment.getStatus()
                                == AppointmentStatus.SIGNOS_REGISTRADOS
                )
                .canOpenConsultation(
                        appointment.getStatus()
                                == AppointmentStatus.CONSULTA_MEDICA
                                || appointment.getStatus()
                                == AppointmentStatus.CONSULTA_EVALUADA
                )
                .canFinishCare(
                        appointment.getStatus()
                                == AppointmentStatus.CONSULTA_EVALUADA
                )
                .canMarkNoShow(
                        appointment.getStatus()
                                == AppointmentStatus.SIGNOS_REGISTRADOS
                )
                .build();
    }

    private StartConsultationResponse toStartResponse(
            MedicalConsultation consultation,
            Appointment appointment,
            String message
    ) {
        return StartConsultationResponse.builder()
                .appointmentId(
                        appointment.getId()
                )
                .consultationId(
                        consultation.getId()
                )
                .patientName(
                        appointment
                                .getPatient()
                                .getFullName()
                )
                .appointmentStatus(
                        appointment
                                .getStatus()
                                .name()
                )
                .consultationStatus(
                        consultation
                                .getStatus()
                                .name()
                )
                .startedAt(
                        consultation
                                .getStartedAt()
                                .toString()
                )
                .message(message)
                .build();
    }

    private MedicalConsultationResponse
    toConsultationResponse(
            MedicalConsultation consultation,
            Appointment appointment,
            VitalSigns vitalSigns,
            String message
    ) {
        return MedicalConsultationResponse.builder()
                .consultationId(
                        consultation.getId()
                )
                .appointmentId(
                        appointment.getId()
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
                        consultation
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
                .priority(
                        appointment
                                .getPriority()
                                .name()
                )
                .appointmentStatus(
                        appointment
                                .getStatus()
                                .name()
                )
                .consultationStatus(
                        consultation
                                .getStatus()
                                .name()
                )
                .appointmentDateTime(
                        appointment
                                .getAppointmentDateTime()
                                .toString()
                )
                .visitReason(
                        consultation.getVisitReason()
                )
                .clinicalFindings(
                        consultation
                                .getClinicalFindings()
                )
                .icd10Code(
                        consultation.getIcd10Code()
                )
                .icd10Description(
                        consultation
                                .getIcd10Description()
                )
                .diagnosis(
                        consultation.getDiagnosis()
                )
                .treatmentPlan(
                        consultation
                                .getTreatmentPlan()
                )
                .additionalNotes(
                        consultation
                                .getAdditionalNotes()
                )
                .startedAt(
                        consultation
                                .getStartedAt()
                                .toString()
                )
                .updatedAt(
                        consultation
                                .getUpdatedAt()
                                .toString()
                )
                .finishedAt(
                        consultation.getFinishedAt() != null
                                ? consultation
                                .getFinishedAt()
                                .toString()
                                : null
                )
                .careClosedAt(
                        consultation.getCareClosedAt() != null
                                ? consultation
                                .getCareClosedAt()
                                .toString()
                                : null
                )
                .vitalSigns(
                        toVitalSignsResponse(
                                vitalSigns
                        )
                )
                .message(message)
                .build();
    }

    private VitalSignsResponse toVitalSignsResponse(
            VitalSigns vitalSigns
    ) {
        List<String> alerts =
                parseClinicalAlerts(
                        vitalSigns
                                .getClinicalAlerts()
                );

        Appointment appointment =
                vitalSigns.getAppointment();

        return VitalSignsResponse.builder()
                .vitalSignsId(
                        vitalSigns.getId()
                )
                .sourceType("APPOINTMENT")
                .appointmentId(
                        appointment.getId()
                )
                .emergencyReceptionId(null)
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
                .nurseName(
                        vitalSigns
                                .getNurse()
                                .getFullName()
                )
                .systolicPressure(
                        vitalSigns
                                .getSystolicPressure()
                )
                .diastolicPressure(
                        vitalSigns
                                .getDiastolicPressure()
                )
                .temperature(
                        vitalSigns.getTemperature()
                )
                .weight(
                        vitalSigns.getWeight()
                )
                .height(
                        vitalSigns.getHeight()
                )
                .heartRate(
                        vitalSigns.getHeartRate()
                )
                .emergency(
                        vitalSigns.getEmergency()
                )
                .clinicalAlerts(alerts)
                .recordedAt(
                        vitalSigns
                                .getRecordedAt()
                                .toString()
                )
                .appointmentStatus(
                        appointment
                                .getStatus()
                                .name()
                )
                .message(
                        "Signos vitales registrados."
                )
                .build();
    }

    private List<String> parseClinicalAlerts(
            String clinicalAlerts
    ) {
        if (clinicalAlerts == null
                || clinicalAlerts.isBlank()) {

            return List.of();
        }

        return List.of(
                clinicalAlerts.split(
                        "\\s*\\|\\s*"
                )
        );
    }
}