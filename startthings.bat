@echo off

start "Registry" gradlew registry -Pargv="['1099']"

PING localhost -n 4 >NUL

start "Grid Scheduler 1" gradlew gs -Pargv="['gs1', '127.0.0.1', '1099']"
PING localhost -n 4 >NUL
start "Grid Scheduler 2" gradlew gs -Pargv="['gs2', '127.0.0.1', '1099', 'gs1']"

PING localhost -n 4 >NUL
start "Frontend" gradlew frontend -Pargv="['frontend1', '127.0.0.1', '1099', 'gs1']"

PING localhost -n 4 >NUL
start "Resource Manager 1" gradlew rm -Pargv="['rm1', '10', '127.0.0.1', '1099', 'gs1']"
start "Resource Manager 1" gradlew rm -Pargv="['rm2', '10', '127.0.0.1', '1099', 'gs1']"
start "Resource Manager 1" gradlew rm -Pargv="['rm3', '10', '127.0.0.1', '1099', 'gs2']"

:loop
PAUSE

start "Client 1" gradlew client -Pargv="['client1', '5', '5000', '14000', '127.0.0.1','1099', 'rm1']"
start "Client 2" gradlew client -Pargv="['client2', '5', '5000', '14000', '127.0.0.1','1099', 'rm2']"
start "Client 3" gradlew client -Pargv="['client3', '5', '5000', '14000', '127.0.0.1','1099', 'rm3']"

GOTO loop