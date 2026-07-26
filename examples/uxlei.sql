#macro

   	$ARG_DEFAULT_VALUE horas = 48

#source = oper.conf

	select 	now() - interval '2 hours' as data_limite_equip

	#source = oper.conf

		select updated as last_updated
		from ensitec.equipamento
		where updated <= @data_limite_equip		
		order by updated desc limit 1
	

		#source = oper.conf
			$LABEL = Atualizando equipamentos...
		
			select ensitec.refresh_equipamentos(ensitec.obter_token_embasa(), true)

		#\
	#\


#source = oper.conf


	SELECT 
		seq,
		NOW() - (interval '60 minutes' * seq) - (interval '29 minutes 59 seconds 999 milliseconds') AS dh_ini,
		NOW() - (interval '60 minutes' * seq) AS dh_fim
	from (select generate_series(0, @arg[horas]) AS seq)
	ORDER BY dh_ini DESC


#source = oper.conf
	

	select 	e.sigla,
			diahora,
			variavel,
			valor,
			3 as fonte,
			now() as agora,
			diahora_insercao
	from ensitec.buscar_leituras_geral(ensitec.obter_token_embasa(), @dh_ini, @dh_fim) m
	join ensitec.equipamento e on m.equipamento_id = e.id
	join tlm.ponto p on e.sigla = p.sigla
	left join lateral (
		select 	case when porta_nome ilike 'Press%o Instant%nea%' then 'P'
					 when porta_nome ilike 'Press%o Montante%' then 'P'
					 when porta_nome ilike 'Press%o Jusante%' then 'P2'
					 when porta_nome ilike 'Vaz%o Instant%nea%' then 'V'
					 when porta_nome ilike 'Totalizador%' then 'VA'
				end as variavel
	) v on true
	where variavel is not null
	  
	$DELAY_TIME_AFTER = 20000
	
	
#target = oper.conf

    $SHOW_SCENE=true
	
	select tlm.gravar_leitura(@diahora, @sigla, @variavel, @valor, @fonte, @agora, @diahora_insercao);
