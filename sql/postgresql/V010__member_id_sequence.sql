-- Builtin identity backend uses a plain database sequence for member ids
-- (small incrementing integers, JS-safe). Privchat mode still inserts the
-- external server user_id explicitly, which bypasses this sequence.
CREATE SEQUENCE IF NOT EXISTS member_users_id_seq AS bigint START WITH 10000 INCREMENT BY 1;

-- Keep the sequence ahead of existing rows, but ignore any oversized ids left
-- over from earlier experiments (JS Number is only exact below 2^53); floor 10000.
SELECT setval(
    'member_users_id_seq',
    GREATEST(
        (SELECT COALESCE(MAX(id), 0) FROM public.member_users WHERE id < 9007199254740991),
        10000
    )
);
