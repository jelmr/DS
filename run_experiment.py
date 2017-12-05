import subprocess
import pexpect

RMI_PORT = 1098

RMS = [7,5,4,3,1]
RM_NUM_NODES = 50

# start registry
print("Starting registry")
registry = pexpect.spawn("gradle registry -Pargv=\"['%d']\"" % RMI_PORT)
registry.expect("75% EXECUTING")


# Start GS1
print("Starting gs1")
gs1 = pexpect.spawn("gradle gs -Pargv=\"['gs1', '127.0.0.1', '%s']\"" % RMI_PORT)
gs1.expect("75% EXECUTING")


# Start GS2 - GS5
other_gss = []
for n in range(2,6):
    name = "gs%d" % n
    print "Starting %s" % name
    other_gss.append(pexpect.spawn("gradle gs -Pargv=\"['%s', '127.0.0.1', '%s', 'gs1']\"" % (name, RMI_PORT)))


for gs in other_gss:
    gs.expect("75% EXECUTING")

print "All GSs running."


print "Starting Frontend"
frontend = pexpect.spawn("gradle frontend -Pargv=\"['frontend1', '127.0.0.1', '%s', 'gs1']\"" % (RMI_PORT,))
print "Frontend running."


print "Starting RMs"
all_rms = []
rm_num = 0
for gs_num, rm_count in enumerate(RMS):
    for n in range(1, rm_count+1):
        rm_num += 1

        rm_name = "rm%d" % rm_num
        gs_name = "gs%d" % (gs_num+1)

        print "Starting %s, connected to %s" % (rm_name, gs_name)
        all_rms.append(pexpect.spawn("gradle rm -Pargv=\"['%s', '%d', '127.0.0.1', '%s', '%s']\"" % (rm_name, RM_NUM_NODES, RMI_PORT, gs_name)))


for rm in all_rms:
    rm.expect("75% EXECUTING")

print "All RMs running"





