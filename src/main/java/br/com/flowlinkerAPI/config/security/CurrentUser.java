package br.com.flowlinkerAPI.config.security;

public record CurrentUser(String username, Long customerId, String deviceFingerprint, String role) {

    public boolean hasRole(String expected) {
        if (role == null || expected == null) {
            return false;
        }
        return role.equalsIgnoreCase(expected);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
