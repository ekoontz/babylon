# t3a.xlarge | 4200 ms/parse
# 

echo "(load \"menard/nederlands\")(take 10 (repeatedly #(->> (-> \"een aardig bedroefd buitengewoon afzonderlijk dik nieuwswaardig ongerust teleurgesteld eigenwijs geheim verrast prachtig slim vrouw probeert mannen te zien\" menard.nederlands/parse time) (map menard.nederlands/syntax-tree) (map println))))" | lein repl
