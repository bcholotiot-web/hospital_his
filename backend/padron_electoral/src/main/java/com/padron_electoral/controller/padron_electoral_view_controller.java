package com.padron_electoral.controller;

import com.padron_electoral.model.padron_electoral;
import com.padron_electoral.service.padron_electoral_service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vista/padron_electoral")
public class padron_electoral_view_controller {

    private final padron_electoral_service service;

    public padron_electoral_view_controller(
            padron_electoral_service service) {
        this.service = service;
    }

    @GetMapping
    public String index(Model model) {

        model.addAttribute(
                "registros",
                service.listar());

        return "padron_electoral/index";
    }

    @GetMapping("/create")
    public String create(Model model) {

        model.addAttribute(
                "registro",
                new padron_electoral());

        return "padron_electoral/create";
    }

    @PostMapping("/save")
    public String save(
            @ModelAttribute padron_electoral registro) {

        service.guardar(registro);

        return "redirect:/vista/padron_electoral";
    }
}