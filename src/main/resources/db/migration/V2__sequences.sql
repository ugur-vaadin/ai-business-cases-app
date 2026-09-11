-- The dataset's tables use plain INT keys seeded from CSV. These sequences start far above any seeded id so new rows
-- never collide with the data; Hibernate draws one value per insert (allocationSize = 1 on the entities).
CREATE SEQUENCE IF NOT EXISTS claims_seq START WITH 1000000;
CREATE SEQUENCE IF NOT EXISTS price_history_seq START WITH 1000000;
CREATE SEQUENCE IF NOT EXISTS saved_widgets_seq START WITH 1000000;
