#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Runs a tim experiment(c)
"""
import pexpect
import time
import random

processes = []
log = open("tim.log", 'wb+')

def java_process(program, *args, cp='build/classes/java/main', package='distributed.systems.gridscheduler', waitfor=None):
    command = 'java -cp %s %s.%s %s' % (cp, package, program, ' '.join(map(str, args)))
    print(INFO + "%s" % command)
    jp = pexpect.spawn(command, logfile=log)
    
    if waitfor:
        jp.expect(waitfor)

    return jp


# Success colour
INFO    = "[\033[34m!\033[0m] "
SUCCESS = "[\033[32m+\033[0m] "
FAILURE = "[\033[31m+\033[0m] "

# Constants
RMI_PORT = 1337
NODES_PER_CLUSTER = 50

# Let's have one grid scheduler cluster for now (lighter to simulate)
RM_PER_GS = [1,1,1,1,1]

# Let's first run gradle to build this shit
print(INFO + "Building project")
builder = pexpect.spawn("gradle compileJava")
builder.expect("BUILD SUCCESSFUL")
print(SUCCESS + "Building complete")

# Let's run the registry
print(INFO + "Running registry")
registry = java_process('RegistryHost', RMI_PORT, waitfor='Started a registry on')
print(SUCCESS + "Registry up\n")

# Let's run the grid schedulers
active_gs = []
names_gs = []

print(INFO + "Running gridschedulers")
for i, count in enumerate(RM_PER_GS):
    gs_name = 'Gs%d' % i
    gs = java_process('RemoteGridSchedulerImpl', gs_name, 'localhost', RMI_PORT, *names_gs, 
        waitfor="GridScheduler '%s' has registered itself with the registry at localhost:%d." % (gs_name, RMI_PORT)
    )

    active_gs.append(gs)
    names_gs.append(gs_name)
    print(SUCCESS + "Grid scheduler %s is up" % gs_name)
print("")

# Let's run the resource managers
active_rm = []
names_rm = []

print(INFO + "Running resource managers")
for i, count in enumerate(RM_PER_GS):
    gs_name = 'Gs%d' % i

    for j in range(count):
        rm_name = '%sRm%d' % (gs_name, j)

        rm = java_process('RemoteResourceManagerImpl', rm_name, NODES_PER_CLUSTER, 'localhost', RMI_PORT, gs_name, *[g for g in names_gs if g != gs_name], 
            waitfor="ResourceManager '%s' has registered itself with the registry at localhost:%d." % (rm_name, RMI_PORT)
        )

        active_rm.append(rm)
        names_rm.append(rm_name)
        print(SUCCESS + "Resource manager %s is up" % rm_name)
print("")

###############################################################
# REPLACE ME WITH TIM CODE 
###############################################################

# Generate a tim script to run
print(INFO + "Running resource managers")
with open('test.tim', 'w+') as f:
    f.write("// Auto generated, do not edit\n\n")

    for i in range(100):
        f.write('%d, %d, %s\n' % (i, random.randint(0, 5), random.choice(names_rm)))


tester = java_process('MultiClient', 'test.tim', 'localhost', RMI_PORT)
tester.expect('Test complete')
