package com.hospital.his.vitalsigns.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentPriority;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import com.hospital.his.vitalsigns.dto.NursingQueueResponse;
import com.hospital.his.vitalsigns.dto.RegisterVitalSignsRequest;
import com.hospital.his.vitalsigns.dto.VitalSignsResponse;
import com.hospital.his.vitalsigns.entity.VitalSigns;
import com.hospital.his.vitalsigns.repository.VitalSignsRepository;
import com.hospital.his.reception.entity.EmergencyReception;
import com.hospital.his.reception.entity.EmergencyReceptionStatus;
import com.hospital.his.reception.repository.EmergencyReceptionRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Comparator;

@Service
public class NursingService {

    private static final Set<AppointmentStatus>
            NURSING_QUEUE_STATUSES = Set.of(
            AppointmentStatus.PACIENTE_PRESENTE,
            AppointmentStatus.SIGNOS_VITALES
    );

    private final AppointmentRepository appointmentRepository;

    private final VitalSignsRepository vitalSignsRepository;

    private final UserRepository userRepository;

    private final EmergencyReceptionRepository emergencyReceptionRepository;

    private final AuditService auditService;

    public NursingService(
            AppointmentRepository appointmentRepository,
            VitalSignsRepository vitalSignsRepository,
            UserRepository userRepository, EmergencyReceptionRepository emergencyReceptionRepository,
            AuditService auditService
    ) {
        this.appointmentRepository =
                appointmentRepository;

        this.vitalSignsRepository =
                vitalSignsRepository;

        this.userRepository =
                userRepository;
        this.emergencyReceptionRepository = emergencyReceptionRepository;

        this.auditService =
                auditService;
    }

    /*
     * Devuelve los pacientes que esperan ser llamados
     * y los que ya están en proceso de toma de signos.
     */
    @Transactional(readOnly = true)
    public List<NursingQueueResponse> getNursingQueue() {

        List<Appointment> appointments =
                appointmentRepository
                        .findByStatusInAndActiveTrueOrderByPriorityDescArrivalTimeAsc(
                                NURSING_QUEUE_STATUSES
                        );

        Set<EmergencyReceptionStatus> emergencyStatuses =
                Set.of(
                        EmergencyReceptionStatus.PACIENTE_PRESENTE,
                        EmergencyReceptionStatus.EN_SIGNOS_VITALES
                );

        List<EmergencyReception> emergencies =
                emergencyReceptionRepository
                        .findByStatusInAndActiveTrueOrderByArrivalTimeAsc(
                                emergencyStatuses
                        );

        List<NursingQueueResponse> queue =
                new ArrayList<>();

        appointments.stream()
                .map(this::toQueueResponse)
                .forEach(queue::add);

        emergencies.stream()
                .map(this::toEmergencyQueueResponse)
                .forEach(queue::add);

        queue.sort(
                Comparator
                        .comparing(
                                NursingQueueResponse::getEmergency,
                                Comparator
                                        .nullsLast(
                                                Comparator.reverseOrder()
                                        )
                        )
                        .thenComparing(
                                NursingQueueResponse::getArrivalTime,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
        );

        return queue;
    }

    /*
     * Cambia la cita de PACIENTE_PRESENTE
     * a SIGNOS_VITALES.
     */
    @Transactional
    public NursingQueueResponse callPatient(Long appointmentId,String nurseUsername) {
        validateAuthenticatedNurse(
                nurseUsername
        );

        if (appointmentId == null) {
            throw new RuntimeException(
                    "Debe indicar el número de cita."
            );
        }

        Appointment appointment =
                appointmentRepository
                        .findById(appointmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cita no encontrada."
                                )
                        );

        if (!Boolean.TRUE.equals(
                appointment.getActive()
        )) {
            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.SIGNOS_VITALES) {

            throw new RuntimeException(
                    "El paciente ya fue llamado para toma de signos vitales."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.SIGNOS_REGISTRADOS) {

            throw new RuntimeException(
                    "Los signos vitales de esta cita ya fueron registrados."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus.PACIENTE_PRESENTE) {

            throw new RuntimeException(
                    "Solamente pueden llamarse pacientes con estado Paciente Presente."
            );
        }

        if (appointment.getArrivalTime() == null) {
            throw new RuntimeException(
                    "La cita no tiene registrada la hora de llegada."
            );
        }

        appointment.setStatus(
                AppointmentStatus.SIGNOS_VITALES
        );

        try {
            appointment =
                    appointmentRepository
                            .saveAndFlush(
                                    appointment
                            );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "La cita fue actualizada por otro usuario. Actualice la cola e intente nuevamente."
            );
        }

        auditService.log(
                nurseUsername,
                "CALL_PATIENT_FOR_VITAL_SIGNS",
                "NURSING",
                "Paciente llamado para toma de signos vitales. Cita ID: "
                        + appointment.getId()
                        + "."
        );

        return toQueueResponse(
                appointment
        );
    }

    /*
     * Registra los signos vitales y obtiene al
     * personal de enfermería desde el JWT.
     *
     aceptar tanto citas normales como registros de emergencia.

     */
    @Transactional
    public VitalSignsResponse registerVitalSigns(RegisterVitalSignsRequest request, String nurseUsername) {
        validateRegisterRequest(request);

        User nurse =
                validateAuthenticatedNurse(
                        nurseUsername
                );

        String sourceType =
                request.getSourceType()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (sourceType.equals("APPOINTMENT")) {
            return registerAppointmentVitalSigns(
                    request,
                    nurse,
                    nurseUsername
            );
        }

        return registerEmergencyVitalSigns(
                request,
                nurse,
                nurseUsername
        );
    }

    private VitalSignsResponse registerAppointmentVitalSigns(RegisterVitalSignsRequest request, User nurse, String nurseUsername) {
        Appointment appointment =
                appointmentRepository
                        .findById(
                                request.getAppointmentId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cita no encontrada."
                                )
                        );

        validateAppointmentForVitalSigns(
                appointment
        );

        if (vitalSignsRepository
                .existsByAppointment_Id(
                        appointment.getId()
                )) {

            throw new RuntimeException(
                    "Los signos vitales de esta cita ya fueron registrados."
            );
        }

        boolean emergency =
                Boolean.TRUE.equals(
                        request.getEmergency()
                )
                        || appointment.getPriority()
                        == AppointmentPriority.EMERGENCIA;

        List<String> clinicalAlerts =
                buildClinicalAlerts(request);

        VitalSigns vitalSigns =
                VitalSigns.builder()
                        .appointment(appointment)
                        .emergencyReception(null)
                        .nurse(nurse)
                        .systolicPressure(
                                request.getSystolicPressure()
                        )
                        .diastolicPressure(
                                request.getDiastolicPressure()
                        )
                        .temperature(
                                request.getTemperature()
                        )
                        .weight(
                                request.getWeight()
                        )
                        .height(
                                request.getHeight()
                        )
                        .heartRate(
                                request.getHeartRate()
                        )
                        .emergency(emergency)
                        .clinicalAlerts(
                                clinicalAlerts.isEmpty()
                                        ? null
                                        : String.join(
                                        " | ",
                                        clinicalAlerts
                                )
                        )
                        .recordedAt(
                                LocalDateTime.now()
                        )
                        .build();

        try {
            vitalSigns =
                    vitalSignsRepository.saveAndFlush(
                            vitalSigns
                    );

        } catch (DataIntegrityViolationException exception) {
            throw new RuntimeException(
                    "Los signos vitales de esta cita ya fueron registrados."
            );
        }

        appointment.setStatus(
                AppointmentStatus.SIGNOS_REGISTRADOS
        );

        if (emergency) {
            appointment.setPriority(
                    AppointmentPriority.EMERGENCIA
            );
        }

        try {
            appointmentRepository.saveAndFlush(
                    appointment
            );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "La cita fue actualizada por otro usuario. Verifique su estado actual."
            );
        }

        auditService.log(
                nurseUsername,
                "REGISTER_VITAL_SIGNS",
                "NURSING",
                "Signos vitales registrados para la cita ID "
                        + appointment.getId()
                        + ". Emergencia: "
                        + emergency
                        + ". Alertas clínicas: "
                        + clinicalAlerts.size()
                        + "."
        );

        String message =
                emergency
                        ? "Signos vitales de emergencia registrados para paciente "
                          + appointment
                        .getPatient()
                        .getFullName()
                          + ". El paciente debe pasar directamente a consulta médica."
                        : "Signos vitales del paciente "
                          + appointment
                        .getPatient()
                        .getFullName()
                          + " registrados correctamente. "
                          + "El paciente puede regresar a la sala de espera.";

        return toAppointmentVitalSignsResponse(
                vitalSigns,
                appointment,
                clinicalAlerts,
                message
        );
    }

    private VitalSignsResponse registerEmergencyVitalSigns(RegisterVitalSignsRequest request, User nurse, String nurseUsername) {
        EmergencyReception emergencyReception =
                emergencyReceptionRepository
                        .findById(
                                request.getEmergencyReceptionId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Registro de emergencia no encontrado."
                                )
                        );

        validateEmergencyForVitalSigns(
                emergencyReception
        );

        if (vitalSignsRepository
                .existsByEmergencyReception_Id(
                        emergencyReception.getId()
                )) {

            throw new RuntimeException(
                    "Los signos vitales de esta emergencia ya fueron registrados."
            );
        }

        List<String> clinicalAlerts =
                buildClinicalAlerts(request);

        VitalSigns vitalSigns =
                VitalSigns.builder()
                        .appointment(null)
                        .emergencyReception(
                                emergencyReception
                        )
                        .nurse(nurse)
                        .systolicPressure(
                                request.getSystolicPressure()
                        )
                        .diastolicPressure(
                                request.getDiastolicPressure()
                        )
                        .temperature(
                                request.getTemperature()
                        )
                        .weight(
                                request.getWeight()
                        )
                        .height(
                                request.getHeight()
                        )
                        .heartRate(
                                request.getHeartRate()
                        )
                        .emergency(true)
                        .clinicalAlerts(
                                clinicalAlerts.isEmpty()
                                        ? null
                                        : String.join(
                                        " | ",
                                        clinicalAlerts
                                )
                        )
                        .recordedAt(
                                LocalDateTime.now()
                        )
                        .build();

        try {
            vitalSigns =
                    vitalSignsRepository.saveAndFlush(
                            vitalSigns
                    );

        } catch (DataIntegrityViolationException exception) {
            throw new RuntimeException(
                    "Los signos vitales de esta emergencia ya fueron registrados."
            );
        }

        emergencyReception.setStatus(
                EmergencyReceptionStatus.SIGNOS_REGISTRADOS
        );

        try {
            emergencyReceptionRepository.saveAndFlush(
                    emergencyReception
            );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "El registro de emergencia fue actualizado por otro usuario."
            );
        }

        auditService.log(
                nurseUsername,
                "REGISTER_EMERGENCY_VITAL_SIGNS",
                "NURSING",
                "Signos vitales registrados para la emergencia ID "
                        + emergencyReception.getId()
                        + ". Alertas clínicas: "
                        + clinicalAlerts.size()
                        + "."
        );

        String message =
                "Signos vitales de emergencia registrados para paciente "
                        + emergencyReception.getPatientName()
                        + ". El paciente debe pasar directamente a consulta médica.";

        return toEmergencyVitalSignsResponse(
                vitalSigns,
                emergencyReception,
                clinicalAlerts,
                message
        );
    }

    //Validaciones para emergencias
    private void validateEmergencyForVitalSigns(EmergencyReception emergency) {
        if (!Boolean.TRUE.equals(
                emergency.getActive()
        )) {
            throw new RuntimeException(
                    "El registro de emergencia no se encuentra activo."
            );
        }

        if (emergency.getStatus()
                == EmergencyReceptionStatus.PACIENTE_PRESENTE) {

            throw new RuntimeException(
                    "Debe llamar al paciente de emergencia antes de registrar los signos vitales."
            );
        }

        if (emergency.getStatus()
                == EmergencyReceptionStatus.SIGNOS_REGISTRADOS) {

            throw new RuntimeException(
                    "Los signos vitales de esta emergencia ya fueron registrados."
            );
        }

        if (emergency.getStatus()
                != EmergencyReceptionStatus.EN_SIGNOS_VITALES) {

            throw new RuntimeException(
                    "El paciente de emergencia no se encuentra en proceso de toma de signos vitales."
            );
        }
    }

    private VitalSignsResponse toAppointmentVitalSignsResponse(VitalSigns vitalSigns, Appointment appointment, List<String> clinicalAlerts, String message) {
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
                        vitalSigns.getSystolicPressure()
                )
                .diastolicPressure(
                        vitalSigns.getDiastolicPressure()
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
                .clinicalAlerts(
                        clinicalAlerts
                )
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
                .message(message)
                .build();
    }

    private VitalSignsResponse toEmergencyVitalSignsResponse(VitalSigns vitalSigns, EmergencyReception emergency, List<String> clinicalAlerts, String message) {
        return VitalSignsResponse.builder()
                .vitalSignsId(
                        vitalSigns.getId()
                )
                .sourceType(
                        "EMERGENCY_RECEPTION"
                )
                .appointmentId(null)
                .emergencyReceptionId(
                        emergency.getId()
                )
                .patientName(
                        emergency.getPatientName()
                )
                .patientDpi(
                        emergency.getPatientDpi()
                )
                .nurseName(
                        vitalSigns
                                .getNurse()
                                .getFullName()
                )
                .systolicPressure(
                        vitalSigns.getSystolicPressure()
                )
                .diastolicPressure(
                        vitalSigns.getDiastolicPressure()
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
                .emergency(true)
                .clinicalAlerts(
                        clinicalAlerts
                )
                .recordedAt(
                        vitalSigns
                                .getRecordedAt()
                                .toString()
                )
                .appointmentStatus(
                        emergency
                                .getStatus()
                                .name()
                )
                .message(message)
                .build();
    }
    /*
     * Consulta los signos ya registrados de una cita.
     */
    @Transactional(readOnly = true)
    public VitalSignsResponse getVitalSignsByAppointment(Long appointmentId) {
        if (appointmentId == null) {
            throw new RuntimeException(
                    "Debe indicar el número de cita."
            );
        }

        VitalSigns vitalSigns =
                vitalSignsRepository
                        .findByAppointment_Id(
                                appointmentId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontraron signos vitales para la cita indicada."
                                )
                        );

        List<String> alerts =
                parseClinicalAlerts(
                        vitalSigns.getClinicalAlerts()
                );

        return toAppointmentVitalSignsResponse(
                vitalSigns,
                vitalSigns.getAppointment(),
                alerts,
                "Signos vitales encontrados."
        );
    }

    private User validateAuthenticatedNurse(String nurseUsername) {
        if (nurseUsername == null
                || nurseUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al personal de enfermería autenticado."
            );
        }

        User nurse =
                userRepository
                        .findByUsername(
                                nurseUsername
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontró al personal de enfermería autenticado."
                                )
                        );

        if (!Boolean.TRUE.equals(
                nurse.getActive()
        )) {
            throw new RuntimeException(
                    "La cuenta del personal de enfermería se encuentra inactiva."
            );
        }

        String roleName =
                nurse.getRole() != null
                        ? nurse
                        .getRole()
                        .getName()
                        : null;

        if (!isNursingRole(roleName)) {
            throw new RuntimeException(
                    "Solamente el personal de enfermería puede realizar esta operación."
            );
        }

        return nurse;
    }

    private boolean isNursingRole(String roleName) {
        if (roleName == null) {
            return false;
        }

        String normalizedRole =
                roleName
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replace("Í", "I");

        return normalizedRole.equals(
                "ENFERMERIA"
        );
    }

    private void validateAppointmentForVitalSigns(Appointment appointment) {
        if (!Boolean.TRUE.equals(
                appointment.getActive()
        )) {
            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.PACIENTE_PRESENTE) {

            throw new RuntimeException(
                    "Debe llamar al paciente antes de registrar los signos vitales."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.SIGNOS_REGISTRADOS) {

            throw new RuntimeException(
                    "Los signos vitales de esta cita ya fueron registrados."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus.SIGNOS_VITALES) {

            throw new RuntimeException(
                    "La cita no se encuentra en proceso de toma de signos vitales."
            );
        }
    }

    /*
     * Valida los rangos obligatorios de captura
     * definidos en CU-07.
     */
    private void validateRegisterRequest(RegisterVitalSignsRequest request) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de signos vitales son obligatorios."
            );
        }

        if (request.getSourceType() == null ||
                request.getSourceType().isBlank()) {

            throw new RuntimeException(
                    "Debe indicar el tipo de origen del paciente."
            );
        }

        String sourceType =
                request.getSourceType()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (!sourceType.equals("APPOINTMENT") &&
                !sourceType.equals(
                        "EMERGENCY_RECEPTION"
                )) {

            throw new RuntimeException(
                    "El tipo de origen no es válido. Use APPOINTMENT o EMERGENCY_RECEPTION."
            );
        }

        if (sourceType.equals("APPOINTMENT")) {
            if (request.getAppointmentId() == null) {
                throw new RuntimeException(
                        "Debe indicar el número de cita."
                );
            }

            if (request.getEmergencyReceptionId() != null) {
                throw new RuntimeException(
                        "No debe enviar un registro de emergencia para una cita normal."
                );
            }
        }

        if (sourceType.equals(
                "EMERGENCY_RECEPTION"
        )) {
            if (request.getEmergencyReceptionId() == null) {
                throw new RuntimeException(
                        "Debe indicar el registro de emergencia."
                );
            }

            if (request.getAppointmentId() != null) {
                throw new RuntimeException(
                        "No debe enviar un número de cita para un registro de emergencia."
                );
            }
        }

        validateIntegerRange(
                request.getSystolicPressure(),
                60,
                250,
                "La presión sistólica",
                "mmHg"
        );

        validateIntegerRange(
                request.getDiastolicPressure(),
                40,
                150,
                "La presión diastólica",
                "mmHg"
        );

        if (request.getSystolicPressure()
                <= request.getDiastolicPressure()) {

            throw new RuntimeException(
                    "La presión sistólica debe ser mayor que la presión diastólica."
            );
        }

        validateDecimalRange(
                request.getTemperature(),
                new BigDecimal("34.0"),
                new BigDecimal("42.0"),
                "La temperatura",
                "°C"
        );

        validateDecimalRange(
                request.getWeight(),
                new BigDecimal("0.5"),
                new BigDecimal("300.0"),
                "El peso",
                "kg"
        );

        validateDecimalRange(
                request.getHeight(),
                new BigDecimal("30.0"),
                new BigDecimal("250.0"),
                "La talla",
                "cm"
        );

        validateIntegerRange(
                request.getHeartRate(),
                30,
                220,
                "La frecuencia cardíaca",
                "lpm"
        );

        if (request.getEmergency() == null) {
            request.setEmergency(false);
        }
    }

    private void validateIntegerRange(Integer value, int minimum, int maximum, String fieldName, String unit) {
        if (value == null) {
            throw new RuntimeException(
                    fieldName
                            + " es obligatoria."
            );
        }

        if (value < minimum
                || value > maximum) {

            throw new RuntimeException(
                    fieldName
                            + " debe encontrarse entre "
                            + minimum
                            + " y "
                            + maximum
                            + " "
                            + unit
                            + "."
            );
        }
    }

    private void validateDecimalRange(BigDecimal value, BigDecimal minimum, BigDecimal maximum, String fieldName, String unit) {
        if (value == null) {
            throw new RuntimeException(
                    fieldName
                            + " es obligatorio."
            );
        }

        if (
                value.compareTo(minimum) < 0
                        || value.compareTo(maximum) > 0
        ) {
            throw new RuntimeException(
                    fieldName
                            + " debe encontrarse entre "
                            + minimum
                            + " y "
                            + maximum
                            + " "
                            + unit
                            + "."
            );
        }
    }

    /*
     * Umbrales visuales iniciales para adultos.
     * No realizan un diagnóstico.
     */
    private List<String> buildClinicalAlerts(RegisterVitalSignsRequest request) {
        List<String> alerts =
                new ArrayList<>();

        if (
                request.getSystolicPressure() < 90
                        || request.getDiastolicPressure() < 60
        ) {
            alerts.add(
                    "Presión arterial por debajo del rango clínico configurado."
            );
        }

        if (
                request.getSystolicPressure() > 120
                        || request.getDiastolicPressure() > 80
        ) {
            alerts.add(
                    "Presión arterial por encima del rango clínico configurado."
            );
        }

        if (
                request.getTemperature()
                        .compareTo(
                                new BigDecimal("36.5")
                        ) < 0
        ) {
            alerts.add(
                    "Temperatura por debajo del rango clínico configurado."
            );
        }

        if (
                request.getTemperature()
                        .compareTo(
                                new BigDecimal("37.3")
                        ) > 0
        ) {
            alerts.add(
                    "Temperatura por encima del rango clínico configurado."
            );
        }

        if (request.getHeartRate() < 60) {
            alerts.add(
                    "Frecuencia cardíaca por debajo del rango clínico configurado."
            );
        }

        if (request.getHeartRate() > 100) {
            alerts.add(
                    "Frecuencia cardíaca por encima del rango clínico configurado."
            );
        }

        return alerts;
    }

    private List<String> parseClinicalAlerts(String clinicalAlerts) {
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

    //Cita normal
    private NursingQueueResponse toQueueResponse(Appointment appointment) {
        return NursingQueueResponse.builder()
                .sourceType("APPOINTMENT")
                .sourceId(appointment.getId())
                .appointmentId(appointment.getId())
                .emergencyReceptionId(null)
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
                .registeredPatient(true)
                .canCallPatient(
                        appointment.getStatus()
                                == AppointmentStatus.PACIENTE_PRESENTE
                )
                .canRegisterVitalSigns(
                        appointment.getStatus()
                                == AppointmentStatus.SIGNOS_VITALES
                )
                .build();
    }

    private VitalSignsResponse toVitalSignsResponse(VitalSigns vitalSigns, Appointment appointment, List<String> clinicalAlerts, String message) {
        return VitalSignsResponse.builder()
                .vitalSignsId(
                        vitalSigns.getId()
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
                .clinicalAlerts(
                        clinicalAlerts
                )
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
                .message(message)
                .build();
    }

    private NursingQueueResponse
    toEmergencyQueueResponse(EmergencyReception emergency) {
        return NursingQueueResponse.builder()
                .sourceType(
                        "EMERGENCY_RECEPTION"
                )
                .sourceId(
                        emergency.getId()
                )
                .appointmentId(null)
                .emergencyReceptionId(
                        emergency.getId()
                )
                .patientName(
                        emergency.getPatientName()
                )
                .patientDpi(
                        emergency.getPatientDpi()
                )
                .doctorName(null)
                .specialty(
                        "ATENCION_DE_EMERGENCIA"
                )
                .branch(
                        emergency
                                .getBranch()
                                .getName()
                )
                .priority(
                        "EMERGENCIA"
                )
                .status(
                        emergency
                                .getStatus()
                                .name()
                )
                .appointmentDateTime(null)
                .arrivalTime(
                        emergency
                                .getArrivalTime()
                                .toString()
                )
                .emergency(true)
                .registeredPatient(
                        emergency.getPatient() != null
                )
                .canCallPatient(
                        emergency.getStatus()
                                == EmergencyReceptionStatus
                                .PACIENTE_PRESENTE
                )
                .canRegisterVitalSigns(
                        emergency.getStatus()
                                == EmergencyReceptionStatus
                                .EN_SIGNOS_VITALES
                )
                .build();
    }
    @Transactional
    public NursingQueueResponse callEmergencyPatient(Long emergencyReceptionId, String nurseUsername) {
        validateAuthenticatedNurse(
                nurseUsername
        );

        if (emergencyReceptionId == null) {
            throw new RuntimeException(
                    "Debe indicar el registro de emergencia."
            );
        }

        EmergencyReception emergency =
                emergencyReceptionRepository
                        .findById(
                                emergencyReceptionId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Registro de emergencia no encontrado."
                                )
                        );

        if (!Boolean.TRUE.equals(
                emergency.getActive()
        )) {
            throw new RuntimeException(
                    "El registro de emergencia no se encuentra activo."
            );
        }

        if (emergency.getStatus()
                == EmergencyReceptionStatus.EN_SIGNOS_VITALES) {

            throw new RuntimeException(
                    "El paciente de emergencia ya fue llamado."
            );
        }

        if (emergency.getStatus()
                != EmergencyReceptionStatus.PACIENTE_PRESENTE) {

            throw new RuntimeException(
                    "El registro de emergencia no está disponible para ser llamado."
            );
        }

        emergency.setStatus(
                EmergencyReceptionStatus.EN_SIGNOS_VITALES
        );

        emergency =
                emergencyReceptionRepository
                        .saveAndFlush(
                                emergency
                        );

        auditService.log(
                nurseUsername,
                "CALL_EMERGENCY_FOR_VITAL_SIGNS",
                "NURSING",
                "Paciente de emergencia llamado para signos vitales. "
                        + "Registro de emergencia ID: "
                        + emergency.getId()
                        + "."
        );

        return toEmergencyQueueResponse(
                emergency
        );
    }

    //Consulta de signos por emergencia
    @Transactional(readOnly = true)
    public VitalSignsResponse getVitalSignsByEmergency(Long emergencyReceptionId) {
        if (emergencyReceptionId == null) {
            throw new RuntimeException(
                    "Debe indicar el registro de emergencia."
            );
        }

        VitalSigns vitalSigns =
                vitalSignsRepository
                        .findByEmergencyReception_Id(
                                emergencyReceptionId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontraron signos vitales para la emergencia indicada."
                                )
                        );

        List<String> alerts =
                parseClinicalAlerts(
                        vitalSigns.getClinicalAlerts()
                );

        return toEmergencyVitalSignsResponse(
                vitalSigns,
                vitalSigns.getEmergencyReception(),
                alerts,
                "Signos vitales de emergencia encontrados."
        );
    }
}