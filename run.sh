max=5
for WINSIZE in 1 2 4 8 16 32 64 128 256
do
	for (( i=0; i <$max; i++ ))
	do
	    java UDPClient 192.168.0.106 50090 512 $WINSIZE
	done
done