package com.hospital.his.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyDpiResponse {

    /*
     * Indica si existe una cuenta con el DPI consultado.
     */
    private Boolean registered;

    /*
     * Indica si la cuenta corresponde a un paciente.
     */
    private Boolean patient;

    /*
     * Acción que debe ejecutar el frontend.
     *
     * LOGIN
     * REGISTER
     * INTERNAL_USER
     * INACTIVE
     */
    private String action;

    /*
     * Mensaje que debe mostrarse en la interfaz.
     */
    private String message;

    /*Al coincidir que el DPI corresponde a un paciente nos devuelve el usuario*/
    private String username;
}