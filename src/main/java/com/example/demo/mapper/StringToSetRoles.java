package com.example.demo.mapper;

import com.example.demo.entity.Role;
import com.example.demo.repository.RoleRepository;
import org.modelmapper.AbstractConverter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class StringToSetRoles extends AbstractConverter<Set<String>, Set<Role>> {

    @Autowired
    private RoleRepository roleRepository;
    @Override
    protected Set<Role> convert(Set<String> strings) {
        Set<Role> roles = new HashSet<>();
        strings.forEach(role ->
        {
            Optional<Role> dbRole = roleRepository.findRoleByName(role);
            if(dbRole.isPresent())
            {
                roles.add(dbRole.get());
            }else
                throw new RuntimeException("role not found");
        });
        return roles;
    }
}