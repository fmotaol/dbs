$QUERY_UNDEFINED_ARGS=false


#source = localhost.conf

  select case when random() > 0.9 then true else false end as incluirsenha


#target = localhost.conf

  select id, nome, matricula,
	 login,
	 @if(@incluirsenha){password as senha,} 
         data_nascimento
  from usuario