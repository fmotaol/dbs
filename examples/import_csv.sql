#source = files

	properties "@arg[1]"


#source = files

	load "@arg[1]"
	
	$COLUMN_SEPARATOR=,

	
#target = oper.conf


	default import;
	

#final = files

	move "@arg[1]" done
