SELECT id, name, status, score
FROM sbtest_users
/*BEGIN*/
WHERE 1=1
/*IF statuses != null*/
  AND status IN /*statuses*/('ACTIVE')
/*END*/
/*IF keyword != null*/
  AND (
    name LIKE /*keyword*/'%'
    OR description LIKE /*keyword*/'%'
  )
/*END*/
/*IF minScore != null*/
  AND score >= /*minScore*/0
/*END*/
/*IF includeInactive == false*/
  AND status != 'INACTIVE'
/*END*/
/*END*/
ORDER BY id
