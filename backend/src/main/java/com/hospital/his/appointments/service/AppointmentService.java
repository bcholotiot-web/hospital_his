package com.hospital.his.appointments.service;

import com.hospital.his.appointments.dto.AppointmentResponse;
import com.hospital.his.appointments.dto.AvailableSlotResponse;
import com.hospital.his.appointments.dto.CreateAppointmentRequest;
import com.hospital.his.appointments.dto.DoctorResponse;
import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentOrigin;
import com.hospital.his.appointments.entity.AppointmentPriority;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.dto.BranchResponse;
import com.hospital.his.catalogs.dto.SpecialtyResponse;
import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.entity.BranchSpecialty;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.catalogs.repository.BranchRepository;
import com.hospital.his.catalogs.repository.BranchSpecialtyRepository;
import com.hospital.his.catalogs.repository.SpecialtyRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final SpecialtyRepository specialtyRepository;
    private final BranchSpecialtyRepository branchSpecialtyRepository;
    private final AuditService auditService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            SpecialtyRepository specialtyRepository,
            BranchSpecialtyRepository branchSpecialtyRepository,
            AuditService auditService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.specialtyRepository = specialtyRepository;
        this.branchSpecialtyRepository = branchSpecialtyRepository;
        this.auditService = auditService;
    }

    /*
     * Paso 1 del wizard:
     * obtiene solamente las sucursales activas.
     */
    public List<BranchResponse> getActiveBranches() {

        List<Branch> branches =
                branchRepository.findByActiveTrue();

        if (branches.isEmpty()) {
            throw new RuntimeException(
                    "No hay sucursales activas disponibles."
            );
        }

        return branches.stream()
                .map(branch ->
                        BranchResponse.builder()
                                .id(branch.getId())
                                .name(branch.getName())
                                .address(branch.getAddress())
                                .active(branch.getActive())
                                .build()
                )
                .toList();
    }

    /*
     * Paso 2 del wizard:
     * obtiene las especialidades activas configuradas
     * para la sucursal seleccionada.
     */
    public List<SpecialtyResponse> getSpecialtiesByBranch(
            Long branchId
    ) {
        if (branchId == null) {
            throw new RuntimeException(
                    "Debe seleccionar una sucursal."
            );
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sucursal no encontrada."
                        )
                );

        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new RuntimeException(
                    "La sucursal seleccionada no se encuentra activa."
            );
        }

        List<BranchSpecialty> branchSpecialties =
                branchSpecialtyRepository
                        .findByBranch_IdAndActiveTrue(branchId);

        List<SpecialtyResponse> specialties =
                branchSpecialties.stream()
                        .filter(branchSpecialty ->
                                branchSpecialty.getSpecialty() != null
                                        && Boolean.TRUE.equals(
                                        branchSpecialty
                                                .getSpecialty()
                                                .getActive()
                                )
                        )
                        .map(branchSpecialty ->
                                SpecialtyResponse.builder()
                                        .id(
                                                branchSpecialty
                                                        .getSpecialty()
                                                        .getId()
                                        )
                                        .name(
                                                branchSpecialty
                                                        .getSpecialty()
                                                        .getName()
                                        )
                                        .description(
                                                branchSpecialty
                                                        .getSpecialty()
                                                        .getDescription()
                                        )
                                        .active(
                                                branchSpecialty
                                                        .getSpecialty()
                                                        .getActive()
                                        )
                                        .build()
                        )
                        .toList();

        if (specialties.isEmpty()) {
            throw new RuntimeException(
                    "No hay especialidades disponibles para la sucursal "
                            + branch.getName()
                            + ". Seleccione otra sucursal."
            );
        }

        return specialties;
    }

    /*
     * Paso 3 del wizard:
     * obtiene médicos activos asociados con la sucursal
     * y la especialidad seleccionadas.
     */
    public List<DoctorResponse> getDoctorsByBranchAndSpecialty(
            Long branchId,
            Long specialtyId
    ) {
        if (branchId == null) {
            throw new RuntimeException(
                    "Debe seleccionar una sucursal."
            );
        }

        if (specialtyId == null) {
            throw new RuntimeException(
                    "Debe seleccionar una especialidad médica."
            );
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sucursal no encontrada."
                        )
                );

        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new RuntimeException(
                    "La sucursal seleccionada no se encuentra activa."
            );
        }

        Specialty specialty =
                specialtyRepository.findById(specialtyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Especialidad no encontrada."
                                )
                        );

        if (!Boolean.TRUE.equals(specialty.getActive())) {
            throw new RuntimeException(
                    "La especialidad seleccionada no se encuentra activa."
            );
        }

        boolean branchHasSpecialty =
                branchSpecialtyRepository
                        .existsByBranch_IdAndSpecialty_IdAndActiveTrue(
                                branchId,
                                specialtyId
                        );

        if (!branchHasSpecialty) {
            throw new RuntimeException(
                    "La especialidad "
                            + specialty.getName()
                            + " no está disponible en la sucursal "
                            + branch.getName()
                            + "."
            );
        }

        /*
         * Usa exactamente el nombre almacenado en roles.name.
         * En tu base de datos actualmente es "Médico".
         */
        List<User> doctors =
                userRepository
                        .findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue(
                                "Médico",
                                branchId,
                                specialtyId
                        );

        if (doctors.isEmpty()) {
            throw new RuntimeException(
                    "No se encontraron horarios disponibles para la especialidad "
                            + specialty.getName()
                            + " en la sede "
                            + branch.getName()
                            + ". Por favor, seleccione otra especialidad o sede."
            );
        }

        return doctors.stream()
                .map(doctor ->
                        DoctorResponse.builder()
                                .id(doctor.getId())
                                .fullName(doctor.getFullName())
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
                                .build()
                )
                .toList();
    }

    /*
     * Paso 4 del wizard:
     * genera los horarios y marca los ocupados o pasados
     * como no disponibles.
     */
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, String date) {
        if (doctorId == null) {
            throw new RuntimeException("Debe seleccionar un médico.");
        }

        if (date == null || date.isBlank()) {
            throw new RuntimeException("Debe seleccionar una fecha.");
        }

        User doctor = userRepository.findById(doctorId).orElseThrow(() -> new RuntimeException("Médico no encontrado."));

        if (!Boolean.TRUE.equals(doctor.getActive())) {
            throw new RuntimeException(
                    "El médico seleccionado no se encuentra activo."
            );
        }

        String doctorRole =
                doctor.getRole() != null
                        ? doctor.getRole().getName()
                        : null;

        if (!isDoctorRole(doctorRole)) {
            throw new RuntimeException(
                    "El usuario seleccionado no corresponde a un médico."
            );
        }

        LocalDate selectedDate;

        try {
            selectedDate = LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            throw new RuntimeException(
                    "El formato de la fecha no es válido. Use el formato yyyy-MM-dd."
            );
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            throw new RuntimeException(
                    "Debe seleccionar una fecha actual o futura."
            );
        }

        LocalDateTime startOfDay =
                selectedDate.atStartOfDay();

        LocalDateTime endOfDay =
                selectedDate.atTime(LocalTime.MAX);

        List<Appointment> existingAppointments =
                appointmentRepository
                        .findByDoctorIdAndAppointmentDateTimeBetween(
                                doctorId,
                                startOfDay,
                                endOfDay
                        );

        LocalDateTime now = LocalDateTime.now();

        List<AvailableSlotResponse> availableSlots =
                new ArrayList<>();

        for (LocalTime slot : getBaseSlots()) {

            LocalDateTime slotDateTime =
                    selectedDate.atTime(slot);

            boolean occupied =
                    existingAppointments.stream()
                            .filter(appointment ->
                                    appointment
                                            .getAppointmentDateTime()
                                            .equals(slotDateTime)
                            )
                            .anyMatch(
                                    this::isAppointmentBlockingSlot
                            );

            boolean pastOrPresent =
                    !slotDateTime.isAfter(
                            LocalDateTime.now()
                    );

            availableSlots.add(
                    AvailableSlotResponse.builder()
                            .dateTime(
                                    slotDateTime.toString()
                            )
                            .time(
                                    slot.toString()
                            )
                            .available(
                                    !occupied
                                            && !pastOrPresent
                            )
                            .build()
            );
        }

        boolean hasAvailableSlot =
                availableSlots.stream()
                        .anyMatch(slot ->
                                Boolean.TRUE.equals(
                                        slot.getAvailable()
                                )
                        );

        if (!hasAvailableSlot) {
            throw new RuntimeException(
                    "No se encontraron horarios disponibles para el médico seleccionado en la fecha indicada."
            );
        }

        return availableSlots;
    }

    /*
     * Paso 5 del wizard:
     * registra la cita usando el username obtenido del JWT.
     */
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request, String authenticatedUsername) {
        validateCreateAppointment(request);

        if (authenticatedUsername == null
                || authenticatedUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al usuario autenticado."
            );
        }

        User patient =
                userRepository
                        .findByUsername(authenticatedUsername)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontró el usuario autenticado."
                                )
                        );

        if (!Boolean.TRUE.equals(patient.getActive())) {
            throw new RuntimeException(
                    "La cuenta del paciente se encuentra inactiva."
            );
        }

        String patientRole =
                patient.getRole() != null
                        ? patient.getRole().getName()
                        : null;

        if (!isPatientRole(patientRole)) {
            throw new RuntimeException(
                    "Solamente un paciente puede agendar una cita médica."
            );
        }

        User doctor =
                userRepository
                        .findById(request.getDoctorUserId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Médico no encontrado."
                                )
                        );

        if (!Boolean.TRUE.equals(doctor.getActive())) {
            throw new RuntimeException(
                    "El médico seleccionado no se encuentra activo."
            );
        }

        String doctorRole =
                doctor.getRole() != null
                        ? doctor.getRole().getName()
                        : null;

        if (!isDoctorRole(doctorRole)) {
            throw new RuntimeException(
                    "El usuario seleccionado no corresponde a un médico."
            );
        }

        Branch branch =
                branchRepository
                        .findById(request.getBranchId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Sucursal no encontrada."
                                )
                        );

        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new RuntimeException(
                    "La sucursal seleccionada no se encuentra activa."
            );
        }

        Specialty specialty =
                specialtyRepository
                        .findById(request.getSpecialtyId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Especialidad no encontrada."
                                )
                        );

        if (!Boolean.TRUE.equals(specialty.getActive())) {
            throw new RuntimeException(
                    "La especialidad seleccionada no se encuentra activa."
            );
        }

        boolean branchHasSpecialty =
                branchSpecialtyRepository
                        .existsByBranch_IdAndSpecialty_IdAndActiveTrue(
                                branch.getId(),
                                specialty.getId()
                        );

        if (!branchHasSpecialty) {
            throw new RuntimeException(
                    "La especialidad seleccionada no está disponible en la sucursal indicada."
            );
        }

        validateDoctorAssignment(
                doctor,
                branch,
                specialty
        );

        LocalDateTime appointmentDateTime;

        try {
            appointmentDateTime =
                    LocalDateTime.parse(
                            request.getAppointmentDateTime()
                    );
        } catch (DateTimeParseException exception) {
            throw new RuntimeException(
                    "El formato de la fecha y hora no es válido."
            );
        }

        if (!appointmentDateTime.isAfter(
                LocalDateTime.now()
        )) {
            throw new RuntimeException(
                    "Debe seleccionar una fecha y hora futuras."
            );
        }

        if (!isSupportedSlot(
                appointmentDateTime.toLocalTime()
        )) {
            throw new RuntimeException(
                    "El horario seleccionado no pertenece a los horarios disponibles."
            );
        }

        boolean occupied =
                appointmentRepository
                        .existsByDoctorAndAppointmentDateTimeAndActiveTrue(
                                doctor,
                                appointmentDateTime
                        );

        if (occupied) {
            throw new RuntimeException(
                    "El horario seleccionado ya no está disponible. Por favor, elija otro horario."
            );
        }

        Appointment appointment =
                Appointment.builder()
                        .patient(patient)
                        .doctor(doctor)
                        .branch(branch)
                        .specialty(specialty)
                        .appointmentDateTime(
                                appointmentDateTime
                        )
                        .reason(
                                request.getReason().trim()
                        )
                        .status(AppointmentStatus.PENDIENTE_DE_PAGO)
                        .reservationExpiresAt(LocalDateTime.now().plusMinutes(5))
                        .priority(AppointmentPriority.NORMAL)
                        .origin(AppointmentOrigin.PORTAL)
                        .arrivalTime(null)
                        .active(true)
                        .build();

        try {
            appointment =
                    appointmentRepository.saveAndFlush(
                            appointment
                    );
        } catch (DataIntegrityViolationException exception) {
            throw new RuntimeException(
                    "El horario seleccionado ya no está disponible. Por favor, elija otro horario."
            );
        }

        auditService.log(
                patient.getUsername(),
                "CREATE_APPOINTMENT",
                "APPOINTMENTS",
                "Cita registrada con estado pendiente de pago. ID: "
                        + appointment.getId()
        );

        return toResponse(appointment);
    }

    /*Este método:

    Obtiene el paciente a partir del username del JWT.
    Comprueba que esté activo.
    Comprueba que tenga rol paciente.
    Devuelve solamente las citas que le pertenecen.
    No recibe ningún ID de paciente desde React.*/
    public List<AppointmentResponse> getMyAppointments(String authenticatedUsername) {
        if (authenticatedUsername == null
                || authenticatedUsername.isBlank()) {

            throw new RuntimeException(
                    "No se pudo identificar al paciente autenticado."
            );
        }

        User patient = userRepository
                .findByUsername(authenticatedUsername)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No se encontró el paciente autenticado."
                        )
                );

        if (!Boolean.TRUE.equals(patient.getActive())) {
            throw new RuntimeException(
                    "La cuenta del paciente se encuentra inactiva."
            );
        }

        String roleName = patient.getRole() != null
                ? patient.getRole().getName()
                : null;

        if (!isPatientRole(roleName)) {
            throw new RuntimeException(
                    "Solamente los pacientes pueden consultar sus citas."
            );
        }

        List<Appointment> appointments =
                appointmentRepository
                        .findByPatient_UsernameOrderByAppointmentDateTimeDesc(
                                authenticatedUsername
                        );

        return appointments.stream()
                .map(this::toResponse)
                .toList();
    }
    /*
     * Valida los datos básicos enviados para crear la cita.
     */
    private void validateCreateAppointment(CreateAppointmentRequest request) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la cita son obligatorios."
            );
        }

        if (request.getDoctorUserId() == null) {
            throw new RuntimeException(
                    "Debe seleccionar un médico."
            );
        }

        if (request.getBranchId() == null) {
            throw new RuntimeException(
                    "Debe seleccionar una sucursal."
            );
        }

        if (request.getSpecialtyId() == null) {
            throw new RuntimeException(
                    "Debe seleccionar una especialidad médica."
            );
        }

        if (request.getAppointmentDateTime() == null
                || request
                .getAppointmentDateTime()
                .isBlank()) {

            throw new RuntimeException(
                    "Debe seleccionar una fecha y hora."
            );
        }

        if (request.getReason() == null
                || request.getReason().isBlank()) {

            throw new RuntimeException(
                    "El motivo de consulta es obligatorio."
            );
        }

        String cleanReason =
                request.getReason().trim();

        if (cleanReason.length() < 10
                || cleanReason.length() > 2000) {

            throw new RuntimeException(
                    "El motivo debe contener entre 10 y 2000 caracteres. Usted ingresó "
                            + cleanReason.length()
                            + " caracteres."
            );
        }
    }

    /*
     * Confirma que el médico esté asignado a la sucursal
     * y especialidad seleccionadas.
     */
    private void validateDoctorAssignment(User doctor, Branch branch, Specialty specialty) {
        if (doctor.getBranch() == null
                || !doctor
                .getBranch()
                .getId()
                .equals(branch.getId())) {

            throw new RuntimeException(
                    "El médico seleccionado no pertenece a la sucursal indicada."
            );
        }

        if (doctor.getSpecialty() == null
                || !doctor
                .getSpecialty()
                .getId()
                .equals(specialty.getId())) {

            throw new RuntimeException(
                    "El médico seleccionado no pertenece a la especialidad indicada."
            );
        }
    }

    /*
     * Reconoce las variantes actuales del rol paciente.
     */
    private boolean isPatientRole(String roleName) {
        if (roleName == null) {
            return false;
        }

        return roleName.equalsIgnoreCase("PACIENTE")
                || roleName.equalsIgnoreCase("Paciente");
    }

    /*
     * Reconoce las variantes actuales del rol médico.
     */
    private boolean isDoctorRole(String roleName) {
        if (roleName == null) {
            return false;
        }

        return roleName.equalsIgnoreCase("MEDICO")
                || roleName.equalsIgnoreCase("Médico")
                || roleName.equalsIgnoreCase("Medico");
    }

    /*
     * Horarios habilitados temporalmente para CU-03.
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

    /*
     * Valida que la hora enviada pertenezca
     * a los horarios mostrados por disponibilidad.
     */
    private boolean isSupportedSlot(LocalTime time) {
        return getBaseSlots().contains(time);
    }

    /*
     * Convierte Appointment a AppointmentResponse.
     */
    private AppointmentResponse toResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientName(appointment.getPatient().getFullName())
                .doctorName(appointment.getDoctor().getFullName())
                .branch(appointment.getBranch().getName())
                .specialty(appointment.getSpecialty().getName())
                .appointmentDateTime(appointment.getAppointmentDateTime().toString())
                .reason(appointment.getReason())
                .status(appointment.getStatus().name())
                .reservationExpiresAt(appointment.getReservationExpiresAt().toString())
                .build();
    }

    //valida estado cancelado o expiradas de las citas para liberar el horario
    private boolean isAppointmentBlockingSlot(Appointment appointment) {
        if (!Boolean.TRUE.equals(
                appointment.getActive())) {

            return false;
        }

        if (appointment.getStatus()
                == AppointmentStatus.PENDIENTE_DE_PAGO) {

            LocalDateTime expiration =
                    appointment.getReservationExpiresAt();

            return expiration != null
                    && expiration.isAfter(
                    LocalDateTime.now()
            );
        }

        return appointment.getStatus()
                == AppointmentStatus.PAGADA

                || appointment.getStatus()
                == AppointmentStatus.CONFIRMADA

                || appointment.getStatus()
                == AppointmentStatus.PACIENTE_PRESENTE;
    }
}