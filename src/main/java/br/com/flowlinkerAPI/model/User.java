package br.com.flowlinkerAPI.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 24)
    private Role role;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User() {

    }

    @PrePersist
    @PreUpdate
    public void ensureRole() {
        if (role == null) {
            role = Role.USER;
        }
    }

    @PostLoad
    public void ensureRoleAfterLoad() {
        if (role == null) {
            role = Role.USER;
        }
    }

    public enum Role {
        USER,
        ADMIN
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
