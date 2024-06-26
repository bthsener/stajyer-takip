package com.batuhansener.stajyerTakip.service;

import com.batuhansener.stajyerTakip.dto.ProjectUserDto;
import com.batuhansener.stajyerTakip.dto.converter.ProjectUserConverter;
import com.batuhansener.stajyerTakip.dto.response.InternDto;
import com.batuhansener.stajyerTakip.dto.response.MentorDto;
import com.batuhansener.stajyerTakip.dto.response.UserDto;
import com.batuhansener.stajyerTakip.dto.converter.UserDtoConverter;
import com.batuhansener.stajyerTakip.dto.request.auth.CreateUserRequest;
import com.batuhansener.stajyerTakip.model.*;
import com.batuhansener.stajyerTakip.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final InternService internService;
    private final MentorService mentorService;
    private final UserDtoConverter userDtoConverter;
    private final ProjectUserConverter projectUserDtoConverter;

    public Optional<User> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(CreateUserRequest request) {
//        Set<Role> authorities = request.authorities().stream()
//                .map(role -> Role.fromValue(String.valueOf(role)))
//                .collect(Collectors.toSet());

        User newUser = User.builder()
                .name(request.name())
                .surname(request.surname())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .authorities(request.authorities())
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .isEnabled(true)
                .accountNonLocked(true)
                .projects(new ArrayList<>())
                .comments(new HashSet<>())
                .build();
        System.out.println(newUser.getAuthorities().size());

        newUser = userRepository.save(newUser);

        User finalNewUser = newUser;

        newUser.getAuthorities().stream().forEach(auth-> {
            if (auth.getAuthority().equals("ROLE_MENTOR")) {
                System.out.println(auth.getAuthority());
                mentorService.createMentor(finalNewUser);
            }else if (auth.getAuthority().equals("ROLE_INTERN")) {
                System.out.println(auth.getAuthority());
                internService.createIntern(finalNewUser);
            }
        });

        return newUser;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = getByUsername(username);
        System.out.println(user.get().getId());
        return user.orElseThrow(EntityNotFoundException::new);
    }

    public String findAuthenticatedUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = loadUserByUsername(authentication.getName());
        Optional<User> user = getByUsername(userDetails.getUsername());
        return user.get().getId();
    }

    public User findUserById(String id){
        return userRepository.findById(id).orElseThrow(()->new UsernameNotFoundException("User id yok!"));
    }

    public User findAuthenticatedUser(){
        return findUserById(findAuthenticatedUserId());
    }

    public User genericUpdateUser(User user){
        return userRepository.saveAndFlush(user);
    }

    public UserDto assignInternToProject(User user, Project project){
        return userDtoConverter.convert(genericUpdateUser(user));
    }

    public List<UserDto> getAllInterns() {
        List<UserDto> interns = userRepository.findByAuthoritiesContaining(Role.ROLE_INTERN)
                .stream().map(userDtoConverter::convert).collect(Collectors.toList());
        return interns;
    }

    public List<UserDto> getAllMentors(){
        List<UserDto> mentors = userRepository.findByAuthoritiesContaining(Role.ROLE_MENTOR)
                .stream().map(userDtoConverter::convert).collect(Collectors.toList());
        return mentors;
    }

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(userDtoConverter::convert).collect(Collectors.toList());
    }

    public void addUserComment(User user, Comment comment) {
        user.getComments().add(comment);
        userRepository.saveAndFlush(user);
    }

    public List<ProjectUserDto> getProjectUsers(Project project) {
        List<User> users = userRepository.findByProjects(project);
        System.out.println(users);
        return users.stream().map(projectUserDtoConverter::convert).collect(Collectors.toList());
    }

    public List<ProjectUserDto> getProjectUsersByRole(Project project, Role role) {
        List<User> users = userRepository.findByAuthoritiesContainingAndProjects(role, project);
        System.out.println(users);
        return users.stream().map(projectUserDtoConverter::convert).collect(Collectors.toList());
    }
}