#target = localhost.conf

  select * from funcionario
  where true
    @ifhas{and matricula = @arg[matricula]}
    @ifhasany{and data_admissao between '@arg[adm_inicio]' and '@arg[adm_final]'}
