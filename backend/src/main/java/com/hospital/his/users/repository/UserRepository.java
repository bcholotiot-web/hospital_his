package com.hospital.his.users.repository;

import com.hospital.his.users.entity.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long>{

    //Validaciones para que el usuario, correo y DPI sean valores unicos
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByDpi(String dpi);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByDpi(String dpi);

    List<User> findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue(
            String roleName,
            Long branchId,
            Long specialtyId
    );

    List<User> findByBranch_IdAndSpecialtyIsNotNullAndActiveTrue(
            Long branchId
    );

    List<User> findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue(String roleName, Long branchId, Long specialtyId, Boolean active, Sort sort, Limit limit);

}
