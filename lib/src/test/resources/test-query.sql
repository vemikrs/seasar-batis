SELECT 
    id,
    sequence_no,
    amount,
    rate,
    score,
    is_active,
    name,
    description,
    memo,
    char_code,
    DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') as created_at,
    DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') as updated_at,
    DATE_FORMAT(birth_date, '%Y-%m-%d') as birth_date,
    TIME_FORMAT(work_time, '%H:%i:%s') as work_time,
    status,
    user_type,
    preferences
FROM sbtest_users
/*BEGIN*/
WHERE 1=1
    /*IF id != null*/
    AND id = /*id*/1
    /*END*/
    /*IF name != null*/
    AND name LIKE /*name*/'%test%'
    /*END*/
    /*IF amount > 0*/
    AND amount > /*amount*/5000.00
    /*END*/
    /*IF score >= 0*/
    AND score >= /*score*/70.0
    /*END*/
    /*IF status != null*/
    AND status = /*status*/'ACTIVE'
    /*END*/
    /*IF birth_date != null*/
    AND birth_date >= /*birth_date*/'2000-01-01'
    /*END*/
    /*IF user_type != null*/
    AND FIND_IN_SET(/*user_type*/'ADMIN', user_type)
    /*END*/
/*END*/
ORDER BY id