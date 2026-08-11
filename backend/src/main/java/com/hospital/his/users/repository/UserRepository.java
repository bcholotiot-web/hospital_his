package com.hospital.his.users.repository;

import com.hospital.his.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{

    //Validaciones para que el usuario, correo y DPI sean valores unicos
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByDpi(String dpi);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByDpi(String dpi);
}
