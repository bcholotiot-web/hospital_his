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
import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.catalogs.repository.BranchRepository;
import com.hospital.his.catalogs.repository.SpecialtyRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hospital.his.users.dto.RoleResponse;
import java.util.List;
import java.text.Normalizer;
import java.util.Locale;


@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final BranchRepository branchRepository;
    private final SpecialtyRepository specialtyRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuditService auditService, BranchRepository branchRepository, SpecialtyRepository specialtyRepository){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.branchRepository = branchRepository;
        this.specialtyRepository = specialtyRepository;
    }

    //Se registra usuario al estar logueado
    public UserResponse createUser(CreateUserRequest request){

        Branch branch = null;

        if (request.getBranchId() != null) {
            branch = branchRepository.findById( request.getBranchId()).orElseThrow(() -> new RuntimeException("Sucursal no encontrada."));
        }

        Specialty specialty = null;

        if (request.getSpecialtyId() != null) {
            specialty = specialtyRepository.findById(request.getSpecialtyId()).orElseThrow(() -> new RuntimeException("Especialidad no encontrada."));
        }
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
                .branch(branch)
                .specialty(specialty)
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
                .branch(user.getBranch() != null ? user.getBranch().getName() : null)
                .specialty(user.getSpecialty() != null ? user.getSpecialty().getName() : null)
                .active(user.getActive())
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .specialtyId(user.getSpecialty() != null ? user.getSpecialty().getId() : null)
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
            throw new RuntimeException("El nombre de usuario ya se encuentra registrado. Por favor, elija otro");

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
                        .roleId(user.getRole() != null ? user.getRole().getId() : null)
                        .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                        .specialtyId(user.getSpecialty() != null ? user.getSpecialty().getId() : null)
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
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .specialtyId(user.getSpecialty() != null ? user.getSpecialty().getId() : null)
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .specialtyId(user.getSpecialty() != null ? user.getSpecialty().getId() : null)
                .build();
    }

    //Actualización de usuario
    public UserResponse updateUser( Long id,UpdateUserRequest request) {
        /*
         * Validar que el usuario exista.
         */
        User user = userRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Usuario no encontrado."
                        )
                );

        /*
         * Validar que el rol seleccionado exista.
         */
        Role role = roleRepository
                .findById(request.getRoleId())
                .orElseThrow(
                        () -> new RuntimeException(
                                "Rol no encontrado."
                        )
                );

        /*
         * Buscar la sucursal únicamente cuando se recibió
         * un identificador.
         */
        Branch branch = null;

        if (request.getBranchId() != null) {
            branch = branchRepository
                    .findById(request.getBranchId())
                    .orElseThrow(
                            () -> new RuntimeException(
                                    "Sucursal no encontrada."
                            )
                    );
        }

        /*
         * Determinar si el nuevo rol seleccionado
         * corresponde al rol MEDICO.
         */
        boolean doctorRole = isDoctorRole(role);

        /*
         * La especialidad solamente se consulta y asigna
         * cuando el rol seleccionado es MEDICO.
         */
        Specialty specialty = null;

        if (doctorRole) {
            if (request.getSpecialtyId() == null) {
                throw new RuntimeException(
                        "Debe seleccionar una especialidad para el médico."
                );
            }

            specialty = specialtyRepository
                    .findById(request.getSpecialtyId())
                    .orElseThrow(
                            () -> new RuntimeException(
                                    "Especialidad no encontrada."
                            )
                    );
        }

        /*
         * FA-04:
         * La nueva contraseña es opcional.
         *
         * Si no se recibe una nueva contraseña, se conserva
         * la contraseña actual.
         */
        String newPassword = request.getNewPassword();

        boolean passwordChanged =
                newPassword != null &&
                        !newPassword.isBlank();

        if (passwordChanged) {
            if (newPassword.length() < 12) {
                throw new RuntimeException(
                        "La nueva contraseña debe contener al menos 12 caracteres."
                );
            }

            /*
             * Nunca se debe guardar la contraseña directamente.
             * La nueva contraseña debe almacenarse cifrada.
             */
            user.setPassword(
                    passwordEncoder.encode(newPassword)
            );
        }

        /*
         * Actualizar los datos generales del usuario.
         */
        user.setFullName(request.getFullName());
        user.setDpi(request.getDpi());
        user.setNit(request.getNit());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setInsuranceNumber(
                request.getInsuranceNumber()
        );
        user.setActive(request.getActive());

        /*
         * Actualizar relaciones.
         */
        user.setRole(role);
        user.setBranch(branch);

        /*
         * Si el usuario es MEDICO, specialty contendrá
         * la especialidad seleccionada.
         *
         * Si el usuario no es MEDICO, specialty será null,
         * eliminando cualquier especialidad anterior.
         */
        user.setSpecialty(specialty);

        /*
         * Persistir todos los cambios.
         */
        user = userRepository.save(user);

        /*
         * Registrar la operación en auditoría.
         *
         * No se registra la contraseña ni su contenido.
         */
        String auditDescription = passwordChanged
                ? "Usuario actualizado y contraseña modificada correctamente."
                : "Usuario actualizado correctamente.";

        auditService.log(
                user.getUsername(),
                "UPDATE_USER",
                "USERS",
                auditDescription
        );

        /*
         * Construir la respuesta.
         *
         * Nunca se incluye la contraseña.
         */
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .dpi(user.getDpi())
                .nit(user.getNit())
                .phone(user.getPhone())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(
                        user.getRole() != null
                                ? user.getRole().getName()
                                : null
                )
                .insuranceNumber(
                        user.getInsuranceNumber()
                )
                .branch(
                        user.getBranch() != null
                                ? user.getBranch().getName()
                                : null
                )
                .specialty(
                        user.getSpecialty() != null
                                ? user.getSpecialty().getName()
                                : null
                )
                .active(user.getActive())
                .roleId(
                        user.getRole() != null
                                ? user.getRole().getId()
                                : null
                )
                .branchId(
                        user.getBranch() != null
                                ? user.getBranch().getId()
                                : null
                )
                .specialtyId(
                        user.getSpecialty() != null
                                ? user.getSpecialty().getId()
                                : null
                )
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
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .specialtyId(user.getSpecialty() != null ? user.getSpecialty().getId() : null)
                .build();
    }

    public List<RoleResponse> getRoles() {
        return roleRepository.findAll()
                .stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .build())
                .toList();
    }

    private boolean isDoctorRole(Role role) {
        if (
                role == null ||
                        role.getName() == null
        ) {
            return false;
        }

        String normalizedRoleName = Normalizer
                .normalize(
                        role.getName(),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        return normalizedRoleName.equals("MEDICO");
    }

}
