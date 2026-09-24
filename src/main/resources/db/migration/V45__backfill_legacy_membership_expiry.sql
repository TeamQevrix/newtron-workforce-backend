UPDATE worker_memberships
SET expires_at = DATE_ADD(activated_at, INTERVAL 1 MONTH)
WHERE status = 'ACTIVE' 
  AND expires_at IS NULL 
  AND activated_at IS NOT NULL;
