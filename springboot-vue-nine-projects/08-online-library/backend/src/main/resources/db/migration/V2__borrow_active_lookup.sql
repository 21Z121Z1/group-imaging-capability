-- Supports serialized active-borrow checks after acquiring the book row lock.
CREATE INDEX idx_borrow_active_lookup ON borrow_record (book_id, user_id, status);
