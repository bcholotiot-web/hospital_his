package com.hospital.his.users.service;

import com.hospital.his.users.dto.UpdateUserRequest;
import com.hospital.his.users.repository.UserRepository;
import com.hospital.his.users.repository.RoleRepository;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.users.dto.CreateUserRequest;
import com.hospital.his.users.dto.UserResponse;
import com.hospital.his.users.entity.Role;
import com.hospital.his.users.entity.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuditService auditService){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    //Se registra usuario al estar logueado
    public UserResponse createUser(CreateUserRequest request){
        validateCreateUserRequest(request);

        Role role = roleRepository.findById(request.getRoleId()).orElseThrow(()->new RuntimeException("Rol no encontrado"));

        User user = User.builder()
                .fullName(request.getFullName())
                .dpi(request.getDpi())
                .nit(request.getNit())
                .phone(request.getPhone())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .insuranceNumber(request.getInsuranceNumber())
                .active(request.getActive())
                .role(role)
                .build();

        user = userRepository.save(user);

        auditService.log(user.getUsername(), "CREATE_USER", "users", "Usuario interno creado correctamente");

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .dpi(user.getDpi())
                .nit(user.getNit())
                .phone(user.getPhone())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .insuranceNumber(user.getInsuranceNumber())
                .active(user.getActive())
                .build();
    }

    //SE VALIDA QUE NO EXISTA DPI, CORREO O USUARIO REGISTRADO PREVIAMENTE.
    public void validateCreateUserRequest(CreateUserRequest request){
        if(userRepository.existsByDpi(request.getDpi())){
            throw new RuntimeException("Ya existe un usuario con ese DPI");
        }
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Ya existe un usuario con ese correo");
        }
        if(userRepository.existsByUsername(request.getUsername())){
            throw new RuntimeException("Ya existe un usuario con ese nombre de usuario");

        }
    }

    //Se listan los usuarios
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .dpi(user.getDpi())
                        .nit(user.getNit())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(user.getRole().getName())
                        .insuranceNumber(
                                user.getInsuranceNumber())
                        .active(user.getActive())
                        .build())
                .toList();
    }

    //Buscar usuario por ID
    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .dpi(user.getDpi())
                .nit(user.getNit())
                .phone(user.getPhone())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .insuranceNumber(user.getInsuranceNumber())
                .active(user.getActive())
                .build();
    }

    //Actualización de usuario
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        //Validacion usuario y role existentes
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        Role role = roleRepository.findById(request.getRoleId()).orElseThrow(() ->new RuntimeException("Rol no encontrado."));

        user.setFullName(request.getFullName());
        user.setDpi(request.getDpi());
        user.setNit(request.getNit());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setInsuranceNumber(request.getInsuranceNumber());
        user.setActive(request.getActive());
        user.setRole(role);

        user = userRepository.save(user);

        auditService.log(user.getUsername(),"UPDATE_USER","USERS","Usuario actualizado correctamente.");

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .dpi(user.getDpi())
                .nit(user.getNit())
                .phone(user.getPhone())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .insuranceNumber(user.getInsuranceNumber())
                .active(user.getActive())
                .build();
    }

    //Validar cambios de estatus
    public UserResponse changeStatus(Long id,Boolean active) {

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        user.setActive(active);
        user = userRepository.save(user);

        auditService.log(
                user.getUsername(),
                "CHANGE_STATUS",
                "USERS",
                "Estado del usuario actualizado.");

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .dpi(user.getDpi())
                .nit(user.getNit())
                .phone(user.getPhone())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .insuranceNumber(user.getInsuranceNumber())
                .active(user.getActive())
                .build();
    }
}
