package ir.ebb.external.rayan.login.dto.request;

public record RayanLoginRequest(
        String username,
        String password,
        String applicationKey
) {}
