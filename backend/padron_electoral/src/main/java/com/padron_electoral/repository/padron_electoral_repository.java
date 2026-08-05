package com.padron_electoral.repository;

import com.padron_electoral.model.padron_electoral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface padron_electoral_repository extends JpaRepository<padron_electoral, String> {

    List<padron_electoral> findByActivoTrue();

    Optional<padron_electoral> findByCuiAndActivoTrue(String cui);

    boolean existsByCui(String cui);
}