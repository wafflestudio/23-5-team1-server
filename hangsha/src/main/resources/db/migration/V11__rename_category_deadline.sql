UPDATE categories c
    JOIN category_groups cg ON cg.id = c.group_id
    SET c.name = '모집마감'
WHERE cg.name = '모집현황'
  AND c.name = '마감';