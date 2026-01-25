UPDATE events e
    JOIN categories r ON r.id = e.event_type_id
    JOIN category_groups cg ON cg.id = r.group_id AND cg.name = '프로그램 유형'
    JOIN categories etc ON etc.group_id = r.group_id AND etc.name = '기타'
    SET e.event_type_id = etc.id
WHERE r.name = '레크리에이션';

DELETE c
FROM categories c
JOIN category_groups cg ON cg.id = c.group_id AND cg.name = '프로그램 유형'
LEFT JOIN events e ON e.event_type_id = c.id
WHERE c.name = '레크리에이션'
  AND e.id IS NULL;