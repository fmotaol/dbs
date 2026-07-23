#target = localhost.conf

	DO $$
	BEGIN
	  FOR i IN 1..20 LOOP
		RAISE NOTICE 'Execução número: %', i;
		PERFORM pg_sleep(0.2); -- Pausa de 200ms entre as iterações
	  END LOOP;
	END $$;


