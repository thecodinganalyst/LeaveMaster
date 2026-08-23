ALTER TABLE leave_type ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE leave_type ADD COLUMN statutory BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE leave_type ADD COLUMN paid BOOLEAN;
ALTER TABLE leave_type ADD COLUMN source_name VARCHAR(255);
ALTER TABLE leave_type ADD COLUMN source_url VARCHAR(1000);
ALTER TABLE leave_type ADD COLUMN effective_from DATE;
ALTER TABLE leave_type ADD COLUMN effective_to DATE;

UPDATE leave_type lt
SET active = jlt.active,
    statutory = jlt.statutory,
    paid = jlt.paid,
    source_name = jlt.source_name,
    source_url = jlt.source_url,
    effective_from = jlt.effective_from,
    effective_to = jlt.effective_to
FROM jurisdiction_leave_type jlt
WHERE jlt.id = lt.source_jurisdiction_leave_type_id;
