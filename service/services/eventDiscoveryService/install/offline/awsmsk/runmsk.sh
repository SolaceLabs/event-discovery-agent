cp /Library/Java/JavaVirtualMachines/jdk-13.0.2+8/Contents/Home/lib/security/cacerts /tmp/kafka.client.truststore.jks
docker run -d -e "JAVA_OPTS=-Xms256m -Xmx1024m -Dapp.name=event-discovery-agent" \
  --name event-discovery \
  -p=8120:8120 \
  --volume=/tmp/kafka.client.truststore.jks:/tmp/kafka.client.truststore.jks:ro \
  --add-host=b-2.testcluster.fyrzmc.c4.kafka.us-east-1.amazonaws.com:172.24.0.3 \
  --add-host=b-3.testcluster.fyrzmc.c4.kafka.us-east-1.amazonaws.com:172.24.0.4 \
  --add-host=b-1.testcluster.fyrzmc.c4.kafka.us-east-1.amazonaws.com:172.24.0.2 \
  --network=cluster_default \
  solace/event-discovery:1.0
