#source = oper-prod.conf

	select  --'2026-6-26' as data_ini,
			--'2026-7-25' as data_fim,
			'@arg[data_ini]' as data_ini,
			'@arg[data_fim]' as data_fim,
			now()::date as hoje,
			(random() * 10000)::int as gen_id

#source = oper-prod.conf

	select sigla as ur,
		    case when sigla in ('UMC', 'UMS') then 'UMC-UMS'
		        else 'SSA'
			end as escopo
	from cad.unidade_regional
	where sigla ilike 'UM%'
	order by sigla

#source = oper-prod.conf

	select data
	from todos_os_dias
	where data between @data_ini and @data_fim
	order by data

#source = oper-prod.conf

	select 	ur,
			p.id as ponto_id,
			p.sigla as ident			
	from tlm.ponto p
	join ensitec.equipamento e on p.sigla = e.sigla
	where p.ur = @ur
	  --and p.sigla = any(select rows(''))
	order by ponto_id

	
#source = oper-prod.conf

	select * 
	from tlm.medicao_leituras
	where ponto_id = @ponto_id
	  and dia = @data
	  and var_id <> 'VA'
	order by dia, hora, var_id
	
#target = files

	$OUTPUT_FILE = med_ID_OA_@hoje_@gen_id.csv
	
	default import;

	