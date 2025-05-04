CREATE OR REPLACE FUNCTION geodistance(
    lat_a DOUBLE PRECISION,
    lng_a DOUBLE PRECISION,
    lat_b DOUBLE PRECISION,
    lng_b DOUBLE PRECISION
) RETURNS DOUBLE PRECISION AS '
DECLARE
    radius_earth_km CONSTANT DOUBLE PRECISION := 6371.0;
    delta_lat_rad DOUBLE PRECISION;
    delta_lng_rad DOUBLE PRECISION;
    a DOUBLE PRECISION;
    c DOUBLE PRECISION;
BEGIN
    -- Convert delta values to radians
    delta_lat_rad := radians(lat_b - lat_a);
    delta_lng_rad := radians(lng_b - lng_a);

    -- Apply the haversine formula
    a := sin(delta_lat_rad / 2)^2
        + cos(radians(lat_a)) * cos(radians(lat_b))
             * sin(delta_lng_rad / 2)^2;

    c := 2 * atan2(sqrt(a), sqrt(1 - a));

    RETURN radius_earth_km * c / 1.609344; -- convert from km to miles
END;
' LANGUAGE plpgsql;