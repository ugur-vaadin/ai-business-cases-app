-- Row-level scoping for the AI's read-only accounts. Each view filters by the connecting user's countries
-- (scope_membership joined on CURRENT_USER()) and by the optional per-query country filter (@country). In
-- PostgreSQL the same design is a row-level security policy on the base tables using current_user and
-- current_setting('app.country'); the views and the membership table are the H2 form of it.

CREATE TABLE IF NOT EXISTS scope_membership (db_user VARCHAR(64) NOT NULL, country VARCHAR(2) NOT NULL);
INSERT INTO scope_membership (db_user, country) VALUES
    ('AI_READER_ALL', 'FI'), ('AI_READER_ALL', 'EE'), ('AI_READER_ALL', 'SE'),
    ('AI_READER_ALL', 'NO'), ('AI_READER_ALL', 'DK'), ('AI_READER_ALL', 'DE'),
    ('AI_READER_FI_EE', 'FI'), ('AI_READER_FI_EE', 'EE'),
    ('AI_READER_SE_NO', 'SE'), ('AI_READER_SE_NO', 'NO'),
    ('AI_READER_DK_DE', 'DK'), ('AI_READER_DK_DE', 'DE');

CREATE TABLE IF NOT EXISTS scope_assignment (user_id INT PRIMARY KEY, role_scope VARCHAR(16) NOT NULL);
INSERT INTO scope_assignment (user_id, role_scope) VALUES (1, 'all'), (8, 'fi_ee'), (9, 'se_no'), (10, 'dk_de');

CREATE SCHEMA IF NOT EXISTS scoped;

-- One view per table the dataset exposes to the AI account, under the same name, so the schema text the
-- model reads needs no change. A table without customers in it is visible in full.
CREATE OR REPLACE VIEW scoped.warehouses AS SELECT t.* FROM public.warehouses t;
CREATE OR REPLACE VIEW scoped.categories AS SELECT t.* FROM public.categories t;
CREATE OR REPLACE VIEW scoped.suppliers AS SELECT t.* FROM public.suppliers t;
CREATE OR REPLACE VIEW scoped.products AS SELECT t.* FROM public.products t;
CREATE OR REPLACE VIEW scoped.price_history AS SELECT t.* FROM public.price_history t;
CREATE OR REPLACE VIEW scoped.promotions AS SELECT t.* FROM public.promotions t;
CREATE OR REPLACE VIEW scoped.inventory AS SELECT t.* FROM public.inventory t;
CREATE OR REPLACE VIEW scoped.customers AS
  SELECT t.* FROM public.customers t
  WHERE t.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR t.country = @country);
CREATE OR REPLACE VIEW scoped.delivery_addresses AS
  SELECT t.* FROM public.delivery_addresses t
  JOIN public.customers c ON c.id = t.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.orders AS
  SELECT t.* FROM public.orders t
  JOIN public.customers c ON c.id = t.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.order_lines AS
  SELECT t.* FROM public.order_lines t
  JOIN public.orders o ON o.id = t.order_id
  JOIN public.customers c ON c.id = o.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.shipments AS
  SELECT t.* FROM public.shipments t
  JOIN public.orders o ON o.id = t.order_id
  JOIN public.customers c ON c.id = o.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.shipment_lines AS
  SELECT t.* FROM public.shipment_lines t
  JOIN public.shipments s ON s.id = t.shipment_id
  JOIN public.orders o ON o.id = s.order_id
  JOIN public.customers c ON c.id = o.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.delivery_events AS
  SELECT t.* FROM public.delivery_events t
  JOIN public.shipments s ON s.id = t.shipment_id
  JOIN public.orders o ON o.id = s.order_id
  JOIN public.customers c ON c.id = o.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.claims AS
  SELECT t.* FROM public.claims t
  JOIN public.customers c ON c.id = t.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.claim_lines AS
  SELECT t.* FROM public.claim_lines t
  JOIN public.claims k ON k.id = t.claim_id
  JOIN public.customers c ON c.id = k.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.return_authorisations AS
  SELECT t.* FROM public.return_authorisations t
  JOIN public.customers c ON c.id = t.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.return_lines AS
  SELECT t.* FROM public.return_lines t
  JOIN public.return_authorisations r ON r.id = t.return_id
  JOIN public.customers c ON c.id = r.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.credit_notes AS
  SELECT t.* FROM public.credit_notes t
  JOIN public.customers c ON c.id = t.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.stock_movements AS SELECT t.* FROM public.stock_movements t;
CREATE OR REPLACE VIEW scoped.product_current_prices AS SELECT t.* FROM public.product_current_prices t;
CREATE OR REPLACE VIEW scoped.late_shipments AS
  SELECT t.* FROM public.late_shipments t
  JOIN public.customers c ON c.id = t.customer_id
  WHERE c.country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR c.country = @country);
CREATE OR REPLACE VIEW scoped.open_claims AS
  SELECT t.* FROM public.open_claims t
  WHERE t.customer_country IN (SELECT m.country FROM public.scope_membership m WHERE m.db_user = CURRENT_USER())
    AND (@country IS NULL OR t.customer_country = @country);
CREATE OR REPLACE VIEW scoped.staff AS SELECT t.* FROM public.staff t;

-- One read-only account per business role. The password below never works: the application replaces it
-- with a random value it keeps in memory, so no credential is written in any file.
CREATE USER IF NOT EXISTS ai_reader_all PASSWORD 'placeholder';
CREATE USER IF NOT EXISTS ai_reader_fi_ee PASSWORD 'placeholder';
CREATE USER IF NOT EXISTS ai_reader_se_no PASSWORD 'placeholder';
CREATE USER IF NOT EXISTS ai_reader_dk_de PASSWORD 'placeholder';

-- The accounts may read the filtered views and nothing else. The views read the base tables and the
-- membership table with the rights of their owner.
GRANT SELECT ON scoped.warehouses TO ai_reader_all;
GRANT SELECT ON scoped.categories TO ai_reader_all;
GRANT SELECT ON scoped.suppliers TO ai_reader_all;
GRANT SELECT ON scoped.products TO ai_reader_all;
GRANT SELECT ON scoped.price_history TO ai_reader_all;
GRANT SELECT ON scoped.promotions TO ai_reader_all;
GRANT SELECT ON scoped.inventory TO ai_reader_all;
GRANT SELECT ON scoped.customers TO ai_reader_all;
GRANT SELECT ON scoped.delivery_addresses TO ai_reader_all;
GRANT SELECT ON scoped.orders TO ai_reader_all;
GRANT SELECT ON scoped.order_lines TO ai_reader_all;
GRANT SELECT ON scoped.shipments TO ai_reader_all;
GRANT SELECT ON scoped.shipment_lines TO ai_reader_all;
GRANT SELECT ON scoped.delivery_events TO ai_reader_all;
GRANT SELECT ON scoped.claims TO ai_reader_all;
GRANT SELECT ON scoped.claim_lines TO ai_reader_all;
GRANT SELECT ON scoped.return_authorisations TO ai_reader_all;
GRANT SELECT ON scoped.return_lines TO ai_reader_all;
GRANT SELECT ON scoped.credit_notes TO ai_reader_all;
GRANT SELECT ON scoped.stock_movements TO ai_reader_all;
GRANT SELECT ON scoped.product_current_prices TO ai_reader_all;
GRANT SELECT ON scoped.late_shipments TO ai_reader_all;
GRANT SELECT ON scoped.open_claims TO ai_reader_all;
GRANT SELECT ON scoped.staff TO ai_reader_all;

GRANT SELECT ON scoped.warehouses TO ai_reader_fi_ee;
GRANT SELECT ON scoped.categories TO ai_reader_fi_ee;
GRANT SELECT ON scoped.suppliers TO ai_reader_fi_ee;
GRANT SELECT ON scoped.products TO ai_reader_fi_ee;
GRANT SELECT ON scoped.price_history TO ai_reader_fi_ee;
GRANT SELECT ON scoped.promotions TO ai_reader_fi_ee;
GRANT SELECT ON scoped.inventory TO ai_reader_fi_ee;
GRANT SELECT ON scoped.customers TO ai_reader_fi_ee;
GRANT SELECT ON scoped.delivery_addresses TO ai_reader_fi_ee;
GRANT SELECT ON scoped.orders TO ai_reader_fi_ee;
GRANT SELECT ON scoped.order_lines TO ai_reader_fi_ee;
GRANT SELECT ON scoped.shipments TO ai_reader_fi_ee;
GRANT SELECT ON scoped.shipment_lines TO ai_reader_fi_ee;
GRANT SELECT ON scoped.delivery_events TO ai_reader_fi_ee;
GRANT SELECT ON scoped.claims TO ai_reader_fi_ee;
GRANT SELECT ON scoped.claim_lines TO ai_reader_fi_ee;
GRANT SELECT ON scoped.return_authorisations TO ai_reader_fi_ee;
GRANT SELECT ON scoped.return_lines TO ai_reader_fi_ee;
GRANT SELECT ON scoped.credit_notes TO ai_reader_fi_ee;
GRANT SELECT ON scoped.stock_movements TO ai_reader_fi_ee;
GRANT SELECT ON scoped.product_current_prices TO ai_reader_fi_ee;
GRANT SELECT ON scoped.late_shipments TO ai_reader_fi_ee;
GRANT SELECT ON scoped.open_claims TO ai_reader_fi_ee;
GRANT SELECT ON scoped.staff TO ai_reader_fi_ee;

GRANT SELECT ON scoped.warehouses TO ai_reader_se_no;
GRANT SELECT ON scoped.categories TO ai_reader_se_no;
GRANT SELECT ON scoped.suppliers TO ai_reader_se_no;
GRANT SELECT ON scoped.products TO ai_reader_se_no;
GRANT SELECT ON scoped.price_history TO ai_reader_se_no;
GRANT SELECT ON scoped.promotions TO ai_reader_se_no;
GRANT SELECT ON scoped.inventory TO ai_reader_se_no;
GRANT SELECT ON scoped.customers TO ai_reader_se_no;
GRANT SELECT ON scoped.delivery_addresses TO ai_reader_se_no;
GRANT SELECT ON scoped.orders TO ai_reader_se_no;
GRANT SELECT ON scoped.order_lines TO ai_reader_se_no;
GRANT SELECT ON scoped.shipments TO ai_reader_se_no;
GRANT SELECT ON scoped.shipment_lines TO ai_reader_se_no;
GRANT SELECT ON scoped.delivery_events TO ai_reader_se_no;
GRANT SELECT ON scoped.claims TO ai_reader_se_no;
GRANT SELECT ON scoped.claim_lines TO ai_reader_se_no;
GRANT SELECT ON scoped.return_authorisations TO ai_reader_se_no;
GRANT SELECT ON scoped.return_lines TO ai_reader_se_no;
GRANT SELECT ON scoped.credit_notes TO ai_reader_se_no;
GRANT SELECT ON scoped.stock_movements TO ai_reader_se_no;
GRANT SELECT ON scoped.product_current_prices TO ai_reader_se_no;
GRANT SELECT ON scoped.late_shipments TO ai_reader_se_no;
GRANT SELECT ON scoped.open_claims TO ai_reader_se_no;
GRANT SELECT ON scoped.staff TO ai_reader_se_no;

GRANT SELECT ON scoped.warehouses TO ai_reader_dk_de;
GRANT SELECT ON scoped.categories TO ai_reader_dk_de;
GRANT SELECT ON scoped.suppliers TO ai_reader_dk_de;
GRANT SELECT ON scoped.products TO ai_reader_dk_de;
GRANT SELECT ON scoped.price_history TO ai_reader_dk_de;
GRANT SELECT ON scoped.promotions TO ai_reader_dk_de;
GRANT SELECT ON scoped.inventory TO ai_reader_dk_de;
GRANT SELECT ON scoped.customers TO ai_reader_dk_de;
GRANT SELECT ON scoped.delivery_addresses TO ai_reader_dk_de;
GRANT SELECT ON scoped.orders TO ai_reader_dk_de;
GRANT SELECT ON scoped.order_lines TO ai_reader_dk_de;
GRANT SELECT ON scoped.shipments TO ai_reader_dk_de;
GRANT SELECT ON scoped.shipment_lines TO ai_reader_dk_de;
GRANT SELECT ON scoped.delivery_events TO ai_reader_dk_de;
GRANT SELECT ON scoped.claims TO ai_reader_dk_de;
GRANT SELECT ON scoped.claim_lines TO ai_reader_dk_de;
GRANT SELECT ON scoped.return_authorisations TO ai_reader_dk_de;
GRANT SELECT ON scoped.return_lines TO ai_reader_dk_de;
GRANT SELECT ON scoped.credit_notes TO ai_reader_dk_de;
GRANT SELECT ON scoped.stock_movements TO ai_reader_dk_de;
GRANT SELECT ON scoped.product_current_prices TO ai_reader_dk_de;
GRANT SELECT ON scoped.late_shipments TO ai_reader_dk_de;
GRANT SELECT ON scoped.open_claims TO ai_reader_dk_de;
GRANT SELECT ON scoped.staff TO ai_reader_dk_de;

