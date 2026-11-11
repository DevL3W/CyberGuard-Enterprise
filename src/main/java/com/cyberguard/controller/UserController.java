package com.cyberguard.controller;

import com.cyberguard.dto.CreateUserRequest;
import com.cyberguard.dto.PasswordResetRequest;
import com.cyberguard.dto.UpdateUserRequest;
import com.cyberguard.dto.UserDto;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @GetMapping
    public List<UserDto> list(@AuthenticationPrincipal AppUserDetails actor) { return service.list(actor); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@AuthenticationPrincipal AppUserDetails actor, @Valid @RequestBody CreateUserRequest req) {
        return service.create(actor, req);
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                              @Valid @RequestBody PasswordResetRequest req) {
        service.resetPassword(actor, id, req.newPassword());
    }

    @PatchMapping("/{id}")
    public UserDto update(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                          @RequestBody UpdateUserRequest req) {
        return service.update(actor, id, req);
    }
}
