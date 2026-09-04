package com.hospital.his.laboratory.service;

import com.hospital.his.appointments.entity.Appointment;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.repository.AppointmentRepository;
import com.hospital.his.audit.service.AuditService;
import com.hospital.his.laboratory.dto.CreateLaboratoryOrderRequest;
import com.hospital.his.laboratory.dto.LaboratoryOrderItemResponse;
import com.hospital.his.laboratory.dto.LaboratoryOrderResponse;
import com.hospital.his.laboratory.dto.LaboratoryOrderSummaryResponse;
import com.hospital.his.laboratory.dto.LaboratoryTestResponse;
import com.hospital.his.laboratory.entity.LaboratoryOrder;
import com.hospital.his.laboratory.entity.LaboratoryOrderItem;
import com.hospital.his.laboratory.entity.LaboratoryOrderItemStatus;
import com.hospital.his.laboratory.entity.LaboratoryOrderStatus;
import com.hospital.his.laboratory.entity.LaboratoryTest;
import com.hospital.his.laboratory.repository.LaboratoryOrderRepository;
import com.hospital.his.laboratory.repository.LaboratoryTestRepository;
import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import com.hospital.his.medicalconsultation.entity.MedicalConsultationStatus;
import com.hospital.his.medicalconsultation.repository.MedicalConsultationRepository;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DoctorLaboratoryOrderService {

    private static final String CURRENCY = "GTQ";

    private final AppointmentRepository
            appointmentRepository;

    private final MedicalConsultationRepository
            medicalConsultationRepository;

    private final LaboratoryTestRepository
            laboratoryTestRepository;

    private final LaboratoryOrderRepository
            laboratoryOrderRepository;

    private final UserRepository
            userRepository;

    private final AuditService
            auditService;

    public DoctorLaboratoryOrderService(
            AppointmentRepository appointmentRepository,
            MedicalConsultationRepository medicalConsultationRepository,
            LaboratoryTestRepository laboratoryTestRepository,
            LaboratoryOrderRepository laboratoryOrderRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.appointmentRepository =
                appointmentRepository;

        this.medicalConsultationRepository =
                medicalConsultationRepository;

        this.laboratoryTestRepository =
                laboratoryTestRepository;

        this.laboratoryOrderRepository =
                laboratoryOrderRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /**
     * Devuelve el catálogo de exámenes activos
     * para el formulario del médico.
     */
    @Transactional(readOnly = true)
    public List<LaboratoryTestResponse>
    getActiveLaboratoryTests(
            String doctorUsername
    ) {
        validateAuthenticatedDoctor(
                doctorUsername
        );

        return laboratoryTestRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toTestResponse)
                .toList();
    }

    /**
     * Genera una orden desde una consulta evaluada.
     */
    @Transactional
    public LaboratoryOrderResponse
    createLaboratoryOrder(
            CreateLaboratoryOrderRequest request,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        validateCreateRequest(
                request
        );

        Appointment appointment =
                findDoctorAppointment(
                        request.getAppointmentId(),
                        doctor.getUsername()
                );

        validateAppointmentForOrder(
                appointment
        );

        MedicalConsultation consultation =
                medicalConsultationRepository
                        .findByAppointment_IdAndDoctor_Username(
                                appointment.getId(),
                                doctor.getUsername()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se encontró la consulta médica asociada."
                                )
                        );

        validateConsultationForOrder(
                consultation
        );

        boolean existingActiveOrder =
                laboratoryOrderRepository
                        .existsByMedicalConsultation_IdAndActiveTrue(
                                consultation.getId()
                        );

        if (existingActiveOrder) {
            throw new RuntimeException(
                    "La consulta ya cuenta con una orden de laboratorio activa."
            );
        }

        List<Long> uniqueTestIds =
                normalizeTestIds(
                        request.getLaboratoryTestIds()
                );

        List<LaboratoryTest> tests =
                loadSelectedTests(
                        uniqueTestIds
                );

        BigDecimal totalAmount =
                tests.stream()
                        .map(
                                LaboratoryTest::getPrice
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        LocalDateTime now =
                LocalDateTime.now();

        LaboratoryOrder order =
                LaboratoryOrder.builder()
                        .orderNumber(
                                generateOrderNumber()
                        )
                        .appointment(
                                appointment
                        )
                        .medicalConsultation(
                                consultation
                        )
                        .patient(
                                appointment.getPatient()
                        )
                        .doctor(
                                doctor
                        )
                        .status(
                                LaboratoryOrderStatus
                                        .PENDIENTE_DE_PAGO
                        )
                        .totalAmount(
                                totalAmount
                        )
                        .externalOrder(
                                Boolean.TRUE.equals(
                                        request.getExternalOrder()
                                )
                        )
                        .notes(
                                normalizeNotes(
                                        request.getNotes()
                                )
                        )
                        .createdAt(now)
                        .updatedAt(now)
                        .paidAt(null)
                        .completedAt(null)
                        .active(true)
                        .items(
                                new ArrayList<>()
                        )
                        .build();

        for (LaboratoryTest test : tests) {
            LaboratoryOrderItem item =
                    LaboratoryOrderItem.builder()
                            .laboratoryOrder(
                                    order
                            )
                            .laboratoryTest(
                                    test
                            )
                            .unitPrice(
                                    test.getPrice()
                            )
                            .status(
                                    LaboratoryOrderItemStatus
                                            .PENDIENTE
                            )
                            .resultValue(null)
                            .resultUnit(
                                    test.getDefaultUnit()
                            )
                            .resultDate(null)
                            .outOfRange(false)
                            .resultNotes(null)
                            .resultSavedAt(null)
                            .resultSavedBy(null)
                            .published(false)
                            .publishedAt(null)
                            .publishedBy(null)
                            .build();

            order.getItems().add(
                    item
            );
        }

        try {
            order =
                    laboratoryOrderRepository
                            .saveAndFlush(order);

        } catch (DataIntegrityViolationException exception) {
            throw new RuntimeException(
                    "No fue posible crear la orden. "
                            + "Verifique que la consulta no tenga otra orden activa."
            );
        }

        auditService.log(
                doctorUsername,
                "CREATE_LABORATORY_ORDER",
                "LABORATORY",
                "Orden de laboratorio "
                        + order.getOrderNumber()
                        + " creada para la cita ID "
                        + appointment.getId()
                        + ". Total de exámenes: "
                        + order.getItems().size()
                        + ". Monto: Q"
                        + order.getTotalAmount()
                        + "."
        );

        return toOrderResponse(
                order,
                "Orden de laboratorio creada correctamente. "
                        + "La orden se encuentra pendiente de pago."
        );
    }

    /**
     * Devuelve las órdenes creadas por el médico
     * autenticado.
     */
    @Transactional(readOnly = true)
    public List<LaboratoryOrderSummaryResponse>
    getMyLaboratoryOrders(
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        return laboratoryOrderRepository
                .findByDoctor_UsernameAndActiveTrueOrderByCreatedAtDesc(
                        doctor.getUsername()
                )
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Consulta el detalle de una orden creada por
     * el médico autenticado.
     */
    @Transactional(readOnly = true)
    public LaboratoryOrderResponse
    getMyLaboratoryOrder(
            Long orderId,
            String doctorUsername
    ) {
        User doctor =
                validateAuthenticatedDoctor(
                        doctorUsername
                );

        if (orderId == null) {
            throw new RuntimeException(
                    "Debe indicar la orden de laboratorio."
            );
        }

        LaboratoryOrder order =
                laboratoryOrderRepository
                        .findByIdAndDoctor_UsernameAndActiveTrue(
                                orderId,
                                doctor.getUsername()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La orden no fue encontrada o no pertenece al médico autenticado."
                                )
                        );

        return toOrderResponse(
                order,
                "Orden de laboratorio encontrada."
        );
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
                    "La cuenta del médico está inactiva."
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
                    "Solamente un médico puede generar órdenes de laboratorio."
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
                    "Debe indicar la cita."
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

    private void validateAppointmentForOrder(
            Appointment appointment
    ) {
        if (!Boolean.TRUE.equals(
                appointment.getActive()
        )) {
            throw new RuntimeException(
                    "La cita no se encuentra activa."
            );
        }

        if (appointment.getStatus()
                != AppointmentStatus
                .CONSULTA_EVALUADA) {

            throw new RuntimeException(
                    "La orden de laboratorio solamente puede generarse "
                            + "después de finalizar la evaluación médica."
            );
        }
    }

    private void validateConsultationForOrder(
            MedicalConsultation consultation
    ) {
        if (consultation.getStatus()
                != MedicalConsultationStatus
                .FINALIZADA) {

            throw new RuntimeException(
                    "La consulta clínica debe estar finalizada antes de generar la orden."
            );
        }

        if (consultation.getDiagnosis() == null
                || consultation
                .getDiagnosis()
                .isBlank()) {

            throw new RuntimeException(
                    "No es posible generar una orden sin diagnóstico."
            );
        }
    }

    private void validateCreateRequest(
            CreateLaboratoryOrderRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la orden son obligatorios."
            );
        }

        if (request.getAppointmentId() == null) {
            throw new RuntimeException(
                    "Debe indicar la cita."
            );
        }

        if (request.getLaboratoryTestIds() == null
                || request
                .getLaboratoryTestIds()
                .isEmpty()) {

            throw new RuntimeException(
                    "Debe seleccionar al menos un examen de laboratorio."
            );
        }

        if (request.getLaboratoryTestIds()
                .size() > 50) {

            throw new RuntimeException(
                    "No puede seleccionar más de 50 exámenes por orden."
            );
        }

        if (request.getNotes() != null
                && request.getNotes()
                .trim()
                .length() > 1000) {

            throw new RuntimeException(
                    "Las observaciones no pueden exceder los 1000 caracteres."
            );
        }
    }

    private List<Long> normalizeTestIds(
            List<Long> testIds
    ) {
        if (testIds.stream()
                .anyMatch(id -> id == null)) {

            throw new RuntimeException(
                    "La lista de exámenes contiene identificadores inválidos."
            );
        }

        Set<Long> uniqueIds =
                new HashSet<>(
                        testIds
                );

        if (uniqueIds.size()
                != testIds.size()) {

            throw new RuntimeException(
                    "No debe seleccionar el mismo examen más de una vez."
            );
        }

        return List.copyOf(
                uniqueIds
        );
    }

    private List<LaboratoryTest> loadSelectedTests(
            List<Long> testIds
    ) {
        List<LaboratoryTest> tests =
                testIds.stream()
                        .map(testId ->
                                laboratoryTestRepository
                                        .findByIdAndActiveTrue(
                                                testId
                                        )
                                        .orElseThrow(() ->
                                                new RuntimeException(
                                                        "El examen con ID "
                                                                + testId
                                                                + " no existe o está inactivo."
                                                )
                                        )
                        )
                        .toList();

        if (tests.size() != testIds.size()) {
            throw new RuntimeException(
                    "No fue posible cargar todos los exámenes seleccionados."
            );
        }

        return tests;
    }

    private String normalizeNotes(
            String notes
    ) {
        if (notes == null
                || notes.isBlank()) {

            return null;
        }

        String cleanNotes =
                notes.trim();

        if (cleanNotes.length() > 1000) {
            throw new RuntimeException(
                    "Las observaciones no pueden exceder los 1000 caracteres."
            );
        }

        return cleanNotes;
    }

    private String generateOrderNumber() {
        String code =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase(
                                Locale.ROOT
                        );

        return "LAB-" + code;
    }

    private LaboratoryTestResponse toTestResponse(
            LaboratoryTest test
    ) {
        return LaboratoryTestResponse.builder()
                .id(test.getId())
                .code(test.getCode())
                .name(test.getName())
                .description(
                        test.getDescription()
                )
                .referenceRange(
                        test.getReferenceRange()
                )
                .defaultUnit(
                        test.getDefaultUnit()
                )
                .price(test.getPrice())
                .build();
    }

    private LaboratoryOrderSummaryResponse
    toSummaryResponse(
            LaboratoryOrder order
    ) {
        int totalTests =
                order.getItems().size();

        int publishedTests =
                (int) order.getItems()
                        .stream()
                        .filter(item ->
                                Boolean.TRUE.equals(
                                        item.getPublished()
                                )
                        )
                        .count();

        return LaboratoryOrderSummaryResponse
                .builder()
                .orderId(order.getId())
                .orderNumber(
                        order.getOrderNumber()
                )
                .patientName(
                        order.getPatient()
                                .getFullName()
                )
                .doctorName(
                        order.getDoctor()
                                .getFullName()
                )
                .branch(
                        order.getAppointment()
                                .getBranch()
                                .getName()
                )
                .status(
                        order.getStatus()
                                .name()
                )
                .totalAmount(
                        order.getTotalAmount()
                )
                .externalOrder(
                        order.getExternalOrder()
                )
                .totalTests(totalTests)
                .publishedTests(
                        publishedTests
                )
                .createdAt(
                        order.getCreatedAt()
                                .toString()
                )
                .build();
    }

    private LaboratoryOrderResponse toOrderResponse(
            LaboratoryOrder order,
            String message
    ) {
        List<LaboratoryOrderItemResponse>
                itemResponses =
                order.getItems()
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        int totalTests =
                itemResponses.size();

        int publishedTests =
                (int) itemResponses
                        .stream()
                        .filter(item ->
                                Boolean.TRUE.equals(
                                        item.getPublished()
                                )
                        )
                        .count();

        return LaboratoryOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(
                        order.getOrderNumber()
                )
                .appointmentId(
                        order.getAppointment()
                                .getId()
                )
                .consultationId(
                        order.getMedicalConsultation()
                                .getId()
                )
                .patientName(
                        order.getPatient()
                                .getFullName()
                )
                .patientDpi(
                        order.getPatient()
                                .getDpi()
                )
                .doctorName(
                        order.getDoctor()
                                .getFullName()
                )
                .branch(
                        order.getAppointment()
                                .getBranch()
                                .getName()
                )
                .status(
                        order.getStatus()
                                .name()
                )
                .totalAmount(
                        order.getTotalAmount()
                )
                .currency(CURRENCY)
                .externalOrder(
                        order.getExternalOrder()
                )
                .notes(order.getNotes())
                .createdAt(
                        order.getCreatedAt()
                                .toString()
                )
                .paidAt(
                        order.getPaidAt() != null
                                ? order.getPaidAt()
                                .toString()
                                : null
                )
                .completedAt(
                        order.getCompletedAt() != null
                                ? order.getCompletedAt()
                                .toString()
                                : null
                )
                .totalTests(totalTests)
                .publishedTests(
                        publishedTests
                )
                .allResultsPublished(
                        totalTests > 0
                                && totalTests
                                == publishedTests
                )
                .items(itemResponses)
                .message(message)
                .build();
    }

    private LaboratoryOrderItemResponse
    toItemResponse(
            LaboratoryOrderItem item
    ) {
        LaboratoryTest test =
                item.getLaboratoryTest();

        return LaboratoryOrderItemResponse
                .builder()
                .itemId(item.getId())
                .laboratoryTestId(
                        test.getId()
                )
                .testCode(test.getCode())
                .testName(test.getName())
                .referenceRange(
                        test.getReferenceRange()
                )
                .unitPrice(
                        item.getUnitPrice()
                )
                .status(
                        item.getStatus()
                                .name()
                )
                .resultValue(
                        item.getResultValue()
                )
                .resultUnit(
                        item.getResultUnit()
                )
                .resultDate(
                        item.getResultDate() != null
                                ? item.getResultDate()
                                .toString()
                                : null
                )
                .outOfRange(
                        item.getOutOfRange()
                )
                .resultNotes(
                        item.getResultNotes()
                )
                .published(
                        item.getPublished()
                )
                .resultSavedAt(
                        item.getResultSavedAt() != null
                                ? item.getResultSavedAt()
                                .toString()
                                : null
                )
                .publishedAt(
                        item.getPublishedAt() != null
                                ? item.getPublishedAt()
                                .toString()
                                : null
                )
                .canSaveResult(
                        orderAllowsResultEntry(
                                item.getLaboratoryOrder()
                        )
                                && !Boolean.TRUE.equals(
                                item.getPublished()
                        )
                )
                .canPublishResult(
                        item.getStatus()
                                == LaboratoryOrderItemStatus
                                .RESULTADO_GUARDADO
                                && !Boolean.TRUE.equals(
                                item.getPublished()
                        )
                )
                .build();
    }

    private boolean orderAllowsResultEntry(
            LaboratoryOrder order
    ) {
        return order.getStatus()
                == LaboratoryOrderStatus
                .EN_PROCESO;
    }
}