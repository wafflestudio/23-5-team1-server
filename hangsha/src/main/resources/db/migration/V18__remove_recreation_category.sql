SET @recreation_id := (
  SELECT c.id
  FROM categories c
  JOIN category_groups cg ON cg.id = c.group_id
  WHERE cg.name = '프로그램 유형' AND c.name = '레크리에이션'
  LIMIT 1
);

SET @etc_id := (
  SELECT c.id
  FROM categories c
  JOIN category_groups cg ON cg.id = c.group_id
  WHERE cg.name = '프로그램 유형' AND c.name = '기타'
  LIMIT 1
);

UPDATE events
SET event_type_id = @etc_id
WHERE event_type_id = @recreation_id;

DELETE FROM categories
WHERE id = @recreation_id;