package com.lakshy.blog;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lakshy.blog.config.AppConstants;
import com.lakshy.blog.entities.Role;
import com.lakshy.blog.repositories.RoleRepo;

@SpringBootApplication
public class BloggingAppApisApplication implements CommandLineRunner {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RoleRepo roleRepo;

	public static void main(String[] args) {
		SpringApplication.run(BloggingAppApisApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Override
	public void run(String... args) throws Exception {
		Role adminRole = new Role();
		adminRole.setId(AppConstants.ADMIN_USER);
		adminRole.setName("ROLE_ADMIN");

		Role normalRole = new Role();
		normalRole.setId(AppConstants.NORMAL_USER);
		normalRole.setName("ROLE_NORMAL");

		List<Role> roles = List.of(adminRole, normalRole);

		try {
			List<Role> result = this.roleRepo.saveAll(roles);
			result.forEach(r -> System.out.println("Seeded role: " + r.getName()));
		} catch (DataIntegrityViolationException e) {
			// Roles already exist from a previous startup — safe to ignore.
		} catch (Exception e) {
			// Any other failure means the DB is misconfigured; fail fast so the problem
			// is visible rather than silently running without required roles.
			throw new RuntimeException("Application startup failed: unable to seed roles", e);
		}
	}
}
