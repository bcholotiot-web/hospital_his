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
import com.hospital.his.common.exception.LoginAuthenticationException;
import com.hospital.his.security.jwt.JwtService;
import com.hospital.his.auth.dto.VerifyDpiRequest;
import com.hospital.his.auth.dto.VerifyDpiResponse;
import com.hospital.his.notifications.service.EmailService;
import com.hospital.his.auth.dto.AuthenticatedUserResponse;
import com.hospital.his.security.UserAuthorityService;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;


@Service
public class AuthService {

    //Repositorios
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /**
     * Recupera las autoridades actuales desde
     * PostgreSQL.
     */
    private final UserAuthorityService userAuthorityService;
    private final EmailService emailService;
    @Value("${security.login.max-attempts:3}")
    private int maxLoginAttempts;
    @Value("${security.login.lock-duration-minutes:15}")
    private long lockDurationMinutes;

    // Constructor
    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PatientRepository patientRepository,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService, UserAuthorityService userAuthorityService,
            EmailService emailService){

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userAuthorityService = userAuthorityService;
        this.emailService = emailService;
    }

    //Metodo para registro
    public RegisterResponse register(RegisterRequest request) {
        /*
         * Validar los datos del registro.
         */
        validateRegisterRequest(request);

        /*
         * Buscar el rol PACIENTE.
         */
        Role patientRole = roleRepository
                .findByName("PACIENTE")
                .orElseThrow(
                        () -> new RuntimeException(
                                "Rol PACIENTE no encontrado."
                        )
                );

        /*
         * Construir el usuario externo.
         */
        User user = User.builder()
                .fullName(request.getFullName())
                .dpi(request.getDpi())
                .nit(request.getNit())
                .phone(request.getPhone())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .active(true)
                .role(patientRole)
                .build();

        /*
         * Guardar el usuario.
         */
        user = userRepository.save(user);

        System.out.println(
                "Usuario registrado correctamente. ID: "
                        + user.getId()
        );

        /*
         * Crear el registro del paciente relacionado
         * con el usuario.
         */
        Patient patient = Patient.builder()
                .user(user)
                .insuranceNumber(
                        request.getInsuranceNumber()
                )
                .build();

        /*
         * Guardar el paciente.
         */
        patientRepository.save(patient);

        System.out.println(
                "Paciente registrado correctamente."
        );

        /*
         * Registrar auditoría del registro.
         */
        auditService.log(
                user.getUsername(),
                "REGISTER",
                "AUTH",
                "Usuario registrado exitosamente."
        );

        /*
         * Intentar enviar el correo una sola vez.
         */
        boolean welcomeEmailSent = false;

        try {
            System.out.println(
                    "Intentando enviar correo de bienvenida a: "
                            + user.getEmail()
            );

            emailService.sendWelcomeEmail(
                    user.getEmail(),
                    user.getFullName()
            );

            /*
             * Solo se asigna true si sendWelcomeEmail()
             * finalizó sin lanzar una excepción.
             */
            welcomeEmailSent = true;

            System.out.println(
                    "Correo de bienvenida enviado correctamente a: "
                            + user.getEmail()
            );

            auditService.log(
                    user.getUsername(),
                    "WELCOME_EMAIL_SENT",
                    "AUTH",
                    "Correo de bienvenida enviado correctamente."
            );

        } catch (RuntimeException exception) {
            /*
             * El registro ya fue completado.
             * La falla del correo no elimina al usuario.
             */
            welcomeEmailSent = false;

            auditService.log(
                    user.getUsername(),
                    "WELCOME_EMAIL_FAILED",
                    "AUTH",
                    "El registro fue completado, pero falló "
                            + "el envío del correo de bienvenida."
            );

            System.err.println(
                    "No fue posible enviar el correo de bienvenida a "
                            + user.getEmail()
            );

            System.err.println(
                    "Causa: "
                            + exception.getMessage()
            );

            /*
             * Temporal durante las pruebas.
             * Permite visualizar la causa completa del error SMTP.
             */
            exception.printStackTrace();
        }

        String responseMessage;

        if (welcomeEmailSent) {
            responseMessage =
                    "¡Registro exitoso! Su cuenta ha sido creada "
                            + "y el correo electrónico de bienvenida "
                            + "fue enviado correctamente. "
                            + "Ahora puede iniciar sesión con sus credenciales.";
        } else {
            responseMessage =
                    "¡Registro exitoso! Su cuenta ha sido creada. "
                            + "No fue posible enviar el correo electrónico "
                            + "de bienvenida, pero puede iniciar sesión "
                            + "con sus credenciales.";
        }

        return RegisterResponse.builder()
                .userId(user.getId())
                .message(responseMessage)
                .build();
    }

    //Metodo para login
    /**
     * RN-GLOBAL-007: Autenticación de usuarios internos.
     *
     * Reglas:
     * 1. Permite un máximo configurable de intentos fallidos.
     * 2. Informa cuántos intentos quedan.
     * 3. Bloquea temporalmente la cuenta al alcanzar el máximo.
     * 4. No incrementa intentos mientras existe un bloqueo activo.
     * 5. Restablece el contador cuando el bloqueo vence.
     * 6. Restablece el contador después de un acceso exitoso.
     */
    public LoginResponse login(LoginRequest request) {

        /*Esta validación evita procesar solicitudes sin usuario*/
        validateLoginRequest(request);
        String normalizedUsername = request.getUsername().trim();

        /*
         * Buscar al usuario por username.
         *
         * Para evitar revelar si una cuenta existe, se utiliza
         * el mismo mensaje de credenciales incorrectas.
         *
         * Una cuenta inexistente no puede acumular intentos en
         * users porque no existe un registro que actualizar.
         */
        Optional<User> optionalUser =
                userRepository.findByUsername(
                        normalizedUsername
                );

        if (optionalUser.isEmpty()) {
            throw LoginAuthenticationException
                    .invalidCredentials(
                            Math.max(
                                    maxLoginAttempts - 1,
                                    0
                            )
                    );
        }

        User user = optionalUser.get();

        LocalDateTime now = LocalDateTime.now();

        /*
         * Paso 1:
         * Verificar si la cuenta ya se encuentra bloqueada.
         *
         * Si lockedUntil contiene una fecha futura, el bloqueo
         * todavía se encuentra vigente.
         */
        if (
                user.getLockedUntil() != null &&
                        user.getLockedUntil().isAfter(now)
        ) {
            /*
             * No se incrementa el contador mientras la cuenta
             * está bloqueada.
             */
            throw LoginAuthenticationException
                    .currentlyBlocked(
                            user.getLockedUntil()
                    );
        }

        /*
         * Paso 2:
         * Si existe una fecha de bloqueo, pero ya venció,
         * se limpia automáticamente el bloqueo y se reinicia
         * el contador.
         */
        if (
                user.getLockedUntil() != null &&
                        !user.getLockedUntil().isAfter(now)
        ) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);

            userRepository.save(user);
        }

        /*
         * Paso 3:
         * Validar si el usuario está activo.
         *
         * Una cuenta inactiva no debe incrementar intentos
         * porque el problema no corresponde a la contraseña.
         */
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw LoginAuthenticationException
                    .inactiveAccount();
        }

        /*
         * Paso 4:
         * Validar la contraseña.
         */
        boolean validPassword =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!validPassword) {
            registerFailedLogin(user);

            /*
             * registerFailedLogin siempre lanza una excepción:
             *
             * 401 si quedan intentos.
             * 423 si se alcanzó el máximo.
             *
             * Este return solamente satisface el flujo del método.
             */
            throw new IllegalStateException(
                    "No fue posible procesar el intento fallido."
            );
        }

        /*
         * Paso 5:
         * La contraseña es correcta.
         *
         * Se reinician los intentos y cualquier bloqueo que
         * pudiera permanecer registrado.
         */
        resetLoginFailures(user);

        /*
         * Registrar la auditoría del inicio de sesión exitoso.
         */
        auditService.log(
                user.getUsername(),
                "LOGIN",
                "AUTH",
                "Inicio de sesión exitoso"
        );

        /*
         * Generar token JWT utilizando la lógica que ya
         * existía en la aplicación.
         */
        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole().getName()
        );

        return LoginResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .token(token)
                .message("Inicio de sesión exitoso.")
                .build();
    }

    /**
     * Devuelve la información actual de la sesión.
     *
     * Los permisos no se obtienen del contenido del JWT.
     * Se consultan desde PostgreSQL para reflejar
     * inmediatamente asignaciones y revocaciones.
     */
    public AuthenticatedUserResponse getAuthenticatedUser(String username) {
        if (
                username == null ||
                        username.isBlank()
        ) {
            throw new RuntimeException(
                    "No se pudo identificar al usuario autenticado."
            );
        }

        User user =
                userRepository
                        .findByUsername(
                                username
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Usuario autenticado no encontrado."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        user.getActive()
                )
        ) {
            throw new RuntimeException(
                    "La cuenta del usuario se encuentra inactiva."
            );
        }

        if (
                user.getRole() == null ||
                        !Boolean.TRUE.equals(
                                user.getRole().getActive()
                        )
        ) {
            throw new RuntimeException(
                    "El usuario no tiene un rol activo."
            );
        }

        /*
         * UserAuthorityService ya valida:
         *
         * - Usuario activo.
         * - Rol activo.
         * - Cuenta no bloqueada.
         * - Permisos activos.
         * - Asignaciones activas.
         */
        List<String> authorities =
                userAuthorityService
                        .getAuthorities(
                                username
                        )
                        .stream()
                        .map(
                                SimpleGrantedAuthority
                                        ::getAuthority
                        )
                        .distinct()
                        .sorted()
                        .toList();

        return AuthenticatedUserResponse
                .builder()
                .userId(
                        user.getId()
                )
                .username(
                        user.getUsername()
                )
                .fullName(
                        user.getFullName()
                )
                .roleId(
                        user.getRole()
                                .getId()
                )
                .role(
                        user.getRole()
                                .getName()
                )
                .branchId(
                        user.getBranch() != null
                                ? user.getBranch()
                                .getId()
                                : null
                )
                .branchName(
                        user.getBranch() != null
                                ? user.getBranch()
                                .getName()
                                : null
                )
                .authorities(
                        authorities
                )
                .build();
    }


    /**
     * CU-00: Verificar el registro de una persona antes
     * de permitir el agendamiento de una cita.
     *
     * Resultados:
     *
     * LOGIN:
     * El DPI pertenece a un usuario activo con rol PACIENTE.
     *
     * REGISTER:
     * El DPI no se encuentra registrado.
     *
     * INTERNAL_USER:
     * El DPI pertenece a un usuario interno.
     *
     * INACTIVE:
     * El DPI pertenece a una cuenta inactiva.
     */

    public VerifyDpiResponse verifyDpi(VerifyDpiRequest request) {
        /*
         * Aplicar RN-GLOBAL-001.
         */
        String dpi = validateAndNormalizeDpi(
                request
        );

        Optional<User> optionalUser =
                userRepository.findByDpi(dpi);

        /*
         * Si el DPI no está registrado, el flujo continúa
         * hacia el formulario de registro.
         */
        if (optionalUser.isEmpty()) {
            return VerifyDpiResponse.builder()
                    .registered(false)
                    .patient(false)
                    .action("REGISTER")
                    .message(
                            "No se encontró un registro asociado " +
                                    "a este DPI. Será redirigido al " +
                                    "formulario de registro."
                    )
                    .build();
        }

        User user = optionalUser.get();

        /*
         * Antes de revisar el rol, se comprueba que la cuenta
         * se encuentre activa.
         */
        if (!Boolean.TRUE.equals(user.getActive())) {
            return VerifyDpiResponse.builder()
                    .registered(true)
                    .patient(false)
                    .action("INACTIVE")
                    .message(
                            "La cuenta asociada a este DPI " +
                                    "se encuentra inactiva. " +
                                    "Por favor, contacte a recepción."
                    )
                    .build();
        }

        /*
         * Validación defensiva por si existe un usuario
         * mal registrado sin un rol asociado.
         */
        if (
                user.getRole() == null ||
                        user.getRole().getName() == null
        ) {
            return VerifyDpiResponse.builder()
                    .registered(true)
                    .patient(false)
                    .action("INTERNAL_USER")
                    .message(
                            "No fue posible determinar el tipo " +
                                    "de cuenta asociado a este DPI. " +
                                    "Por favor, contacte a recepción."
                    )
                    .build();
        }

        String roleName =
                user.getRole()
                        .getName()
                        .trim();

        /*
         * Se utiliza equalsIgnoreCase para admitir temporalmente
         * valores como PACIENTE, Paciente o paciente.
         */
        boolean isPatient =
                "PACIENTE".equalsIgnoreCase(
                        roleName
                );

        if (isPatient) {
            return VerifyDpiResponse.builder()
                    .registered(true)
                    .patient(true)
                    .action("LOGIN")
                    .message(
                            "Registro encontrado. " +
                                    "Inicie sesión para continuar con " +
                                    "el agendamiento de su cita."
                    )
                    .username(user.getUsername())
                    .build();
        }

        /*
         * El DPI existe, pero pertenece a un usuario interno.
         */
        return VerifyDpiResponse.builder()
                .registered(true)
                .patient(false)
                .action("INTERNAL_USER")
                .message(
                        "Este DPI pertenece a un usuario " +
                                "interno del sistema. " +
                                "Por favor, utilice el acceso " +
                                "correspondiente o contacte a recepción."
                )
                .build();
    }

    /**
     * Registra un intento fallido de autenticación.
     *
     * Si se alcanza el máximo permitido, establece la fecha
     * de finalización del bloqueo y devuelve HTTP 423.
     *
     * Si todavía quedan intentos, devuelve HTTP 401 junto
     * con la cantidad de intentos restantes.
     */
    private void registerFailedLogin(User user) {

        /*
         * Compatibilidad con registros existentes que puedan
         * contener null.
         */
        int currentFailedAttempts =
                user.getFailedLoginAttempts() == null
                        ? 0
                        : user.getFailedLoginAttempts();

        int newFailedAttempts =
                currentFailedAttempts + 1;

        user.setFailedLoginAttempts(
                newFailedAttempts
        );

        int remainingAttempts =
                maxLoginAttempts - newFailedAttempts;

        /*
         * Si el contador alcanzó o superó el máximo permitido,
         * se aplica el bloqueo temporal.
         */
        if (remainingAttempts <= 0) {

            LocalDateTime lockedUntil =
                    LocalDateTime.now()
                            .plusMinutes(
                                    lockDurationMinutes
                            );

            /*
             * El contador queda limitado al máximo configurado.
             *
             * De esta manera nunca aparecerán valores como 4, 5
             * o superiores si el máximo es 3.
             */
            user.setFailedLoginAttempts(
                    maxLoginAttempts
            );

            user.setLockedUntil(
                    lockedUntil
            );

            /*
             * Es indispensable guardar antes de lanzar
             * la excepción.
             */
            userRepository.save(user);

            /*
             * Respuesta HTTP 423 Locked.
             */
            throw LoginAuthenticationException
                    .accountBlocked(
                            lockedUntil
                    );
        }

        /*
         * Todavía quedan intentos disponibles.
         */
        user.setLockedUntil(null);

        /*
         * Guardar el contador antes de lanzar la excepción.
         */
        userRepository.save(user);

        /*
         * Respuesta HTTP 401 Unauthorized.
         */
        throw LoginAuthenticationException
                .invalidCredentials(
                        remainingAttempts
                );
    }

    /**
     * Reinicia el contador de intentos y elimina cualquier
     * bloqueo temporal después de una autenticación exitosa.
     */
    private void resetLoginFailures(User user) {

        int currentFailedAttempts =
                user.getFailedLoginAttempts() == null
                        ? 0
                        : user.getFailedLoginAttempts();

        boolean hasFailedAttempts =
                currentFailedAttempts > 0;

        boolean hasLockedUntil =
                user.getLockedUntil() != null;

        /*
         * Solo se actualiza la base de datos si realmente
         * existe información que limpiar.
         */
        if (hasFailedAttempts || hasLockedUntil) {

            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);

            userRepository.save(user);
        }
    }

    /**
     * RN-GLOBAL-001: Validación de DPI.
     *
     * 1. Es obligatorio.
     * 2. Debe tener exactamente 13 dígitos.
     * 3. Solamente puede contener números.
     */
    private String validateAndNormalizeDpi(VerifyDpiRequest request) {
        if (request == null) {
            throw new RuntimeException(
                    "El campo DPI es obligatorio. " +
                            "Por favor, ingrese su número de DPI."
            );
        }

        String dpi = request.getDpi();

        if (dpi == null || dpi.isBlank()) {
            throw new RuntimeException(
                    "El campo DPI es obligatorio. " +
                            "Por favor, ingrese su número de DPI."
            );
        }

        dpi = dpi.trim();

        /*
         * Primero se comprueba si existen caracteres
         * diferentes de números.
         */
        if (!dpi.matches("\\d+")) {
            throw new RuntimeException(
                    "El DPI debe contener únicamente números. " +
                            "No se permiten letras ni caracteres especiales."
            );
        }

        /*
         * Después se verifica la longitud para poder informar
         * cuántos dígitos fueron ingresados.
         */
        if (dpi.length() != 13) {
            throw new RuntimeException(
                    "El DPI debe contener exactamente 13 dígitos. " +
                            "Usted ingresó " +
                            dpi.length() +
                            " dígitos."
            );
        }

        return dpi;
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
