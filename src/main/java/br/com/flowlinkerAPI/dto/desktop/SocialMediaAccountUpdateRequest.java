package br.com.flowlinkerAPI.dto.desktop;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
public class SocialMediaAccountUpdateRequest {
    public String platform;     // opcional (ex.: "FACEBOOK")
    public String username;     // opcional (se enviar, não pode ser vazio)
    public String nomePerfil;   // opcional
    @JsonAlias({"senha"})
    public String password;     // opcional; se enviado e não vazio, será atualizado
}