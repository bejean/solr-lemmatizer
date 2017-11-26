Parse Dela French dictionnary

egrep '^[^[:space:],-]+,[^[:space:]\.-]+\.N' dela-fr-public.dic | grep -v 'NPropre' | sed -E "s/([^,]*),([^\.]*).*/\1\t\2\tnoun/"
egrep '^[^[:space:],-]+,[^[:space:]\.-]+\.V' dela-fr-public.dic | sed -E "s/([^,]*),([^\.]*).*/\1\t\2\tverb/"
egrep '^[^[:space:],-]+,[^[:space:]\.-]+\.DET\+' dela-fr-public.dic | sed -E "s/([^,]*),([^\.]*).*/\1\t\2\tdeterminant/"
egrep '^[^[:space:],-]+,[^[:space:]\.-]+\.A\+' dela-fr-public.dic | sed -E "s/([^,]*),([^\.]*).*/\1\t\2\tadjective/"


http://www.patenotte.name/Middlebury/Middlebury3201/Grammaire/EtiquettesGrammaticales.html
