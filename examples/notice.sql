#target = oper.conf

	DO $$
	BEGIN
	  --SET client_min_messages = notice;
	  
	  FOR i IN 1..@arg[1] LOOP
		RAISE NOTICE 'Execução número: %', i;
		PERFORM pg_sleep(0.2); -- Pausa de 200ms entre as iterações
	  END LOOP;
	END $$;


