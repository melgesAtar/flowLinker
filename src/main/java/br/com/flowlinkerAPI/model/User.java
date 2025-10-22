package br.com.flowlinkerAPI.model;
import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;

@Entity
@Setter
@Getter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    @OneToOne
    @JoinColumn(name = "customer_id", unique = true)
    private Customer customer;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User() {

    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
