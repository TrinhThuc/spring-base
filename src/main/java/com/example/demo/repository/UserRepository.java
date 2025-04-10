package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
            "(:name IS NULL OR CONCAT(TRIM(UPPER(u.lastName)), ' ', TRIM(UPPER(u.firstName))) LIKE CONCAT('%', UPPER(:name), '%'))")
    Page<User> findUserByName(@Param("name") String name, Pageable pageable);

}
