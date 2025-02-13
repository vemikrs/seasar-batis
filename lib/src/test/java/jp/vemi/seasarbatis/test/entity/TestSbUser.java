package jp.vemi.seasarbatis.test.entity;

import jp.vemi.seasarbatis.core.meta.SBColumnMeta;
import jp.vemi.seasarbatis.core.meta.SBTableMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SBTableMeta(name = "sbtest_users")
public class TestSbUser {

    @SBColumnMeta(name = "id", primaryKey = true)
    private Long id;

    @SBColumnMeta(name = "sequence_no")
    private Integer sequenceNo;

    @SBColumnMeta(name = "amount")
    private Double amount;

    @SBColumnMeta(name = "rate")
    private Float rate;

    @SBColumnMeta(name = "score")
    private Double score;

    @lombok.Getter(value = lombok.AccessLevel.NONE)
    @lombok.Setter(value = lombok.AccessLevel.NONE)
    @SBColumnMeta(name = "is_active")
    private Boolean isActive;

    @SBColumnMeta(name = "name")
    private String name;

    @SBColumnMeta(name = "description")
    private String description;

    @SBColumnMeta(name = "memo")
    private String memo;

    @SBColumnMeta(name = "char_code")
    private String charCode;

    @SBColumnMeta(name = "created_at")
    private java.sql.Timestamp createdAt;

    @SBColumnMeta(name = "updated_at")
    private java.sql.Timestamp updatedAt;

    @SBColumnMeta(name = "birth_date")
    private java.sql.Date birthDate;

    @SBColumnMeta(name = "work_time")
    private java.sql.Time workTime;

    @SBColumnMeta(name = "status")
    private String status;

    @SBColumnMeta(name = "user_type")
    private String userType;

    @SBColumnMeta(name = "preferences")
    private String preferences;

    // 個別のgetter/setter
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}