package com.cyberguard.dto;

/** El token solo se devuelve en el momento de crear la invitación; después no se puede recuperar. */
public record InvitationCreated(InvitationDto invitation, String token) {}
