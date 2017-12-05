#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Eh dislike the interface :$
"""

from argparse import ArgumentParser
from os import system

PARSER = ArgumentParser(description="job launcher")
PARSER.add_argument('jobs', type=int, help='Number of jobs to launch')
PARSER.add_argument('rms', nargs='+', metavar="resource manager", help="resource managers")
PARSER.add_argument('-n', '--name', type=str, help="Client name", default='client1')
PARSER.add_argument('-l', '--min-duration', type=int, help="Number of seconds the job lasts minimum in seconds (default:1)", default=1)
PARSER.add_argument('-u', '--max-duration', type=int, help="Number of seconds the job lasts maximum in seconds (default:3)", default=3)
PARSER.add_argument('-r', '--registry', type=str, help='Registry location, default: localhost', default='localhost')
PARSER.add_argument('-p', '--registry-port', type=int, help='Registry port, default: 1099', default=1099)

ARGS = PARSER.parse_args()

# Convert to microseconds
ARGS.min_duration = int(ARGS.min_duration * 1000)
ARGS.max_duration = int(ARGS.max_duration * 1000)

GRADLE_STR = '''gradle client -Pargv="['{name}', '{jobs}', '{min_duration}', '{max_duration}', '{registry}', '{registry_port}', {rmlist}]"'''

system(GRADLE_STR.format(rmlist=', '.join("'%s'" % m for m in ARGS.rms), **ARGS.__dict__))


