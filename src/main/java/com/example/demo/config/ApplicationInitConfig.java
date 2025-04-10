package com.example.demo.config;


import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;
    static final String ADMIN_USER_NAME = "admin";
    static final String ADMIN_PASSWORD = "admin";


    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository)
    {
        return args -> {
            if(userRepository.findByUsername(ADMIN_USER_NAME).isEmpty())
            {
               Role adminRole =  roleRepository.save(Role.builder()
                        .name("ADMIN")
                        .build());

                roleRepository.save(Role.builder()
                        .name("USER")
                        .build());

                Set<Role> roles = new HashSet<>();
                roles.add(adminRole);


               userRepository.save(User.builder()
                               .username(ADMIN_USER_NAME)
                               .password(passwordEncoder.encode(ADMIN_PASSWORD))
                               .lotusPoint(0L)
                               .roles(roles)
                       .build());

                log.warn("admin user has been created with default password: admin, please change it");
            }
            log.info("Application initialization completed .....");
        };
    }
}
