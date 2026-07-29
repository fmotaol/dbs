#source = oper.conf

	select now() as start_time



#source = oper.conf

	select 	active_connections,
			blocked_transactions,
			active_connections * 10 + blocked_transactions * 20 as sleep_time,
			1000 as queue_size
	from (
		SELECT
			(SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'active') AS active_connections,
			(SELECT COUNT(*) FROM pg_locks WHERE NOT granted) AS blocked_transactions
	) x
	
	$REPEAT_WHILE_SUB_FOUND=true


#source = oper.conf

	select *
	from syncdt.refresh_queue_ux
	where status = 'Pending'
	  and (now() - @start_time)::interval < '01:00:00'::interval --encerra após 1h, para evitar réplicas
	limit 1

#target = oper.conf

	select 	syncdt.update_changes(@queue_size)

#target = oper.conf

	select pg_sleep(@sleep_time) --(Aguarda @sleep_time segundos)	
