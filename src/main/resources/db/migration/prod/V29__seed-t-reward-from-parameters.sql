INSERT INTO public.t_reward (account_id, owner, multiplier, category, cpp)
SELECT a.account_id,
       a.owner,
       6.0,
       unnest(string_to_array(p.parameter_value, ',')),
       COALESCE((SELECT parameter_value::NUMERIC
                 FROM public.t_parameter
                 WHERE owner = a.owner
                   AND parameter_name = 'rewards_cpp_' || a.account_name_owner), 0.01)
FROM public.t_account a
         JOIN public.t_parameter p ON p.owner = a.owner
    AND p.parameter_name = 'rewards_6x_categories_' || a.account_name_owner;

INSERT INTO public.t_reward (account_id, owner, multiplier, category, cpp)
SELECT a.account_id,
       a.owner,
       3.0,
       unnest(string_to_array(p.parameter_value, ',')),
       COALESCE((SELECT parameter_value::NUMERIC
                 FROM public.t_parameter
                 WHERE owner = a.owner
                   AND parameter_name = 'rewards_cpp_' || a.account_name_owner), 0.01)
FROM public.t_account a
         JOIN public.t_parameter p ON p.owner = a.owner
    AND p.parameter_name = 'rewards_3x_categories_' || a.account_name_owner;

INSERT INTO public.t_reward (account_id, owner, multiplier, category, cpp)
SELECT a.account_id,
       a.owner,
       2.0,
       unnest(string_to_array(p.parameter_value, ',')),
       COALESCE((SELECT parameter_value::NUMERIC
                 FROM public.t_parameter
                 WHERE owner = a.owner
                   AND parameter_name = 'rewards_cpp_' || a.account_name_owner), 0.01)
FROM public.t_account a
         JOIN public.t_parameter p ON p.owner = a.owner
    AND p.parameter_name = 'rewards_2x_categories_' || a.account_name_owner;
