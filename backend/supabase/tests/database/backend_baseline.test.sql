begin;
select plan(1);

select has_schema('public', 'public API schema exists');

select * from finish();
rollback;
