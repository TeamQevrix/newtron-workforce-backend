-- 1. Alter existing expected_salary column to allow NULL
ALTER TABLE worker_professional_details
MODIFY COLUMN expected_salary DECIMAL(10,2) NULL;

-- 2. Backfill legacy MONTHLY workers.
-- Move their expected_salary into expected_monthly_salary safely, 
-- and then set expected_salary to NULL.
UPDATE worker_professional_details
SET expected_monthly_salary = expected_salary,
    expected_salary = NULL
WHERE preferred_work_type = 'MONTHLY'
  AND expected_salary IS NOT NULL
  AND expected_monthly_salary IS NULL;
