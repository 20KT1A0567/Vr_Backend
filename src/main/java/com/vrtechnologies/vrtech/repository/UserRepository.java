package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    @Query("""
            SELECT u FROM User u
            WHERE u.role <> com.vrtechnologies.vrtech.entity.enums.Role.USER
              AND (:role IS NULL OR u.role = :role)
              AND (:search IS NULL OR :search = ''
                OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<User> findAdmins(@Param("role") Role role, @Param("search") String search, Pageable pageable);

    long countByAdminRoleKey(String adminRoleKey);

    boolean existsByAdminRoleKey(String adminRoleKey);
}
