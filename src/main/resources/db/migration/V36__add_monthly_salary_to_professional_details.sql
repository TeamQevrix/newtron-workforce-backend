-- Add expected monthly salary
ALTER TABLE worker_professional_details
ADD COLUMN expected_monthly_salary DECIMAL(12,2) NULL;
