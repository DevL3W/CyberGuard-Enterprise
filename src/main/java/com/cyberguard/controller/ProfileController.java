package com.cyberguard.controller;

import com.cyberguard.dto.LoginResponse;
import com.cyberguard.dto.PasswordChangeRequest;
import com.cyberguard.dto.ProfileDto;
import com.cyberguard.dto.ProfileUpdateRequest;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.GuestService;
import com.cyberguard.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService service;
    private final GuestService guest;

    public ProfileController(ProfileService service, GuestService guest) {
        this.service = service; this.guest = guest;
    }

    @GetMapping
    public ProfileDto get(@AuthenticationPrincipal AppUserDetails actor) { return service.get(actor); }

    @PutMapping
    public ProfileDto update(@AuthenticationPrincipal AppUserDetails actor, @Valid @RequestBody ProfileUpdateRequest req) {
        guest.denyIfGuest(actor);
        return service.update(actor, req);
    }

    @PostMapping("/password")
    public LoginResponse changePassword(@AuthenticationPrincipal AppUserDetails actor,
                                        @Valid @RequestBody PasswordChangeRequest req) {
        guest.denyIfGuest(actor);
        return service.changePassword(actor, req);
    }
}
