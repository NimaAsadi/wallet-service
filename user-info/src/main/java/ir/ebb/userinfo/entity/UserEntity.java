package ir.ebb.userinfo.entity;

import ir.ebb.common.model.base.BaseEntity;
import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.common.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Table(name = "user_info",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "account_number"})
        })
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(
                    name = "keycloakId",
                    column = @Column(name = "user_id", nullable = false, unique = true)),
            @AttributeOverride(
                    name = "dbsAccountNumber",
                    column = @Column(name = "account_number", nullable = false, unique = true))
    })
    private User user;

    @Column(name = "national_code")
    private String nationalCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "email")
    private String email;

    private Boolean active;

    @Column(name = "is_rq",nullable = false)
    private boolean isRQ = true;
}
