package com.padron_electoral.service.impl;

import com.padron_electoral.model.padron_electoral;
import com.padron_electoral.repository.padron_electoral_repository;
import com.padron_electoral.service.padron_electoral_service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class padron_electoral_service_impl implements padron_electoral_service {

    private final padron_electoral_repository repository;

    //constructor
    public padron_electoral_service_impl(padron_electoral_repository repository) {
        this.repository = repository;
    }

    @Override
    public List<padron_electoral> listar() {
        return repository.findByActivoTrue();
    }

    @Override
    public padron_electoral guardar(padron_electoral registro) {

        System.out.println("CUI recibido: " + registro.getCui());
        boolean existe = repository.existsByCui(registro.getCui());
        System.out.println("Existe: " + existe);

        if (repository.existsByCui(registro.getCui())) {
            throw new RuntimeException("El CUI ya existe");
        }
        registro.setActivo(true);
        return repository.save(registro);
    }

    @Override
    public padron_electoral buscar_por_cui(
            String cui) {

        return repository
                .findByCuiAndActivoTrue(cui)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Registro no encontrado"));
    }

    @Override
    public padron_electoral actualizar(
            String cui,
            padron_electoral registro) {

        padron_electoral actual =
                buscar_por_cui(cui);

        actual.setNombre(
                registro.getNombre());

        actual.setApellido(
                registro.getApellido());

        actual.setDepartamento(
                registro.getDepartamento());

        actual.setMunicipio(
                registro.getMunicipio());

        actual.setFecha_actualizacion(
                LocalDateTime.now());

        return repository.save(actual);
    }

    @Override
    public void eliminar_logico(String cui) {

        padron_electoral actual =buscar_por_cui(cui);

        actual.setActivo(false);

        actual.setFecha_actualizacion( LocalDateTime.now());

        repository.save(actual);
    }
}