#macro = HID
  $SHOW_SCENE=false
  $SHOW_STATUS=false

#init = maya-local.conf

    select db_registra_inicio_proc(@dbsfile::text, @args::text) as pid

#final = maya-local.conf

    select db_registra_fim_proc(@pid, @globalaffectedrows)
    
#error = maya-local.conf

    select db_registra_erro_proc(@pid, @errormsg::text, @globalaffectedrows)
	
 #source = maya-local.conf
 
  select random(999999999) as proc_id,
         301 as tipo_proc

 #source = maya-local.conf
 
  select 'HIDTMATU IS NOT NULL' as filtro,
         '@$MULTIOP{ (HIDTMATU, HIDNNHID) > (@#updated, @#numerohidrometro) }' as partida;

#source = db2-SCI.conf

$SHOW_SQL=false

select 
    trim(HIDNNHID) AS NUMEROHIDROMETRO,
	HIDCDMCA AS MARCA,
	HIDCDTHI AS TIPO, --não gravar este campo.
	HIDCDVCA AS VAZAO,
	HIDCDDHI AS DIAMETRO,
	HIDANFABHID AS ANOFABRICACAO,
	HIDDTAQUHID AS DATAAQUISICAO,
	HIDDTULTREV AS DATAULTIMAREVISAO,
	HIDSITHID AS SITUACAO,
	HIDICMANUT AS INDICADORMANUTENCAO,
	HIDDTENVMAN AS DATAENVIOMANUTENCAO,
	HIDICPROPRIEDADE AS INDICADORPROPRIEDADE,
	HIDTMATU AS UPDATED,
	now() as REFRESHED
  from SCIADM.SCITHID
  where @filtro::native
      @query=maya-local.conf{select ' and ('||condicaopartida||')' from dof_processoatualizacao
                      where tipo = @tipo_proc and escopo = @filtro order by id desc limit 1}::native
      and hidtmatu >= '2017-09-30 13:16:34.689'
  order by HIDTMATU asc, NUMEROHIDROMETRO asc
  fetch first 20000 rows only

  //$FOUND_RECORDS_THRESHOLD = 10

  #iffound=10
    repeat;
	
  #ifnotfound
    stop;
	
 #target = maya-local.conf
  $BATCH_SIZE=1000
  $SHOW_SQL=false
  $LABEL=INSERT @#rowid: HIDNNHID=@NUMEROHIDROMETRO, HIDANFABHID=@ANOFABRICACAO, HIDCDVCA=@VAZAO

 insert into dof_hidrometro (
      hidrometro,
      vazao,
      marca,
      tipo,
      diametro,
      situacao,
      data_aquisicao,
      data_ultima_revisao,
      indicador_manutencao,
      data_envio_manutencao,
      indicador_propriedade,
      updated,
      ano_fabricacao_sci,
      refreshed
  ) values (
      trim(@numerohidrometro),
      /*coalesce(nullifempty(trim(@vazao)), null, null, @vazao)::int,*/
      case when (length(trim(@vazao)) = 0 or trim(@vazao) is null) then null 
      /* case when trim(@numerohidrometro) like 'Y%' then 1
           when trim(@numerohidrometro) like 'A%' then 2
           when trim(@numerohidrometro) like 'B%' then 3
           when trim(@numerohidrometro) like 'C%' then 4
           when trim(@numerohidrometro) like 'D%' then 5
           when trim(@numerohidrometro) like 'E%' then 6
           when trim(@numerohidrometro) like 'F%' then 7
      end ) */ else trim(@vazao) :: int end :: int, 
      @marca,
      @tipo,
      @diametro,
      @situacao::int,
      case when (to_char(@dataaquisicao::date, 'yyyy') = '9999') then null else (@dataaquisicao::date) end,
      case when (to_char(@dataultimarevisao::date, 'yyyy') = '9999') then null else (@dataultimarevisao::date) end,
      @indicadormanutencao,      
      case when (to_char(@dataenviomanutencao::date, 'yyyy') = '9999') then null else (@dataenviomanutencao::date) end,
      @indicadorpropriedade,
      @updated,
      case when (to_char(@anofabricacao, 'yyyy') = '9999') then null else (@anofabricacao) end, @refreshed
  ) on conflict (hidrometro) do update set 
		vazao= excluded.vazao,
		/* marca= excluded.marca,
		tipo= excluded.tipo, */
		diametro= excluded.diametro,
		situacao= excluded.situacao,
		data_aquisicao= excluded.data_aquisicao,
		data_ultima_revisao= excluded.data_ultima_revisao,
		indicador_manutencao= excluded.indicador_manutencao,
		data_envio_manutencao= excluded.data_envio_manutencao,
		indicador_propriedade= excluded.indicador_propriedade,
		updated= excluded.updated,
		ano_fabricacao_sci= excluded.ano_fabricacao_sci,
		refreshed = excluded.refreshed;
  
#target
  set updated = '@updated'

#target
  set numerohidrometro = '@numerohidrometro'

#target
  set row_id = @#rowid

#target
  set partida = @partida

#afterlast = maya-local.conf
$RECURSIVE_REFERENCE=true
insert into dof_processoatualizacao (tipo, escopo, updatedregistro, registros, condicaopartida, proc_id)
values (@tipo_proc, @filtro, @#updated, @#row_id, @#partida::text, @proc_id);