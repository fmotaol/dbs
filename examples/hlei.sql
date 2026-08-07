#source = oper.conf


	select id as var_id
	from tlm.variavel
	where ordem_tab_leitura <= 4
	order by ordem_tab_leitura


#source = oper.conf


	select distinct p.id as ponto_id,
					p.sigla as ponto,
					dia_impar::timestamp as diahora_ini,
					(dia_impar + interval '1 day 23 hours 59 minutes 59 seconds 999 milliseconds')::timestamp as diahora_fim,
					deficit_potencial
	from tlm.ponto p
	join ensitec.equipamento e on p.sigla = e.sigla
	left join tlm.resumo_leituras_dia_ponto_var r on r.ponto_id = p.id and r.var_id = @var_id
	join lateral (select CASE WHEN EXTRACT(DAY FROM r.dia) % 2 = 0 THEN r.dia - INTERVAL '1 day' 
							  ELSE r.dia end::date as dia_impar) d on true
	join lateral (select greatest(leituras_esperadas - leituras_realiz_totais, 0) as deficit_potencial) l on true
	where deficit_potencial > 0
		@ifhas{and r.dia >= @arg[inicio]}
		@ifhas{and r.dia <= @arg[final]}
	order by deficit_potencial desc

	

	
#source = oper.conf

	select 	e.sigla as ident,
			diahora,
			variavel,
			valor,
			3 as fonte,
			now() as agora,
			diahora_insercao
	from ensitec.buscar_leituras_equip(ensitec.obter_token_embasa(), @ponto, @diahora_ini, @diahora_fim) m	
	join ensitec.equipamento e on m.equipamento_id = e.id
	join tlm.ponto p on e.sigla = p.sigla
	left join lateral (
		select 	case when porta_nome ilike 'Press%o Instant%nea' then 'P'
					 when porta_nome ilike 'Press%o Montante' then 'P'
					 when porta_nome ilike 'Press%o Jusante' then 'P2'
					 when porta_nome ilike 'Vaz%o Instant%nea' then 'V'
					 when porta_nome ilike 'Totalizador' then 'VA'
				end as variavel
	) v on true
	where variavel is not null

	  
	$DELAY_TIME_AFTER = 5000

	
	
#target = oper.conf

	$SHOW_SCENE=true
	
	select tlm.gravar_leitura(@diahora, @ident, @variavel, @valor, @fonte, @agora, @diahora_insercao);


