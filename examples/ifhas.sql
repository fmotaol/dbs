


//$FORCE_ARG matricula=123

#source = oper.conf

  select * from funcionario
  where true
    @ifhas{and matricula = @arg[matricula]}@else{}
    @ifhasany{and data_admissao >= '@arg[adm_inicio]' and data_admissao <= '@arg[adm_final]'}@else{}
