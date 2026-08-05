package com.hospital.his.users.controller;

import com.hospital.his.users.dto.CreateUserRequest;
import com.hospital.his.users.dto.UpdateUserRequest;
import com.hospital.his.users.dto.UserResponse;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    //Crear Usuario
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request){
        UserResponse response = userService.createUser(request);

        return ResponseEntity.ok(response);
    }

    //Listar
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    //Buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok( userService.getUserById(id));
    }

    //Actualizar usuario
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,@RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    //Cambio de estatus
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> changeStatus(@PathVariable Long id,@RequestParam Boolean active) {
        return ResponseEntity.ok(userService.changeStatus(id,active));
    }

    @GetMapping("/test")
    public String test() {
        return "Endpoint protegido";
    }

    @GetMapping("/me")
    public String currentUser(Authentication authentication) {
        return authentication.getName();
    }

    @GetMapping("/admin")
    public String adminEndpoint() {
        return "Acceso administrador";
    }

    @GetMapping("/roles")
    public Object roles(Authentication authentication) {
        return authentication.getAuthorities();
    }

    @GetMapping("/debug")
    public String debug(Authentication authentication) {

        return authentication.toString();
    }
}