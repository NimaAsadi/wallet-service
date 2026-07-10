package ir.ebb.userinfo.entity;

import ir.ebb.common.model.base.BaseEntity;
import ir.ebb.common.model.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

    private User user;
    private String nationalCode;
    private String fullName;
    private String accountName;
    private String fatherName;
    private String mobile;
    private String email;
    private Boolean active;
    private boolean isRQ = true;
}
