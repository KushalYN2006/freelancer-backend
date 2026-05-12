package com.freelancerhub.controller;

import com.freelancerhub.repository.UserRepository;
import com.freelancerhub.utils.ApiResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String name,
                                         @RequestParam Integer excludeUserId) {
        if (name == null || name.trim().length() < 2) {
            return ResponseEntity.ok(java.util.List.of());
        }

        return ResponseEntity.ok(userRepository
                .findTop10ByNameContainingIgnoreCaseAndUserIdNot(name.trim(), excludeUserId)
                .stream()
                .map(ApiResponseMapper::userSummary)
                .toList());
    }
}
