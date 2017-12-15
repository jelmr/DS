#!/usr/bin/python
import os
from os import system
import random
import subprocess
import pexpect

PORT  = 1339
NODES = 50

def possible_sets():
  print "The available datasets are:"
  correct_directories = []
  count = 0
  for dirname, dirnames, filenames in os.walk('datasets/'):
    dirname = dirname.split("/")[-1]
    if dirname == "":
        continue
    correct_directories.append(dirname)
    print "%d: %s " % (count, dirname)
    count += 1
  print ""
  return correct_directories


def select_set_number(amount_of_options):
  set_nr = raw_input("Select a dataset index as input[0]: ")
  if set_nr == "":
    return 0
  try:
    set_nr = int(set_nr)
  except:
    print "Incorrect input, not an integer"
    return -1
  if set_nr >= amount_of_options:
    print "Incorrect set index number"
    return -1
  return set_nr

def select_input():
  # Get and print possible inputs
  correct_directories = possible_sets()

  # Select a file
  set_nr = -1
  while set_nr < 0:
    set_nr = select_set_number(len(correct_directories))
  print "You selected input \"%s\"" % correct_directories[set_nr]

  # Open and Read correct gwf file
  input_set = open("datasets/" + correct_directories[set_nr] + "/anon_jobs.gwf", 'r')
  return input_set.read()

def parse_input(text):
  # Parse the input
  text = text.strip().split("\n")
  count = 0
  for line in text:
    if line[0] != '#':
      break
    count += 1
  text = text[count:]
  # - Select correct values
  command_list = []
  for line in text:
    line = line.split()
    if float(line[3]) < 0:
      line[3] = 0
    command_list.append("%16d %16d %16s" % (float(line[0]), float(line[3]), line[17]))
  return command_list

def format_commands(commands):
  scheduler_list = []
  replacement_list = []
  for command in commands:
    command = command.split()
    if not command[2] in scheduler_list:
      print "Found Scheduler %s" % command[2]
      scheduler_list.append(command[2])
      replacement_list.append("gs"+str(len(replacement_list)))
  print "//"+str(replacement_list)
  return [scheduler_list, replacement_list]

def create_rms(scheduler_list):
  rms = []
  for scheduler in scheduler_list:
    r = int(random.random() * 4) + 4
    current = []
    for i in range(r):
      current.append(scheduler + "-rm" + str(i))
    rms.append(current)
  rm_strings = []
  for rm_list in rms:
    string = ""
    for s in rm_list[:-1]:
      string += s +", "
    string += rm_list[-1]
    rm_strings.append(string)
    print "//"+string
  return rm_strings

def file_format(commands, lists, rms):
  new_commands = []
  for c in commands:
    c = c.split()
    index = lists[0].index(c[2])
    new_commands.append("%s, %s, %s" % (c[0], c[1], rms[index]))
  print "Creating input file"
  f = open("input.tim", 'w')
  for c in new_commands:
    f.write(c + '\n')
  f.close()
# .tim

print "Starting test conversion"
input_text = select_input()
commands = parse_input(input_text)
lists = format_commands(commands)
rms = create_rms(lists[1])
file_format(commands, lists, rms)
print "Converted dataset to tim format"
