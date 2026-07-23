#source = files

	properties "@arg[1]"


#source = files

	load "@arg[1]"
	
	$COLUMN_SEPARATOR=,

	
#source = oper.conf
	
	select 	unnest(pontos) as ponto, 
			unnest(variaveis) as variavel, 
			unnest(valores) as valor,
			1 as fonte
	--select tlm.gravar_leituras(@time, pontos, variaveis, valores, ' - ', @creation)
	from (
		select 	sp.pontos, sp.variaveis, c.valores
		from (select string_to_array(replace('@#fieldnames', 'Time, ', ''), ', ') as pontovars,
					 array[@*] as valores) a
			left join lateral (SELECT	array_agg(SPLIT_PART(pontovar, ' - ', 1)) as pontos,
										array_agg(SPLIT_PART(pontovar, ' - ', 2)) as variaveis
									FROM (select unnest(a.pontovars) as pontovar) sub
								) sp on true
			left join lateral (SELECT valores[2:]::float[] as valores) c on true
		) x
	
	
#target = oper.conf

	$LOG_SQL=true
    $SHOW_SCENE=false
    $SHOW_STATUS=false
	//$BATCH_SIZE=500
	
	select tlm.gravar_leitura(@time, @ponto, @variavel, @valor, @fonte, @creation);
#final = files

	move "@arg[1]" done
