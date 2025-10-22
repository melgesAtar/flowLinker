package br.com.flowlinkerAPI.dto;

import br.com.flowlinkerAPI.model.User;
import lombok.Getter;


@Getter
public class CreatedUserResultDTO {

    private final User user;
    private final boolean isNewUser;
    private final String plainPassword;

    public CreatedUserResultDTO(User user, boolean isNewUser, String plainPassword) {
        this.user = user;
        this.isNewUser = isNewUser;
        this.plainPassword = plainPassword;
    }

    
}
