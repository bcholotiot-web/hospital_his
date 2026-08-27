package com.hospital.his.reception.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentPriority;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.dto.BranchResponse;
import com.hospital.his.catalogs.repository.BranchRepository;
import com.hospital.his.reception.dto.ReassignDoctorRequest;
import com.hospital.his.reception.dto.ReceptionAppointmentResponse;
import com.hospital.his.reception.dto.ReceptionDoctorResponse;
import com.hospital.his.reception.dto.ReceptionSearchResponse;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class ReceptionService {

    private static final Set<AppointmentStatus>
            RECEPTION_SEARCH_STATUSES = Set.of(
            AppointmentStatus.CONFIRMADA,
            AppointmentStatus.PACIENTE_PRESENTE,
            AppointmentStatus.PENDIENTE_DE_PAGO,
            AppointmentStatus.CANCELADA
    );

    private final AppointmentRepository appointmentRepository;

    private final UserRepository userRepository;

    private final AuditService auditService;

    private final BranchRepository branchRepository;

    public ReceptionService(AppointmentRepository appointmentRepository, UserRepository userRepository, AuditService auditService, BranchRepository branchRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.branchRepository = branchRepository;
    }

    @Transactional(readOnly = true)
    public ReceptionSearchResponse searchAppointment(String type, String value) {
        validateSearchParameters(type, value);

        String normalizedType =
                type.trim().toUpperCase();

        String cleanValue =
                value.trim();

        return switch (normalizedType) {

            case "APPOINTMENT_ID" -> searchByAppointmentId(cleanValue);
            case "DPI" -> searchByDpi(cleanValue);
            default -> throw new RuntimeException("El tipo de búsqueda no es válido. Use DPI o APPOINTMENT_ID.");
        };
    }

    private ReceptionSearchResponse searchByAppointmentId(String value) {
        Long appointmentId;

        try {
            appointmentId = Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new RuntimeException(
                    "El número de cita debe contener únicamente números."
            );
        }

        Optional<Appointment> appointmentOptional =
                appointmentRepository.findById(
                        appointmentId
                );

        if (appointmentOptional.isEmpty()) {
            return ReceptionSearchResponse.builder()
                    .resultType(
                            "APPOINTMENT_NOT_FOUND"
                    )
                    .message(
                            "No se encontró ninguna cita con el número ingresado."
                    )
                    .subText(
                            "Verifique el número de cita e intente nuevamente."
                    )
                    .showRegisterPatientButton(false)
                    .showNewAppointmentButton(false)
                    .appointment(null)
                    .build();
        }

        return buildAppointmentFoundResponse(
                appointmentOptional.get()
        );
    }

    private ReceptionSearchResponse searchByDpi(String dpi) {
        if (!dpi.matches("\\d{13}")) {
            throw new RuntimeException(
                    "El DPI debe contener exactamente 13 dígitos numéricos."
            );
        }

        Optional<User> patientOptional =
                userRepository.findByDpi(dpi);

        if (patientOptional.isEmpty()) {
            return ReceptionSearchResponse.builder()
                    .resultType(
                            "PATIENT_NOT_FOUND"
                    )
                    .message(
                            "No se encontró ningún paciente con ese DPI."
                    )
                    .subText(
                            "Es necesario registrar al paciente antes de continuar."
                    )
                    .showRegisterPatientButton(true)
                    .showNewAppointmentButton(false)
                    .appointment(null)
                    .build();
        }

        User patient =
                patientOptional.get();

        List<Appointment> appointments =
                appointmentRepository
                        .findByPatient_DpiAndStatusInOrderByAppointmentDateTimeDesc(
                                dpi,
                                RECEPTION_SEARCH_STATUSES
                        );

        if (appointments.isEmpty()) {
            return ReceptionSearchResponse.builder()
                    .resultType(
                            "PATIENT_WITHOUT_ACTIVE_APPOINTMENTS"
                    )
                    .message(
                            "El paciente "
                                    + patient.getFullName()
                                    + " está registrado pero no tiene citas activas."
                    )
                    .subText(
                            "Puede crear una nueva cita para este paciente."
                    )
                    .showRegisterPatientButton(false)
                    .showNewAppointmentButton(true)
                    .appointment(null)
                    .build();
        }

        /*
         * La consulta ya está ordenada de fecha más
         * reciente a menos reciente.
         */
        return buildAppointmentFoundResponse(
                appointments.get(0)
        );
    }

    @Transactional
    public ReceptionAppointmentResponse registerArrival(Long appointmentId, String receptionistUsername) {
        if (appointmentId == null) {
            throw new RuntimeException(
                    "Debe indicar el número de cita."
            );
        }

        if (receptionistUsername == null ||
                receptionistUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al recepcionista autenticado."
            );
        }

        Appointment appointment =
                appointmentRepository.findById(
                                appointmentId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cita no encontrada."
                                )
                        );

        validateArrivalRegistration(
                appointment
        );

        appointment.setStatus(
                AppointmentStatus.PACIENTE_PRESENTE
        );

        appointment.setArrivalTime(
                LocalDateTime.now()
        );

        try {
            appointment =
                    appointmentRepository.saveAndFlush(
                            appointment
                    );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "Operación no permitida. La cita fue actualizada por otro usuario. Verifique el estado actual e intente nuevamente."
            );
        }

        auditService.log(
                receptionistUsername,
                "REGISTER_PATIENT_ARRIVAL",
                "RECEPTION",
                "Llegada registrada para la cita ID "
                        + appointment.getId()
                        + ". Paciente: "
                        + appointment
                        .getPatient()
                        .getUsername()
                        + "."
        );

        String message;

        if (appointment.getPriority()
                == AppointmentPriority.EMERGENCIA) {

            message =
                    "Paciente "
                            + appointment
                            .getPatient()
                            .getFullName()
                            + " registrado con prioridad de EMERGENCIA. "
                            + "El paciente debe pasar directamente a toma de signos vitales.";

        } else {
            message =
                    "La llegada del paciente "
                            + appointment
                            .getPatient()
                            .getFullName()
                            + " ha sido registrada exitosamente. "
                            + "El paciente debe pasar a la sala de espera.";
        }

        return toReceptionResponse(
                appointment,
                message
        );
    }

    @Transactional(readOnly = true)
    public List<ReceptionDoctorResponse> getAvailableDoctorsForReassignment(Long appointmentId) {
        if (appointmentId == null) {
            throw new RuntimeException(
                    "Debe indicar el número de cita."
            );
        }

        Appointment appointment =
                appointmentRepository.findById(
                                appointmentId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cita no encontrada."
                                )
                        );

        validateReassignmentStatus(
                appointment
        );

        Long currentDoctorId =
                appointment
                        .getDoctor()
                        .getId();

        Long branchId =
                appointment
                        .getBranch()
                        .getId();

        Long specialtyId =
                appointment
                        .getSpecialty()
                        .getId();

        List<User> doctors =
                userRepository
                        .findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue(
                                "Médico",
                                branchId,
                                specialtyId
                        );

        return doctors.stream()
                .filter(doctor ->
                        !doctor.getId().equals(
                                currentDoctorId
                        )
                )
                .filter(doctor -> {
                    boolean occupied =
                            appointmentRepository
                                    .existsByDoctorAndAppointmentDateTimeAndActiveTrueAndIdNot(
                                            doctor,
                                            appointment
                                                    .getAppointmentDateTime(),
                                            appointment.getId()
                                    );

                    return !occupied;
                })
                .map(doctor ->
                        ReceptionDoctorResponse.builder()
                                .id(doctor.getId())
                                .fullName(
                                        doctor.getFullName()
                                )
                                .specialty(
                                        doctor.getSpecialty() != null
                                                ? doctor
                                                .getSpecialty()
                                                .getName()
                                                : null
                                )
                                .branch(
                                        doctor.getBranch() != null
                                                ? doctor
                                                .getBranch()
                                                .getName()
                                                : null
                                )
                                .available(true)
                                .build()
                )
                .toList();
    }


    @Transactional
    public ReceptionAppointmentResponse reassignDoctor(Long appointmentId, ReassignDoctorRequest request, String receptionistUsername) {
        if (appointmentId == null) {
            throw new RuntimeException(
                    "Debe indicar el número de cita."
            );
        }

        if (request == null ||
                request.getNewDoctorId() == null) {

            throw new RuntimeException(
                    "Debe seleccionar el nuevo médico."
            );
        }

        if (receptionistUsername == null ||
                receptionistUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al recepcionista autenticado."
            );
        }

        Appointment appointment =
                appointmentRepository.findById(
                                appointmentId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cita no encontrada."
                                )
                        );

        validateReassignmentStatus(
                appointment
        );

        User currentDoctor =
                appointment.getDoctor();

        User newDoctor =
                userRepository.findById(
                                request.getNewDoctorId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "El médico seleccionado no fue encontrado."
                                )
                        );

        validateNewDoctor(
                appointment,
                currentDoctor,
                newDoctor
        );

        boolean occupied =
                appointmentRepository
                        .existsByDoctorAndAppointmentDateTimeAndActiveTrueAndIdNot(
                                newDoctor,
                                appointment
                                        .getAppointmentDateTime(),
                                appointment.getId()
                        );

        if (occupied) {
            throw new RuntimeException(
                    "El médico seleccionado no está disponible en la fecha y hora de la cita."
            );
        }

        String note =
                normalizeReassignmentNote(
                        request.getNote()
                );

        Long previousDoctorId =
                currentDoctor.getId();

        String previousDoctorName =
                currentDoctor.getFullName();

        appointment.setDoctor(
                newDoctor
        );

        try {
            appointment =
                    appointmentRepository.saveAndFlush(
                            appointment
                    );

        } catch (OptimisticLockingFailureException exception) {
            throw new RuntimeException(
                    "Operación no permitida. La cita fue actualizada por otro usuario. Verifique el estado actual e intente nuevamente."
            );
        }

        String auditDescription =
                "Médico reasignado en la cita ID "
                        + appointment.getId()
                        + ". Médico anterior: "
                        + previousDoctorName
                        + " (ID "
                        + previousDoctorId
                        + "). Nuevo médico: "
                        + newDoctor.getFullName()
                        + " (ID "
                        + newDoctor.getId()
                        + ").";

        if (note != null) {
            auditDescription +=
                    " Motivo: " + note;
        }

        auditService.log(
                receptionistUsername,
                "REASSIGN_APPOINTMENT_DOCTOR",
                "RECEPTION",
                auditDescription
        );

        return toReceptionResponse(
                appointment,
                "Médico reasignado correctamente."
        );
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getActiveBranches() {

        List<BranchResponse> branches =
                branchRepository
                        .findByActiveTrue()
                        .stream()
                        .map(branch ->
                                BranchResponse.builder()
                                        .id(branch.getId())
                                        .name(branch.getName())
                                        .address(branch.getAddress())
                                        .active(branch.getActive())
                                        .build()
                        )
                        .toList();

        if (branches.isEmpty()) {
            throw new RuntimeException(
                    "No hay sucursales activas disponibles."
            );
        }

        return branches;
    }

    private void validateReassignmentStatus(Appointment appointment) {
        if (!Boolean.TRUE.equals(
                appointment.getActive())) {

            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        boolean validStatus =
                appointment.getStatus()
                        == AppointmentStatus.CONFIRMADA

                        || appointment.getStatus()
                        == AppointmentStatus.PACIENTE_PRESENTE;

        if (!validStatus) {
            throw new RuntimeException(
                    "Solamente se puede reasignar el médico de una cita confirmada o con el paciente presente."
            );
        }

        if (appointment.getDoctor() == null) {
            throw new RuntimeException(
                    "La cita no tiene un médico asignado."
            );
        }
    }

    private void validateNewDoctor(Appointment appointment, User currentDoctor, User newDoctor) {
        if (newDoctor.getId().equals(
                currentDoctor.getId())) {

            throw new RuntimeException(
                    "Debe seleccionar un médico diferente al médico actual."
            );
        }

        if (!Boolean.TRUE.equals(
                newDoctor.getActive())) {

            throw new RuntimeException(
                    "El médico seleccionado no se encuentra activo."
            );
        }

        String roleName =
                newDoctor.getRole() != null
                        ? newDoctor
                        .getRole()
                        .getName()
                        : null;

        if (!isDoctorRole(roleName)) {
            throw new RuntimeException(
                    "El usuario seleccionado no corresponde a un médico."
            );
        }

        if (newDoctor.getBranch() == null ||
                !newDoctor
                        .getBranch()
                        .getId()
                        .equals(
                                appointment
                                        .getBranch()
                                        .getId()
                        )) {

            throw new RuntimeException(
                    "El nuevo médico debe pertenecer a la misma sucursal de la cita."
            );
        }

        if (newDoctor.getSpecialty() == null ||
                !newDoctor
                        .getSpecialty()
                        .getId()
                        .equals(
                                appointment
                                        .getSpecialty()
                                        .getId()
                        )) {

            throw new RuntimeException(
                    "El nuevo médico debe pertenecer a la misma especialidad de la cita."
            );
        }
    }

    private boolean isDoctorRole(String roleName) {
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

    private String normalizeReassignmentNote(String note) {
        if (note == null ||
                note.isBlank()) {

            return null;
        }

        String cleanNote =
                note.trim();

        if (cleanNote.length() > 500) {
            throw new RuntimeException(
                    "La nota de reasignación no puede exceder los 500 caracteres."
            );
        }

        return cleanNote;
    }

    //Para pruebas con citas futuras, tendrás que usar una cita cuya fecha sea el día actual o asignar temporalmente la fecha desde PostgreSQL.
    private void validateArrivalRegistration(Appointment appointment) {
        if (!Boolean.TRUE.equals(
                appointment.getActive())) {

            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.PENDIENTE_DE_PAGO) {

            throw new RuntimeException(
                    "La cita del paciente tiene estado 'Pendiente de pago'. "
                            + "Debe realizar el pago en caja antes de ser atendido."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.CANCELADA) {

            throw new RuntimeException(
                    "La cita fue cancelada. El paciente debe agendar una nueva cita."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.EXPIRADA) {

            throw new RuntimeException(
                    "La reserva de la cita se encuentra expirada."
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.PACIENTE_PRESENTE) {

            throw new RuntimeException(
                    "La llegada del paciente ya fue registrada."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus.CONFIRMADA) {

            throw new RuntimeException(
                    "La cita no se encuentra en un estado válido para registrar la llegada."
            );
        }

        LocalDate appointmentDate =
                appointment
                        .getAppointmentDateTime()
                        .toLocalDate();

        if (!appointmentDate.equals(
                LocalDate.now())) {

            throw new RuntimeException(
                    "La llegada solamente puede registrarse el día programado de la cita."
            );
        }
    }

    private ReceptionSearchResponse buildAppointmentFoundResponse(Appointment appointment) {
        String message =
                buildSearchMessage(
                        appointment
                );

        return ReceptionSearchResponse.builder()
                .resultType(
                        "APPOINTMENT_FOUND"
                )
                .message(message)
                .subText(
                        buildSearchSubText(
                                appointment
                        )
                )
                .showRegisterPatientButton(false)
                .showNewAppointmentButton(
                        appointment.getStatus()
                                == AppointmentStatus.CANCELADA
                )
                .appointment(
                        toReceptionResponse(
                                appointment,
                                message
                        )
                )
                .build();
    }

    private String buildSearchMessage(Appointment appointment) {
        return switch (appointment.getStatus()) {

            case CONFIRMADA ->
                    "La cita se encuentra confirmada y disponible para registrar llegada.";

            case PACIENTE_PRESENTE ->
                    "Llegada registrada — esperando llamado de enfermería.";

            case PENDIENTE_DE_PAGO ->
                    "La cita del paciente tiene estado 'Pendiente de pago'. "
                            + "Debe realizar el pago en caja antes de ser atendido.";

            case CANCELADA ->
                    "La cita fue cancelada. El paciente debe agendar una nueva cita.";

            default ->
                    "La cita no se encuentra disponible para recepción.";
        };
    }

    private String buildSearchSubText(Appointment appointment) {
        return switch (appointment.getStatus()) {

            case CONFIRMADA ->
                    "Puede registrar la llegada del paciente.";

            case PACIENTE_PRESENTE ->
                    "El paciente ya se encuentra en la sala de espera.";

            case PENDIENTE_DE_PAGO ->
                    "Indique al paciente que debe dirigirse a caja.";

            case CANCELADA ->
                    "Puede crear una nueva cita para el paciente.";

            default ->
                    "Verifique el estado actual de la cita.";
        };
    }

    private ReceptionAppointmentResponse toReceptionResponse(Appointment appointment, String message) {
        return ReceptionAppointmentResponse.builder()
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
                .status(
                        appointment
                                .getStatus()
                                .name()
                )
                .priority(
                        appointment
                                .getPriority()
                                .name()
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
                .doctorName(
                        appointment
                                .getDoctor()
                                .getFullName()
                )
                .appointmentDateTime(
                        appointment
                                .getAppointmentDateTime()
                                .toString()
                )
                .reason(
                        appointment.getReason()
                )
                .arrivalTime(
                        appointment.getArrivalTime() != null
                                ? appointment
                                .getArrivalTime()
                                .toString()
                                : null
                )
                .arrivalRegistered(
                        appointment.getArrivalTime() != null
                )
                .canRegisterArrival(
                        appointment.getStatus()
                                == AppointmentStatus.CONFIRMADA
                                && Boolean.TRUE.equals(
                                appointment.getActive()
                        )
                )
                .message(message)
                .build();
    }

    private void validateSearchParameters(String type, String value) {
        if (type == null ||
                type.isBlank()) {

            throw new RuntimeException(
                    "Debe seleccionar un tipo de búsqueda."
            );
        }

        if (value == null ||
                value.isBlank()) {

            throw new RuntimeException(
                    "Debe ingresar un criterio de búsqueda."
            );
        }

        if (value.trim().length() > 25) {
            throw new RuntimeException(
                    "El criterio de búsqueda no puede exceder los 25 caracteres."
            );
        }
    }
}