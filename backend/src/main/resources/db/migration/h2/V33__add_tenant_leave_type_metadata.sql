ALTER TABLE leave_type ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE leave_type ADD COLUMN statutory BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE leave_type ADD COLUMN paid BOOLEAN;
ALTER TABLE leave_type ADD COLUMN source_name VARCHAR(255);
ALTER TABLE leave_type ADD COLUMN source_url VARCHAR(1000);
ALTER TABLE leave_type ADD COLUMN effective_from DATE;
ALTER TABLE leave_type ADD COLUMN effective_to DATE;

UPDATE leave_type lt
SET active = (SELECT jlt.active FROM jurisdiction_leave_type jlt WHERE jlt.id = lt.source_jurisdiction_leave_type_id),
    statutory = (SELECT jlt.statutory FROM jurisdiction_leave_type jlt WHERE jlt.id = lt.source_jurisdiction_leave_type_id),
    paid = (SELECT jlt.paid FROM jurisdiction_leave_type jlt WHERE jlt.id = lt.source_jurisdiction_leave_type_id),
    source_name = (SELECT jlt.source_name FROM jurisdiction_leave_type jlt WHERE jlt.id = lt.source_jurisdiction_leave_type_id),
    source_url = (SELECT jlt.source_url FROM jurisdiction_leave_type jlt WHERE jlt.id = lt.source_jurisdiction_leave_type_id),
    effective_from = (SELECT jlt.effective_from FROM jurisdiction_leave_type jlt WHERE jlt.id = lt.source_jurisdiction_leave_type_id),
    effective_to = (SELECT jlt.effective_to FROM jurisdiction_leave_type jlt WHERE jlt.id = lt.source_jurisdiction_leave_type_id)
WHERE lt.source_jurisdiction_leave_type_id IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM jurisdiction_leave_type jlt
      WHERE jlt.id = lt.source_jurisdiction_leave_type_id
  );
