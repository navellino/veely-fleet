	package com.veely.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.GrantedAuthority;

import com.veely.entity.Employee;
import com.veely.repository.EmployeeRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import java.util.List;



@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final EmployeeRepository employeeRepository;

    // === Password encoder ===
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // === UserDetailsService: carica l’utente dal DB ===
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            Employee emp = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email non trovata: " + email));

            List<GrantedAuthority> authorities =
            		emp.getRoles().stream()
            	            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName()))
            	            .collect(Collectors.toList());

            return User.withUsername(emp.getEmail())
                       .password(emp.getPassword())
                       .authorities(authorities)
                       .build();
        };
    }

    /*
        '4', 'Administrator'
		'9', 'Expense Report User'
		'1', 'Expense Reports Manager'
		'8', 'Fleet Manager'
		'3', 'Fleet User'
		'5', 'HR Manager'
		'7', 'HR User'
		'6', 'Safety Manager'
		'10', 'Safety User'
		'2', 'User'
     */
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
        .headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts.disable()))
        //.headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000))) // 1 anno
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/fleet/employments/**").hasAnyRole("Administrator","HR Manager")
                .requestMatchers("/fleet/employees/**", "/safety/**").hasAnyRole("Administrator", "HR Manager", "Safety Manager")
                .requestMatchers("/fleet/vehicles/**").hasAnyRole("Administrator", "Fleet Manager", "Fleet User")
                .requestMatchers("/fleet/assignments/**", "/fleet/fuel-cards/**", "/fleet/maintenance/**", "/fleet/refuels/**")
                	.hasAnyRole("Administrator", "Fleet Manager")
                .requestMatchers("/fleet/expense_report/**").hasAnyRole("Administrator", "HR Manager","Expense Report User")
                .requestMatchers("/payslips/**").hasAnyRole("Administrator", "HR Manager")
                .requestMatchers("/correspondence/**").hasAnyRole("Administrator","Mail Manager")
                .requestMatchers("/h2-console/**").permitAll() // solo dev
                .anyRequest().authenticated()
        )
        .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403"))
        .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
        )
        .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
        );
        return http.build();
    }
}

