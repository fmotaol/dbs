#source = testehost.conf

	select 'DEU ERRO!!!' as name1

#source = testehost.conf

	select $$@literal{where @name1 = 'JOÃO'}$$ as filter

  
#target

	
	$LABEL=CONTEÚDO DE FILTER: @filter