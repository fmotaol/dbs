$ARG_DEFAULT_VALUE filter = true
$ARG_USE_DEFAULT_VALUE filter = true
$ARG_DEFAULT_VALUE deleteprevious = false
$ARG_USE_DEFAULT_VALUE deleteprevious = true

#init = @arg[target].conf

	select db_registra_inicio_proc(@dbsfile::text, @args::text) as pid


#final = @arg[target].conf

	select db_registra_fim_proc(@pid, @globalaffectedrows)

	
#error = @arg[target].conf

	select db_registra_erro_proc(@pid, @errormsg::text, @globalaffectedrows)
	
	
#source = @arg[source].conf

	$SHOW_SQL=false
	$LABEL=Localizando @arg[tabela]...

	select 	table_schema as esquema,
			table_name as tabela,
			pg_relation_size(table_schema||'.'||table_name) as table_size
	from information_schema.tables
	where (table_schema || '.' || table_name = '@arg[tabela]' or
		   table_schema = 'public' and table_name = '@arg[tabela]')

#ifnotfound

	error tabela desconhecida: @arg[tabela]
	
#source = @arg[source].conf

	$SHOW_SQL=false
	$LABEL=Levantando metadados de @esquema.@tabela...
	
select 	concat(column_name, ', ' order by ordinal_position) as fieldlist,
			concat(table_name || '.' || column_name, ', ' order by ordinal_position) as fieldlistc,
			concat(case when constraint_types ilike '%PRIMARY KEY%' then column_name end, ', ' order by pk_ordinal_position) as pkfs,
			concat(case when constraint_types ilike '%PRIMARY KEY%' then column_name||' desc' end, ', ' order by pk_ordinal_position) as pkfsdesc,
			concat(case when constraint_types ilike '%PRIMARY KEY%' then 'null as '|| column_name end, ', ' order by pk_ordinal_position) as nullpkfs
from (
	select  c.table_name, c.column_name, c.ordinal_position,
			concat(tco.constraint_type, ', ') as constraint_types,
			same(case when tco.constraint_type = 'PRIMARY KEY' then kcu.ordinal_position end) as pk_ordinal_position
	from information_schema.columns c
	left join information_schema.key_column_usage kcu 
	   on c.table_schema = kcu.table_schema and c.table_name = kcu.table_name and c.column_name = kcu.column_name
	left join information_schema.table_constraints tco on c.table_schema = tco.table_schema 
	      and c.table_name = tco.table_name and kcu.constraint_schema = tco.constraint_schema and kcu.constraint_name = tco.constraint_name 
	where c.table_schema = @esquema and c.table_name = @tabela
	group by c.table_name, c.column_name, c.ordinal_position
	order by c.ordinal_position		
) x

#source = @arg[source].conf

	$ALIAS = blockgen
	$SAVEPOINT = bloco

	#if = @tabela in (amf_historico_ocorrencia, amf_ocorrencia)
		
		select 	ref(seq)||' - '||mat_ini||' a '||mat_fim as bloco,
				' seqref = '||seq||' and matricula between '||mat_ini||' and '||mat_fim as blockfilter
		from view_ano_mes am, view_dof_segmat seg
		where existe_matricula
		  and exists (select seqref from @tabela::. t where t.seqref = am.seq limit 1)
		order by seq, segmat

	#if = @tabela in (dof_hidrometro)
		
		select 	id_ini||' a '||id_fim as bloco,
				--'id between '||id_ini||' and '||id_fim as blockfilter
				'hidrometro ilike chr(37)||chr(92)||chr(92)||chr(37) and id between '||id_ini||' and '||id_fim as blockfilter
		from view_dof_seghd seg
		where existe_hd

	#if = @tabela in (med_hidrometro_condicao)
		
		select 	id_ini||' a '||id_fim as bloco,
				'id_hidrometro between '||id_ini||' and '||id_fim as blockfilter
				--'hidrometro not ilike chr(37)||chr(92)||chr(92)||chr(37) and id_hidrometro between '||id_ini||' and '||id_fim as blockfilter
		from view_dof_seghd seg
		where existe_hd


	#if = @table_size < 102400
	
		select 	null as bloco,
				'true' as blockfilter

 
#source = @arg[source].conf

	$ALIAS = query

	select @fieldlist::. 
	from @tabela::.
	where @blockfilter::.
	  and @arg[filter]
	order by @pkfs::.
	

#onlylast = @arg[target].conf

	select db_registra_estado_proc(@pid, @blockfilter, @globalaffectedrows)

#beforefirst = @arg[target].conf

	$ALIAS=delete

	$SHOW_SQL=false
	$SHOW_STATUS=false
	$SHOW_SCENE=false
	$LABEL=excluindo @bloco
	
	delete from @tabela::.
	where @arg[deleteprevious]
	  and @blockfilter::.

#target = @arg[target].conf

	$ALIAS=upsert

	$SHOW_SQL=false
	$SHOW_STATUS=false
	$SHOW_SCENE=false
	$LABEL=@tabela registro @rowid: @pkfs::. => @@pkfs
	$BATCH_SIZE = 100
	
	insert into @tabela::. (@fieldlist::.) values (@*) 
	on conflict (@pkfs::.) do 
	update set @*=
	where (@fieldlistc::.) is distinct from (@*)
