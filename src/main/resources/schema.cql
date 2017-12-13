DROP KEYSPACE atwater;
CREATE KEYSPACE atwater WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}; // change replication in production

CREATE TABLE IF NOT exists atwater.product(
    product_id timeuuid,
    name text,
    brandname text,
    short_description text,
    department text,
    long_description_path text, // pdf stored ON separate file store
    image_paths frozen<SET<text>>, // images stored ON separate file store
    video_paths frozen<SET<text>>, // videos stored ON separate file store
    sku text,
    upc text,
    product_tags frozen<SET<text>>,
    rating_count int,
    avg_rating float,
    price decimal,
    location_with_stock frozen<SET<text>>,
    PRIMARY KEY((product_id))
);

CREATE TABLE IF NOT exists atwater.product_quantity_by_location(
    product_id timeuuid,
    location text,
    quantity int,
    PRIMARY KEY((product_id),location)
);


CREATE TYPE IF NOT exists atwater.address_udt(
    street text,
    street2 text,
    street3 text,
    city text,
    postal_code text,
    state text,
    country text,
    phone_number text
);


CREATE TABLE IF NOT exists atwater.customer(
    customer_id timeuuid,
    phone_number text,
    email text, // login
    firstname text,
    lastname text,
    addresses frozen<map<text,address_udt>>,
    sex text,
    birth_date date,
    last_10_products_views frozen<set<text>>,
    password text, // hashed
    PRIMARY KEY (customer_id)
);

CREATE MATERIALIZED VIEW  IF NOT exists atwater.customer_by_email
    AS SELECT phone_number,firstname,lastname,addresses,sex,birth_date,last_10_products_views,password
    FROM atwater.customer
    where customer_id is not null AND email is not null
    PRIMARY KEY (email, customer_id);


CREATE TABLE IF NOT exists atwater.rollup (
    rollup_type text,
    rollup_time text,
    rollup_grouping_id text,
    rollup_sub_grouping_id text,
    rollup_value int,
    PRIMARY KEY ((rollup_type, rollup_time), rollup_grouping_id, rollup_sub_grouping_id)
    )WITH compaction = {'class': 'TimeWindowCompactionStrategy', 'compaction_window_unit': 'DAYS', 'compaction_window_size': '1'};


CREATE TABLE atwater.raw_data (
    year int,
    month int,
    day int,
    hour int,
    minute int,
    second int,
    uuid text,
    record_type_code text,
    value text,
    PRIMARY KEY ((year, month, day, hour, minute, second), uuid)
    )WITH compaction = {'class': 'TimeWindowCompactionStrategy', 'compaction_window_unit': 'DAYS', 'compaction_window_size': '1'};


