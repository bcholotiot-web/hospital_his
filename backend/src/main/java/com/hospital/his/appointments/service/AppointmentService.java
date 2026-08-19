package com.hospital.his.appointments.service;

import com.hospital.his.appointments.dto.AppointmentResponse;
import com.hospital.his.appointments.dto.CreateAppointmentRequest;
import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.catalogs.repository.BranchRepository;
import com.hospital.his.catalogs.repository.SpecialtyRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import com.hospital.his.appointments.dto.AvailableSlotResponse;
import com.hospital.his.appointments.dto.DoctorResponse;
import com.hospital.his.catalogs.dto.SpecialtyResponse;
import com.hospital.his.catalogs.entity.BranchSpecialty;
import com.hospital.his.catalogs.repository.BranchSpecialtyRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AuditService auditService;
    private final BranchSpecialtyRepository branchSpecialtyRepository;

    //constructor
    public AppointmentService(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            SpecialtyRepository specialtyRepository,
            AuditService auditService, BranchSpecialtyRepository branchSpecialtyRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.specialtyRepository = specialtyRepository;
        this.auditService = auditService;
        this.branchSpecialtyRepository = branchSpecialtyRepository;
    }

    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        validateCreateAppointment(request);

        User patient = userRepository.findById(request.getPatientUserId()).orElseThrow(() -> new RuntimeException("Paciente no encontrado."));
        User doctor = userRepository.findById(request.getDoctorUserId()).orElseThrow(() -> new RuntimeException("Médico no encontrado."));
        Branch branch = branchRepository.findById(request.getBranchId()).orElseThrow(() -> new RuntimeException("Sucursal no encontrada."));
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId()).orElseThrow(() -> new RuntimeException("Especialidad no encontrada."));
        LocalDateTime dateTime = LocalDateTime.parse(request.getAppointmentDateTime());

        if (appointmentRepository.existsByDoctorAndAppointmentDateTime(doctor, dateTime)) {
            throw new RuntimeException("El horario seleccionado ya no está disponible. Por favor, elija otro horario.");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .branch(branch)
                .specialty(specialty)
                .appointmentDateTime(dateTime)
                .reason(request.getReason())
                .status("PENDIENTE_DE_PAGO")
                .active(true)
                .build();

        appointment = appointmentRepository.save(appointment);

        auditService.log(
                patient.getUsername(),
                "CREATE_APPOINTMENT",
                "APPOINTMENTS",
                "Cita registrada con estado pendiente de pago."
        );

        return toResponse(appointment);
    }

    private void validateCreateAppointment(CreateAppointmentRequest request) {
        if (request.getPatientUserId() == null) {
            throw new RuntimeException("Debe indicar el paciente.");
        }

        if (request.getDoctorUserId() == null) {
            throw new RuntimeException("Debe seleccionar un médico.");
        }

        if (request.getBranchId() == null) {
            throw new RuntimeException("Debe seleccionar una sucursal.");
        }

        if (request.getSpecialtyId() == null) {
            throw new RuntimeException("Debe seleccionar una especialidad médica.");
        }

        if (request.getAppointmentDateTime() == null || request.getAppointmentDateTime().isBlank()) {
            throw new RuntimeException("Debe seleccionar una fecha y hora.");
        }

        LocalDateTime dateTime = LocalDateTime.parse(request.getAppointmentDateTime());

        if (!dateTime.isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.");
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new RuntimeException("El motivo de consulta es obligatorio.");
        }

        if (request.getReason().length() < 10 || request.getReason().length() > 2000) {
            throw new RuntimeException("El motivo debe contener entre 10 y 2000 caracteres.");
        }
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientName(appointment.getPatient().getFullName())
                .doctorName(appointment.getDoctor().getFullName())
                .branch(appointment.getBranch().getName())
                .specialty(appointment.getSpecialty().getName())
                .appointmentDateTime(
                        appointment.getAppointmentDateTime().toString()
                )
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .build();
    }

    //Especialidades por sucursal (listo)
    public List<SpecialtyResponse> getSpecialtiesByBranch(Long branchId) {

        if (branchId == null) {
            throw new RuntimeException(
                    "Debe seleccionar una sucursal."
            );
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sucursal no encontrada."
                        ));

        List<BranchSpecialty> branchSpecialties =
                branchSpecialtyRepository.findByBranch_IdAndActiveTrue(
                        branchId
                );

        if (branchSpecialties.isEmpty()) {
            throw new RuntimeException(
                    "No hay especialidades disponibles para la sucursal "
                            + branch.getName()
                            + ". Seleccione otra sucursal."
            );
        }

        List<SpecialtyResponse> specialties =
                branchSpecialties.stream()
                        .filter(branchSpecialty ->
                                branchSpecialty.getSpecialty() != null
                                        && Boolean.TRUE.equals(
                                        branchSpecialty.getSpecialty().getActive()
                                )
                        )
                        .map(branchSpecialty ->
                                SpecialtyResponse.builder()
                                        .id(branchSpecialty.getSpecialty().getId())
                                        .name(branchSpecialty.getSpecialty().getName())
                                        .description(branchSpecialty.getSpecialty().getDescription())
                                        .active(branchSpecialty.getSpecialty().getActive())
                                        .build()
                        )
                        .toList();

        if (specialties.isEmpty()) {
            throw new RuntimeException(
                    "No hay especialidades activas disponibles para la sucursal "
                            + branch.getName()
                            + ". Seleccione otra sucursal."
            );
        }

        return specialties;
    }

    //Médicos por sucursal y especialidad (listo)
    public List<DoctorResponse> getDoctorsByBranchAndSpecialty(Long branchId, Long specialtyId) {
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

        branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sucursal no encontrada."
                        ));

        specialtyRepository.findById(specialtyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Especialidad no encontrada."
                        ));

        List<User> doctors =
                userRepository.findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue(
                        "Médico",
                        branchId,
                        specialtyId
                );

        if (doctors.isEmpty()) {
            throw new RuntimeException(
                    "No se encontraron médicos disponibles para la especialidad seleccionada en la sucursal indicada."
            );
        }

        return doctors.stream()
                .map(doctor -> DoctorResponse.builder()
                        .id(doctor.getId())
                        .fullName(doctor.getFullName())
                        .specialty(
                                doctor.getSpecialty() != null
                                        ? doctor.getSpecialty().getName()
                                        : null
                        )
                        .branch(
                                doctor.getBranch() != null
                                        ? doctor.getBranch().getName()
                                        : null
                        )
                        .build())
                .toList();
    }

    //Horarios disponibles del médico
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, String date) {
        if (doctorId == null) {
            throw new RuntimeException("Debe seleccionar un médico.");
        }

        if (date == null || date.isBlank()) {
            throw new RuntimeException("Debe seleccionar una fecha.");
        }

        User doctor = userRepository.findById(doctorId).orElseThrow(() -> new RuntimeException("Médico no encontrado."));

        LocalDate selectedDate;

        try {
            selectedDate = LocalDate.parse(date);
        } catch (Exception ex) {
            throw new RuntimeException("El formato de la fecha no es válido. Use el formato yyyy-MM-dd.");
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Debe seleccionar una fecha futura o actual.");
        }

        LocalDateTime startOfDay =
                selectedDate.atStartOfDay();

        LocalDateTime endOfDay =
                selectedDate.atTime(23, 59, 59);

        List<Appointment> existingAppointments =
                appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetween(
                        doctor.getId(),
                        startOfDay,
                        endOfDay
                );

        List<LocalTime> baseSlots =
                List.of(
                        LocalTime.of(8, 0),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        LocalTime.of(16, 0)
                );

        List<AvailableSlotResponse> response =
                new ArrayList<>();

        for (LocalTime slot : baseSlots) {
            LocalDateTime slotDateTime =
                    selectedDate.atTime(slot);

            boolean occupied =
                    existingAppointments.stream()
                            .anyMatch(appointment ->
                                    appointment.getAppointmentDateTime()
                                            .equals(slotDateTime)
                            );

            boolean isPast =
                    slotDateTime.isBefore(LocalDateTime.now()) ||
                            slotDateTime.isEqual(LocalDateTime.now());

            response.add(
                    AvailableSlotResponse.builder()
                            .dateTime(slotDateTime.toString())
                            .time(slot.toString())
                            .available(!occupied && !isPast)
                            .build()
            );
        }

        boolean hasAvailable =
                response.stream()
                        .anyMatch(AvailableSlotResponse::getAvailable);

        if (!hasAvailable) {
            throw new RuntimeException("No se encontraron horarios disponibles para el médico seleccionado.");
        }

        return response;
    }

}