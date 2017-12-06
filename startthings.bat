@echo off

ECHO starting registry
start "Registry" gradlew registry -Pargv="['1099']"
pause

ECHO starting gs1
start "Grid Scheduler 1" gradlew gs -Pargv="['gs1', '127.0.0.1', '1099']"
pause
ECHO starting gs2
start "Grid Scheduler 2" gradlew gs -Pargv="['gs2', '127.0.0.1', '1099', 'gs1']"
pause

ECHO starting Resource Manager 1
start "Resource Manager 1" gradlew rm -Pargv="['rm1', '10', '127.0.0.1', '1099', 'gs1']"
pause
ECHO starting Resource Managers 2 .....
start "Resource Manager 2" gradlew rm -Pargv="['rm2', '10', '127.0.0.1', '1099', 'gs1']"
pause
ECHO starting Resource Managers 3
start "Resource Manager 3" gradlew rm -Pargv="['rm3', '10', '127.0.0.1', '1099', 'gs2']"

pause
:loop

ECHO starting A few clients
start "Client 1" gradlew client -Pargv="['client1', '5', '5000', '14000', '127.0.0.1','1099', 'rm1']"
pause
start "Client 2" gradlew client -Pargv="['client2', '5', '5000', '14000', '127.0.0.1','1099', 'rm2']"
pause
start "Client 3" gradlew client -Pargv="['client3', '5', '5000', '14000', '127.0.0.1','1099', 'rm3']"
pause

GOTO loop