# Job runner

Utility to run defined jobs and controlling their lifecycle, satisfying hardware limits.

## Features

Job tree is described in JSON file, with machines definitions and groups possibly separated.

The following job features are supported:
- Job dependencies.
- Controlling memory usage, either absolutely or relatively.
- Controlling CPU usage, either absolutely or relatively.
- Run on one host within machine group or all hosts from group.

The following machine features are supported:
- Machine hardware limits - CPU and memory.
- Execution agents - local or ssh.
- 

Machines can be grouped into MachineGroups:
- Machine group defines list of machines that are supposed to be used for defined purpose.
- Jobs can be declared to run on particular machine group.

Execution agents supported:
- `local`: runs locally.
- `ssh`: runs remotely via SSH.
- `function`: runs internal Java function.

## Usage

```shell
$./target/job-runner -h
Usage: job-runner options... 
JobRunner - runs jobs required by definition file

Options:
-s,--spec              definition file of tasks
-t,--tasks             definition file of tasks
-m,--machines          definition file of machines
-g,--machine-groups    definition file of machines
```

## Example

Below is an example of process tree definition:
- `first`: base process
- `third`: runs on all machines in group
- `machines`: declare on local and one ssh machine

```json
{
  "tasks": {
    "first": {
      "command": [
        "sleep",
        2
      ],
      "memoryMinimum": 512,
      "cpuPortion": 0.6
    },
    "second": {
      "command": [
        "sleep",
        2
      ],
      "memoryMinimum": 512,
      "cpuPortion": 0.5
    },
    "third": {
      "command": [
        "sleep",
        1
      ],
      "memoryMinimum": 512,
      "cpuMinimum": 1,
      "runAllHosts": true
    },
    "group": {
      "command": [
        "sleep",
        3
      ],
      "memoryMinimum": 512,
      "cpuMinimum": 1,
      "dependencies": [
        "first",
        "second"
      ]
    },
    "last": {
      "command": [
        "sleep",
        1
      ],
      "dependencies": [
        "group",
        "third"
      ]
    },
    "end": {
      "command": [
        "echo",
        "hello"
      ],
      "dependencies": [
        "last"
      ]
    }
  },
  "machines": {
    "local1": {
      "agent": "local",
      "address": "localhost",
      "cpus": 2,
      "memory": 1024
    },
    "remote1": {
      "agent": "ssh",
      "address": "myname@localhost",
      "cpus": 4,
      "memory": 2048
    }
  },
  "machineGroups": {
    "basic": {
      "machines": [
        "local1",
        "remote1"
      ]
    }
  }
}
```

## Build

Maven and Java 11 are required at minimum:
```shell
mvn package
./target/job-runner -h
```

## About

Homepage: http://github.com/kvr000/zbynek-java-exp/tree/master/process-exp/job-runner/

Feel free to contact me at kvr000@gmail.com or http://kvr.znj.cz/software/java/ and http://github.com/kvr000

[//]: # ( vim: set tw=120: )
