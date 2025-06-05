DELETE FROM collection_code_criteria
WHERE collection_type_code = 'JULY'
  AND school_category_code IN ('INDP_FNS', 'INDEPEND')
  AND facility_type_code = 'STANDARD';