package com.padron_electoral.controller;

import com.padron_electoral.model.padron_electoral;
import com.padron_electoral.service.padron_electoral_service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/padron_electoral")
public class padron_electoral_controller {

    private final padron_electoral_service service;

    public padron_electoral_controller(padron_electoral_service service) {
        this.service = service;
    }

    @GetMapping
    public List<padron_electoral> listar() {
        return service.listar();
    }

    @GetMapping("/{cui}")
    public padron_electoral buscar(@PathVariable String cui) {
        return service.buscar_por_cui(cui);
    }

    @PostMapping
    public padron_electoral guardar(@RequestBody padron_electoral registro) {
        return service.guardar(registro);
    }

    @PutMapping("/{cui}")
    public padron_electoral actualizar(@PathVariable String cui,@RequestBody padron_electoral registro) {
        return service.actualizar(cui, registro);
    }

    @DeleteMapping("/{cui}")
    public String eliminar(@PathVariable String cui) {
        service.eliminar_logico(cui);
        return "Registro eliminado correctamente";
    }
}