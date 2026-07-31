/**
 Será responsable de:

 Validar que el DPI no exista.
 Validar que el correo no exista.
 Validar que el username no exista.
 Validar la contraseña (mínimo 12 caracteres).
 Buscar el rol PACIENTE.
 Crear el User.
 Crear el Patient.
 Retornar una respuesta.
*/
package com.hospital.his.auth.service;


import com.hospital.his.auth.dto.LoginRequest;
import com.hospital.his.auth.dto.LoginResponse;
import com.hospital.his.auth.dto.RegisterRequest;
import com.hospital.his.auth.dto.RegisterResponse;
import com.hospital.his.users.entity.User;
import com.hospital.his.users.entity.Role;
import com.hospital.his.users.repository.UserRepository;
import com.hospital.his.users.repository.RoleRepository;
import com.hospital.his.patients.entity.Patient;
import com.hospital.his.patients.repository.PatientRepository;
import com.hospital.his.audit.service.AuditService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hospital.his.security.jwt.JwtService;

@Service
public class AuthService {

    //Repositorios
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Constructor
    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PatientRepository patientRepository,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService){

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    //Metodo para registro
    public RegisterResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        //Busca paciente
        Role patientRole = roleRepository.findByName("PACIENTE").orElseThrow(() -> new RuntimeException("Rol PACIENTE no encontrado."));

        User user = User.builder()
                .fullName(request.getFullName())
                .dpi(request.getDpi())
                .nit(request.getNit())
                .phone(request.getPhone())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .role(patientRole)
                .build();

        //Registra usuario
        user = userRepository.save(user);
        System.out.println("Usuario registrado: " + user.getId());
        Patient patient = Patient.builder()
                .user(user)
                .insuranceNumber(request.getInsuranceNumber())
                .build();

        //Registra paciente
        patientRepository.save(patient);
        System.out.println("Paciente registrado correctamente.");

        auditService.log(user.getUsername(), "REGISTER", "AUTH", "Usuario registrado exitosamente. ");
        return RegisterResponse.builder()
                .userId(user.getId())
                .message("¡Registro exitoso! Su cuenta ha sido creada. Ahora puede iniciar sesión con sus credenciales")
                .build();
    }

    //Metodo para login
    public LoginResponse login(LoginRequest request){
        validateLoginRequest(request);
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow(()-> new RuntimeException("Usuario o contraseña incorrectos."));



        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Usuario o contraseña incorrectos.");
        }

        if(!user.getActive()){
            throw new RuntimeException("La cuenta se encuentra inactiva");
        }

        auditService.log(user.getUsername(), "LOGIN", "AUTH", "Inicio de sesión exitoso");
        String token = jwtService.generateToken(user.getUsername(), user.getRole().getName());
        return LoginResponse.builder()
                        .userId(user.getId())
                                .fullName(user.getFullName())
                                        .role(user.getRole().getName())
                                            .token(token)
                                                .message("Inicio de sesión exitoso.")
                                                    .build();
    }

    //Validaciones login
    private void validateLoginRequest(LoginRequest request) {

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RuntimeException( "Debe ingresar un nombre de usuario.");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Debe ingresar una contraseña.");
        }
    }

    //Validaciones registro
    private void validateRegisterRequest(RegisterRequest request){
        validateDPI(request);
        validateNIT(request);
        validateFullName(request);
        validatePhone(request);
        validateInsuranceNumber(request);
        validateEmail(request);
        validateUserName(request);
        validatePassword(request);
    }

    //Reglas de negocios globales
    /** RN-GLOBAL-001: Validación de DPI
     •	Es obligatorio. Mensaje: "El campo DPI es obligatorio. Por favor, ingrese su número de DPI."
     •	Exactamente 13 caracteres. Mensaje: "El DPI debe contener exactamente 13 dígitos. Usted ingresó [X] dígitos."
     •	Numérico (solo dígitos). Mensaje: "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales."
     •	Aplica a: CU-00, CU-02, CU-05, CU-06, CU-07, CU-09, CU-10, CU-16*/
    private void validateDPI(RegisterRequest request){
        //Validación de DPI FA02
        String dpi = request.getDpi();

        if(dpi == null || dpi.isBlank()){
            throw new RuntimeException("El campo DPI es obligatorio. Por favor, ingrese su número de DPI");
        }
        if(dpi.length() != 13){
            throw new RuntimeException("El DPI debe contener exactamente 13 dígitos. Usted ingreso " + dpi.length() +" dígitos.");
        }
        if (!dpi.matches("\\d+")) {
            throw new RuntimeException("El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales.");
        }
        if(userRepository.existsByDpi(dpi)){
            throw new RuntimeException("Ya existe una cuenta registrada con este número de DPI. Si ya tiene cuenta, inicie sesión.");
        }
    }

    /**RN-GLOBAL-002: Validación de NIT
     •	Obligatorio. Mensaje: "El campo NIT es obligatorio."
     •	Entre 8 y 9 caracteres. Mensaje: "El NIT debe contener entre 8 y 9 caracteres. Usted ingresó [X] caracteres."
     •	Alfanumérico. Mensaje: "El NIT debe contener únicamente caracteres alfanuméricos."
     •	Aplica a: CU-01, CU-02*/
    private void validateNIT(RegisterRequest request){
        String nit = request.getNit();

        if(nit == null || nit.isBlank()){
            throw new RuntimeException("El campo NIT es obligatorio");
        }
        if(nit.length()<8 || nit.length()>9){
            throw new RuntimeException("El NIT debe contener entre 8 y 9 caracteres. Usted ingresó "+ nit.length() + " caracteres.");
        }
        if (!nit.matches("^[a-zA-Z0-9]+$")) {
            throw new RuntimeException(
                    "El NIT debe contener únicamente caracteres alfanuméricos");
        }

    }

    //CU-02: Registro de Usuarios Externos
    /**RN-CU02-01: Nombre Completo
     •	Obligatorio.
     •	10-100 caracteres.
     •	Mensaje: "El nombre debe contener entre 10 y 100 caracteres. Usted ingresó [X] caracteres."*/
    private void validateFullName(RegisterRequest request){
        String fullName = request.getFullName();
        //validacion nombre completo RN-CU02-01 (Obligatorio; 10-100 caracteres)
        if(fullName == null || fullName.isBlank()){
            throw new RuntimeException("El nombre completo es obligatorio");
        }
        if(fullName.length()<10 || fullName.length()>100){
            throw new RuntimeException("El nombre debe contener entre 10 y 100 caracteres. Usted ingreso" + fullName.length() + " caracteres.");
        }
    }

    /**RN-CU02-02: Teléfono
     •	Obligatorio.
     •	8 dígitos.
     •	Mensaje: "El número de teléfono debe contener exactamente 8 dígitos numéricos."*/
    private void validatePhone(RegisterRequest request){
        String phone = request.getPhone();

        //validacion Teléfono RN-CU02-02 (Obligatorio; 8 digitos)
        if(phone == null || phone.isBlank()){
            throw new RuntimeException("El teléfono es obligatorio");
        }
        if (!phone.matches("\\d{8}")) {
            throw new RuntimeException("El número de teléfono debe contener exactamente 8 dígitos numéricos.");
        }

    }

    /**RN-CU02-03: Seguro Médico
     •	Opcional.
     •	5-50 caracteres si se ingresa.*/
    private void validateInsuranceNumber(RegisterRequest request){
        String insuranceNumber = request.getInsuranceNumber();
        if(insuranceNumber == null || insuranceNumber.isBlank()){
            return;
        }
        if(insuranceNumber.length() < 5 || insuranceNumber.length() > 50){
                throw new RuntimeException("El número de seguro debe tener entre 5 y 50 caracteres");
        }
    }

    /**RN-CU02-04: Correo Electrónico
     •	Obligatorio.
     •	Formato email válido.
     •	Mensaje: "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com"
     */
    private void validateEmail(RegisterRequest request){
        String email = request.getEmail();

        if(email==null || email.isBlank()){
            throw new RuntimeException("El correo electrónico es obligatorio");
        }
        email = email.trim();
        //Validación de email FA03
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new RuntimeException("El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com");
        }
        if(userRepository.existsByEmail(email)){
            throw  new RuntimeException("Ya existe una cuenta registrada con este correo electrónico");
        }
    }

    /**RN-CU02-05: Nombre de Usuario
     •	Obligatorio.
     •	Entre 8 y 9 caracteres alfanuméricos.
     •	Mensaje mínimo: “El usuario debe contener al menos 8 caracteres.”
     •	Mensaje máximo: “El usuario no puede exceder los 9 caracteres.”
     •	Único en el sistema. Mensaje: “El nombre de usuario ya se encuentra registrado.”*/
    private void validateUserName(RegisterRequest request){
        String username = request.getUsername();
        if(username == null || username.isBlank()){
            throw new RuntimeException("El usuario es obligatorio");
        }
        if(username.length() < 8 || username.length() > 9){
            throw new RuntimeException("El nombre de usuario debe contener entre 8 y 9 caracteres.");
        }
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            throw new RuntimeException(
                    "El nombre de usuario debe contener únicamente caracteres alfanuméricos.");
        }
        if(userRepository.existsByUsername(username)){
            throw new RuntimeException("El nombre de usuario ya se encuentra registrado.");
        }
    }

    /**RN-CU02-06: Contraseña
     •	Obligatorio.
     •	Mínimo 12 caracteres.
     •	Mensaje: “La contraseña debe contener al menos 12 caracteres.”*/
    private void validatePassword(RegisterRequest request){
        String password = request.getPassword();
        if(password == null || password.isBlank()){
            throw new RuntimeException("La contraseña es obligatorio");
        }
        if(password.length()<12){
            throw new RuntimeException("La contraseña debe contener al menos 12 caracteres.");
        }
    }

}
