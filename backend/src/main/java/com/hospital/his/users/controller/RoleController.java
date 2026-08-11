package com.hospital.his.users.controller;

import com.hospital.his.users.dto.RoleResponse;
import com.hospital.his.users.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final UserService userService;

    public RoleController(
            UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>>
    getRoles() {
        return ResponseEntity.ok(userService.getRoles());
    }
}