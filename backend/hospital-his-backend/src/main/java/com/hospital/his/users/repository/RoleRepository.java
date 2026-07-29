package com.hospital.his.users.repository;

import com.hospital.his.users.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository
    extends JpaRepository<Role, Long>{
    Optional<Role> findByName(String name);
}
