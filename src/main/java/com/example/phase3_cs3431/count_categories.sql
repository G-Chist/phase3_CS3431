CREATE OR REPLACE FUNCTION
    count_categories(b1_id VARCHAR(22),b2_id VARCHAR(22)) RETURNS INT AS '
    DECLARE
        commonCategories INT;
    BEGIN
        WITH categories AS (SELECT categoryname
                            FROM category
                            WHERE business_id = b1_id
                            INTERSECT
                            SELECT categoryname
                            FROM category
                            WHERE business_id = b2_id)
        SELECT count(categories.categoryname)
        INTO commonCategories
        FROM categories;
        RETURN commonCategories;
    END;
' LANGUAGE plpgsql;
