package com.hospital.his.laboratory.controller;

import com.hospital.his.laboratory.dto.CreateLaboratoryOrderRequest;
import com.hospital.his.laboratory.dto.LaboratoryOrderResponse;
import com.hospital.his.laboratory.dto.LaboratoryOrderSummaryResponse;
import com.hospital.his.laboratory.dto.LaboratoryTestResponse;
import com.hospital.his.laboratory.service.DoctorLaboratoryOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/laboratory")
public class DoctorLaboratoryOrderController {

    private final DoctorLaboratoryOrderService
            doctorLaboratoryOrderService;

    public DoctorLaboratoryOrderController(
            DoctorLaboratoryOrderService doctorLaboratoryOrderService
    ) {
        this.doctorLaboratoryOrderService =
                doctorLaboratoryOrderService;
    }

    /*Debe:
    Identificar al médico por JWT.
    Validar que esté activo.
    Validar rol MEDICO.
    Devolver únicamente exámenes activos.
    No devolver exámenes eliminados lógicamente.
    Incluir el precio para mostrar el total informativo.*/
    @GetMapping("/tests")
    public ResponseEntity<List<LaboratoryTestResponse>>
    getActiveLaboratoryTests(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                doctorLaboratoryOrderService
                        .getActiveLaboratoryTests(
                                authentication.getName()
                        )
        );
    }

    /**
     * CU-08 FA01 / CU-09:
     * Genera una orden de laboratorio desde una
     * consulta médica.
     *
     * La identidad del médico se obtiene de la sesión JWT.
     */
    @PostMapping("/orders")
    public ResponseEntity<LaboratoryOrderResponse>
    createLaboratoryOrder(
            @Valid
            @RequestBody
            CreateLaboratoryOrderRequest request,

            Authentication authentication
    ) {
        LaboratoryOrderResponse response =
                doctorLaboratoryOrderService
                        .createLaboratoryOrder(
                                request,
                                authentication.getName()
                        );

        return ResponseEntity.ok(response);
    }

    //LISTAR ORDENES CREADAS POR EL MEDICO
    @GetMapping("/orders")
    public ResponseEntity<
            List<LaboratoryOrderSummaryResponse>
            >
    getMyLaboratoryOrders(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                doctorLaboratoryOrderService
                        .getMyLaboratoryOrders(
                                authentication.getName()
                        )
        );
    }

    //CONSULTAR UNA ORDEN POR MEDIO DEL ID
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<LaboratoryOrderResponse>
    getMyLaboratoryOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                doctorLaboratoryOrderService
                        .getMyLaboratoryOrder(
                                orderId,
                                authentication.getName()
                        )
        );
    }
}