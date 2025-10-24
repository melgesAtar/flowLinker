package br.com.flowlinkerAPI.dto;

public class LoginResponseDTO {
    private String username;
    private String name;
    private String token;  

  
    public LoginResponseDTO(String username, String name) {
        this.username = username;
        this.name = name;
    }

    public LoginResponseDTO(String token) {
        this.token = token;
    }

 
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}