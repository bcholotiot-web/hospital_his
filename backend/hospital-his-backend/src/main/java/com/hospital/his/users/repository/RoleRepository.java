package com.hospital.his.users.repository;

import com.hospital.his.users.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository
    extends JpaRepository<Role, Long>{
}
