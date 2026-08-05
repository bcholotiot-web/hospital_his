package com.padron_electoral.service;

import com.padron_electoral.model.padron_electoral;

import java.util.List;

public interface padron_electoral_service {

    List<padron_electoral> listar();

    padron_electoral guardar(padron_electoral registro);

    padron_electoral buscar_por_cui(String cui);

    padron_electoral actualizar( String cui, padron_electoral registro);

    void eliminar_logico(String cui);
}