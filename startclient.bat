start "Client 1" gradlew client -Pargv="['client1', '200', '4000', '6000', '127.0.0.1', '1099', 'rm1']"
start "Client 2" gradlew client -Pargv="['client2', '200', '4000', '6000', '127.0.0.1', '1099', 'rm2']"
start "Client 3" gradlew client -Pargv="['client3', '200', '4000', '6000', '127.0.0.1', '1099', 'rm3']"
