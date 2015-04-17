# encoding: utf-8
require 'benchmark'

result = Benchmark.realtime do

	pattern = "00111000000" + "10100001000"

	command1 = "./start.sh #{pattern}"
	puts command1
	system command1
	
	command2 = "./deploy.sh #{pattern}"
	puts command2
	system command2

end
puts "実行時間: #{result}s"

